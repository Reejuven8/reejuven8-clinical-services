# NineMo — Remaining Work Reference

> ## ⚠️ SUPERSEDED — HISTORICAL ONLY (as of 2026-08-11)
>
> **Do not implement from this document, and do not cite it as an API contract.**
>
> Part 1 is a full React Native implementation spec. The RN app was retired in the native
> pivot and now lives in `archive/ninemo-frontend/`; the mobile client is
> `ninemo-mobile/` (Kotlin Multiplatform + Compose), phases F0–F6 already built.
>
> The DTOs below are **wrong**. They were written from prose rather than from the real
> Spring controllers, and every one of them was corrected during the KMP build — see
> IS-020, IS-022, IS-024, IS-026, IS-029, IS-030 in `Issue_Tracker.md`. Examples still
> visible here: `LoginRequest` carries a `password` (auth is OTP-only), `TokenResponse`
> claims `userId`/`role` the backend never returns, `SymptomLogRequest.symptoms` is typed
> `string[]` (really `{name,category,severity}` objects), and `DietFoodSafetyResponse`
> lists four safety ratings (there are three: `SAFE`/`CAUTION`/`AVOID`).
>
> **For current state read instead:**
> `Backend_Feature_Tracker.md` (what is built) · `Issue_Tracker.md` (what is broken)
> · `Cross_Platform_Strategy.md` (the pivot) · `UI_Design.md` (screen specs).
> For any contract, read the Spring controller/DTO source directly.
>
> Kept only as a record of the original plan.

---

> All backend Phases 0–7 complete. This document specifies everything left to build.
> Execute section by section.

---

## Part 1 — Frontend (`ninemo-frontend/`)

### 1.1 Project Setup

**`package.json`**
```json
{
  "name": "ninemo",
  "version": "1.0.0",
  "private": true,
  "scripts": {
    "android": "react-native run-android",
    "ios": "react-native run-ios",
    "start": "react-native start",
    "test": "jest",
    "lint": "eslint src --ext .ts,.tsx",
    "type-check": "tsc --noEmit"
  },
  "dependencies": {
    "react": "18.3.1",
    "react-native": "0.75.3",
    "@react-navigation/native": "^6.1.18",
    "@react-navigation/native-stack": "^6.11.0",
    "@react-navigation/bottom-tabs": "^6.6.1",
    "react-native-screens": "^3.34.0",
    "react-native-safe-area-context": "^4.11.0",
    "react-native-gesture-handler": "^2.19.0",
    "@tanstack/react-query": "^5.51.23",
    "@reduxjs/toolkit": "^2.2.7",
    "react-redux": "^9.1.2",
    "axios": "^1.7.7",
    "@react-native-async-storage/async-storage": "^2.0.0"
  },
  "devDependencies": {
    "@babel/core": "^7.24.0",
    "@react-native/babel-preset": "0.75.3",
    "@react-native/eslint-config": "0.75.3",
    "@react-native/metro-config": "0.75.3",
    "@react-native/typescript-config": "0.75.3",
    "@types/react": "^18.3.1",
    "eslint": "^8.57.0",
    "jest": "^29.7.0",
    "typescript": "5.3.3"
  },
  "jest": { "preset": "react-native" }
}
```

**`tsconfig.json`**
```json
{
  "extends": "@react-native/typescript-config/tsconfig.json",
  "compilerOptions": {
    "strict": true,
    "baseUrl": ".",
    "paths": { "@/*": ["src/*"] }
  },
  "include": ["src", "index.js", "App.tsx"]
}
```

**`babel.config.js`**
```js
module.exports = { presets: ['module:@react-native/babel-preset'] };
```

**`metro.config.js`**
```js
const { getDefaultConfig, mergeConfig } = require('@react-native/metro-config');
module.exports = mergeConfig(getDefaultConfig(__dirname), {});
```

**`app.json`**
```json
{ "name": "NineMo", "displayName": "NineMo" }
```

**`index.js`**
```js
import { AppRegistry } from 'react-native';
import App from './App';
import { name as appName } from './app.json';
AppRegistry.registerComponent(appName, () => App);
```

**`App.tsx`**
```tsx
import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Provider } from 'react-redux';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { store } from './src/store';
import { AppNavigator } from './src/navigation/AppNavigator';

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: 2, staleTime: 30_000 } },
});

export default function App() {
  return (
    <GestureHandlerRootView style={{ flex: 1 }}>
      <Provider store={store}>
        <QueryClientProvider client={queryClient}>
          <NavigationContainer>
            <AppNavigator />
          </NavigationContainer>
        </QueryClientProvider>
      </Provider>
    </GestureHandlerRootView>
  );
}
```

---

### 1.2 Types (`src/types/api.ts`)

All interfaces mirror backend DTO field names exactly. No renaming.

```ts
// ─── Envelope ────────────────────────────────────────────────────────────────
export interface ApiResponse<T> {
  status: string;
  data: T;
  error: null;
  metadata?: Record<string, unknown>;
}

export interface PagedResponse<T> {
  status: string;
  data: {
    content: T[];
    pagination: { page: number; size: number; totalElements: number; totalPages: number };
  };
}

// ─── Auth ────────────────────────────────────────────────────────────────────
export interface LoginRequest {
  phoneNumber: string;
  password: string;
}

export interface RegisterRequest {
  phoneNumber: string;
  password: string;
  fullName: string;
  role: 'PATIENT' | 'DOCTOR';
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  userId: string;
  role: 'PATIENT' | 'DOCTOR' | 'ADMIN';
}

// ─── Timeline ────────────────────────────────────────────────────────────────
export interface BabyDevelopment {
  sizeComparison: string;
  weightGrams: number;
  lengthCm: number;
  developmentHighlights: string[];
}

export interface Milestone {
  title: string;
  description: string;
  dueDate: string;
  category: string;
}

export interface DietTip {
  tip: string;
  foodItems: string[];
  avoidItems: string[];
}

export interface YogaRoutine {
  routineName: string;
  durationMinutes: number;
  exercises: string[];
}

export interface TimelineResponse {
  gestationalWeek: number;
  trimester: 1 | 2 | 3;
  babyDevelopment: BabyDevelopment;
  maternalChanges: string[];
  scheduledMilestones: Milestone[];
  dietTips: DietTip[];
  yogaRoutine: YogaRoutine | null;
}

// ─── Symptoms ────────────────────────────────────────────────────────────────
export interface VitalsAtLog {
  bpSystolic?: number;
  bpDiastolic?: number;
  heartRate?: number;
  temperature?: number;
  weight?: number;
}

export interface SymptomLogRequest {
  symptoms: string[];
  vitalsAtLog?: VitalsAtLog;
}

export interface SymptomLogResponse {
  id: string;
  patientId: string;
  gestationalWeekAtLog: number;
  symptoms: string[];
  vitalsAtLog?: VitalsAtLog;
  severityFlag: 'NORMAL' | 'WARNING' | 'CRITICAL';
  triageResult: string[];
  loggedAt: string;
}

// ─── Vitals ──────────────────────────────────────────────────────────────────
export interface VitalsLogRequest {
  vitalType: string;
  measurements: Record<string, number>;
  source?: string;
}

export interface VitalsLogResponse {
  id: string;
  patientId: string;
  vitalType: string;
  measurements: Record<string, number>;
  source: string;
  alertTriggered: boolean;
  loggedAt: string;
}

// ─── Kick Counter ─────────────────────────────────────────────────────────────
export interface KickCounterSessionResponse {
  id: string;
  patientId: string;
  sessionStart: string;
  sessionEnd: string | null;
  totalKicks: number;
  durationTo10KicksMinutes: number | null;
  kickTimestamps: string[];
  isConcerning: boolean;
  active: boolean;
}

// ─── Contraction Timer ────────────────────────────────────────────────────────
export interface Contraction {
  startTime: string;
  durationSeconds: number;
  intervalFromPreviousSeconds: number | null;
}

export interface ContractionSessionResponse {
  id: string;
  patientId: string;
  sessionStart: string;
  sessionEnd: string | null;
  contractions: Contraction[];
  totalContractions: number;
  averageIntervalSeconds: number | null;
  averageDurationSeconds: number | null;
  isLaborPattern: boolean;
  alertTriggered: boolean;
}

// ─── Summary Card ─────────────────────────────────────────────────────────────
export interface SummaryCardResponse {
  pregnancyProfileId: string;
  gestationalWeek: number;
  trimester: 1 | 2 | 3;
  edd: string;
  latestVitals: VitalsLogResponse[];
  lastSymptomLog: SymptomLogResponse | null;
  lastKickSession: KickCounterSessionResponse | null;
  highRiskFlags: string[];
}

// ─── Growth ──────────────────────────────────────────────────────────────────
export interface GrowthMeasurementRequest {
  ageInMonths: number;
  measurementDate: string;
  heightCm: number;
  weightKg: number;
  headCircumferenceCm?: number;
}

export interface GrowthMeasurementResponse {
  id: string;
  childId: string;
  ageInMonths: number;
  measurementDate: string;
  heightCm: number;
  weightKg: number;
  headCircumferenceCm: number | null;
  zScores: Record<string, number>;
  percentiles: Record<string, number>;
  alertFlags: string[];
  crossedPercentileLines: number;
}

// ─── Vaccination ──────────────────────────────────────────────────────────────
export type VaccinationStatus = 'PENDING' | 'COMPLETED' | 'SKIPPED' | 'OVERDUE';

export interface VaccinationRecordResponse {
  id: string;
  childId: string;
  vaccineName: string;
  vaccineCode: string;
  doseNumber: number;
  scheduledDate: string;
  administeredDate: string | null;
  status: VaccinationStatus;
  overdue: boolean;
}

// ─── Community ────────────────────────────────────────────────────────────────
export interface Channel {
  channelId: string;
  name: string;
  description: string;
  isDefault: boolean;
}

export interface ClubMember {
  userId: string;
  alias: string;
  joinedAt: string;
}

export interface ClubResponse {
  id: string;
  clubName: string;
  dueDateMonth: string;
  memberCount: number;
  members: ClubMember[];
  channels: Channel[];
}

export interface JoinClubRequest {
  dueDateMonth: string;
  alias: string;
}

export interface SendMessageRequest {
  senderId: string;
  senderAlias: string;
  messageBody: string;
  messageType?: string;
  replyToMessageId?: string;
  imageUrl?: string;
}

export interface ChatMessageResponse {
  id: string;
  clubId: string;
  channelId: string;
  senderId: string;
  senderAlias: string;
  messageType: string;
  messageBody: string;
  replyToMessageId: string | null;
  imageUrl: string | null;
  isDeleted: boolean;
  sentAt: string;
}

// ─── Content ──────────────────────────────────────────────────────────────────
export interface ContentArticle {
  id: string;
  title: string;
  body: string;
  summary: string;
  category: string;
  tags: string[];
  gestationalWeeks: number[];
  publishedAt: string;
}

// ─── Diet ─────────────────────────────────────────────────────────────────────
export interface DietFoodSafetyResponse {
  id: string;
  ingredientName: string;
  ingredientNameHindi: string | null;
  safetyRating: 'SAFE' | 'MODERATE' | 'AVOID' | 'UNSAFE';
  description: string;
  trimesterTags: string[];
  categories: string[];
}
```

---

### 1.3 API Client (`src/services/apiClient.ts`)

```ts
import axios from 'axios';
import AsyncStorage from '@react-native-async-storage/async-storage';

const BASE_URL = __DEV__ ? 'http://localhost:8080/api/v1' : 'https://api.reejuven8.com/api/v1';

export const apiClient = axios.create({ baseURL: BASE_URL, timeout: 15_000 });

apiClient.interceptors.request.use(async config => {
  const token = await AsyncStorage.getItem('accessToken');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

apiClient.interceptors.response.use(
  res => res,
  async error => {
    if (error.response?.status === 401) {
      await AsyncStorage.multiRemove(['accessToken', 'refreshToken', 'userId', 'role']);
    }
    return Promise.reject(error);
  },
);
```

---

### 1.4 Services (`src/services/`)

**`authService.ts`**
```ts
import { apiClient } from './apiClient';
import { ApiResponse, LoginRequest, RegisterRequest, TokenResponse } from '../types/api';

export const authService = {
  login: async (req: LoginRequest): Promise<TokenResponse> => {
    const res = await apiClient.post<ApiResponse<TokenResponse>>('/identity/auth/login', req);
    return res.data.data;
  },
  register: async (req: RegisterRequest): Promise<TokenResponse> => {
    const res = await apiClient.post<ApiResponse<TokenResponse>>('/identity/auth/register', req);
    return res.data.data;
  },
  refresh: async (refreshToken: string): Promise<TokenResponse> => {
    const res = await apiClient.post<ApiResponse<TokenResponse>>('/identity/auth/refresh', { refreshToken });
    return res.data.data;
  },
  logout: async (): Promise<void> => {
    await apiClient.post('/identity/auth/logout');
  },
};
```

**`timelineService.ts`**
```ts
import { apiClient } from './apiClient';
import { ApiResponse, TimelineResponse } from '../types/api';

export const timelineService = {
  getCurrent: async (): Promise<TimelineResponse> => {
    const res = await apiClient.get<ApiResponse<TimelineResponse>>('/ninemo/timeline/current');
    return res.data.data;
  },
  getWeek: async (week: number): Promise<TimelineResponse> => {
    const res = await apiClient.get<ApiResponse<TimelineResponse>>(`/ninemo/timeline/week/${week}`);
    return res.data.data;
  },
};
```

**`symptomService.ts`**
```ts
import { apiClient } from './apiClient';
import { ApiResponse, SymptomLogRequest, SymptomLogResponse } from '../types/api';

export const symptomService = {
  log: async (req: SymptomLogRequest): Promise<SymptomLogResponse> => {
    const res = await apiClient.post<ApiResponse<SymptomLogResponse>>('/ninemo/symptoms', req);
    return res.data.data;
  },
  getHistory: async (): Promise<SymptomLogResponse[]> => {
    const res = await apiClient.get<ApiResponse<SymptomLogResponse[]>>('/ninemo/symptoms');
    return res.data.data;
  },
};
```

**`vitalsService.ts`**
```ts
import { apiClient } from './apiClient';
import { ApiResponse, VitalsLogRequest, VitalsLogResponse } from '../types/api';

export const vitalsService = {
  log: async (req: VitalsLogRequest): Promise<VitalsLogResponse> => {
    const res = await apiClient.post<ApiResponse<VitalsLogResponse>>('/ninemo/vitals', req);
    return res.data.data;
  },
  getByType: async (vitalType: string): Promise<VitalsLogResponse[]> => {
    const res = await apiClient.get<ApiResponse<VitalsLogResponse[]>>(`/ninemo/vitals/${vitalType}`);
    return res.data.data;
  },
};
```

**`kickCounterService.ts`**
```ts
import { apiClient } from './apiClient';
import { ApiResponse, KickCounterSessionResponse } from '../types/api';

export const kickCounterService = {
  startSession: async (): Promise<KickCounterSessionResponse> => {
    const res = await apiClient.post<ApiResponse<KickCounterSessionResponse>>('/ninemo/kick-counter/sessions');
    return res.data.data;
  },
  recordKick: async (sessionId: string): Promise<KickCounterSessionResponse> => {
    const res = await apiClient.put<ApiResponse<KickCounterSessionResponse>>(
      `/ninemo/kick-counter/sessions/${sessionId}/kick`,
    );
    return res.data.data;
  },
  endSession: async (sessionId: string): Promise<KickCounterSessionResponse> => {
    const res = await apiClient.put<ApiResponse<KickCounterSessionResponse>>(
      `/ninemo/kick-counter/sessions/${sessionId}/end`,
    );
    return res.data.data;
  },
};
```

**`contractionService.ts`**
```ts
import { apiClient } from './apiClient';
import { ApiResponse, ContractionSessionResponse } from '../types/api';

export const contractionService = {
  startSession: async (): Promise<ContractionSessionResponse> => {
    const res = await apiClient.post<ApiResponse<ContractionSessionResponse>>('/ninemo/contractions/sessions');
    return res.data.data;
  },
  recordContraction: async (sessionId: string): Promise<ContractionSessionResponse> => {
    const res = await apiClient.put<ApiResponse<ContractionSessionResponse>>(
      `/ninemo/contractions/sessions/${sessionId}/contraction`,
    );
    return res.data.data;
  },
  endSession: async (sessionId: string): Promise<ContractionSessionResponse> => {
    const res = await apiClient.put<ApiResponse<ContractionSessionResponse>>(
      `/ninemo/contractions/sessions/${sessionId}/end`,
    );
    return res.data.data;
  },
};
```

**`summaryCardService.ts`**
```ts
import { apiClient } from './apiClient';
import { ApiResponse, SummaryCardResponse } from '../types/api';

export const summaryCardService = {
  get: async (patientId: string): Promise<SummaryCardResponse> => {
    const res = await apiClient.get<ApiResponse<SummaryCardResponse>>(`/ninemo/summary-card/${patientId}`);
    return res.data.data;
  },
};
```

**`growthService.ts`**
```ts
import { apiClient } from './apiClient';
import { ApiResponse, GrowthMeasurementRequest, GrowthMeasurementResponse } from '../types/api';

export const growthService = {
  record: async (childId: string, req: GrowthMeasurementRequest): Promise<GrowthMeasurementResponse> => {
    const res = await apiClient.post<ApiResponse<GrowthMeasurementResponse>>(
      `/ninemo/growth/children/${childId}/measurements`,
      req,
    );
    return res.data.data;
  },
  getHistory: async (childId: string): Promise<GrowthMeasurementResponse[]> => {
    const res = await apiClient.get<ApiResponse<GrowthMeasurementResponse[]>>(
      `/ninemo/growth/children/${childId}/measurements`,
    );
    return res.data.data;
  },
};
```

**`vaccinationService.ts`**
```ts
import { apiClient } from './apiClient';
import { ApiResponse, VaccinationRecordResponse } from '../types/api';

export const vaccinationService = {
  getSchedule: async (childId: string): Promise<VaccinationRecordResponse[]> => {
    const res = await apiClient.get<ApiResponse<VaccinationRecordResponse[]>>(
      `/ninemo/vaccinations/children/${childId}/schedule`,
    );
    return res.data.data;
  },
  markCompleted: async (vaccinationId: string, administeredDate: string, administeredBy: string): Promise<VaccinationRecordResponse> => {
    const res = await apiClient.put<ApiResponse<VaccinationRecordResponse>>(
      `/ninemo/vaccinations/${vaccinationId}/mark-completed`,
      null,
      { params: { administeredDate, administeredBy } },
    );
    return res.data.data;
  },
};
```

**`communityService.ts`**
```ts
import { apiClient } from './apiClient';
import {
  ApiResponse, PagedResponse,
  ClubResponse, JoinClubRequest, ChatMessageResponse, SendMessageRequest,
} from '../types/api';

export const communityService = {
  joinClub: async (req: JoinClubRequest): Promise<ClubResponse> => {
    const res = await apiClient.post<ApiResponse<ClubResponse>>('/community/clubs/join', req);
    return res.data.data;
  },
  getMyClubs: async (): Promise<ClubResponse[]> => {
    const res = await apiClient.get<ApiResponse<ClubResponse[]>>('/community/clubs');
    return res.data.data;
  },
  getMessages: async (clubId: string, channelId: string, page = 0): Promise<PagedResponse<ChatMessageResponse>> => {
    const res = await apiClient.get<PagedResponse<ChatMessageResponse>>(
      `/community/clubs/${clubId}/channels/${channelId}/messages`,
      { params: { page, size: 50 } },
    );
    return res.data;
  },
};
```

**`contentService.ts`**
```ts
import { apiClient } from './apiClient';
import { ApiResponse, ContentArticle } from '../types/api';

export const contentService = {
  listPublished: async (): Promise<ContentArticle[]> => {
    const res = await apiClient.get<ApiResponse<ContentArticle[]>>('/community/content');
    return res.data.data;
  },
  listByWeek: async (week: number): Promise<ContentArticle[]> => {
    const res = await apiClient.get<ApiResponse<ContentArticle[]>>(`/community/content/week/${week}`);
    return res.data.data;
  },
};
```

---

### 1.5 Store (`src/store/`)

**`src/store/index.ts`**
```ts
import { configureStore } from '@reduxjs/toolkit';
import authReducer from './authSlice';
import uiReducer from './uiSlice';

export const store = configureStore({
  reducer: { auth: authReducer, ui: uiReducer },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
```

**`src/store/authSlice.ts`**
```ts
import { createSlice, PayloadAction } from '@reduxjs/toolkit';

interface AuthState {
  userId: string | null;
  role: 'PATIENT' | 'DOCTOR' | 'ADMIN' | null;
  isAuthenticated: boolean;
}

const initialState: AuthState = { userId: null, role: null, isAuthenticated: false };

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setCredentials: (state, action: PayloadAction<{ userId: string; role: AuthState['role'] }>) => {
      state.userId = action.payload.userId;
      state.role = action.payload.role;
      state.isAuthenticated = true;
    },
    clearCredentials: state => {
      state.userId = null;
      state.role = null;
      state.isAuthenticated = false;
    },
  },
});

export const { setCredentials, clearCredentials } = authSlice.actions;
export default authSlice.reducer;
```

**`src/store/uiSlice.ts`**
```ts
import { createSlice, PayloadAction } from '@reduxjs/toolkit';

interface UiState {
  activeChildId: string | null;
  activePregnancyId: string | null;
}

const initialState: UiState = { activeChildId: null, activePregnancyId: null };

const uiSlice = createSlice({
  name: 'ui',
  initialState,
  reducers: {
    setActiveChildId: (state, action: PayloadAction<string>) => { state.activeChildId = action.payload; },
    setActivePregnancyId: (state, action: PayloadAction<string>) => { state.activePregnancyId = action.payload; },
  },
});

export const { setActiveChildId, setActivePregnancyId } = uiSlice.actions;
export default uiSlice.reducer;
```

---

### 1.6 Navigation (`src/navigation/`)

**`src/navigation/routes.ts`**
```ts
export const Routes = {
  // Auth stack
  Login: 'Login',
  Register: 'Register',
  // Main tabs
  Timeline: 'Timeline',
  SummaryCard: 'SummaryCard',
  DueDateClub: 'DueDateClub',
  // Nested antenatal
  SymptomLog: 'SymptomLog',
  VitalsWeight: 'VitalsWeight',
  VitalsBP: 'VitalsBP',
  KickCounter: 'KickCounter',
  ContractionTimer: 'ContractionTimer',
  // Nested pediatric
  GrowthChart: 'GrowthChart',
  Vaccination: 'Vaccination',
  // Content
  ContentFeed: 'ContentFeed',
} as const;

export type RouteName = (typeof Routes)[keyof typeof Routes];
```

**`src/navigation/AppNavigator.tsx`**
```tsx
import React from 'react';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { useSelector } from 'react-redux';
import { RootState } from '../store';
import { AuthNavigator } from './AuthNavigator';
import { MainNavigator } from './MainNavigator';
import { Routes } from './routes';

const Stack = createNativeStackNavigator();

export function AppNavigator() {
  const isAuthenticated = useSelector((state: RootState) => state.auth.isAuthenticated);
  return (
    <Stack.Navigator screenOptions={{ headerShown: false }}>
      {isAuthenticated ? (
        <Stack.Screen name="Main" component={MainNavigator} />
      ) : (
        <Stack.Screen name="Auth" component={AuthNavigator} />
      )}
    </Stack.Navigator>
  );
}
```

**`src/navigation/AuthNavigator.tsx`**
```tsx
import React from 'react';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { Routes } from './routes';
import { LoginScreen } from '../screens/auth/LoginScreen';
import { RegisterScreen } from '../screens/auth/RegisterScreen';

const Stack = createNativeStackNavigator();

export function AuthNavigator() {
  return (
    <Stack.Navigator>
      <Stack.Screen name={Routes.Login} component={LoginScreen} options={{ title: 'Sign In' }} />
      <Stack.Screen name={Routes.Register} component={RegisterScreen} options={{ title: 'Create Account' }} />
    </Stack.Navigator>
  );
}
```

**`src/navigation/MainNavigator.tsx`**
```tsx
import React from 'react';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { Routes } from './routes';
import { TimelineScreen } from '../screens/TimelineScreen';
import { SummaryCardScreen } from '../screens/SummaryCardScreen';
import { DueDateClubScreen } from '../screens/DueDateClubScreen';
import { SymptomLogScreen } from '../screens/SymptomLogScreen';
import { VitalsWeightScreen } from '../screens/VitalsWeightScreen';
import { VitalsBPScreen } from '../screens/VitalsBPScreen';
import { KickCounterScreen } from '../screens/KickCounterScreen';
import { ContractionTimerScreen } from '../screens/ContractionTimerScreen';
import { GrowthChartScreen } from '../screens/GrowthChartScreen';
import { VaccinationScreen } from '../screens/VaccinationScreen';

const Tab = createBottomTabNavigator();
const Stack = createNativeStackNavigator();

function HomeStack() {
  return (
    <Stack.Navigator>
      <Stack.Screen name={Routes.Timeline} component={TimelineScreen} options={{ title: 'This Week' }} />
      <Stack.Screen name={Routes.SymptomLog} component={SymptomLogScreen} options={{ title: 'Log Symptoms' }} />
      <Stack.Screen name={Routes.VitalsWeight} component={VitalsWeightScreen} options={{ title: 'Weight' }} />
      <Stack.Screen name={Routes.VitalsBP} component={VitalsBPScreen} options={{ title: 'Blood Pressure' }} />
      <Stack.Screen name={Routes.KickCounter} component={KickCounterScreen} options={{ title: 'Kick Counter' }} />
      <Stack.Screen name={Routes.ContractionTimer} component={ContractionTimerScreen} options={{ title: 'Contractions' }} />
      <Stack.Screen name={Routes.GrowthChart} component={GrowthChartScreen} options={{ title: 'Growth Chart' }} />
      <Stack.Screen name={Routes.Vaccination} component={VaccinationScreen} options={{ title: 'Vaccinations' }} />
    </Stack.Navigator>
  );
}

export function MainNavigator() {
  return (
    <Tab.Navigator>
      <Tab.Screen name="Home" component={HomeStack} options={{ headerShown: false, tabBarLabel: 'Home' }} />
      <Tab.Screen name={Routes.SummaryCard} component={SummaryCardScreen} options={{ title: 'Summary' }} />
      <Tab.Screen name={Routes.DueDateClub} component={DueDateClubScreen} options={{ title: 'Community' }} />
    </Tab.Navigator>
  );
}
```

---

### 1.7 Hooks (`src/hooks/`)

**`src/hooks/useAuth.ts`**
```ts
import { useMutation } from '@tanstack/react-query';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { useDispatch } from 'react-redux';
import { authService } from '../services/authService';
import { setCredentials, clearCredentials } from '../store/authSlice';
import { LoginRequest, RegisterRequest } from '../types/api';

export function useLogin() {
  const dispatch = useDispatch();
  return useMutation({
    mutationFn: (req: LoginRequest) => authService.login(req),
    onSuccess: async token => {
      await AsyncStorage.multiSet([
        ['accessToken', token.accessToken],
        ['refreshToken', token.refreshToken],
        ['userId', token.userId],
        ['role', token.role],
      ]);
      dispatch(setCredentials({ userId: token.userId, role: token.role }));
    },
  });
}

export function useRegister() {
  const dispatch = useDispatch();
  return useMutation({
    mutationFn: (req: RegisterRequest) => authService.register(req),
    onSuccess: async token => {
      await AsyncStorage.multiSet([
        ['accessToken', token.accessToken],
        ['refreshToken', token.refreshToken],
        ['userId', token.userId],
        ['role', token.role],
      ]);
      dispatch(setCredentials({ userId: token.userId, role: token.role }));
    },
  });
}

export function useLogout() {
  const dispatch = useDispatch();
  return useMutation({
    mutationFn: () => authService.logout(),
    onSettled: async () => {
      await AsyncStorage.multiRemove(['accessToken', 'refreshToken', 'userId', 'role']);
      dispatch(clearCredentials());
    },
  });
}
```

**`src/hooks/useTimeline.ts`**
```ts
import { useQuery } from '@tanstack/react-query';
import { timelineService } from '../services/timelineService';

export function useCurrentWeekTimeline() {
  return useQuery({ queryKey: ['timeline', 'current'], queryFn: timelineService.getCurrent });
}

export function useWeekTimeline(week: number) {
  return useQuery({ queryKey: ['timeline', week], queryFn: () => timelineService.getWeek(week) });
}
```

**`src/hooks/useSymptomLog.ts`**
```ts
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { symptomService } from '../services/symptomService';
import { SymptomLogRequest } from '../types/api';

export function useSymptomHistory() {
  return useQuery({ queryKey: ['symptoms'], queryFn: symptomService.getHistory });
}

export function useLogSymptom() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (req: SymptomLogRequest) => symptomService.log(req),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['symptoms'] }),
  });
}
```

**`src/hooks/useVitals.ts`**
```ts
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { vitalsService } from '../services/vitalsService';
import { VitalsLogRequest } from '../types/api';

export function useVitalsByType(vitalType: string) {
  return useQuery({ queryKey: ['vitals', vitalType], queryFn: () => vitalsService.getByType(vitalType) });
}

export function useLogVitals() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (req: VitalsLogRequest) => vitalsService.log(req),
    onSuccess: (_data, req) => qc.invalidateQueries({ queryKey: ['vitals', req.vitalType] }),
  });
}
```

**`src/hooks/useKickCounter.ts`**
```ts
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { kickCounterService } from '../services/kickCounterService';

export function useStartKickSession() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: kickCounterService.startSession,
    onSuccess: data => qc.setQueryData(['kickSession', 'active'], data),
  });
}

export function useRecordKick(sessionId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => kickCounterService.recordKick(sessionId),
    onSuccess: data => qc.setQueryData(['kickSession', 'active'], data),
  });
}

export function useEndKickSession(sessionId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => kickCounterService.endSession(sessionId),
    onSuccess: () => qc.removeQueries({ queryKey: ['kickSession', 'active'] }),
  });
}
```

**`src/hooks/useContractionTimer.ts`**
```ts
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { contractionService } from '../services/contractionService';

export function useStartContractionSession() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: contractionService.startSession,
    onSuccess: data => qc.setQueryData(['contractionSession', 'active'], data),
  });
}

export function useRecordContraction(sessionId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => contractionService.recordContraction(sessionId),
    onSuccess: data => qc.setQueryData(['contractionSession', 'active'], data),
  });
}

export function useEndContractionSession(sessionId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => contractionService.endSession(sessionId),
    onSuccess: () => qc.removeQueries({ queryKey: ['contractionSession', 'active'] }),
  });
}
```

**`src/hooks/useSummaryCard.ts`**
```ts
import { useQuery } from '@tanstack/react-query';
import { useSelector } from 'react-redux';
import { RootState } from '../store';
import { summaryCardService } from '../services/summaryCardService';

export function useSummaryCard() {
  const userId = useSelector((state: RootState) => state.auth.userId);
  return useQuery({
    queryKey: ['summaryCard', userId],
    queryFn: () => summaryCardService.get(userId!),
    enabled: !!userId,
  });
}
```

**`src/hooks/useGrowthChart.ts`**
```ts
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { growthService } from '../services/growthService';
import { GrowthMeasurementRequest } from '../types/api';

export function useGrowthHistory(childId: string) {
  return useQuery({ queryKey: ['growth', childId], queryFn: () => growthService.getHistory(childId) });
}

export function useRecordGrowth(childId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (req: GrowthMeasurementRequest) => growthService.record(childId, req),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['growth', childId] }),
  });
}
```

**`src/hooks/useVaccination.ts`**
```ts
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { vaccinationService } from '../services/vaccinationService';

export function useVaccinationSchedule(childId: string) {
  return useQuery({ queryKey: ['vaccination', childId], queryFn: () => vaccinationService.getSchedule(childId) });
}

export function useMarkVaccineCompleted(childId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, date, by }: { id: string; date: string; by: string }) =>
      vaccinationService.markCompleted(id, date, by),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['vaccination', childId] }),
  });
}
```

**`src/hooks/useCommunity.ts`**
```ts
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { communityService } from '../services/communityService';
import { JoinClubRequest } from '../types/api';

export function useMyClubs() {
  return useQuery({ queryKey: ['clubs'], queryFn: communityService.getMyClubs });
}

export function useJoinClub() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (req: JoinClubRequest) => communityService.joinClub(req),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['clubs'] }),
  });
}

export function useChannelMessages(clubId: string, channelId: string) {
  return useQuery({
    queryKey: ['messages', clubId, channelId],
    queryFn: () => communityService.getMessages(clubId, channelId),
  });
}
```

---

### 1.8 Shared Components (`src/components/`)

**`src/components/LoadingSpinner.tsx`**
```tsx
import React from 'react';
import { ActivityIndicator, StyleSheet, View } from 'react-native';

export function LoadingSpinner() {
  return (
    <View style={styles.container}>
      <ActivityIndicator size="large" color="#E91E8C" />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, justifyContent: 'center', alignItems: 'center' },
});
```

**`src/components/ErrorView.tsx`**
```tsx
import React from 'react';
import { StyleSheet, Text, TouchableOpacity, View } from 'react-native';

interface Props {
  error: Error;
  onRetry?: () => void;
}

export function ErrorView({ error, onRetry }: Props) {
  return (
    <View style={styles.container}>
      <Text style={styles.message}>{error.message}</Text>
      {onRetry && (
        <TouchableOpacity style={styles.button} onPress={onRetry}>
          <Text style={styles.buttonText}>Retry</Text>
        </TouchableOpacity>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, justifyContent: 'center', alignItems: 'center', padding: 24 },
  message: { fontSize: 16, color: '#B00020', textAlign: 'center', marginBottom: 16 },
  button: { backgroundColor: '#E91E8C', borderRadius: 8, paddingHorizontal: 24, paddingVertical: 12 },
  buttonText: { color: '#fff', fontWeight: '600' },
});
```

---

### 1.9 Screens (`src/screens/`)

**`src/screens/auth/LoginScreen.tsx`**
```tsx
import React, { useState } from 'react';
import { Alert, StyleSheet, Text, TextInput, TouchableOpacity, View } from 'react-native';
import { useLogin } from '../../hooks/useAuth';

export function LoginScreen() {
  const [phoneNumber, setPhoneNumber] = useState('');
  const [password, setPassword] = useState('');
  const { mutate: login, isPending } = useLogin();

  const handleLogin = () => {
    login(
      { phoneNumber, password },
      { onError: e => Alert.alert('Login failed', (e as Error).message) },
    );
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>NineMo</Text>
      <TextInput
        style={styles.input}
        placeholder="Phone number"
        keyboardType="phone-pad"
        value={phoneNumber}
        onChangeText={setPhoneNumber}
      />
      <TextInput
        style={styles.input}
        placeholder="Password"
        secureTextEntry
        value={password}
        onChangeText={setPassword}
      />
      <TouchableOpacity style={styles.button} onPress={handleLogin} disabled={isPending}>
        <Text style={styles.buttonText}>{isPending ? 'Signing in…' : 'Sign In'}</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, justifyContent: 'center', padding: 24, backgroundColor: '#fff' },
  title: { fontSize: 32, fontWeight: '700', color: '#E91E8C', textAlign: 'center', marginBottom: 40 },
  input: { borderWidth: 1, borderColor: '#ddd', borderRadius: 8, padding: 12, marginBottom: 16, fontSize: 16 },
  button: { backgroundColor: '#E91E8C', borderRadius: 8, padding: 16, alignItems: 'center' },
  buttonText: { color: '#fff', fontSize: 16, fontWeight: '600' },
});
```

**`src/screens/auth/RegisterScreen.tsx`**
```tsx
import React, { useState } from 'react';
import { Alert, StyleSheet, Text, TextInput, TouchableOpacity, View } from 'react-native';
import { useRegister } from '../../hooks/useAuth';

export function RegisterScreen() {
  const [fullName, setFullName] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [password, setPassword] = useState('');
  const { mutate: register, isPending } = useRegister();

  const handleRegister = () => {
    register(
      { fullName, phoneNumber, password, role: 'PATIENT' },
      { onError: e => Alert.alert('Registration failed', (e as Error).message) },
    );
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Create Account</Text>
      <TextInput style={styles.input} placeholder="Full name" value={fullName} onChangeText={setFullName} />
      <TextInput style={styles.input} placeholder="Phone number" keyboardType="phone-pad" value={phoneNumber} onChangeText={setPhoneNumber} />
      <TextInput style={styles.input} placeholder="Password" secureTextEntry value={password} onChangeText={setPassword} />
      <TouchableOpacity style={styles.button} onPress={handleRegister} disabled={isPending}>
        <Text style={styles.buttonText}>{isPending ? 'Creating account…' : 'Register'}</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, justifyContent: 'center', padding: 24, backgroundColor: '#fff' },
  title: { fontSize: 28, fontWeight: '700', color: '#E91E8C', textAlign: 'center', marginBottom: 32 },
  input: { borderWidth: 1, borderColor: '#ddd', borderRadius: 8, padding: 12, marginBottom: 16, fontSize: 16 },
  button: { backgroundColor: '#E91E8C', borderRadius: 8, padding: 16, alignItems: 'center' },
  buttonText: { color: '#fff', fontSize: 16, fontWeight: '600' },
});
```

**`src/screens/TimelineScreen.tsx`**
```tsx
import React from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { useCurrentWeekTimeline } from '../hooks/useTimeline';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { ErrorView } from '../components/ErrorView';

export function TimelineScreen() {
  const { data, isLoading, error, refetch } = useCurrentWeekTimeline();

  if (isLoading) return <LoadingSpinner />;
  if (error) return <ErrorView error={error as Error} onRetry={refetch} />;

  return (
    <ScrollView style={styles.container}>
      <Text style={styles.week}>Week {data?.gestationalWeek}</Text>
      <Text style={styles.trimester}>Trimester {data?.trimester}</Text>
      <Text style={styles.sectionTitle}>Baby this week</Text>
      <Text style={styles.body}>{data?.babyDevelopment.sizeComparison}</Text>
      {data?.babyDevelopment.developmentHighlights.map((h, i) => (
        <Text key={i} style={styles.bullet}>• {h}</Text>
      ))}
      <Text style={styles.sectionTitle}>Maternal changes</Text>
      {data?.maternalChanges.map((c, i) => (
        <Text key={i} style={styles.bullet}>• {c}</Text>
      ))}
      <Text style={styles.sectionTitle}>Milestones</Text>
      {data?.scheduledMilestones.map((m, i) => (
        <View key={i} style={styles.milestoneCard}>
          <Text style={styles.milestoneTitle}>{m.title}</Text>
          <Text style={styles.body}>{m.description}</Text>
        </View>
      ))}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff', padding: 16 },
  week: { fontSize: 32, fontWeight: '700', color: '#E91E8C' },
  trimester: { fontSize: 14, color: '#666', marginBottom: 24 },
  sectionTitle: { fontSize: 18, fontWeight: '600', color: '#333', marginTop: 20, marginBottom: 8 },
  body: { fontSize: 15, color: '#444', lineHeight: 22 },
  bullet: { fontSize: 15, color: '#444', lineHeight: 22 },
  milestoneCard: { backgroundColor: '#FFF0F7', borderRadius: 8, padding: 12, marginBottom: 8 },
  milestoneTitle: { fontWeight: '600', color: '#E91E8C', marginBottom: 4 },
});
```

**`src/screens/SymptomLogScreen.tsx`**
```tsx
import React, { useState } from 'react';
import { Alert, ScrollView, StyleSheet, Text, TextInput, TouchableOpacity, View } from 'react-native';
import { useLogSymptom } from '../hooks/useSymptomLog';

const COMMON_SYMPTOMS = ['Headache', 'Nausea', 'Swelling', 'Back pain', 'Fatigue', 'Reduced fetal movement', 'Blurred vision', 'Contractions'];

export function SymptomLogScreen() {
  const [selected, setSelected] = useState<string[]>([]);
  const [custom, setCustom] = useState('');
  const { mutate: logSymptom, isPending } = useLogSymptom();

  const toggle = (s: string) =>
    setSelected(prev => prev.includes(s) ? prev.filter(x => x !== s) : [...prev, s]);

  const handleSubmit = () => {
    const symptoms = custom.trim() ? [...selected, custom.trim()] : selected;
    if (!symptoms.length) { Alert.alert('Select at least one symptom'); return; }
    logSymptom(
      { symptoms },
      {
        onSuccess: data => Alert.alert(
          data.severityFlag === 'CRITICAL' ? '⚠️ Critical' : data.severityFlag === 'WARNING' ? 'Warning' : 'Logged',
          data.triageResult.join('\n') || 'Symptoms recorded.',
        ),
        onError: e => Alert.alert('Error', (e as Error).message),
      },
    );
  };

  return (
    <ScrollView style={styles.container}>
      <Text style={styles.label}>Select symptoms</Text>
      <View style={styles.chips}>
        {COMMON_SYMPTOMS.map(s => (
          <TouchableOpacity
            key={s}
            style={[styles.chip, selected.includes(s) && styles.chipActive]}
            onPress={() => toggle(s)}
          >
            <Text style={[styles.chipText, selected.includes(s) && styles.chipTextActive]}>{s}</Text>
          </TouchableOpacity>
        ))}
      </View>
      <TextInput
        style={styles.input}
        placeholder="Other symptom…"
        value={custom}
        onChangeText={setCustom}
      />
      <TouchableOpacity style={styles.button} onPress={handleSubmit} disabled={isPending}>
        <Text style={styles.buttonText}>{isPending ? 'Logging…' : 'Log Symptoms'}</Text>
      </TouchableOpacity>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff', padding: 16 },
  label: { fontSize: 16, fontWeight: '600', color: '#333', marginBottom: 12 },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginBottom: 16 },
  chip: { borderWidth: 1, borderColor: '#ddd', borderRadius: 20, paddingHorizontal: 14, paddingVertical: 8 },
  chipActive: { backgroundColor: '#E91E8C', borderColor: '#E91E8C' },
  chipText: { fontSize: 14, color: '#333' },
  chipTextActive: { color: '#fff' },
  input: { borderWidth: 1, borderColor: '#ddd', borderRadius: 8, padding: 12, marginBottom: 16, fontSize: 15 },
  button: { backgroundColor: '#E91E8C', borderRadius: 8, padding: 16, alignItems: 'center' },
  buttonText: { color: '#fff', fontWeight: '600', fontSize: 16 },
});
```

**`src/screens/VitalsWeightScreen.tsx`**
```tsx
import React, { useState } from 'react';
import { Alert, StyleSheet, Text, TextInput, TouchableOpacity, View } from 'react-native';
import { useLogVitals, useVitalsByType } from '../hooks/useVitals';
import { LoadingSpinner } from '../components/LoadingSpinner';

export function VitalsWeightScreen() {
  const [weight, setWeight] = useState('');
  const { data: history, isLoading } = useVitalsByType('WEIGHT');
  const { mutate: logVital, isPending } = useLogVitals();

  const handleLog = () => {
    const kg = parseFloat(weight);
    if (isNaN(kg)) { Alert.alert('Enter a valid weight'); return; }
    logVital(
      { vitalType: 'WEIGHT', measurements: { weightKg: kg } },
      { onSuccess: () => { Alert.alert('Weight logged'); setWeight(''); }, onError: e => Alert.alert('Error', (e as Error).message) },
    );
  };

  if (isLoading) return <LoadingSpinner />;

  return (
    <View style={styles.container}>
      <TextInput
        style={styles.input}
        placeholder="Weight in kg"
        keyboardType="decimal-pad"
        value={weight}
        onChangeText={setWeight}
      />
      <TouchableOpacity style={styles.button} onPress={handleLog} disabled={isPending}>
        <Text style={styles.buttonText}>{isPending ? 'Saving…' : 'Log Weight'}</Text>
      </TouchableOpacity>
      {history?.map((v, i) => (
        <View key={i} style={styles.row}>
          <Text style={styles.rowDate}>{new Date(v.loggedAt).toLocaleDateString()}</Text>
          <Text style={styles.rowValue}>{v.measurements.weightKg} kg</Text>
          {v.alertTriggered && <Text style={styles.alert}>⚠️</Text>}
        </View>
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff', padding: 16 },
  input: { borderWidth: 1, borderColor: '#ddd', borderRadius: 8, padding: 12, marginBottom: 12, fontSize: 16 },
  button: { backgroundColor: '#E91E8C', borderRadius: 8, padding: 16, alignItems: 'center', marginBottom: 24 },
  buttonText: { color: '#fff', fontWeight: '600', fontSize: 16 },
  row: { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 10, borderBottomWidth: 1, borderColor: '#eee' },
  rowDate: { color: '#666', fontSize: 14 },
  rowValue: { fontWeight: '600', color: '#333' },
  alert: { fontSize: 14 },
});
```

**`src/screens/VitalsBPScreen.tsx`**
```tsx
import React, { useState } from 'react';
import { Alert, StyleSheet, Text, TextInput, TouchableOpacity, View } from 'react-native';
import { useLogVitals, useVitalsByType } from '../hooks/useVitals';
import { LoadingSpinner } from '../components/LoadingSpinner';

export function VitalsBPScreen() {
  const [systolic, setSystolic] = useState('');
  const [diastolic, setDiastolic] = useState('');
  const { data: history, isLoading } = useVitalsByType('BLOOD_PRESSURE');
  const { mutate: logVital, isPending } = useLogVitals();

  const handleLog = () => {
    const s = parseInt(systolic, 10);
    const d = parseInt(diastolic, 10);
    if (isNaN(s) || isNaN(d)) { Alert.alert('Enter valid BP values'); return; }
    logVital(
      { vitalType: 'BLOOD_PRESSURE', measurements: { bpSystolic: s, bpDiastolic: d } },
      { onSuccess: () => { Alert.alert('BP logged'); setSystolic(''); setDiastolic(''); }, onError: e => Alert.alert('Error', (e as Error).message) },
    );
  };

  if (isLoading) return <LoadingSpinner />;

  return (
    <View style={styles.container}>
      <View style={styles.row}>
        <TextInput style={[styles.input, { flex: 1, marginRight: 8 }]} placeholder="Systolic" keyboardType="number-pad" value={systolic} onChangeText={setSystolic} />
        <TextInput style={[styles.input, { flex: 1 }]} placeholder="Diastolic" keyboardType="number-pad" value={diastolic} onChangeText={setDiastolic} />
      </View>
      <TouchableOpacity style={styles.button} onPress={handleLog} disabled={isPending}>
        <Text style={styles.buttonText}>{isPending ? 'Saving…' : 'Log Blood Pressure'}</Text>
      </TouchableOpacity>
      {history?.map((v, i) => (
        <View key={i} style={styles.historyRow}>
          <Text style={styles.date}>{new Date(v.loggedAt).toLocaleDateString()}</Text>
          <Text style={styles.value}>{v.measurements.bpSystolic}/{v.measurements.bpDiastolic} mmHg</Text>
          {v.alertTriggered && <Text style={styles.alert}>⚠️</Text>}
        </View>
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff', padding: 16 },
  row: { flexDirection: 'row', marginBottom: 12 },
  input: { borderWidth: 1, borderColor: '#ddd', borderRadius: 8, padding: 12, fontSize: 16 },
  button: { backgroundColor: '#E91E8C', borderRadius: 8, padding: 16, alignItems: 'center', marginBottom: 24 },
  buttonText: { color: '#fff', fontWeight: '600', fontSize: 16 },
  historyRow: { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 10, borderBottomWidth: 1, borderColor: '#eee' },
  date: { color: '#666', fontSize: 14 },
  value: { fontWeight: '600', color: '#333' },
  alert: { fontSize: 14 },
});
```

**`src/screens/KickCounterScreen.tsx`**
```tsx
import React, { useState } from 'react';
import { Alert, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { KickCounterSessionResponse } from '../types/api';
import { useEndKickSession, useRecordKick, useStartKickSession } from '../hooks/useKickCounter';

export function KickCounterScreen() {
  const [session, setSession] = useState<KickCounterSessionResponse | null>(null);
  const { mutate: start, isPending: starting } = useStartKickSession();
  const { mutate: kick, isPending: kicking } = useRecordKick(session?.id ?? '');
  const { mutate: end, isPending: ending } = useEndKickSession(session?.id ?? '');

  const handleStart = () => start(undefined, { onSuccess: setSession });
  const handleKick = () => kick(undefined, { onSuccess: setSession });
  const handleEnd = () =>
    end(undefined, {
      onSuccess: data => {
        Alert.alert(data.isConcerning ? '⚠️ Contact your doctor' : 'Session complete', `${data.totalKicks} kicks recorded`);
        setSession(null);
      },
    });

  return (
    <View style={styles.container}>
      {!session ? (
        <TouchableOpacity style={styles.bigButton} onPress={handleStart} disabled={starting}>
          <Text style={styles.bigButtonText}>Start Session</Text>
        </TouchableOpacity>
      ) : (
        <>
          <Text style={styles.count}>{session.totalKicks}</Text>
          <Text style={styles.label}>kicks</Text>
          <TouchableOpacity style={styles.kickButton} onPress={handleKick} disabled={kicking}>
            <Text style={styles.kickButtonText}>+ Kick</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.endButton} onPress={handleEnd} disabled={ending}>
            <Text style={styles.endButtonText}>End Session</Text>
          </TouchableOpacity>
        </>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff', justifyContent: 'center', alignItems: 'center', padding: 24 },
  bigButton: { backgroundColor: '#E91E8C', borderRadius: 60, width: 160, height: 160, justifyContent: 'center', alignItems: 'center' },
  bigButtonText: { color: '#fff', fontSize: 18, fontWeight: '700' },
  count: { fontSize: 80, fontWeight: '700', color: '#E91E8C' },
  label: { fontSize: 20, color: '#666', marginBottom: 32 },
  kickButton: { backgroundColor: '#E91E8C', borderRadius: 40, width: 120, height: 120, justifyContent: 'center', alignItems: 'center', marginBottom: 24 },
  kickButtonText: { color: '#fff', fontSize: 22, fontWeight: '700' },
  endButton: { borderWidth: 1, borderColor: '#E91E8C', borderRadius: 8, paddingHorizontal: 32, paddingVertical: 12 },
  endButtonText: { color: '#E91E8C', fontWeight: '600', fontSize: 16 },
});
```

**`src/screens/ContractionTimerScreen.tsx`**
```tsx
import React, { useState } from 'react';
import { Alert, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { ContractionSessionResponse } from '../types/api';
import { useEndContractionSession, useRecordContraction, useStartContractionSession } from '../hooks/useContractionTimer';

export function ContractionTimerScreen() {
  const [session, setSession] = useState<ContractionSessionResponse | null>(null);
  const { mutate: start, isPending: starting } = useStartContractionSession();
  const { mutate: record, isPending: recording } = useRecordContraction(session?.id ?? '');
  const { mutate: end, isPending: ending } = useEndContractionSession(session?.id ?? '');

  const handleEnd = () =>
    end(undefined, {
      onSuccess: data => {
        const msg = data.isLaborPattern ? `Avg interval: ${data.averageIntervalSeconds}s\nAvg duration: ${data.averageDurationSeconds}s` : `${data.totalContractions} contractions recorded`;
        Alert.alert(data.alertTriggered ? '⚠️ Possible premature labor' : data.isLaborPattern ? 'Labor pattern detected' : 'Session complete', msg);
        setSession(null);
      },
    });

  return (
    <View style={styles.container}>
      {!session ? (
        <TouchableOpacity style={styles.bigButton} onPress={() => start(undefined, { onSuccess: setSession })} disabled={starting}>
          <Text style={styles.bigButtonText}>Start</Text>
        </TouchableOpacity>
      ) : (
        <>
          <Text style={styles.count}>{session.totalContractions}</Text>
          <Text style={styles.label}>contractions</Text>
          {session.averageIntervalSeconds && (
            <Text style={styles.stat}>Avg interval: {Math.round(session.averageIntervalSeconds / 60)} min</Text>
          )}
          <TouchableOpacity style={styles.recordButton} onPress={() => record(undefined, { onSuccess: setSession })} disabled={recording}>
            <Text style={styles.recordText}>Record Contraction</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.endButton} onPress={handleEnd} disabled={ending}>
            <Text style={styles.endText}>End Session</Text>
          </TouchableOpacity>
        </>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff', justifyContent: 'center', alignItems: 'center', padding: 24 },
  bigButton: { backgroundColor: '#E91E8C', borderRadius: 60, width: 160, height: 160, justifyContent: 'center', alignItems: 'center' },
  bigButtonText: { color: '#fff', fontSize: 20, fontWeight: '700' },
  count: { fontSize: 72, fontWeight: '700', color: '#E91E8C' },
  label: { fontSize: 18, color: '#666', marginBottom: 8 },
  stat: { fontSize: 14, color: '#444', marginBottom: 32 },
  recordButton: { backgroundColor: '#E91E8C', borderRadius: 8, paddingHorizontal: 32, paddingVertical: 16, marginBottom: 16 },
  recordText: { color: '#fff', fontWeight: '700', fontSize: 16 },
  endButton: { borderWidth: 1, borderColor: '#E91E8C', borderRadius: 8, paddingHorizontal: 32, paddingVertical: 12 },
  endText: { color: '#E91E8C', fontWeight: '600', fontSize: 16 },
});
```

**`src/screens/SummaryCardScreen.tsx`**
```tsx
import React from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSummaryCard } from '../hooks/useSummaryCard';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { ErrorView } from '../components/ErrorView';

export function SummaryCardScreen() {
  const { data, isLoading, error, refetch } = useSummaryCard();

  if (isLoading) return <LoadingSpinner />;
  if (error) return <ErrorView error={error as Error} onRetry={refetch} />;

  return (
    <ScrollView style={styles.container}>
      <Text style={styles.week}>Week {data?.gestationalWeek} — T{data?.trimester}</Text>
      <Text style={styles.edd}>EDD: {data?.edd ? new Date(data.edd).toLocaleDateString() : '—'}</Text>
      {data?.highRiskFlags.length ? (
        <View style={styles.alertBox}>
          <Text style={styles.alertTitle}>⚠️ Risk Flags</Text>
          {data.highRiskFlags.map((f, i) => <Text key={i} style={styles.alertItem}>• {f}</Text>)}
        </View>
      ) : null}
      <Text style={styles.sectionTitle}>Latest Vitals</Text>
      {data?.latestVitals.map((v, i) => (
        <View key={i} style={styles.row}>
          <Text style={styles.rowLabel}>{v.vitalType}</Text>
          <Text style={styles.rowValue}>{JSON.stringify(v.measurements)}</Text>
        </View>
      ))}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff', padding: 16 },
  week: { fontSize: 24, fontWeight: '700', color: '#E91E8C', marginBottom: 4 },
  edd: { fontSize: 14, color: '#666', marginBottom: 20 },
  alertBox: { backgroundColor: '#FFF3E0', borderRadius: 8, padding: 12, marginBottom: 16 },
  alertTitle: { fontWeight: '700', color: '#E65100', marginBottom: 8 },
  alertItem: { color: '#BF360C', fontSize: 14 },
  sectionTitle: { fontSize: 16, fontWeight: '600', color: '#333', marginBottom: 8, marginTop: 8 },
  row: { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 8, borderBottomWidth: 1, borderColor: '#eee' },
  rowLabel: { color: '#666', fontSize: 14 },
  rowValue: { color: '#333', fontWeight: '500', fontSize: 14 },
});
```

**`src/screens/GrowthChartScreen.tsx`**
```tsx
import React from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSelector } from 'react-redux';
import { RootState } from '../store';
import { useGrowthHistory } from '../hooks/useGrowthChart';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { ErrorView } from '../components/ErrorView';

export function GrowthChartScreen() {
  const childId = useSelector((state: RootState) => state.ui.activeChildId);
  const { data, isLoading, error, refetch } = useGrowthHistory(childId ?? '');

  if (!childId) return <View style={styles.container}><Text>No child profile selected</Text></View>;
  if (isLoading) return <LoadingSpinner />;
  if (error) return <ErrorView error={error as Error} onRetry={refetch} />;

  return (
    <ScrollView style={styles.container}>
      {data?.map((m, i) => (
        <View key={i} style={styles.card}>
          <Text style={styles.age}>Month {m.ageInMonths}</Text>
          <Text style={styles.stat}>Weight: {m.weightKg} kg</Text>
          <Text style={styles.stat}>Height: {m.heightCm} cm</Text>
          {m.alertFlags.length > 0 && <Text style={styles.alert}>⚠️ {m.alertFlags.join(', ')}</Text>}
        </View>
      ))}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff', padding: 16 },
  card: { backgroundColor: '#F3E5F5', borderRadius: 8, padding: 12, marginBottom: 12 },
  age: { fontWeight: '700', color: '#6A1B9A', marginBottom: 4 },
  stat: { fontSize: 14, color: '#333' },
  alert: { color: '#B71C1C', fontSize: 13, marginTop: 4 },
});
```

**`src/screens/VaccinationScreen.tsx`**
```tsx
import React from 'react';
import { Alert, ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { useSelector } from 'react-redux';
import { RootState } from '../store';
import { useMarkVaccineCompleted, useVaccinationSchedule } from '../hooks/useVaccination';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { ErrorView } from '../components/ErrorView';

const STATUS_COLORS: Record<string, string> = {
  COMPLETED: '#2E7D32',
  PENDING: '#1565C0',
  OVERDUE: '#B71C1C',
  SKIPPED: '#757575',
};

export function VaccinationScreen() {
  const childId = useSelector((state: RootState) => state.ui.activeChildId);
  const { data, isLoading, error, refetch } = useVaccinationSchedule(childId ?? '');
  const { mutate: markCompleted } = useMarkVaccineCompleted(childId ?? '');

  const handleMark = (id: string, name: string) => {
    Alert.alert(`Mark ${name} as done?`, '', [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Confirm', onPress: () =>
          markCompleted({ id, date: new Date().toISOString().split('T')[0], by: 'Doctor' }),
      },
    ]);
  };

  if (!childId) return <View style={styles.container}><Text>No child profile selected</Text></View>;
  if (isLoading) return <LoadingSpinner />;
  if (error) return <ErrorView error={error as Error} onRetry={refetch} />;

  return (
    <ScrollView style={styles.container}>
      {data?.map(v => (
        <View key={v.id} style={styles.row}>
          <View style={styles.info}>
            <Text style={styles.name}>{v.vaccineName}</Text>
            <Text style={styles.date}>Due: {new Date(v.scheduledDate).toLocaleDateString()}</Text>
          </View>
          <TouchableOpacity
            style={[styles.badge, { backgroundColor: STATUS_COLORS[v.status] }]}
            onPress={() => v.status === 'PENDING' || v.status === 'OVERDUE' ? handleMark(v.id, v.vaccineName) : null}
          >
            <Text style={styles.badgeText}>{v.status}</Text>
          </TouchableOpacity>
        </View>
      ))}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff', padding: 16 },
  row: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingVertical: 12, borderBottomWidth: 1, borderColor: '#eee' },
  info: { flex: 1 },
  name: { fontWeight: '600', color: '#333', fontSize: 15 },
  date: { fontSize: 12, color: '#666', marginTop: 2 },
  badge: { borderRadius: 4, paddingHorizontal: 8, paddingVertical: 4 },
  badgeText: { color: '#fff', fontSize: 11, fontWeight: '700' },
});
```

**`src/screens/DueDateClubScreen.tsx`**
```tsx
import React from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { useMyClubs } from '../hooks/useCommunity';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { ErrorView } from '../components/ErrorView';

export function DueDateClubScreen() {
  const { data: clubs, isLoading, error, refetch } = useMyClubs();

  if (isLoading) return <LoadingSpinner />;
  if (error) return <ErrorView error={error as Error} onRetry={refetch} />;

  return (
    <ScrollView style={styles.container}>
      {clubs?.length === 0 && <Text style={styles.empty}>You haven't joined a club yet.</Text>}
      {clubs?.map(club => (
        <View key={club.id} style={styles.card}>
          <Text style={styles.clubName}>{club.clubName}</Text>
          <Text style={styles.members}>{club.memberCount} members · {club.dueDateMonth}</Text>
          {club.channels.map(ch => (
            <Text key={ch.channelId} style={styles.channel}>#{ch.name}</Text>
          ))}
        </View>
      ))}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff', padding: 16 },
  empty: { color: '#666', fontSize: 15, textAlign: 'center', marginTop: 40 },
  card: { backgroundColor: '#FFF0F7', borderRadius: 8, padding: 14, marginBottom: 12 },
  clubName: { fontWeight: '700', fontSize: 16, color: '#333', marginBottom: 4 },
  members: { fontSize: 13, color: '#666', marginBottom: 8 },
  channel: { fontSize: 13, color: '#E91E8C' },
});
```

---

## Part 2 — Missing Backend Tests

### 2.1 `identity-abha-service` — Unit Tests

**File:** `services/identity-abha-service/src/test/java/com/reejuven8/identity/security/JwtTokenProviderTest.java`

```java
package com.reejuven8.identity.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "jwtSecret", "test-secret-key-at-least-32-characters-long-for-hs256");
        ReflectionTestUtils.setField(provider, "jwtExpirationMs", 900_000L);
    }

    @Test
    void generateToken_thenValidate_returnsTrue() {
        UUID userId = UUID.randomUUID();
        String token = provider.generateToken(userId, "PATIENT");
        assertTrue(provider.validateToken(token));
    }

    @Test
    void extractUserId_matchesInput() {
        UUID userId = UUID.randomUUID();
        String token = provider.generateToken(userId, "PATIENT");
        assertEquals(userId, provider.extractUserId(token));
    }

    @Test
    void extractRole_matchesInput() {
        UUID userId = UUID.randomUUID();
        String token = provider.generateToken(userId, "DOCTOR");
        assertEquals("DOCTOR", provider.extractRole(token));
    }

    @Test
    void validateToken_withTamperedToken_returnsFalse() {
        UUID userId = UUID.randomUUID();
        String token = provider.generateToken(userId, "PATIENT");
        assertFalse(provider.validateToken(token + "tampered"));
    }

    @Test
    void extractJti_isNonNull() {
        String token = provider.generateToken(UUID.randomUUID(), "PATIENT");
        assertNotNull(provider.extractJti(token));
    }
}
```

**File:** `services/identity-abha-service/src/test/java/com/reejuven8/identity/security/RsaEncryptionServiceTest.java`

```java
package com.reejuven8.identity.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RsaEncryptionServiceTest {

    RsaEncryptionService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new RsaEncryptionService();
        // Uses ABDM sandbox public key; in tests verify ciphertext format only
    }

    @Test
    void encrypt_producesBase64Output() throws Exception {
        String plaintext = "123456"; // OTP
        String encrypted = service.encrypt(plaintext);
        assertNotNull(encrypted);
        // Base64 encoded RSA-OAEP ciphertext is always longer than plaintext
        assertTrue(encrypted.length() > plaintext.length());
        // Must be valid Base64
        assertDoesNotThrow(() -> java.util.Base64.getDecoder().decode(encrypted));
    }

    @Test
    void encrypt_differentCallsProduceDifferentCiphertexts() throws Exception {
        // OAEP uses random padding — same plaintext produces different ciphertext each call
        String c1 = service.encrypt("123456");
        String c2 = service.encrypt("123456");
        assertNotEquals(c1, c2);
    }
}
```

---

### 2.2 `ai-parsing-service` — pytest Tests

**File:** `services/ai-parsing-service/tests/test_ner_service.py`

```python
import pytest
from app.services.ner_service import NerService

@pytest.fixture
def ner():
    return NerService()

def test_extracts_hemoglobin(ner):
    text = "Haemoglobin: 10.5 g/dL"
    observations = ner.extract_observations(text)
    hb = next((o for o in observations if "haemoglobin" in o.parameter_name.lower()), None)
    assert hb is not None
    assert abs(hb.value - 10.5) < 0.01
    assert hb.unit == "g/dL"

def test_extracts_glucose(ner):
    text = "Fasting Blood Glucose: 92 mg/dL"
    observations = ner.extract_observations(text)
    glucose = next((o for o in observations if "glucose" in o.parameter_name.lower()), None)
    assert glucose is not None
    assert abs(glucose.value - 92.0) < 0.01

def test_extracts_multiple(ner):
    text = "Hb: 11.2 g/dL\nTSH: 2.5 mIU/L\nGlucose: 88 mg/dL"
    observations = ner.extract_observations(text)
    assert len(observations) >= 3

def test_empty_text_returns_empty_list(ner):
    assert ner.extract_observations("") == []

def test_no_match_returns_empty_list(ner):
    assert ner.extract_observations("Patient name: John. DOB: 01/01/1990.") == []
```

**File:** `services/ai-parsing-service/tests/test_loinc_mapper.py`

```python
import pytest
from app.utils.loinc_mapper import LoincMapper

@pytest.fixture
def mapper():
    return LoincMapper()

def test_haemoglobin_exact_match(mapper):
    code, confidence = mapper.map("Haemoglobin")
    assert code == "718-7"
    assert confidence >= 0.9

def test_hemoglobin_variant(mapper):
    code, confidence = mapper.map("Hemoglobin")
    assert code == "718-7"
    assert confidence >= 0.75

def test_tsh_maps_correctly(mapper):
    code, confidence = mapper.map("TSH")
    assert code == "3016-3"

def test_glucose_maps_correctly(mapper):
    code, confidence = mapper.map("Fasting Blood Glucose")
    assert code == "2339-0"
    assert confidence >= 0.75

def test_unknown_term_returns_none(mapper):
    code, confidence = mapper.map("xyzabc123")
    assert code is None
    assert confidence == 0.0
```

**File:** `services/ai-parsing-service/tests/test_fhir_mapper.py`

```python
import pytest
from app.services.fhir_mapper import FhirMapper
from app.models.parsed_observation import ParsedObservation

@pytest.fixture
def mapper():
    return FhirMapper()

@pytest.fixture
def observation():
    return ParsedObservation(
        parameter_name="Haemoglobin",
        value=10.5,
        unit="g/dL",
        loinc_code="718-7",
        confidence=0.95,
    )

def test_builds_fhir_observation(mapper, observation):
    fhir = mapper.build_observation(observation, patient_id="patient-uuid-123")
    assert fhir["resourceType"] == "Observation"
    assert fhir["status"] == "final"
    assert fhir["subject"]["reference"] == "Patient/patient-uuid-123"

def test_fhir_has_loinc_coding(mapper, observation):
    fhir = mapper.build_observation(observation, patient_id="patient-uuid-123")
    codings = fhir["code"]["coding"]
    loinc = next((c for c in codings if c["system"] == "http://loinc.org"), None)
    assert loinc is not None
    assert loinc["code"] == "718-7"

def test_fhir_value_quantity(mapper, observation):
    fhir = mapper.build_observation(observation, patient_id="patient-uuid-123")
    vq = fhir["valueQuantity"]
    assert abs(vq["value"] - 10.5) < 0.01
    assert vq["unit"] == "g/dL"
```

---

### 2.3 Integration Tests (Testcontainers)

These are lower priority — skip for now, add when CI is in place. Placeholder here as reminder:

- `identity-abha-service`: `AuthServiceIntegrationTest` — full OTP → JWT flow with real PostgreSQL + Redis
- `health-data-service`: `FhirResourceServiceIntegrationTest` — MongoDB CRUD + RabbitMQ message
- `ninemo-clinical-service`: `TimelineServiceIntegrationTest` — Flyway migration + timeline feed

---

## Part 3 — Unimplemented Backend Features

### 3.1 `DevelopmentalMilestoneService`

**File:** `services/ninemo-clinical-service/src/main/java/com/reejuven8/ninemo/clinical/service/pediatric/DevelopmentalMilestoneService.java`

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class DevelopmentalMilestoneService {

    private final DevelopmentalMilestoneRepository milestoneRepository;
    private final ChildProfileRepository childProfileRepository;
    private final MilestoneReminderPublisher milestoneReminderPublisher;

    // WHO developmental milestone categories
    private static final Map<Integer, Map<String, List<String>>> WHO_MILESTONES = Map.of(
        2, Map.of(
            "GROSS_MOTOR", List.of("Lifts head when on tummy", "Moves both arms and legs"),
            "SOCIAL", List.of("Smiles at people"),
            "LANGUAGE", List.of("Makes cooing sounds")
        ),
        4, Map.of(
            "GROSS_MOTOR", List.of("Holds head steady", "Pushes down on legs when feet on surface"),
            "FINE_MOTOR", List.of("Reaches for toys", "Brings hands to mouth"),
            "SOCIAL", List.of("Smiles spontaneously", "Likes to play with people")
        ),
        6, Map.of(
            "GROSS_MOTOR", List.of("Rolls over both ways", "Begins to sit without support"),
            "FINE_MOTOR", List.of("Passes toy from hand to hand"),
            "LANGUAGE", List.of("Responds to sounds", "Makes vowel sounds")
        ),
        9, Map.of(
            "GROSS_MOTOR", List.of("Stands holding on", "Pulls to stand"),
            "FINE_MOTOR", List.of("Picks up small objects with finger and thumb"),
            "LANGUAGE", List.of("Says 'mama'/'dada'", "Copies sounds and gestures")
        ),
        12, Map.of(
            "GROSS_MOTOR", List.of("Walks holding onto furniture", "May stand alone"),
            "LANGUAGE", List.of("Says 1-2 words besides mama/dada"),
            "SOCIAL", List.of("Cries when parent leaves", "Plays simple games like peek-a-boo")
        ),
        18, Map.of(
            "GROSS_MOTOR", List.of("Walks independently", "Climbs stairs with support"),
            "LANGUAGE", List.of("Says several single words", "Points to show"),
            "SOCIAL", List.of("Parallel play")
        ),
        24, Map.of(
            "GROSS_MOTOR", List.of("Runs", "Kicks a ball"),
            "LANGUAGE", List.of("Uses 2-word phrases", "Vocabulary of 50+ words"),
            "SOCIAL", List.of("Copies others' behaviors")
        )
    );

    public DevelopmentalMilestone checkIn(UUID childId, int month) {
        ChildProfile child = childProfileRepository.findById(childId)
            .orElseThrow(() -> new ResourceNotFoundException("ChildProfile", childId.toString()));

        return milestoneRepository
            .findByChildIdAndMonth(childId.toString(), month)
            .orElseGet(() -> createCheckIn(child, month));
    }

    public DevelopmentalMilestone markMilestone(String documentId, String milestoneKey, boolean achieved) {
        DevelopmentalMilestone doc = milestoneRepository.findById(documentId)
            .orElseThrow(() -> new ResourceNotFoundException("DevelopmentalMilestone", documentId));

        doc.getMilestones().put(milestoneKey, achieved);
        doc.setLastUpdated(LocalDateTime.now());

        long achieved_count = doc.getMilestones().values().stream().filter(v -> v).count();
        long total = doc.getMilestones().size();

        if (achieved_count < total * 0.5) {
            doc.setDelayRisk("HIGH");
            milestoneReminderPublisher.publishMilestoneReminder(
                doc.getChildId(), "DEVELOPMENTAL_DELAY_RISK_" + doc.getMonth() + "M", 0);
        }

        return milestoneRepository.save(doc);
    }

    public List<DevelopmentalMilestone> getHistory(UUID childId) {
        return milestoneRepository.findByChildIdOrderByMonthAsc(childId.toString());
    }

    private DevelopmentalMilestone createCheckIn(ChildProfile child, int month) {
        int nearestMonth = WHO_MILESTONES.keySet().stream()
            .min(Comparator.comparingInt(k -> Math.abs(k - month)))
            .orElse(12);

        Map<String, Boolean> milestones = new LinkedHashMap<>();
        WHO_MILESTONES.getOrDefault(nearestMonth, Map.of())
            .forEach((category, items) -> items.forEach(item -> milestones.put(category + ": " + item, false)));

        DevelopmentalMilestone doc = new DevelopmentalMilestone();
        doc.setChildId(child.getId().toString());
        doc.setMonth(month);
        doc.setMilestones(milestones);
        doc.setDelayRisk("NONE");
        doc.setLastUpdated(LocalDateTime.now());
        return milestoneRepository.save(doc);
    }
}
```

**Repository:** `DevelopmentalMilestoneRepository.java`

```java
public interface DevelopmentalMilestoneRepository extends MongoRepository<DevelopmentalMilestone, String> {
    Optional<DevelopmentalMilestone> findByChildIdAndMonth(String childId, int month);
    List<DevelopmentalMilestone> findByChildIdOrderByMonthAsc(String childId);
}
```

**Controller:** `MilestoneController.java`

```
GET  /api/v1/ninemo/milestones/children/{childId}
GET  /api/v1/ninemo/milestones/children/{childId}/month/{month}
PUT  /api/v1/ninemo/milestones/{documentId}/achieve?milestoneKey={key}&achieved={bool}
```

---

### 3.2 API Gateway — Wire Rate Limiting to Routes

**File:** `services/api-gateway/src/main/resources/application.yml`

Add to every route that currently lacks it:

```yaml
- name: RequestRateLimiter
  args:
    redis-rate-limiter.replenishRate: 20
    redis-rate-limiter.burstCapacity: 40
    key-resolver: "#{@ipKeyResolver}"
```

Routes to update: `identity-abha-service`, `health-data-service`, `ninemo-clinical-service`, `notification-service`, `ninemo-community-service`.

---

## Part 4 — DevOps / CI/CD

### 4.1 GitHub Actions

**File:** `.github/workflows/ci.yml` (at monorepo root `/Work/Backend/NineMo/`)

```yaml
name: CI

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  backend-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'temurin' }
      - name: Maven test
        working-directory: ninemo-backend
        run: mvn clean test -Dnet.bytebuddy.experimental=true

  python-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v5
        with: { python-version: '3.11' }
      - name: Install deps
        working-directory: ninemo-backend/services/ai-parsing-service
        run: pip install -r requirements.txt pytest
      - name: Run pytest
        working-directory: ninemo-backend/services/ai-parsing-service
        run: pytest tests/ -v

  build-java-images:
    needs: backend-test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    strategy:
      matrix:
        service: [api-gateway, identity-abha-service, health-data-service, notification-service, ninemo-clinical-service, ninemo-community-service]
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'temurin' }
      - name: Build Docker image
        working-directory: ninemo-backend/services/${{ matrix.service }}
        run: docker build -t ninemo/${{ matrix.service }}:${{ github.sha }} .

  build-python-image:
    needs: python-test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v4
      - name: Build ai-parsing-service image
        working-directory: ninemo-backend/services/ai-parsing-service
        run: docker build -t ninemo/ai-parsing-service:${{ github.sha }} .
```

---

### 4.2 Kubernetes Manifests

**Location:** `ninemo-backend/k8s/`

**Pattern** (repeat for each service, only port differs):

```yaml
# k8s/identity-abha-service.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: identity-abha-service
  namespace: ninemo
spec:
  replicas: 2
  selector:
    matchLabels: { app: identity-abha-service }
  template:
    metadata:
      labels: { app: identity-abha-service }
    spec:
      containers:
        - name: identity-abha-service
          image: ninemo/identity-abha-service:latest
          ports: [{ containerPort: 8081 }]
          envFrom: [{ secretRef: { name: ninemo-secrets } }]
          readinessProbe:
            httpGet: { path: /actuator/health, port: 8081 }
            initialDelaySeconds: 30
            periodSeconds: 10
          livenessProbe:
            httpGet: { path: /actuator/health, port: 8081 }
            initialDelaySeconds: 60
            periodSeconds: 20
---
apiVersion: v1
kind: Service
metadata:
  name: identity-abha-service
  namespace: ninemo
spec:
  selector: { app: identity-abha-service }
  ports: [{ port: 8081, targetPort: 8081 }]
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: identity-abha-service-hpa
  namespace: ninemo
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: identity-abha-service
  minReplicas: 2
  maxReplicas: 5
  metrics:
    - type: Resource
      resource:
        name: cpu
        target: { type: Utilization, averageUtilization: 70 }
```

Services to create manifests for: `api-gateway` (8080), `identity-abha-service` (8081), `health-data-service` (8082), `ai-parsing-service` (8083), `ninemo-clinical-service` (8084), `notification-service` (8085), `ninemo-community-service` (8086).

Also needed:
- `k8s/namespace.yaml` — `ninemo` namespace
- `k8s/secrets.yaml` — template for DB passwords, JWT secret, AWS keys
- `k8s/ingress.yaml` — NGINX ingress routing to `api-gateway:8080`
- `k8s/configmap.yaml` — non-secret env vars (ABDM URLs, Kafka brokers)

---

## Part 5 — Minor Polish

### 5.1 Correlation ID in Kafka/RabbitMQ Events

In `ClinicalRiskPublisher` and `MilestoneReminderPublisher`, set `BaseEvent.correlationId` from MDC before publishing:

```java
String correlationId = MDC.get("correlationId");
event.setCorrelationId(correlationId != null ? correlationId : UUID.randomUUID().toString());
```

Same pattern in `DocumentUploadedPublisher` (health-data-service) and `EventPublisher` (identity-abha-service).

### 5.2 Swagger `@Operation` Annotations

Add to all controllers in `identity-abha-service`, `health-data-service`, `ninemo-clinical-service`, `ninemo-community-service`. Pattern:

```java
@Operation(summary = "Log symptom", description = "Evaluates triage rules and returns severity flag")
@ApiResponse(responseCode = "200", description = "Symptom logged and triaged")
@PostMapping
public ResponseEntity<ApiResponse<SymptomLogResponse>> logSymptom(@RequestBody SymptomLogRequest req) { ... }
```

---

## Execution Checklist

| # | Section | Status |
|---|---|---|
| 1.1 | Frontend project setup (package.json, tsconfig, babel, metro, index, App) | ⬜ |
| 1.2 | `src/types/api.ts` | ⬜ |
| 1.3 | `src/services/apiClient.ts` | ⬜ |
| 1.4 | All service files (8 files) | ⬜ |
| 1.5 | Store (index, authSlice, uiSlice) | ⬜ |
| 1.6 | Navigation (routes, AppNavigator, AuthNavigator, MainNavigator) | ⬜ |
| 1.7 | All hooks (10 files) | ⬜ |
| 1.8 | Shared components (LoadingSpinner, ErrorView) | ⬜ |
| 1.9 | All screens (11 files) | ⬜ |
| 2.1 | identity-abha-service unit tests (JwtTokenProviderTest, RsaEncryptionServiceTest) | ⬜ |
| 2.2 | ai-parsing-service pytest (test_ner_service, test_loinc_mapper, test_fhir_mapper) | ⬜ |
| 2.3 | Integration tests (Testcontainers) | ⬜ |
| 3.1 | DevelopmentalMilestoneService + repository + controller | ⬜ |
| 3.2 | Gateway rate limiting wired to routes | ⬜ |
| 4.1 | GitHub Actions CI/CD workflow | ⬜ |
| 4.2 | Kubernetes manifests (8 services + namespace + secrets + ingress) | ⬜ |
| 5.1 | Correlation ID in event publishers | ⬜ |
| 5.2 | Swagger @Operation annotations on controllers | ⬜ |
