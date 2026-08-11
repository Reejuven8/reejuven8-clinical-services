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
