import { apiClient } from './apiClient';
import { ApiResponse, KickCounterSessionResponse } from '../types/api';

export const kickCounterService = {
  startSession: async (): Promise<KickCounterSessionResponse> => {
    const res = await apiClient.post<ApiResponse<KickCounterSessionResponse>>('/ninemo/kick-counter/sessions');
    return res.data.data;
  },
  recordKick: async (sessionId: string): Promise<KickCounterSessionResponse> => {
    const res = await apiClient.put<ApiResponse<KickCounterSessionResponse>>(
      `/ninemo/kick-counter/sessions/${sessionId}/kick`,
    );
    return res.data.data;
  },
  endSession: async (sessionId: string): Promise<KickCounterSessionResponse> => {
    const res = await apiClient.put<ApiResponse<KickCounterSessionResponse>>(
      `/ninemo/kick-counter/sessions/${sessionId}/end`,
    );
    return res.data.data;
  },
};
