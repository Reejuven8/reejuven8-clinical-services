# NineMo — Native Kotlin Android Migration Analysis

> Analysis of the current architecture (backend + React Native client) and the complete
> blueprint for rebuilding the mobile app as a native Kotlin Android application.
> Companion to `Backend_Architecture.md`. Backend is **unchanged** by this migration.

---

## 1. Executive Summary

NineMo today = 7 backend microservices (fully built, Phases 1–7 complete) + a React Native
thin client (13 screens, 10 hook modules, 12 services). The RN codebase was deliberately
architected for this migration — `docs/CLAUDE.md` §Migration Guardrails defines the exact
RN→Kotlin layer mapping, and every RN layer has a 1:1 Kotlin equivalent.

**Key facts that make this migration low-risk:**

1. **Zero business logic on the client.** EDD math, triage rules, WHO Z-scores, IAP
   schedules — all server-side. The Android app is a renderer over the same REST API.
2. **API contract is frozen.** `src/types/api.ts` mirrors backend DTOs exactly; these
   translate mechanically to Kotlin data classes.
3. **No offline-first.** No local DB sync to rebuild — Retrofit + in-memory/ViewModel
   caching replaces React Query's cache.
4. **Auth model is simple.** JWT Bearer (15-min access / 7-day refresh), token storage,
   401 → refresh-or-logout. Standard OkHttp `Authenticator` territory.

**Recommendation:** Single-activity Jetpack Compose app, MVVM + Repository, Hilt DI,
Retrofit/OkHttp, Kotlin Coroutines + StateFlow, Navigation Compose. Phased build,
~6 weeks to feature parity.

---

## 2. Current State Analysis

### 2.1 Backend Surface Consumed by the Client (unchanged)

All traffic goes through `api-gateway:8080` → `/api/v1/**`. JWT validated at gateway;
`X-User-Id` / `X-User-Role` injected downstream.

| Domain | Endpoints | Backing Service |
|---|---|---|
| Auth | `POST /identity/auth/{otp/send, login, register, refresh, logout}` | identity-abha (8081) |
| ABHA | `POST /identity/abha/enroll/{otp/generate, otp/verify, address}` | identity-abha |
| Consent | `POST /identity/consent/{grant, revoke/{id}}`, `GET /identity/consent/list` | identity-abha |
| Health records | `GET /health/records[...]`, `POST /health/files/upload`, `GET /health/files/{key}/download` | health-data (8082) |
| Timeline | `GET /ninemo/timeline/{current, week/{n}}` | clinical (8084) |
| Symptoms | `POST/GET /ninemo/symptoms` | clinical |
| Vitals | `POST /ninemo/vitals`, `GET /ninemo/vitals/{type}` | clinical |
| Kick counter | `POST /ninemo/kick-counter/sessions`, `PUT .../{id}/{kick, end}` | clinical |
| Contractions | `POST /ninemo/contractions/sessions`, `PUT .../{id}/{contraction, end}` | clinical |
| Summary card | `GET /ninemo/summary-card/{patientId}` | clinical |
| Growth | `POST/GET /ninemo/growth/children/{childId}/measurements` | clinical |
| Vaccination | `GET /ninemo/vaccinations/children/{childId}/schedule`, `PUT /{id}/mark-completed` | clinical |
| Milestones | `GET /ninemo/milestones/children/{childId}[/month/{m}]`, `PUT /{docId}/achieve` | clinical |
| Diet | `GET /ninemo/diet/search?q=` | clinical |
| Mode | `POST /ninemo/mode/transition-to-postnatal/{pregnancyProfileId}` | clinical |
| Clubs/Chat | `POST /ninemo/community/clubs/join`, `GET clubs[...]`, `GET/DELETE .../messages` | community (8086) |
| Chat (live) | `WS /ws/connect` — STOMP; SEND `/app/chat.send/{club}/{ch}`, SUB `/topic/club.{club}.{ch}` | community |
| Content | `GET /ninemo/community/content[...]` | community |

Response envelope: `ApiResponse<T> { status, data, error, metadata }` (some clinical
endpoints return raw DTOs — Timeline, SummaryCard, Diet).

### 2.2 React Native App Inventory (the thing being replaced)

```
ninemo-frontend/src/
  types/api.ts            ← ~40 interfaces mirroring backend DTOs
  services/  (12 files)   ← apiClient + 11 domain services (axios, stateless)
  hooks/     (10 files)   ← React Query hooks (queries + mutations)
  screens/   (13 files)   ← Login, Register, Timeline, SymptomLog, VitalsWeight,
                            VitalsBP, KickCounter, ContractionTimer, SummaryCard,
                            DueDateClub, GrowthChart, Vaccination (+ ContentFeed route)
  store/     (2 slices)   ← authSlice (userId, role, isAuthenticated), uiSlice
                            (activeChildId, activePregnancyId)
  navigation/             ← routes.ts constants, Auth/Main/App navigators
  components/             ← LoadingSpinner, ErrorView
```

Patterns in force (all carry over conceptually):
- Services = stateless typed HTTP adapters
- Hooks own server state; screens render only
- Redux holds *session* state only (never server data)
- Route names = constants
- 401 interceptor clears storage
- `enabled: !!childId`-style guards on dependent queries

---

## 3. Target Architecture (Kotlin Android)

### 3.1 Stack Decision

| Concern | Choice | Rationale |
|---|---|---|
| Language | Kotlin 2.x | — |
| Min/Target SDK | 26 / 35 | ABDM-era devices; covers ~95% Indian Android market |
| UI | Jetpack Compose + Material 3 | Declarative like RN; guardrails doc assumes Composables |
| Architecture | MVVM + Repository (Google's recommended arch) | Direct mapping from hooks/services |
| DI | Hilt | Standard; ViewModel injection |
| HTTP | Retrofit 2 + OkHttp 4 | Replaces axios |
| JSON | kotlinx.serialization | Kotlin-first; compile-time; no reflection |
| Async | Coroutines + Flow/StateFlow | Replaces React Query + Redux |
| Navigation | Navigation Compose (type-safe routes) | Replaces React Navigation |
| Token storage | Jetpack DataStore + Android Keystore (EncryptedSharedPreferences fallback) | Replaces AsyncStorage — with real encryption |
| WebSocket STOMP | Krossbow (`krossbow-stomp-core` + OkHttp adapter) | Only maintained Kotlin STOMP client; SockJS not needed (raw WS) |
| Push | Firebase Cloud Messaging | Backend already sends FCM via notification-service |
| Charts (growth/weight) | Vico (Compose-native) | Z-score curves + measurement plots |
| Images | Coil | Compose-native |
| Server cache | In-ViewModel StateFlow + repository memory cache | No offline-first (rule §8); Room NOT used |
| Testing | JUnit5, MockK, Turbine, MockWebServer, Compose UI test | — |

**Explicitly excluded** (mirrors RN rules): Room/SQLDelight for server data, WorkManager
background sync, any client-side clinical calculation.

### 3.2 Module Structure

Single Gradle app module with strict package layering — matches team size and the RN
codebase's scale. Split into `:core` + `:feature:*` modules later only if build times hurt.

```
app/src/main/java/com/reejuven8/ninemo/
├── NineMoApplication.kt              ← @HiltAndroidApp
├── MainActivity.kt                   ← single activity, setContent { NineMoApp() }
├── core/
│   ├── network/
│   │   ├── ApiClientModule.kt        ← Hilt: OkHttp + Retrofit singletons
│   │   ├── AuthInterceptor.kt        ← attaches Bearer token
│   │   ├── TokenAuthenticator.kt     ← 401 → refresh → retry (or logout)
│   │   └── ApiResponse.kt            ← envelope + unwrap extension
│   ├── datastore/
│   │   └── SessionStore.kt           ← tokens + userId + role (encrypted)
│   ├── model/                        ← ALL DTOs (mirror of types/api.ts)
│   └── ui/
│       ├── theme/                    ← Material 3 theme
│       └── components/               ← LoadingSpinner, ErrorView, etc.
├── data/                             ← Repositories (was services/)
│   ├── AuthRepository.kt
│   ├── TimelineRepository.kt
│   ├── SymptomRepository.kt
│   ├── VitalsRepository.kt
│   ├── KickCounterRepository.kt
│   ├── ContractionRepository.kt
│   ├── SummaryCardRepository.kt
│   ├── GrowthRepository.kt
│   ├── VaccinationRepository.kt
│   ├── MilestoneRepository.kt
│   ├── CommunityRepository.kt
│   ├── ContentRepository.kt
│   └── api/                          ← Retrofit interfaces per domain
│       ├── AuthApi.kt … CommunityApi.kt
├── feature/                          ← screen + viewmodel pairs (was screens/ + hooks/)
│   ├── auth/        LoginScreen.kt, RegisterScreen.kt, AuthViewModel.kt
│   ├── timeline/    TimelineScreen.kt, TimelineViewModel.kt
│   ├── symptoms/    SymptomLogScreen.kt, SymptomViewModel.kt
│   ├── vitals/      VitalsWeightScreen.kt, VitalsBPScreen.kt, VitalsViewModel.kt
│   ├── kickcounter/ KickCounterScreen.kt, KickCounterViewModel.kt
│   ├── contractions/ContractionTimerScreen.kt, ContractionViewModel.kt
│   ├── summarycard/ SummaryCardScreen.kt, SummaryCardViewModel.kt
│   ├── growth/      GrowthChartScreen.kt, GrowthViewModel.kt
│   ├── vaccination/ VaccinationScreen.kt, VaccinationViewModel.kt
│   ├── community/   DueDateClubScreen.kt, ChatViewModel.kt, ClubViewModel.kt
│   └── content/     ContentFeedScreen.kt, ContentViewModel.kt
├── session/
│   └── SessionViewModel.kt           ← was authSlice + uiSlice (activeChildId etc.)
└── navigation/
    ├── Routes.kt                     ← @Serializable route objects
    └── NineMoNavHost.kt              ← auth-gated graph (was AppNavigator)
```

---

## 4. Layer Mapping (RN → Kotlin) — Concrete

Per `docs/CLAUDE.md` guardrails table, now made concrete:

| RN artifact | Kotlin artifact | Notes |
|---|---|---|
| `services/apiClient.ts` (axios instance) | `ApiClientModule` + `AuthInterceptor` + `TokenAuthenticator` | Interceptor = request interceptor; Authenticator = 401 handling **with refresh**, an upgrade over RN's clear-storage-only |
| `services/timelineService.ts` | `TimelineApi` (Retrofit) + `TimelineRepository` | Retrofit interface = HTTP shape; repository = the stateless adapter |
| `hooks/useTimeline.ts` | `TimelineViewModel` | `StateFlow<UiState<TimelineResponse>>`; `refetch()` = re-launch coroutine |
| `hooks/useLogSymptom` (mutation) | `SymptomViewModel.logSymptom()` fun + invalidate = re-fetch history flow | |
| `screens/TimelineScreen.tsx` | `TimelineScreen.kt` `@Composable` | `collectAsStateWithLifecycle()` replaces hook destructure |
| `store/authSlice.ts` | `SessionStore` (persistence) + `SessionViewModel` (in-memory StateFlow) | Redux dispatch → suspend fun on store |
| `store/uiSlice.ts` (activeChildId…) | `SessionViewModel` StateFlows | |
| `types/api.ts` | `core/model/*.kt` `@Serializable data class` | Field names identical — no `@SerialName` renames needed |
| `navigation/routes.ts` | `Routes.kt` `@Serializable` objects | Type-safe nav args replace string params |
| `AppNavigator` auth gate | `NineMoNavHost` switching on `SessionViewModel.isAuthenticated` | |
| React Query cache | ViewModel-scoped StateFlow; repository memory cache where shared | Accept cache loss on process death — matches online-first rule |
| `enabled: !!childId` guard | `flatMapLatest` on `activeChildId` filterNotNull | |
| `onSettled` logout cleanup | `finally` block in logout suspend fun | |

---

## 5. Core Patterns (reference implementations)

### 5.1 DTOs — mirror backend exactly (rule §4 carries over)

```kotlin
@Serializable
data class ApiResponse<T>(
    val status: String,
    val data: T?,
    val error: ErrorBody? = null,
    val metadata: Map<String, JsonElement>? = null,
)

@Serializable
data class TimelineResponse(
    val gestationalWeek: Int,
    val trimester: Int,                 // 1 | 2 | 3
    val babyDevelopment: BabyDevelopment,
    val maternalChanges: List<String>,
    val scheduledMilestones: List<Milestone>,
    val dietTips: List<DietTip>,
    val yogaRoutine: YogaRoutine? = null,
)

@Serializable
enum class VaccinationStatus { PENDING, COMPLETED, SKIPPED, OVERDUE }
```

Rule: **no client-side renames, no collapsed types, no `Any`.** `Any` is the Kotlin `any`.

### 5.2 Retrofit API + Repository

```kotlin
interface TimelineApi {
    @GET("ninemo/timeline/current")
    suspend fun getCurrentWeek(): TimelineResponse

    @GET("ninemo/timeline/week/{week}")
    suspend fun getWeek(@Path("week") week: Int): TimelineResponse
}

class TimelineRepository @Inject constructor(private val api: TimelineApi) {
    suspend fun currentWeek(): TimelineResponse = api.getCurrentWeek()
    suspend fun week(week: Int): TimelineResponse = api.getWeek(week)
}
```

Repository stays a stateless typed adapter — same rule as RN services. No transformation,
no caching logic beyond an optional memory cache, no UI state.

### 5.3 ViewModel + UiState (the hook replacement)

```kotlin
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val throwable: Throwable) : UiState<Nothing>
}

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val repo: TimelineRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<UiState<TimelineResponse>>(UiState.Loading)
    val state: StateFlow<UiState<TimelineResponse>> = _state.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.value = UiState.Loading
        _state.value = runCatching { repo.currentWeek() }
            .fold({ UiState.Success(it) }, { UiState.Error(it) })
    }
}
```

### 5.4 Composable screen (dumb renderer — rule §6 carries over)

```kotlin
@Composable
fun TimelineScreen(viewModel: TimelineViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val s = state) {
        is UiState.Loading -> LoadingSpinner()
        is UiState.Error -> ErrorView(s.throwable, onRetry = viewModel::refresh)
        is UiState.Success -> TimelineFeed(s.data)
    }
}
```

No calculation, no conditional clinical logic, no repository calls in Composables.

### 5.5 Auth plumbing (upgrade over RN)

```kotlin
class AuthInterceptor @Inject constructor(private val session: SessionStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { session.accessToken.firstOrNull() }
        val req = if (token != null)
            chain.request().newBuilder().header("Authorization", "Bearer $token").build()
        else chain.request()
        return chain.proceed(req)
    }
}

class TokenAuthenticator @Inject constructor(
    private val session: SessionStore,
    private val authApi: Lazy<AuthApi>,          // Lazy — avoids DI cycle
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.priorResponse != null) return null   // one retry max
        val newToken = runBlocking {
            val refresh = session.refreshToken.firstOrNull() ?: return@runBlocking null
            runCatching { authApi.get().refresh("Bearer $refresh") }
                .getOrNull()?.also { session.save(it) }?.accessToken
        } ?: run { runBlocking { session.clear() }; return null }
        return response.request.newBuilder()
            .header("Authorization", "Bearer $newToken").build()
    }
}
```

RN client only cleared storage on 401; native app does silent refresh (backend already
exposes `POST /identity/auth/refresh`). Tokens live in DataStore backed by Keystore-wrapped
encryption — never plain SharedPreferences, never logged (security rule).

### 5.6 Session state (Redux replacement)

```kotlin
@Singleton
class SessionStore @Inject constructor(@ApplicationContext ctx: Context) {
    // encrypted DataStore: accessToken, refreshToken, userId, role
    val isAuthenticated: Flow<Boolean> = accessToken.map { it != null }
    suspend fun save(tokens: TokenResponse) { /* multiSet equivalent */ }
    suspend fun clear() { /* multiRemove equivalent */ }
}
```

`activeChildId` / `activePregnancyId` (was uiSlice) → StateFlows in a shared
`SessionViewModel` scoped to the activity; dependent screens `filterNotNull()` before
fetching (the `enabled:` guard equivalent).

### 5.7 Type-safe navigation (routes.ts replacement)

```kotlin
sealed interface Routes {
    @Serializable data object Login : Routes
    @Serializable data object Register : Routes
    @Serializable data object Timeline : Routes
    @Serializable data object SymptomLog : Routes
    @Serializable data object VitalsWeight : Routes
    @Serializable data object VitalsBP : Routes
    @Serializable data object KickCounter : Routes
    @Serializable data object ContractionTimer : Routes
    @Serializable data class SummaryCard(val patientId: String) : Routes
    @Serializable data object DueDateClub : Routes
    @Serializable data class GrowthChart(val childId: String) : Routes
    @Serializable data class Vaccination(val childId: String) : Routes
    @Serializable data object ContentFeed : Routes
}
```

Bottom bar (Timeline / Tools / Locker / Community) + nested stacks, gated:

```kotlin
NavHost(navController, startDestination = if (isAuthed) Routes.Timeline else Routes.Login)
```

### 5.8 STOMP chat (DueDateClub live chat)

```kotlin
class ChatRepository @Inject constructor(private val session: SessionStore) {
    private val stomp = StompClient(OkHttpWebSocketClient())

    suspend fun connect(): StompSession =
        stomp.connect("wss://api.reejuven8.com/ws/connect")

    fun messages(s: StompSession, clubId: String, ch: String): Flow<ChatMessage> =
        s.subscribe(StompSubscribeHeaders("/topic/club.$clubId.$ch"))
            .map { json.decodeFromString<ChatMessage>(it.bodyAsText) }

    suspend fun send(s: StompSession, clubId: String, ch: String, body: SendMessageRequest) =
        s.sendText("/app/chat.send/$clubId/$ch", json.encodeToString(body))
}
```

`ChatViewModel` collects into a `StateFlow<List<ChatMessage>>`, merges paged REST history
(`GET .../messages`) with live frames. Backend WebSocketConfig has SockJS enabled but
raw WS endpoint works for native clients — verify `/ws/connect` accepts non-SockJS
upgrade in integration testing (Spring `withSockJS()` still serves `/ws/connect/websocket`).

### 5.9 FCM

`notification-service` already dispatches FCM. Android side: `FirebaseMessagingService`
subclass → post to `NotificationManager` with channels: `clinical_alerts` (HIGH,
CRITICAL risk alerts), `milestones` (DEFAULT), `community` (LOW). Token registration
endpoint needs to be added to backend (gap — see §8).

---

## 6. Complete Screen Migration Map

| # | RN Screen | Kotlin Screen + ViewModel | Endpoints | Special components |
|---|---|---|---|---|
| 1 | LoginScreen | `LoginScreen` / `AuthViewModel` | otp/send, login | OTP input |
| 2 | RegisterScreen | `RegisterScreen` / `AuthViewModel` | register | |
| 3 | TimelineScreen | `TimelineScreen` / `TimelineViewModel` | timeline/current, week/{n} | Week pager (HorizontalPager) |
| 4 | SymptomLogScreen | `SymptomLogScreen` / `SymptomViewModel` | symptoms POST/GET | Severity badge from server flag |
| 5 | VitalsWeightScreen | `VitalsWeightScreen` / `VitalsViewModel` | vitals POST, GET WEIGHT | Vico line chart |
| 6 | VitalsBPScreen | `VitalsBPScreen` / `VitalsViewModel` | vitals POST, GET BLOOD_PRESSURE | Alert banner if `alertTriggered` |
| 7 | KickCounterScreen | `KickCounterScreen` / `KickCounterViewModel` | sessions, kick, end | Big-tap counter; timer is UI-only |
| 8 | ContractionTimerScreen | `ContractionTimerScreen` / `ContractionViewModel` | sessions, contraction, end | Server returns isLaborPattern |
| 9 | SummaryCardScreen | `SummaryCardScreen` / `SummaryCardViewModel` | summary-card/{id} | Single-scroll doctor card |
| 10 | GrowthChartScreen | `GrowthChartScreen` / `GrowthViewModel` | growth measurements | Vico + server-computed Z-scores/percentiles — **never compute locally** |
| 11 | VaccinationScreen | `VaccinationScreen` / `VaccinationViewModel` | schedule, mark-completed | Status chips (PENDING/COMPLETED/OVERDUE) |
| 12 | DueDateClubScreen | `DueDateClubScreen` / `ClubViewModel` + `ChatViewModel` | clubs/*, messages, STOMP | LazyColumn reverse layout chat |
| 13 | ContentFeed (route) | `ContentFeedScreen` / `ContentViewModel` | content/* | Week-filtered articles |
| — | (new) MilestoneChecklist | `MilestoneScreen` / `MilestoneViewModel` | milestones/* | Backend built (Phase 3.1); RN never shipped a screen — parity+1 |

---

## 7. Gradle Setup (version catalog excerpt)

```toml
[versions]
kotlin = "2.1.0"
compose-bom = "2025.05.00"
hilt = "2.53"
retrofit = "2.11.0"
okhttp = "4.12.0"
kotlinx-serialization = "1.7.3"
krossbow = "9.1.0"
vico = "2.0.0"
coil = "3.0.0"
nav = "2.8.4"

[libraries]
retrofit = { module = "com.squareup.retrofit2:retrofit", version.ref = "retrofit" }
retrofit-kotlinx = { module = "com.squareup.retrofit2:converter-kotlinx-serialization", version.ref = "retrofit" }
krossbow-stomp = { module = "org.hildan.krossbow:krossbow-stomp-kxserialization", version.ref = "krossbow" }
krossbow-okhttp = { module = "org.hildan.krossbow:krossbow-websocket-okhttp", version.ref = "krossbow" }
vico-compose = { module = "com.patrykandpatrick.vico:compose-m3", version.ref = "vico" }
# + compose BOM, hilt, nav-compose, datastore, security-crypto, firebase-messaging, coil
```

Build variants: `dev` (BASE_URL `http://10.0.2.2:8080/api/v1` — emulator loopback,
replaces RN's `localhost`), `prod` (`https://api.reejuven8.com/api/v1`).

---

## 8. Gaps & Backend Asks

| Gap | Impact | Action |
|---|---|---|
| No FCM device-token registration endpoint | Push lands nowhere per-device | Add `POST /api/v1/notifications/devices` to notification-service; store token per userId |
| WS auth: JWT not validated on STOMP CONNECT (only `/ws/**` permitted) | Chat spoofing risk | Add STOMP ChannelInterceptor validating Bearer on CONNECT |
| Some clinical endpoints return raw DTOs, others `ApiResponse<T>` envelope | Two deserialization paths | Live with it (document per-endpoint), or normalize backend to envelope everywhere |
| `X-User-Id` header set by gateway — dev builds hitting services directly need it manually | Dev friction only | Debug interceptor adds header when BASE_URL ≠ gateway |
| Certificate pinning | Prod hardening | Pin `api.reejuven8.com` leaf+intermediate in OkHttp `CertificatePinner` |

---

## 9. Security Carry-Over (non-negotiable)

- Tokens in encrypted DataStore (Keystore master key) — never plain storage, never logs
- Never log Aadhaar, OTPs, JWT payloads — Timber tree strips in release; OkHttp
  logging interceptor **debug builds only**, level BASIC (no bodies on auth routes)
- `android:allowBackup="false"`; `FLAG_SECURE` on screens showing medical data
- 15-min presigned S3 URLs — fetch on-demand, never persist
- ProGuard/R8 keep rules for `@Serializable` models only; obfuscate the rest

---

## 10. Testing Strategy

| Layer | Tool | Mirrors |
|---|---|---|
| Repository | MockWebServer + JUnit5 | RN service tests |
| ViewModel | MockK + Turbine (StateFlow assertions) | React Query hook tests |
| Compose UI | `createAndroidComposeRule` — loading/error/success snapshots | screen render tests |
| E2E happy path | Maestro or Compose-test against staging gateway | Functional_Test_Document.md flows |

Rule: no test asserts clinical values computed on-device — there are none. Tests assert
*rendering* of server-provided flags (e.g. `severityFlag=CRITICAL` shows red banner).

---

## 11. Phased Execution Plan

| Phase | Scope | Est. |
|---|---|---|
| **A0** | Project scaffold: Gradle catalog, Hilt, theme, nav skeleton, ApiClient + interceptor/authenticator, SessionStore, all DTOs ported from `types/api.ts` | 3–4 d |
| **A1** | Auth vertical: Login/Register/OTP, token refresh, auth-gated NavHost | 3 d |
| **A2** | Pregnancy core: Timeline, SymptomLog, Vitals×2 (+Vico), SummaryCard | 5 d |
| **A3** | Tools: KickCounter, ContractionTimer | 3 d |
| **A4** | Pediatric: GrowthChart, Vaccination, MilestoneChecklist (new), mode-transition handling | 4–5 d |
| **A5** | Community: Clubs, STOMP chat, ContentFeed | 4–5 d |
| **A6** | FCM (incl. backend device-token endpoint), notification channels, deep links | 3 d |
| **A7** | Hardening: cert pinning, FLAG_SECURE, R8, error taxonomy, a11y pass | 3 d |
| **A8** | Test suite to parity + Maestro E2E + Play internal track | 4 d |

**Total: ~5.5–6.5 weeks** single dev; phases A2–A5 parallelize across two devs to ~4 weeks.

Big-bang replacement (not incremental brownfield RN+native hybrid) is correct here:
the RN app has no production users yet, and hybrid bridging would cost more than the
rewrite it was designed to avoid.

---

## 12. Risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| STOMP/SockJS handshake mismatch on `/ws/connect` | Medium | Test early (A0 spike); fall back to `/ws/connect/websocket` SockJS raw-WS path or add plain endpoint to WebSocketConfig |
| Envelope inconsistency causes deserialization bugs | Medium | Per-endpoint return types in Retrofit interfaces; integration tests against live gateway in CI |
| Vico chart fidelity for Z-score bands | Low | Server already returns percentile band values; chart is dumb plotting |
| Team Kotlin/Compose ramp-up | Low–Med | Architecture intentionally mirrors RN layer-for-layer; this doc + guardrails table is the onboarding |
| Scope creep to offline-first | Med | Same rule as RN: online-first, React-Query-equivalent caching only. Escalate before adding Room. |

---

## 13. Definition of Done (parity checklist)

- [ ] All 13 RN screens + MilestoneChecklist live in Compose
- [ ] Auth: OTP login, register, silent refresh, logout blacklisting verified
- [ ] All DTOs mirror backend field names — zero renames
- [ ] Zero clinical computation on device (code-review gate, grep for threshold literals)
- [ ] STOMP chat send/receive against staging
- [ ] FCM CRITICAL alert → heads-up notification
- [ ] Tokens encrypted; no PII in logcat on release build
- [ ] Test suite green in CI (add `android-build` job to `.github/workflows/ci.yml`)
