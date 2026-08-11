# NineMo — Cross-Platform Strategy (Kotlin Multiplatform)

> Strategy for shipping NineMo on **Android (Kotlin/Compose)** and **iOS (Swift/SwiftUI)**
> from one shared Kotlin data layer. Companion to `Android_Native_Migration_Analysis.md`
> — this doc **supersedes** that doc's networking/DI choices where noted in §9.
> Backend (`Backend_Architecture.md`) is unchanged.

---

## 1. Decision

**Kotlin Multiplatform (KMP): shared logic, native UI.**

| Strategy | Shared | Verdict |
|---|---|---|
| Two fully native apps | 0% | Rejected — every DTO/repo/VM written twice, contract drift risk forever |
| **KMP — shared data layer, native UI** | **~60–70%** | **Chosen** |
| Compose Multiplatform (shared UI) | ~90% | Rejected — non-native iOS feel; healthcare app needs platform trust/a11y |

**Why KMP fits NineMo unusually well:** the thin-client rule means the client contains
*zero* business logic — no EDD math, no triage, no Z-scores. Everything below the UI is
pure plumbing (DTOs, HTTP, auth refresh, session, STOMP) — exactly the code that is
identical on both platforms and pointless to write twice. UI stays 100% native
(Compose + SwiftUI), so platform look, accessibility, and store review posture are
uncompromised.

---

## 2. High-Level Architecture

```
┌──────────────────────────┐   ┌──────────────────────────┐
│  androidApp (Kotlin)     │   │  iosApp (Swift)          │
│  Compose + Material 3    │   │  SwiftUI                 │
│  Screens, theme, nav,    │   │  Views, theme, nav,      │
│  FCM, platform glue      │   │  APNs, platform glue     │
└───────────┬──────────────┘   └───────────┬──────────────┘
            │  Kotlin (direct)             │  Swift ← SKIE-generated API
┌───────────┴──────────────────────────────┴──────────────┐
│  shared (KMP module)                                    │
│  ├── model/       ← ~40 DTOs (kotlinx.serialization)    │
│  ├── network/     ← Ktor client + auth plugin           │
│  ├── data/        ← 12 repositories + ChatRepository    │
│  ├── session/     ← SessionStore (expect/actual)        │
│  └── viewmodel/   ← shared ViewModels (StateFlow)       │
└───────────────────────┬─────────────────────────────────┘
                        │ HTTPS + WSS
              api-gateway :8080 → 6 services
```

Compiles to: JVM/ART bytecode for Android; native `.framework` (XCFramework) for iOS.

---

## 3. Shared Module Layout

```
shared/src/
├── commonMain/kotlin/com/reejuven8/ninemo/shared/
│   ├── model/                     ← ALL DTOs — single source of truth for API contract
│   │   ├── ApiResponse.kt, TokenResponse.kt, TimelineResponse.kt, …
│   │   └── enums: VaccinationStatus, SeverityFlag, …
│   ├── network/
│   │   ├── NineMoHttpClient.kt    ← Ktor client factory (JSON, logging, timeouts)
│   │   ├── AuthPlugin.kt          ← attaches Bearer; 401 → refresh → retry once
│   │   └── ApiRoutes.kt           ← endpoint path constants
│   ├── data/
│   │   ├── AuthRepository.kt … ContentRepository.kt   (12 total)
│   │   └── ChatRepository.kt      ← Krossbow STOMP (multiplatform ✓)
│   ├── session/
│   │   ├── SessionStore.kt        ← expect class (tokens, userId, role, activeChildId)
│   │   └── SessionState.kt        ← Flow<AuthState> for UI gating
│   ├── viewmodel/
│   │   ├── UiState.kt             ← sealed interface Loading/Success/Error
│   │   ├── TimelineViewModel.kt … MilestoneViewModel.kt  (14 total)
│   │   └── SessionViewModel.kt
│   └── di/
│       └── SharedModule.kt        ← Koin module wiring all of the above
├── androidMain/kotlin/…
│   ├── SessionStore.android.kt    ← actual: encrypted Jetpack DataStore (Keystore key)
│   └── HttpEngine.android.kt      ← actual: Ktor OkHttp engine
└── iosMain/kotlin/…
    ├── SessionStore.ios.kt        ← actual: Keychain (kSecClass GenericPassword)
    └── HttpEngine.ios.kt          ← actual: Ktor Darwin engine (NSURLSession)
```

**Rules carried over from the RN/Android guardrails — now enforced in one place:**
- DTO field names mirror backend exactly; no renames, no `Any`
- Repositories are stateless typed adapters — no transformation, no UI state
- No clinical computation anywhere in `shared/` (code-review gate: grep for threshold literals)
- Online-first: no SQLDelight/Room, no offline sync

---

## 4. Layer Mapping — Three Codebases, One Architecture

| Concept | RN (legacy) | shared (KMP) | androidApp | iosApp |
|---|---|---|---|---|
| API types | `types/api.ts` | `model/*.kt` | — | — |
| HTTP client | axios `apiClient` | Ktor + AuthPlugin | — | — |
| Domain adapter | `services/*.ts` | `data/*Repository.kt` | — | — |
| Server state | React Query hooks | `viewmodel/*ViewModel.kt` (StateFlow) | collect | observe (SKIE) |
| Session state | Redux slices | `SessionStore` + `SessionViewModel` | — | — |
| UI | `screens/*.tsx` | — | `*Screen.kt` Composable | `*View.swift` SwiftUI |
| Navigation | routes.ts | — | Navigation Compose | NavigationStack |
| Components | components/ | — | Compose components | SwiftUI components |
| Push | — | — | FCM | APNs (via FCM iOS SDK) |
| Charts | — | — | Vico | Swift Charts |

---

## 5. Core Patterns

### 5.1 Ktor client with auth (replaces Retrofit + OkHttp Interceptor/Authenticator)

```kotlin
// commonMain — network/NineMoHttpClient.kt
fun nineMoHttpClient(engine: HttpClientEngine, session: SessionStore) = HttpClient(engine) {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    install(Auth) {
        bearer {
            loadTokens {
                session.tokens()?.let { BearerTokens(it.access, it.refresh) }
            }
            refreshTokens {                      // Ktor calls this on 401 automatically
                val old = oldTokens?.refreshToken ?: return@refreshTokens null
                runCatching { client.post("${ApiRoutes.AUTH}/refresh") {
                    header("Authorization", "Bearer $old")
                    markAsRefreshTokenRequest()
                }.body<ApiResponse<TokenResponse>>() }
                    .getOrNull()?.data
                    ?.also { session.save(it) }
                    ?.let { BearerTokens(it.accessToken, it.refreshToken) }
                    ?: run { session.clear(); null }   // refresh dead → logged out
            }
        }
    }
    defaultRequest { url(BuildKonfig.BASE_URL) }   // dev: 10.0.2.2 (Android) / localhost (iOS sim)
}
```

Ktor's `Auth` plugin natively implements the load→401→refresh→retry loop that needed a
hand-written `TokenAuthenticator` in the Android-only plan.

### 5.2 Repository (identical role, now written once)

```kotlin
// commonMain — data/TimelineRepository.kt
class TimelineRepository(private val client: HttpClient) {
    suspend fun currentWeek(): TimelineResponse =
        client.get("${ApiRoutes.NINEMO}/timeline/current").body()
    suspend fun week(week: Int): TimelineResponse =
        client.get("${ApiRoutes.NINEMO}/timeline/week/$week").body()
}
```

### 5.3 Shared ViewModel

Use plain classes + StateFlow with `androidx.lifecycle:lifecycle-viewmodel` (KMP-compatible
since 2.8) so the *same* class is an AndroidX ViewModel on Android and a plain observable
object on iOS.

```kotlin
// commonMain — viewmodel/TimelineViewModel.kt
class TimelineViewModel(private val repo: TimelineRepository) : ViewModel() {
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

### 5.4 Android consumption (unchanged from Android doc)

```kotlin
@Composable
fun TimelineScreen(viewModel: TimelineViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val s = state) {
        is UiState.Loading -> LoadingSpinner()
        is UiState.Error -> ErrorView(s.throwable, onRetry = viewModel::refresh)
        is UiState.Success -> TimelineFeed(s.data)
    }
}
```

### 5.5 iOS consumption via SKIE

**SKIE** (Touchlab, free/OSS) post-processes the generated framework so Swift sees:
sealed classes as Swift enums (exhaustive `switch`), `Flow` as `AsyncSequence`,
suspend funs as `async` funs with cancellation.

```swift
// iosApp — TimelineView.swift
struct TimelineView: View {
    @State private var state: UiState<TimelineResponse> = UiStateLoading()
    private let viewModel: TimelineViewModel = Koin.get()

    var body: some View {
        content.task {
            for await s in viewModel.state {   // SKIE: StateFlow → AsyncSequence
                state = s
            }
        }
    }

    @ViewBuilder private var content: some View {
        switch onEnum(of: state) {             // SKIE: sealed → exhaustive enum
        case .loading: LoadingSpinner()
        case .error(let e): ErrorView(error: e.throwable) { viewModel.refresh() }
        case .success(let s): TimelineFeed(data: s.data)
        }
    }
}
```

SwiftUI views obey the same dumb-renderer rule: no computation, no HTTP, no clinical logic.

### 5.6 SessionStore — expect/actual

```kotlin
// commonMain
expect class SessionStore {
    suspend fun save(tokens: TokenResponse)
    suspend fun tokens(): StoredTokens?
    suspend fun clear()
    val isAuthenticated: Flow<Boolean>
    val activeChildId: MutableStateFlow<String?>      // was uiSlice
    val activePregnancyId: MutableStateFlow<String?>
}
```

| Platform | actual backing |
|---|---|
| Android | Jetpack DataStore, values encrypted with Keystore-held AES key |
| iOS | Keychain (`kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`) |

Never plain storage, never logged, on either platform.

### 5.7 STOMP chat — shared as-is

Krossbow is multiplatform: `krossbow-websocket-okhttp` (androidMain engine),
`krossbow-websocket-darwin` (iosMain engine), STOMP + kotlinx-serialization in commonMain.
`ChatRepository` and `ChatViewModel` from the Android doc move to `shared/` unmodified in
design. Same integration risk as before: verify `/ws/connect` accepts raw WS upgrade
(SockJS `withSockJS()` also serves `/ws/connect/websocket`) — spike in Phase K0.

### 5.8 DI — Koin

Hilt is Android-only; Koin is KMP-native and initializes from both entry points:

```kotlin
// commonMain — di/SharedModule.kt
val sharedModule = module {
    single { nineMoHttpClient(get(), get()) }
    singleOf(::TimelineRepository); singleOf(::AuthRepository)   // … all 13
    viewModelOf(::TimelineViewModel)                              // … all 14
}
```

Android: `startKoin` in `NineMoApplication` + `koinViewModel()` in Composables.
iOS: `KoinKt.doInitKoin()` in `@main` App init + a small `Koin.get()` Swift helper.

---

## 6. Push Notifications

| Piece | Android | iOS |
|---|---|---|
| Transport | FCM | APNs via FCM iOS SDK (one backend integration — notification-service already speaks FCM) |
| Token registration | `POST /api/v1/notifications/devices` (backend gap from Android doc §8 — now needs `platform: ANDROID \| IOS` field) | same endpoint |
| Channels/categories | `clinical_alerts` (HIGH), `milestones`, `community` | `UNNotificationCategory` equivalents; critical alerts require entitlement — apply early, Apple review takes weeks |

---

## 7. Project Structure & Build

```
ninemo-mobile/                       ← new top-level dir (replaces ninemo-frontend/)
├── settings.gradle.kts              ← :shared, :androidApp
├── gradle/libs.versions.toml
├── shared/                          ← KMP module (§3)
├── androidApp/                      ← Compose app; depends on :shared
└── iosApp/                          ← Xcode project; embeds Shared.xcframework
    └── iosApp.xcodeproj             ← build phase runs :shared:embedAndSignAppleFrameworkForXcode
```

Version catalog deltas vs the Android-only doc:

```toml
[versions]
ktor = "3.0.3"           # replaces retrofit/okhttp-interceptor stack
koin = "4.0.1"           # replaces hilt
skie = "0.10.1"
lifecycle-kmp = "2.8.7"  # KMP ViewModel
krossbow = "9.1.0"       # unchanged — already KMP
kotlinx-serialization = "1.7.3"   # unchanged
# androidApp keeps: compose-bom, nav, vico, coil, firebase-messaging
```

`BuildKonfig` (or Gradle-generated) supplies `BASE_URL` per flavor:
Android dev `http://10.0.2.2:8080/api/v1`, iOS simulator dev `http://localhost:8080/api/v1`,
prod `https://api.reejuven8.com/api/v1`.

### CI (extends `.github/workflows/ci.yml`)

| Job | Runner | Does |
|---|---|---|
| `shared-test` | ubuntu | `./gradlew :shared:allTests` (common + JVM) |
| `android-build` | ubuntu | `:androidApp:assembleDebug` + unit tests |
| `ios-build` | **macos** | `xcodebuild -scheme iosApp` + shared iosSimulatorArm64 tests |

---

## 8. Testing Strategy

| Layer | Where | Tools |
|---|---|---|
| DTO serialization round-trips | commonTest | kotlinx-serialization + sample JSON fixtures from backend |
| Repositories | commonTest | Ktor `MockEngine` (no MockWebServer needed — engine-level fakes run on both targets) |
| ViewModels | commonTest | Turbine + kotlinx-coroutines-test |
| Compose UI | androidApp | `createAndroidComposeRule` |
| SwiftUI | iosApp | XCTest + ViewInspector (light — views are dumb) |
| E2E | per platform | Maestro (works on both Android & iOS) against staging gateway |

Shared tests run once and certify both apps' entire data layer — the main testing win of KMP.

---

## 9. Deltas vs `Android_Native_Migration_Analysis.md`

That doc remains the reference for everything Android-UI-side (screens map §6, theme,
navigation, Vico, FCM channels, FLAG_SECURE, R8). The following choices are **replaced**:

| Android doc said | This strategy says | Why |
|---|---|---|
| Retrofit + OkHttp interceptor/authenticator | Ktor + Auth plugin (commonMain) | Retrofit is JVM-only |
| Hilt everywhere | Koin (shared) — Hilt dropped entirely for consistency | Hilt is Android-only |
| DTOs in `androidApp/core/model` | DTOs in `shared/model` | single contract |
| Repos/VMs in androidApp | `shared/data`, `shared/viewmodel` | the point of KMP |
| Jetpack DataStore SessionStore | expect/actual: DataStore ↔ Keychain | iOS parity |
| Single `app` Gradle module | `:shared` + `:androidApp` + `iosApp/` | — |

Unchanged: Compose/M3, Navigation Compose, Vico, Coil, kotlinx.serialization, Krossbow,
online-first rule, all security rules, screen inventory (13 + MilestoneChecklist).

---

## 10. Phased Execution Plan

| Phase | Scope | Est. |
|---|---|---|
| **K0** | Scaffold: KMP project, version catalog, Koin, Ktor client + Auth plugin, SessionStore expect/actual, **all ~40 DTOs**, SKIE wired, WS-handshake spike, both apps boot to a "hello, authenticated?" screen | 5 d |
| **K1** | Shared: Auth + Timeline + Symptom repos & VMs w/ commonTests. Android: Login/Register/Timeline/SymptomLog screens | 5 d |
| **K2** | Shared: Vitals, KickCounter, Contraction, SummaryCard. Android screens for same | 5 d |
| **K3** | iOS catch-up #1: SwiftUI for K1+K2 scope (shared layer already done — UI only) | 5 d |
| **K4** | Shared: Growth, Vaccination, Milestone, mode transition. Android + iOS screens (Vico / Swift Charts) | 5 d |
| **K5** | Community: shared ChatRepository (STOMP) + Club/Content repos; chat UI both platforms | 5 d |
| **K6** | Push: backend device-token endpoint (+`platform` field), FCM + APNs, deep links; APNs critical-alert entitlement request submitted | 4 d |
| **K7** | Hardening both: cert pinning (OkHttp pinner / Darwin `SecTrust`), FLAG_SECURE / iOS screen-capture guard, R8, release-log stripping, a11y | 4 d |
| **K8** | CI (3 jobs), Maestro E2E, Play internal track + TestFlight | 4 d |

**Total: ~8.5 weeks one dev; ~5–6 weeks with a second (iOS-focused) dev** — vs ~12+ weeks
for two fully native apps. After K0, iOS screens trail Android by one phase but ride the
same shared layer, so parity gap stays small and closes at K8.

---

## 11. Risks

| Risk | L | Mitigation |
|---|---|---|
| iOS dev friction with Kotlin-generated APIs | M | SKIE removes most of it (enums, async/await, Flows); iOS devs never *write* Kotlin, only consume |
| Ktor Auth plugin refresh edge cases (parallel 401s) | M | Plugin serializes refresh internally; commonTest with MockEngine simulating expiry |
| STOMP raw-WS handshake vs SockJS | M | K0 spike; fallback `/ws/connect/websocket` |
| Xcode/Gradle build integration flakiness | M | Pin Kotlin/AGP/Xcode versions; `embedAndSignAppleFrameworkForXcode` standard flow; CI on macos runner from K0 |
| KMP ViewModel/lifecycle API churn | L | On stable `lifecycle-viewmodel` 2.8+; pin versions |
| Team scope creep to Compose Multiplatform "since we're here" | L | Decision recorded here: native UI, revisit only post-launch |
| Apple critical-alert entitlement delay | M | Apply at K6 start; ship v1 with time-sensitive notifications if pending |

---

## 12. Definition of Done

- [ ] `shared/` owns 100% of DTOs, networking, repositories, session, ViewModels — zero duplicated data-layer code in androidApp/iosApp
- [ ] All 14 screens live natively on both platforms (13 RN-parity + MilestoneChecklist)
- [ ] Silent token refresh verified on both (Ktor Auth plugin, single-flight)
- [ ] STOMP chat send/receive on both against staging
- [ ] CRITICAL clinical alert → heads-up (Android) / time-sensitive or critical (iOS) notification
- [ ] Tokens: Keystore-encrypted DataStore (Android) / Keychain (iOS); no PII in logs, release builds strip logging
- [ ] commonTest suite covers every repository + ViewModel; CI green on all 3 jobs
- [ ] Zero clinical computation in any client code (grep gate in CI)
