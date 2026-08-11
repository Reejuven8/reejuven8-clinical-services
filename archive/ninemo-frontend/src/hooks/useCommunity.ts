import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { communityService } from '../services/communityService';
import { JoinClubRequest } from '../types/api';

export function useMyClubs() {
  return useQuery({ queryKey: ['clubs'], queryFn: communityService.getMyClubs });
}

export function useJoinClub() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (req: JoinClubRequest) => communityService.joinClub(req),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['clubs'] }),
  });
}

export function useChannelMessages(clubId: string, channelId: string) {
  return useQuery({
    queryKey: ['messages', clubId, channelId],
    queryFn: () => communityService.getMessages(clubId, channelId),
    enabled: !!clubId && !!channelId,
  });
}
