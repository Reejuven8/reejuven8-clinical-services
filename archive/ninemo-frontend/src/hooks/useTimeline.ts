import { useQuery } from '@tanstack/react-query';
import { timelineService } from '../services/timelineService';

export function useCurrentWeekTimeline() {
  return useQuery({ queryKey: ['timeline', 'current'], queryFn: timelineService.getCurrent });
}

export function useWeekTimeline(week: number) {
  return useQuery({ queryKey: ['timeline', week], queryFn: () => timelineService.getWeek(week) });
}
