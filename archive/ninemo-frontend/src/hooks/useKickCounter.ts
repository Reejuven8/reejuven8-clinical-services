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
