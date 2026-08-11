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
