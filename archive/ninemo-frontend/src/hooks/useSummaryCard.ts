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
