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
    pagination: {
      page: number;
      size: number;
      totalElements: number;
      totalPages: number;
    };
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

// ─── Developmental Milestones ─────────────────────────────────────────────────
export interface DevelopmentalMilestoneResponse {
  id: string;
  childId: string;
  month: number;
  milestones: Record<string, boolean>;
  delayRisk: 'NONE' | 'MODERATE' | 'HIGH';
  lastUpdated: string;
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
