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
