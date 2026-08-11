# NineMo Mobile — CLAUDE.md

## Project Context
NineMo is a React Native maternity & childcare app backed by a Java/Spring Boot 
microservices backend. The mobile app is intentionally a **thin client**. All medical 
logic lives on the backend. The RN codebase is architected to migrate cleanly to 
Native Android — every structural decision must support that future migration.

---

## Non-Negotiable Rules

### 1. Zero Business Logic on the Client
The backend owns all computation. The mobile app renders results.

**Never implement on the client:**
- EDD / gestational week calculation
- Symptom triage rules (Preeclampsia, Anemia, GDM thresholds)
- WHO Z-score or percentile calculation
- IAP vaccination schedule generation
- BMI or weight-gain curve calculation
- Any clinical threshold evaluation

If you find yourself writing medical logic in TypeScript, stop. It belongs in 
`ninemo-clinical-service`. The client receives the computed result and renders it.

### 2. Strict TypeScript — No Exceptions
- `strict: true` is set in `tsconfig.json`. Do not disable it.
- `any` is forbidden. Use `unknown` if the type is genuinely unknown, then narrow it.
- Every API response must have a corresponding interface in `src/types/api.ts`.
- Every component prop must be explicitly typed — no implicit prop inference.
- No `// @ts-ignore` or `// @ts-expect-error` without a comment explaining why.
- Return types on all service functions are required — never inferred from usage.

### 3. Strict Layer Separation
Every feature must follow this four-layer structure. Do not mix layers.

```
src/
  services/     ← API calls ONLY. No state, no UI, no transformation logic.
  hooks/        ← Data-fetching and state. Calls services. No JSX.
  screens/      ← UI only. Calls hooks. No direct API calls. No business logic.
  store/        ← Global state slices. State shape only — no side effects.
  types/        ← All TypeScript interfaces and enums. No runtime code.
  components/   ← Reusable UI primitives. Purely presentational.
  navigation/   ← Route definitions and navigator config only.
```

**Violations:**
- A `screen` calling `fetch()` or `axios` directly → move to a `service`
- A `service` holding React state → move to a `hook`
- A `hook` containing JSX → move to a `screen` or `component`
- Business logic inside a `useEffect` → move to the backend or a `service`

### 4. API Contracts Are the Source of Truth
Before writing any feature code, define the API types in `src/types/api.ts` first.
Types must exactly mirror the backend DTO field names (camelCase from Spring Boot).
No renaming fields in the client — what the API returns is what the type says.

```ts
// Correct — mirrors backend TimelineResponse DTO exactly
export interface TimelineResponse {
  gestationalWeek: number;
  trimester: 1 | 2 | 3;
  babyDevelopment: BabyDevelopment;
  maternalChanges: string[];
  scheduledMilestones: Milestone[];
  dietTips: DietTip[];
}

// Wrong — do not transform or rename fields from the API response
export interface WeekData {
  week: number; // renamed from gestationalWeek
  baby: any;    // collapsed nested object, typed as any
}
```

### 5. Services Are Stateless HTTP Adapters
A service file does exactly one thing: make HTTP calls and return typed responses.

```ts
// Correct
export const timelineService = {
  getCurrentWeek: async (): Promise<TimelineResponse> => {
    const response = await apiClient.get<TimelineResponse>('/ninemo/timeline');
    return response.data;
  },
};

// Wrong — transformation, caching, and error handling do not belong in services
export const timelineService = {
  getCurrentWeek: async () => {
    const response = await apiClient.get('/ninemo/timeline'); // untyped
    const transformed = mapToLocalFormat(response.data);      // transformation
    cache.set('timeline', transformed);                        // caching
    return transformed;
  },
};
```

### 6. Screens Are Dumb Renderers
A screen component must not contain:
- `useState` for server data (use React Query / SWR hooks instead)
- Direct calls to `fetch`, `axios`, or any service
- Conditional logic based on raw API data (derive in the hook, pass result to screen)
- Any calculation or data transformation

```tsx
// Correct
function TimelineScreen() {
  const { data, isLoading, error } = useCurrentWeekTimeline();
  if (isLoading) return <LoadingSpinner />;
  if (error) return <ErrorView error={error} />;
  return <TimelineFeed data={data} />;
}

// Wrong
function TimelineScreen() {
  const [data, setData] = useState(null);
  useEffect(() => {
    fetch('/ninemo/timeline')
      .then(r => r.json())
      .then(d => setData({ ...d, weekLabel: `Week ${d.gestationalWeek}` })); // transform
  }, []);
  return <View>{data?.gestationalWeek > 12 ? <SecondTrimesterView /> : <FirstTrimesterView />}</View>; // logic
}
```

### 7. Navigation Routes Are Named Constants
Never use string literals for route names inline. All route names live in 
`src/navigation/routes.ts` as a const enum.

```ts
export const Routes = {
  Timeline: 'Timeline',
  SymptomLog: 'SymptomLog',
  VitalsWeight: 'VitalsWeight',
  VitalsBP: 'VitalsBP',
  KickCounter: 'KickCounter',
  ContractionTimer: 'ContractionTimer',
  SummaryCard: 'SummaryCard',
  DueDateClub: 'DueDateClub',
} as const;
```

### 8. No Offline-First Complexity
This app is online-first. Do not build:
- Local database sync (WatermelonDB, SQLite replication)
- Redux Persist for server data
- Background sync workers for health data

React Query's built-in stale-while-revalidate cache is sufficient. 
If a screen needs to work offline, discuss with the team before implementing.

---

## Migration Guardrails
These rules exist because this codebase will be migrated to Native Android.
The mapping when that happens:

| React Native | Native Android |
|---|---|
| `services/*.ts` | `*Repository.kt` |
| `hooks/use*.ts` | `*ViewModel.kt` |
| `screens/*.tsx` | `*Screen.kt` (Composable) |
| `store/*Slice.ts` | StateFlow in ViewModel |
| `types/api.ts` interfaces | Kotlin data classes |
| `navigation/routes.ts` | NavGraph destinations |

Every layer of this codebase has a direct Kotlin equivalent. 
Keep that mapping clean.

---

## What to Do When Unsure
- Business logic belongs on the backend. Always.
- If a type is missing, add it to `src/types/api.ts` before writing the feature.
- If a screen is growing complex, it needs a hook extracted.
- If a hook is making multiple API calls, it may need to be split or the 
  backend endpoint may need to aggregate the data.
