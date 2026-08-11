import { apiClient } from './apiClient';
import {
  ApiResponse,
  PagedResponse,
  ClubResponse,
  JoinClubRequest,
  ChatMessageResponse,
} from '../types/api';

export const communityService = {
  joinClub: async (req: JoinClubRequest): Promise<ClubResponse> => {
    const res = await apiClient.post<ApiResponse<ClubResponse>>('/community/clubs/join', req);
    return res.data.data;
  },
  getMyClubs: async (): Promise<ClubResponse[]> => {
    const res = await apiClient.get<ApiResponse<ClubResponse[]>>('/community/clubs');
    return res.data.data;
  },
  getMessages: async (
    clubId: string,
    channelId: string,
    page = 0,
  ): Promise<PagedResponse<ChatMessageResponse>> => {
    const res = await apiClient.get<PagedResponse<ChatMessageResponse>>(
      `/community/clubs/${clubId}/channels/${channelId}/messages`,
      { params: { page, size: 50 } },
    );
    return res.data;
  },
};
