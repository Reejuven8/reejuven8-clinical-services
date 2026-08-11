# NineMo — Backend Communication Patterns

> Survey of inter-service and client↔backend communication methods, pros/cons of each,
> and the recommended (and currently implemented) choice per NineMo service/endpoint.
> Companion to `Backend_Architecture.md` §6 (event bus) and `System_Design.md` §3 (EDA).

---

## 1. What NineMo Uses Today (baseline)

| Method | Where used now |
|---|---|
| REST (JSON over HTTP/1.1) | All client→gateway and gateway→service calls |
| Kafka (pub/sub log) | `abdm.consent.granted`, `abdm.data.received`, `document.data.parsed` |
| RabbitMQ (work queues + DLX) | `document.unstructured.uploaded`, `clinical.risk.detected`, `patient.milestone.due` |
| WebSocket + STOMP | Due Date Club live chat (`/ws/connect`) |
| Webhooks (inbound) | ABDM async callbacks → `CallbackController` |

Everything below evaluates whether these remain correct and where alternatives fit.

---

## 2. The Methods

### 2.1 REST (HTTP + JSON)

Request/response over HTTP verbs; resource-oriented; the default of the ecosystem.

| Pros | Cons |
|---|---|
| Universal — every client (RN, Kotlin, Swift, curl, ABDM) speaks it | Verbose JSON payloads; no binary efficiency |
| Human-readable; trivially debuggable (Postman, browser) | No streaming (one request → one response) |
| Mature middleware: gateway routing, rate limiting, JWT filters all HTTP-native | Over-/under-fetching — endpoint returns fixed shape |
| Caching semantics built into HTTP (ETag, Cache-Control) | N+1 round-trips if client needs composed data |
| OpenAPI/Swagger toolchain (already wired via springdoc) | Contract drift possible without codegen discipline |

**Best for:** public/client-facing APIs, CRUD, anything a third party or mobile app calls.

### 2.2 gRPC (HTTP/2 + Protobuf)

Binary RPC with code-generated stubs from `.proto` contracts; unary + streaming modes.

| Pros | Cons |
|---|---|
| 5–10× smaller payloads, HTTP/2 multiplexing — lowest latency of the list | Not browser/mobile-friendly without gRPC-Web proxy layer |
| Contract-first: `.proto` is compile-time enforced on both sides | Poor human debuggability (binary; needs grpcurl/reflection) |
| Bidirectional streaming built in | Spring Cloud Gateway, rate limiting, JWT filters need extra work vs HTTP |
| Deadlines/cancellation propagate across hops | ABDM and external partners don't speak it |
| Polyglot codegen (Java ↔ Python ideal for ai-parsing-service) | Another toolchain (protoc, plugins) in CI |

**Best for:** internal service-to-service *synchronous* calls on hot paths; polyglot boundaries.

### 2.3 Kafka (distributed log, pub/sub)

Append-only partitioned log; consumers track offsets; events retained and replayable.

| Pros | Cons |
|---|---|
| Immutable, replayable history — audit trail for free (legal requirement for consent) | No per-message routing/priority; consumer gets the whole partition |
| Multiple independent consumer groups read the same event | No native delayed delivery or per-message TTL |
| Massive throughput; partition-keyed ordering (per patient) | Operationally heavy (brokers, partitions, rebalancing) |
| Decouples producer lifetime from consumer availability | At-least-once by default — consumers must be idempotent |
| Backpressure-safe: slow consumer never loses data | Poor fit for "do this task once, retry with backoff" semantics |

**Best for:** facts that happened — audit streams, fan-out to N consumers, replayable state.

### 2.4 RabbitMQ (message broker, work queues)

Smart broker routing to queues; competing consumers; acks, DLX, delayed exchange.

| Pros | Cons |
|---|---|
| Competing-consumers: N workers share a queue, each task done once | Message gone after ack — no replay, no audit history |
| Dead-letter exchanges → automatic retry with backoff | Lower throughput ceiling than Kafka |
| Delayed/scheduled delivery (milestone reminders) | Routing topology (exchanges/bindings) adds config complexity |
| Per-message ack/nack/requeue — fine-grained failure control | Ordering not guaranteed across consumers |
| Lightweight ops vs Kafka | Fan-out to many independent consumer *groups* is awkward |

**Best for:** tasks to be executed — CPU-heavy jobs, notification dispatch, anything needing retry/DLX or delay.

### 2.5 WebSocket (+ STOMP)

Persistent full-duplex TCP connection; STOMP adds pub/sub frames on top.

| Pros | Cons |
|---|---|
| True bidirectional real-time (chat, live presence) | Stateful connections — horizontal scaling needs a broker relay (Redis/RabbitMQ STOMP relay) |
| Server push without polling | Load balancers/proxies need sticky or WS-aware config |
| STOMP gives topics/subscriptions over one socket | Auth is DIY (validate JWT on CONNECT — currently a known gap) |
| Low per-message overhead after handshake | Reconnect/backfill logic falls on the client |

**Best for:** chat, collaborative/live features. Overkill for one-way notification.

### 2.6 Server-Sent Events (SSE)

One-way server→client stream over plain HTTP.

| Pros | Cons |
|---|---|
| Dead simple; plain HTTP — gateway/JWT/rate-limit friendly | One-way only (client still POSTs via REST) |
| Auto-reconnect + Last-Event-ID built into the protocol | HTTP/1.1 connection-per-stream limits (fine on HTTP/2) |
| No special client library needed | Text-only frames; no binary |

**Best for:** live one-way feeds — e.g. streaming OCR parse progress to the app. Not currently used; candidate below.

### 2.7 Webhooks (inbound HTTP callbacks)

External system POSTs to your endpoint when something happens.

| Pros | Cons |
|---|---|
| Only option when the external party dictates the contract (ABDM does) | You must be publicly reachable + verify authenticity |
| Push not poll — immediate | Delivery guarantees are the sender's whim; need idempotent handlers |
| Simple HTTP | Async correlation needed (hence Redis txnId cache, 5-min TTL) |

**Best for:** third-party integration you don't control: ABDM, Twilio delivery receipts, payment gateways.

### 2.8 GraphQL

Single endpoint; client declares the exact shape it wants.

| Pros | Cons |
|---|---|
| Solves over/under-fetch; one round-trip for composed views | Server complexity: resolvers, N+1 dataloaders, query cost limiting |
| Strongly typed schema + introspection | Caching/rate limiting harder than per-route REST |
| Great for aggregate screens (Summary Card is the poster child) | Another runtime + security surface (query depth attacks) |
|  | Thin-client benefit is small: backend already aggregates (SummaryCardService) |

**Best for:** many-clients/many-shapes products. **Not recommended for NineMo** — our rule is "backend aggregates, client renders", which removes GraphQL's main selling point.

---

## 3. Decision Matrix

| Dimension | REST | gRPC | Kafka | RabbitMQ | WS/STOMP | SSE | Webhook |
|---|---|---|---|---|---|---|---|
| Direction | req/resp | req/resp + stream | async pub/sub | async queue | full duplex | server→client | inbound push |
| Coupling | temporal (both up) | temporal | decoupled | decoupled | session | session | decoupled |
| Replayable | ✗ | ✗ | ✔ | ✗ | ✗ | partial (Last-Event-ID) | ✗ |
| Delivery | n/a | n/a | at-least-once | at-least-once + DLX | best effort | best effort | sender-defined |
| Mobile-client friendly | ✔✔ | ✗ (needs proxy) | ✗ | ✗ | ✔ | ✔ | n/a |
| Audit trail | ✗ | ✗ | ✔✔ | ✗ | ✗ | ✗ | ✗ |
| Retry/backoff built-in | ✗ | ✗ | consumer-side | ✔✔ (DLX) | ✗ | reconnect only | ✗ |
| Delayed delivery | ✗ | ✗ | ✗ | ✔ | ✗ | ✗ | ✗ |
| Ops weight | low | low-med | high | med | med | low | low |

**Selection heuristic (NineMo house rules):**

1. Client (mobile/web/third-party) calls us → **REST** through the gateway. Always.
2. A *fact* happened that ≥1 service must react to, or law/compliance needs history → **Kafka**.
3. A *task* must be executed exactly-ish once, with retry/backoff or a delay → **RabbitMQ**.
4. Two humans (or human+server) interact live in both directions → **WebSocket/STOMP**.
5. Server streams one-way progress/updates to a single client → **SSE**.
6. An external party we don't control pushes to us → **Webhook** (+ idempotency + Redis txn correlation).
7. Internal sync call is hot-path, high-QPS, or polyglot → consider **gRPC**; otherwise plain REST is fine.

---

## 4. Recommendation per Service / Endpoint

### 4.1 Client ↔ Backend (through api-gateway)

| Surface | Method | Rationale |
|---|---|---|
| All auth/ABHA/consent endpoints | REST | Public paths, third-party-auditable, JWT filter chain is HTTP |
| All clinical CRUD (timeline, symptoms, vitals, kick, contraction, growth, vaccination, milestones, diet) | REST | Simple request/response; envelope `ApiResponse<T>` |
| Summary Card | REST (keep) | Backend aggregates — no GraphQL needed |
| File upload | REST multipart → S3 + presigned URLs | Already correct; never proxy file bytes through services |
| Due Date Club chat | WS/STOMP `/ws/connect` (keep) + REST for history/pagination | Live bidirectional; history stays REST |
| Clinical risk alerts → device | FCM push (via notification-service) — *not* a socket | Device may be offline; OS-level delivery |
| Document parse progress (upload → OCR → parsed) | **SSE (proposed addition)** `GET /api/v1/health/upload/{fileId}/events` | Today client polls `/status`; SSE removes polling with zero client-lib cost |

### 4.2 Service ↔ Service — Asynchronous (keep exactly as designed)

| Link | Method | Why this and not the other broker |
|---|---|---|
| identity → health-data: `abdm.consent.granted` | **Kafka** | Consent is a legal state change — immutable replayable audit trail |
| identity → health-data: `abdm.data.received` | **Kafka** | High-throughput FHIR payload fan-in; replay on consumer bugfix |
| ai-parsing → clinical + health-data: `document.data.parsed` | **Kafka** | Two independent consumer groups read the same event — Kafka's exact use case |
| health-data → ai-parsing: `document.unstructured.uploaded` | **RabbitMQ** | CPU-heavy OCR task; competing consumers scale workers; DLX retry on Textract failure |
| clinical → notification: `clinical.risk.detected` | **RabbitMQ** | Task with external-API failure modes (Twilio/FCM) → DLX + backoff |
| clinical → notification: `patient.milestone.due` | **RabbitMQ** | Needs *delayed delivery* (remind 7d/1d before) — Kafka can't schedule |

> **Rule (from System_Design.md, restated):** Kafka = facts/audit. RabbitMQ = tasks/retry/delay.
> Never swap. A consent event in RabbitMQ loses its legal audit trail; an OCR job in Kafka
> loses DLX retry and competing-consumer semantics.

### 4.3 Service ↔ Service — Synchronous

Today there are almost no sync service-to-service calls (by design — data ownership +
events). Where they exist or may appear:

| Call | Today | Recommendation |
|---|---|---|
| gateway → all services | REST proxy | Keep — gateway filters are HTTP-native |
| clinical → identity (validate consent for doctor reads) | none (trusts gateway headers) | If added: **gRPC** unary `ConsentCheck` — hot path on every doctor read, tiny payload, internal-only |
| health-data → ai-parsing manual re-parse trigger | REST `POST /api/v1/parse/document` | Keep REST — low QPS, debuggability wins |
| Java ↔ Python boundary (future: bulk NER scoring, embeddings) | — | **gRPC** — polyglot codegen from one `.proto`, streaming for batches |
| notification → Twilio/FCM/Gupshup | vendor REST SDKs | No choice — vendor-defined |

**Verdict on gRPC adoption:** not now. Current sync mesh is gateway→service only, where
HTTP filters (JWT, rate limit, correlation ID) do heavy lifting. Introduce gRPC only when
a measured hot internal path appears (consent checks at doctor-scale, or bulk AI calls).
Adding it today buys latency nobody is waiting on and costs a second toolchain.

### 4.4 External Parties

| Party | Direction | Method |
|---|---|---|
| ABDM gateway | outbound | REST + RSA-OAEP encrypted payloads (mandated) |
| ABDM gateway | inbound | Webhook → `CallbackController`; txnId correlated via Redis (5-min TTL); handlers idempotent |
| Twilio delivery receipts | inbound | Webhook (add `POST /api/v1/notifications/callbacks/twilio` when delivery tracking needed) |
| AWS S3 | outbound | SDK (REST under the hood); presigned URLs 15-min |

---

## 5. Gaps / Proposed Changes Summary

| # | Change | Method | Effort |
|---|---|---|---|
| 1 | Parse-progress stream `GET /health/upload/{fileId}/events` | SSE (Spring MVC `SseEmitter`, fed by `document.data.parsed` consumer) | S |
| 2 | JWT validation on STOMP CONNECT (known gap from Cross_Platform doc §risk) | WS ChannelInterceptor | S |
| 3 | Twilio delivery-receipt webhook + `NotificationLog.status → DELIVERED` | Webhook | S |
| 4 | gRPC for consent-check / Java↔Python bulk calls | gRPC | Deferred — revisit at scale |
| 5 | GraphQL | — | Rejected — backend-aggregation rule removes its value |

---

## 6. Quick Reference Card

```
Who's talking?                          → Use
──────────────────────────────────────────────────────
Mobile/web client → backend             → REST (gateway)
Client needs live 2-way (chat)          → WebSocket/STOMP
Client needs live 1-way (progress)      → SSE
Backend → offline-capable device        → FCM/APNs push
Fact happened, others react / audit     → Kafka
Task to run once w/ retry or delay      → RabbitMQ
Internal hot-path sync / polyglot RPC   → gRPC (when measured need)
External party pushes to us             → Webhook (idempotent + correlated)
Composed read views                     → REST w/ backend aggregation (not GraphQL)
```
