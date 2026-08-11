# NineMo Backend — Explained in Plain English

> A non-jargon walkthrough of what the backend does, feature by feature, with a running
> example and a note on which service owns each piece. Technical reference:
> `Backend_Architecture.md`. Communication choices: `Communication_Patterns.md`.

**Meet Priya** — 28 years old, Bengaluru, 24 weeks pregnant with her first child. Every
feature below is explained through what happens when she uses the app.

---

## The Big Picture

The backend is not one program — it's **seven small programs (microservices)** that each
own one job and talk to each other. Think of it like a hospital: reception (gateway),
records room (health data), lab (AI parsing), the OB-GYN ward (clinical), the phone desk
(notifications), and the mothers' support group room (community).

| # | Service | One-line job | Port |
|---|---|---|---|
| 1 | `api-gateway` | Front door — checks ID, directs traffic, blocks abuse | 8080 |
| 2 | `identity-abha-service` | Who you are — login, government health ID, consent | 8081 |
| 3 | `health-data-service` | Your medical file cabinet — reports, records, files | 8082 |
| 4 | `ai-parsing-service` | The reader — turns photos of lab reports into data | 8083 |
| 5 | `ninemo-clinical-service` | The medical brain — pregnancy & child health logic | 8084 |
| 6 | `notification-service` | The messenger — WhatsApp, SMS, push alerts | 8085 |
| 7 | `ninemo-community-service` | The support group — clubs, chat, articles | 8086 |

**Golden rule:** the mobile app never does medical math. Every calculation — due dates,
risk checks, growth percentiles — happens here, on the backend. The app just displays
answers.

---

## Feature 1: Signing Up & Logging In
**Owner: `identity-abha-service`** (checked at the door by `api-gateway`)

Priya downloads the app and enters her phone number. The backend generates a 6-digit
OTP, remembers it for exactly **5 minutes** (in Redis, a fast short-term memory store),
and sends it to her phone. She types it in; the backend checks it matches, then
**deletes it immediately** so it can't be reused.

She then receives two "passes":
- an **access token** — like a day pass, valid 15 minutes, shown on every request
- a **refresh token** — like a membership card, valid 7 days, used to silently get new
  day passes so she isn't asked to log in constantly

When she logs out, her current day pass is put on a **blacklist** so even a stolen copy
stops working instantly.

*Example:* Priya's phone dies mid-session. She opens the app on her tablet 20 minutes
later — the old access token is expired, but the refresh token quietly fetches a new one.
She never sees a login screen.

---

## Feature 2: The Government Health ID (ABHA)
**Owner: `identity-abha-service`**

India's ABDM program gives every citizen a digital health ID (ABHA) so records can follow
the patient between hospitals. Priya links hers in the app:

1. She enters her mobile number → the backend asks the **government's ABDM gateway** to
   send her an OTP.
2. Anything sensitive we send them (Aadhaar digits, OTPs) is **encrypted with the
   government's public key first** (RSA) — it never travels as plain text.
3. ABDM replies **later, asynchronously** — like ordering at a counter and being called
   when the food's ready. We keep the "order slip" (transaction ID) in Redis for 5
   minutes so we recognise the callback when it arrives.
4. Health data ABDM sends back is encrypted with a different scheme (Curve25519); a
   government-provided library decrypts it.

*Example:* Priya's lab uploads her thyroid report to the ABDM network. Because her ABHA
is linked, the report flows into her NineMo locker automatically — she never carries the
paper.

---

## Feature 3: Doctor Consent — Priya Controls Her Data
**Owner: `identity-abha-service`** (enforced everywhere)

Priya's obstetrician, Dr. Rao, wants to see her records. Nothing is shared until Priya
taps "grant access", which creates a consent record **with an expiry date**. Two things
happen:

1. The grant is saved in the identity database.
2. An event — "consent granted" — is written to **Kafka**, a permanent, tamper-proof
   ledger. Consent is a *legal* fact, so we keep an audit trail that can be replayed
   years later if ever questioned.

Every doctor request for patient data is checked against an **active, unexpired**
consent. Priya can revoke at any time; Dr. Rao's access stops immediately.

---

## Feature 4: The Health Locker — Uploading a Lab Report
**Owners: `health-data-service` (storage) + `ai-parsing-service` (reading)**

Priya photographs her blood test report and uploads it. Behind the scenes:

1. **`health-data-service`** stores the photo in a private cloud vault (S3). Downloads
   happen via links that **self-destruct after 15 minutes**.
2. It drops a job — "new document to read" — onto **RabbitMQ**, a work queue. Queues are
   used for *tasks*: if the reader is busy or crashes, the job waits or retries
   automatically instead of being lost.
3. **`ai-parsing-service`** (the only Python service) picks up the job:
   - **OCR** (AWS Textract) turns the photo into text
   - **Medical NER** finds the values — "Haemoglobin: 10.8 g/dL"
   - Each value is tagged with its universal lab code (**LOINC** — e.g. haemoglobin is
     `718-7`) so "Hb", "Haemoglobin" and "Hemoglobin" all mean the same thing
   - The result is packaged in **FHIR**, the international medical data format
4. The parsed result is announced on **Kafka** — and *two* listeners react at once:
   `health-data-service` files the structured values into the locker, and
   `ninemo-clinical-service` checks them against pregnancy norms.
5. While all this runs, Priya's app holds open a live one-way channel (**SSE**) and shows
   "Processing…" → "Done" the moment parsing finishes — no refresh-button mashing.

*Example:* Priya's Hb comes back 10.8. The clinical service knows that at 24 weeks a mild
drop is often normal blood dilution — context a raw number can't give.

---

## Feature 5: The Pregnancy Timeline
**Owner: `ninemo-clinical-service`**

The app's home screen. From Priya's last period date the backend computed her **due date**
(LMP + 280 days — the standard Naegele's rule; IVF and ultrasound-based methods are also
supported) and from that, her current week — **week 24, trimester 2**.

Each week it serves: how big the baby is ("the size of a corn cob"), what body changes to
expect, this week's diet tips and safe yoga, and **auto-scheduled Indian medical
milestones** — her 20-week anomaly scan (done), the 24–28-week sugar test (upcoming), the
28-week TT injection. Reminders fire 7 days and 1 day before each.

---

## Feature 6: Symptom Checking (Triage)
**Owner: `ninemo-clinical-service`** → alerts via **`notification-service`**

Priya logs "headache" and "blurred vision", and her home BP reading of 148/94. The
symptom log runs through a **chain of medical rules**, each looking for a specific danger
in the context of *her* week:

| Rule | Fires when | Verdict |
|---|---|---|
| Preeclampsia | BP ≥ 140/90 + neurological symptom, after week 20 | **CRITICAL** |
| Anaemia | Hb < 11 after week 14 | WARNING |
| Gestational diabetes | Fasting glucose ≥ 92 during weeks 24–28 | WARNING |
| Premature labour | Labour signs before week 37 | CRITICAL |
| Reduced fetal movement | From week 28 | CRITICAL |

Priya's combination — high BP *plus* headache and blurred vision *at week 24* — trips the
preeclampsia rule: **CRITICAL**. A "risk detected" task goes onto RabbitMQ, and the
notification service immediately sends her a push alert and a WhatsApp message: *contact
your doctor now*. For harmless symptoms she'd instead get gentle home-care tips.

---

## Feature 7: Vitals, Kick Counter & Contraction Timer
**Owner: `ninemo-clinical-service`**

- **Weight & BP logs** are checked against normal ranges on every entry; her weight plots
  against a personalised gain curve.
- **Kick counter** (third trimester): Priya taps once per kick. WHO guidance says fewer
  than 10 kicks in 2 hours is concerning — if so, a CRITICAL alert fires.
- **Contraction timer**: logs each contraction's start and length; the backend computes
  frequency and duration. Contractions ≤ 5 minutes apart lasting ≥ 60 seconds is a
  labour pattern — and if that happens **before week 37**, it's flagged as possible
  premature labour.

The stopwatch runs on the phone; the *judgement* always runs on the backend.

---

## Feature 8: The Doctor's Flash Card
**Owner: `ninemo-clinical-service`**

Dr. Rao has 4 minutes per patient. One request assembles a single-screen snapshot —
current week, cumulative weight gain, latest symptoms with severity, latest vitals, last
kick-counter session — gathered from PostgreSQL and MongoDB in one go, so neither the app
nor the doctor stitches anything together.

---

## Feature 9: "Is It Safe?" — Indian Diet Lookup
**Owner: `ninemo-clinical-service`**

Priya's mother-in-law insists papaya is dangerous. Priya types "papaya" — the search is
**fuzzy** (typos like "papya" still match, via PostgreSQL trigram indexing) and returns
an evidence-based rating with trimester-specific notes, from a database of Indian
ingredients (Hindi names included).

---

## Feature 10: Baby Arrives — Child Mode
**Owner: `ninemo-clinical-service`**

When the delivery date is logged, one transition: the pregnancy timeline **locks**, a
child profile is created, and the app pivots to childcare:

- **Vaccination tracker** — the full Indian Academy of Pediatrics schedule (37 doses,
  BCG at birth through boosters) is generated from the birth date, each with status
  PENDING → COMPLETED (or OVERDUE), with reminders.
- **WHO growth charts** — each height/weight/head measurement is converted to a
  **Z-score** against WHO reference tables and a percentile. If the baby's curve falls
  across two major percentile lines, a paediatric red flag is raised.
- **Developmental milestones** — monthly checklists (smiles at 2 months, sits at 6,
  walks at 12–18…). If less than half a month's milestones are met, a
  developmental-delay-risk alert fires.

---

## Feature 11: Notifications — WhatsApp, SMS, Push
**Owner: `notification-service`**

The messenger never *decides* anything — it receives tasks from queues and delivers:

- **Channels:** WhatsApp & SMS (Twilio), push (Firebase/FCM).
- **Device registry:** each phone registers its push token at login (`POST
  /notifications/devices`), so alerts reach every device Priya owns — and stop when she
  logs out.
- **Delivery receipts:** Twilio calls us back ("delivered"), and the log for that message
  is updated — we know not just that we *sent* the preeclampsia alert, but that it
  *arrived*. Every notification attempt is recorded with its outcome.
- Failed sends retry automatically with increasing back-off (dead-letter queues).

---

## Feature 12: Due Date Clubs & Chat
**Owner: `ninemo-community-service`**

Priya is auto-placed in the **"November 2026 Moms"** club with others due the same month.
Inside are topic channels (General, Questions, Milestones). Chat is **live** over a
WebSocket — messages appear instantly, no refresh.

Safety: the connection itself demands a valid login token *before* it opens, and a
message's sender identity comes from that token — **not** from anything the app claims —
so nobody can impersonate another mother. Deleted messages are soft-hidden; expert
articles are served matched to her exact week.

---

## Feature 13: The Front Door
**Owner: `api-gateway`**

Every single request enters here first. The gateway:

1. **Checks the token** (signature, expiry, blacklist) — invalid = turned away with 401
2. **Stamps the request** with the verified user ID and role, so inner services trust
   the stamp instead of re-checking
3. **Rate-limits** (100 requests/second per client) to absorb abuse and bugs
4. **Routes** by path — `/ninemo/**` → clinical, `/health/**` → health data, etc.
5. If a service is down, a **circuit breaker** returns a graceful "try again shortly"
   instead of hanging

Public exceptions: login/OTP endpoints, ABDM callbacks, and vendor webhooks (Twilio) —
those verify themselves cryptographically instead.

---

## Behind the Scenes (applies to everything)

**Two kinds of messaging, never mixed:**
- **Kafka = the ledger.** Facts that happened and must be auditable/replayable — consent
  granted, ABDM data received, document parsed. Multiple services can read the same fact.
- **RabbitMQ = the to-do list.** Tasks to be done once, with retries and scheduled
  delays — parse this document, send this alert, remind in 7 days.

**One story ID per request:** the moment Priya's upload hits the gateway it's tagged with
a correlation ID that travels through Java, Python, Kafka, and RabbitMQ. One search in
the logs replays her document's entire journey across five services.

**Watching the watchers:** every service exports metrics (Prometheus → Grafana
dashboards) and distributed traces (Zipkin) — e.g. a counter of triage evaluations by
severity, so a spike in CRITICALs is visible instantly.

**Data ownership:** each service owns its tables/collections exclusively. Identity data
in PostgreSQL; flexible medical documents (FHIR, symptom logs, chat) in MongoDB; links
between them are plain UUIDs — no service ever reaches into another's database.

---

## Feature → Service Cheat Sheet

| You (or Priya) want to… | Handled by |
|---|---|
| Log in / stay logged in / log out | identity-abha |
| Link ABHA / receive govt records | identity-abha (+ health-data via Kafka) |
| Grant or revoke doctor access | identity-abha |
| Upload a report / fetch records | health-data |
| Turn a report photo into data | ai-parsing |
| See weekly timeline & milestones | ninemo-clinical |
| Log symptoms and get a verdict | ninemo-clinical → notification |
| Track BP, weight, kicks, contractions | ninemo-clinical |
| Doctor's one-screen summary | ninemo-clinical |
| Check if a food is safe | ninemo-clinical |
| Switch to child mode; vaccines, growth, milestones | ninemo-clinical |
| Get a WhatsApp/SMS/push alert | notification |
| Chat with your due-date club | ninemo-community |
| Read week-matched articles | ninemo-community |
| Any request at all, first stop | api-gateway |
