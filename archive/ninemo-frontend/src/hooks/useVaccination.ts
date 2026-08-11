import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { vaccinationService } from '../services/vaccinationService';

export function useVaccinationSchedule(childId: string) {
  return useQuery({
    queryKey: ['vaccination', childId],
    queryFn: () => vaccinationService.getSchedule(childId),
    enabled: !!childId,
  });
}

export function useMarkVaccineCompleted(childId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, date, by }: { id: string; date: string; by: string }) =>
      vaccinationService.markCompleted(id, date, by),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['vaccination', childId] }),
  });
}
