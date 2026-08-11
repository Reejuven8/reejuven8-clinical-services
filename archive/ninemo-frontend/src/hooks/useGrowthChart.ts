import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { growthService } from '../services/growthService';
import { GrowthMeasurementRequest } from '../types/api';

export function useGrowthHistory(childId: string) {
  return useQuery({
    queryKey: ['growth', childId],
    queryFn: () => growthService.getHistory(childId),
    enabled: !!childId,
  });
}

export function useRecordGrowth(childId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (req: GrowthMeasurementRequest) => growthService.record(childId, req),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['growth', childId] }),
  });
}
