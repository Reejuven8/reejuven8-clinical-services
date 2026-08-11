# NineMo — UI Design Document (Central Reference)

> Single source of truth for all UI work. Derived from `NineMo_Functional_Requirement.txt`
> (5 pillars), the backend API surface (`Backend_Architecture.md`), and the mobile
> architecture rules (`docs/CLAUDE.md`, `Cross_Platform_Strategy.md`).
> Applies to both targets: Android (Compose) and iOS (SwiftUI) — same page inventory,
> platform-native rendering.
>
> **Governing rule (shapes every page):** the client renders backend answers. No page
> computes EDD, triage severity, Z-scores, schedules, or percentiles — pages display
> flags/values the API returns.

---

## 1. App Shell & Navigation Model

```
Splash → (not authenticated) → Auth stack: Login → Register → ABHA Link
       → (authenticated, no active profile) → Onboarding stack
       → (authenticated) → Main shell: 5 bottom tabs
┌────────┬────────┬────────┬───────────┬─────────┐
│  Home  │ Locker │ Tools  │ Community │ Profile │
└────────┴────────┴────────┴───────────┴─────────┘
```

- **Mode awareness:** the shell renders in *Pregnancy mode* or *Child mode* (post
  `transition-to-postnatal`). Same tabs, different content — Home shows gestational
  timeline vs child dashboard; Tools swaps kick counter/contractions for growth/vaccines.
- Route names = constants (`Routes.kt` / `routes.ts`); no inline strings.

---

## 2. Page Inventory (all pages, at a glance)

| # | Page | Nav location | Mode | Backend endpoints | Status |
|---|---|---|---|---|---|
| P0 | Splash / Session Gate | entry | both | token refresh | Screen exists (AppNavigator gate) |
| P1 | Login (OTP) | Auth stack | — | `auth/otp/send`, `auth/login` | ✅ built (RN) |
| P2 | Register | Auth stack | — | `auth/register` | ✅ built (RN) |
| P3 | ABHA Link | Auth stack / Profile | — | `abha/enroll/*` | ⬜ not built |
| P4 | Pregnancy Onboarding | post-auth, once | — | creates pregnancy profile | ⬜ not built (**gap — see §5**) |
| P5 | Home / Timeline | Tab 1 | pregnancy | `timeline/current`, `timeline/week/{n}` | ✅ built |
| P6 | Child Dashboard | Tab 1 | child | growth/vaccination/milestone summaries | ⬜ not built |
| P7 | Health Locker | Tab 2 | both | `health/records*`, `files/upload`, `files/events` (SSE) | ⬜ not built (**gap**) |
| P8 | Document Detail | from P7 | both | `records/{id}`, `files/download` | ⬜ not built |
| P9 | Consent Manager | Tab 2 (tab) / Profile | both | `consent/grant`, `revoke`, `list` | ⬜ not built (**gap**) |
| P10 | Tools Hub | Tab 3 | both | — (launcher) | ⬜ not built |
| P11 | Symptom Log | Tools | pregnancy | `symptoms` POST/GET | ✅ built |
| P12 | Vitals (Weight/BP) | Tools | both | `vitals` POST, `vitals/{type}` | ✅ built (2 screens) |
| P13 | Kick Counter | Tools | pregnancy T3 | `kick-counter/sessions*` | ✅ built |
| P14 | Contraction Timer | Tools | pregnancy T3 | `contractions/sessions*` | ✅ built |
| P15 | Diet "Is It Safe?" | Tools | pregnancy | `diet/search?q=` | ⬜ not built (backend ready) |
| P16 | Summary Card (Doctor Flash Card) | Tools / Home quick action | both | `summary-card/{patientId}` | ✅ built |
| P17 | Growth Chart | Tools (child) | child | `growth/children/{id}/measurements` | ✅ built |
| P18 | Vaccination Tracker | Tools (child) | child | `vaccinations/children/{id}/schedule`, `mark-completed` | ✅ built |
| P19 | Milestone Checklist | Tools (child) | child | `milestones/children/{id}*` | ⬜ not built (backend ready) |
| P20 | Due Date Club (list + chat) | Tab 4 | both | `community/clubs*`, WS STOMP | ✅ built (single screen; split below) |
| P21 | Content Feed | Tab 4 (tab) | both | `community/content*` | route exists, screen ⬜ |
| P22 | Profile & Settings | Tab 5 | both | profile, devices, logout | ⬜ not built |
| P23 | Mode Transition ("Baby is here!") | modal from P5 | pregnancy→child | `mode/transition-to-postnatal` | ⬜ not built |

Future (backend not ready — tables exist, no endpoints yet): Medication Pillbox,
Hospital Bag Checklist, Appointments (FRD Pillar 4). Listed in §6.

---

## 3. Page Responsibilities & Tabs

### P0 — Splash / Session Gate
**Responsibility:** decide where the user lands. Attempt silent token refresh; route to
Auth stack, Onboarding, or Main shell. Shows brand + loading only. No tabs.

### P1 — Login (OTP)
**Responsibility:** phone number entry → request OTP → 6-digit input → login. Handles
resend cooldown (server-driven), error display. Stores tokens securely on success.
No tabs. *Never displays or logs the OTP anywhere else.*

### P2 — Register
**Responsibility:** first-time account creation (name, phone, OTP verify). Role is
always PATIENT from this app. On success → P4 Onboarding. No tabs.

### P3 — ABHA Link
**Responsibility:** connect the government health ID. Two entry flows as steps (not tabs):
1. **Create/Link via mobile OTP** — `enroll/otp/generate` → `verify` → set ABHA address
2. **Scan QR** — placeholder (backend Phase-2 stub, show "coming soon")
Progress indicator per ABDM's async steps; clear success state ("Records will now sync
automatically"). Skippable — app works without ABHA.

### P4 — Pregnancy Onboarding (wizard, 3 steps — sequential, not tabs)
**Responsibility:** capture what the clinical engine needs, once.
| Step | Collects | Notes |
|---|---|---|
| 1. Dates | LMP **or** ultrasound date **or** IVF transfer date | Backend computes EDD — the page never shows a locally calculated date |
| 2. Body metrics | age, height, pre-pregnancy weight, blood group | BMI computed server-side |
| 3. Risk flags | PCOS, hypothyroid, T2 diabetes, etc. (multi-select) | Adjusts triage thresholds server-side |
Ends with "Your due date: {server EDD}" confirmation → Main shell.

### P5 — Home / Timeline (Tab 1, pregnancy mode)
**Responsibility:** the daily-open screen. Week-centric feed with horizontal week pager
(weeks 1–42, current highlighted; past/future browsable via `timeline/week/{n}`).

**Tabs inside P5:**
| Tab | Responsibility |
|---|---|
| **This Week** | Baby size comparison card, weight/length, development highlights, expected maternal changes |
| **Milestones** | Auto-scheduled scans/tests (NT scan, anomaly scan, GTT, TT) with due dates + done/upcoming states; reminder chips (7-day/1-day) |
| **Diet & Wellness** | Week's diet tips (veg/non-veg toggle), avoid-list, yoga routine; deep-link to P15 diet search |

Persistent elements: severity banner if latest symptom log returned WARNING/CRITICAL;
quick actions row (Log symptom, Log vitals, Summary card).

### P6 — Child Dashboard (Tab 1, child mode)
**Responsibility:** replaces P5 after delivery. Child's age ("11 weeks old"), next
vaccination due, latest growth percentile, this month's milestone progress.

**Tabs inside P6:**
| Tab | Responsibility |
|---|---|
| **Today** | Age, next vaccine card, alerts (growth red flags, overdue vaccines) |
| **This Month** | Milestone checklist preview (deep-link P19), month-matched articles |

### P7 — Health Locker (Tab 2)
**Responsibility:** every medical record in one place — ABHA-synced and self-uploaded.

**Tabs inside P7:**
| Tab | Responsibility |
|---|---|
| **Records** | Paged list of FHIR records, filter chips by type (Ultrasound, Prescription, Lab, …) and tag (Thyroid, Diabetes); tap → P8 |
| **Upload** | Camera/file picker → multipart upload → live parse status via SSE (`PROCESSING → PARSED`) with extracted-values preview when done |
| **Trends** | Extracted vitals plotted over the journey (Hb, glucose, TSH) — values from parsed records; chart only, no evaluation on client |
| **Consents** | Embed of P9 (doctor access list) |

### P8 — Document Detail
**Responsibility:** one record: original file (15-min presigned URL, re-fetched on
expiry), extracted observations with LOINC names, source metadata, contextual insight
text from backend. No tabs.

### P9 — Consent Manager
**Responsibility:** Pillar of data control. List of doctors with access (status,
granted/expiry dates), grant flow (doctor ID + duration), one-tap revoke with
confirmation. States: GRANTED / EXPIRED / REVOKED. No tabs (single list + actions).

### P10 — Tools Hub (Tab 3)
**Responsibility:** launcher grid for P11–P19. Mode-aware: pregnancy shows symptom/
vitals/kick/contraction/diet/summary; child shows growth/vaccination/milestones/vitals.
Badges (e.g. kick counter "due today" in T3). No tabs.

### P11 — Symptom Log
**Responsibility:** daily symptom capture + triage result display.
- Categorised symptom picker (Nausea, Spotting, Cramping, Swelling, …), severity slider,
  optional vitals attach
- Submits → renders the **server verdict**: NORMAL → home-care tips; WARNING/CRITICAL →
  alert card with "call your doctor" CTA
- History list with severity color coding
No tabs (picker + history in one scroll; history could become a tab if it grows).

### P12 — Vitals
**Responsibility:** manual entry + trend view for body measurements.
**Tabs inside P12:**
| Tab | Responsibility |
|---|---|
| **Weight** | Entry field, chart of logs *vs personalised target curve* (curve from backend) |
| **Blood Pressure** | Sys/dia entry, history chart, red banner when a log returns `alertTriggered=true` |
Future tabs: Sugar, SpO2 (backend ranges already exist).

### P13 — Kick Counter
**Responsibility:** T3 fetal-movement tool. Big tap target, session timer (UI-only
stopwatch), kick count, history of sessions with time-to-10-kicks. If backend flags a
session concerning → CRITICAL card. No tabs.

### P14 — Contraction Timer
**Responsibility:** start/stop each contraction; list with duration + interval
(computed server-side); labor-pattern banner when `isLaborPattern=true`; premature-labor
alert styling when flagged before week 37. No tabs.

### P15 — Diet "Is It Safe?"
**Responsibility:** search Indian ingredients (fuzzy, Hindi names supported) → safety
rating card (SAFE / CAUTION / AVOID) with trimester notes. Recent searches. No tabs.

### P16 — Summary Card (Doctor Flash Card)
**Responsibility:** the "hand your phone to the doctor" screen. Single dense scroll:
week + EDD, cumulative weight gain, recent abnormal flags, meds, latest vitals, last
kick session. Share/PDF export later. Optimised for readability at arm's length. No tabs.

### P17 — Growth Chart (child)
**Responsibility:** record measurements; render WHO curves with the child's dots.
**Tabs inside P17:**
| Tab | Responsibility |
|---|---|
| **Weight** | WHO weight-for-age curve + child's points + percentile labels |
| **Height** | Same for height/length |
| **Head** | Head circumference |
Entry FAB on all tabs → one form (all three measurements). Red-flag banner when backend
reports percentile-line crossings. Z-scores/percentiles come from the API, never computed
in the chart.

### P18 — Vaccination Tracker (child)
**Responsibility:** IAP schedule as a life checklist.
**Tabs inside P18:**
| Tab | Responsibility |
|---|---|
| **Upcoming** | Due/overdue doses sorted by date; OVERDUE styled urgent; mark-completed flow (date + administered-by) |
| **Completed** | Administered history; future: certificate photo attach |

### P19 — Milestone Checklist (child)
**Responsibility:** month check-ins (2/4/6/9/12/18/24/36/48/60). Month selector →
checklist of milestones (tap to toggle achieved) grouped by category
(motor/cognitive/social); delay-risk banner when backend flags <50% achieved. History
of past months. No tabs (month chips act as the selector).

### P20 — Community: Due Date Club (Tab 4)
**Responsibility:** anonymous peer support.
Split into two screens:
- **Club Home:** "November 2026 Moms" header, member count, channel list (General,
  Questions, Milestones, + topical)
- **Channel Chat:** live STOMP chat; paged history on scroll-up; reply-to; long-press
  delete (own messages only); alias display (never real names)

**Tabs inside Tab 4 (Community):**
| Tab | Responsibility |
|---|---|
| **My Club** | Club home + chat entry |
| **Feed** | P21 Content Feed |

### P21 — Content Feed
**Responsibility:** verified articles/videos filtered to the user's exact week (or
child's month). Category chips (Nutrition, Mental Health, Preparation…). Article reader
view. No further tabs.

### P22 — Profile & Settings (Tab 5)
**Responsibility:** account + app management.
Sections (list, not tabs): personal details; ABHA status (link → P3); consent manager
(→ P9); notification preferences; devices (registered push tokens, remove on demand);
mode & family (pregnancy profile, children); logout (clears tokens + unregisters device
token); legal/privacy.

### P23 — Mode Transition ("Baby is here!")
**Responsibility:** celebratory modal flow: delivery date + type + baby's name/sex/birth
weight → calls transition endpoint → explains what changes ("timeline locked, Child Mode
on, vaccination schedule created") → lands on P6. One-way door: confirmation required.

---

## 4. Cross-Page UI Rules

- **Severity language:** NORMAL = neutral, WARNING = amber, CRITICAL = red + haptic +
  persistent until acknowledged. Identical semantics on every page (symptoms, vitals,
  kicks, contractions, growth).
- **Loading/error/empty:** every data page renders exactly `Loading | Error(retry) |
  Empty | Content` from its ViewModel `UiState` — no bespoke spinners.
- **Offline:** online-first; show cached-if-available + "reconnecting" banner. No offline
  editing.
- **Sensitive screens** (Locker, Summary Card, Consent): `FLAG_SECURE` / screenshot
  guard; no PHI in notifications previews beyond generic text.
- **Charts:** Vico (Android) / Swift Charts (iOS); plot server values only.
- **Accessibility:** all severity states also encoded via icon+text (not color alone);
  dynamic type support; Hindi/English content fields already provided by backend where
  applicable (diet).

---

## 5. Gaps This Document Surfaces (UI side)

| Gap | Impact | Suggested ticket |
|---|---|---|
| P4 Onboarding not built — but *every* clinical feature depends on an active pregnancy profile; there is also **no backend endpoint to create a pregnancy profile** (integration test inserted via repository) | Blocks first-run UX end-to-end | Backend: `POST /ninemo/profiles/pregnancy` + UI P4 (candidate NM-B-167) |
| P7 Locker, P9 Consent, P3 ABHA link screens missing | Pillar 2 unusable from the app | UI build items |
| P15 Diet + P19 Milestones screens missing | Backend features invisible to users | UI build items |
| P6 Child Dashboard + P23 transition flow missing | Child mode unreachable from UI | UI build items |

---

## 6. Future Pages (blocked on backend endpoints)

| Page | FRD ref | Backend state |
|---|---|---|
| Medication Pillbox (nagger + refill) | Pillar 4 | `medication_schedules` table exists; no controller |
| Hospital Bag Checklist | Pillar 4 | `hospital_bag_items` table exists; no controller |
| Appointments + document-prep reminders | Pillar 4 | `appointments` table exists; no controller |

---

## 7. Can We Use Claude for the Design Work?

Yes — with an honest scope of what that means. There's no standalone "Claude Design"
product; the workflow is:

1. **Text specs (this document)** — Claude maintains it as the single source of truth;
   every new screen gets added here first.
2. **Interactive HTML mockups via Artifacts** — Claude can generate clickable,
   theme-aware wireframes/mockups of any page above (e.g. the Timeline with its three
   tabs, or the severity banner states) as self-contained web pages you can open, click
   through, and share with the team. Good for layout decisions *before* writing Compose/
   SwiftUI. Ask e.g.: *"mock up P5 Timeline as an artifact."*
3. **Design audits** — the installed `/design-is` skill (claude-mem plugin) audits a
   design against Dieter Rams' 10 principles and hands back a concrete improvement plan;
   useful once mockups exist.
4. **Straight to code** — for this project the mockup step can often be skipped: the
   layer mapping in `Cross_Platform_Strategy.md` means Claude can generate the actual
   `TimelineScreen.kt` Composable / SwiftUI view from this spec directly.

Limits: Claude produces mockups and code, not Figma files; visual polish beyond
HTML/Compose fidelity (brand illustration, iconography) still needs a designer or a
design-system kit (Material 3 defaults get us far).

**Suggested next step:** pick 1–2 high-traffic pages (P5 Timeline, P11 Symptom Log) and
have Claude produce artifact mockups of each state (loading/normal/CRITICAL) for review.
