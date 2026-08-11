export const Routes = {
  Login: 'Login',
  Register: 'Register',
  Timeline: 'Timeline',
  SymptomLog: 'SymptomLog',
  VitalsWeight: 'VitalsWeight',
  VitalsBP: 'VitalsBP',
  KickCounter: 'KickCounter',
  ContractionTimer: 'ContractionTimer',
  SummaryCard: 'SummaryCard',
  DueDateClub: 'DueDateClub',
  GrowthChart: 'GrowthChart',
  Vaccination: 'Vaccination',
  ContentFeed: 'ContentFeed',
} as const;

export type RouteName = (typeof Routes)[keyof typeof Routes];
