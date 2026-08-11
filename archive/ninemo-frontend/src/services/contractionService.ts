import { apiClient } from './apiClient';
import { ApiResponse, ContractionSessionResponse } from '../types/api';

export const contractionService = {
  startSession: async (): Promise<ContractionSessionResponse> => {
    const res = await apiClient.post<ApiResponse<ContractionSessionResponse>>('/ninemo/contractions/sessions');
    return res.data.data;
  },
  recordContraction: async (sessionId: string): Promise<ContractionSessionResponse> => {
    const res = await apiClient.put<ApiResponse<ContractionSessionResponse>>(
      `/ninemo/contractions/sessions/${sessionId}/contraction`,
    );
    return res.data.data;
  },
  endSession: async (sessionId: string): Promise<ContractionSessionResponse> => {
    const res = await apiClient.put<ApiResponse<ContractionSessionResponse>>(
      `/ninemo/contractions/sessions/${sessionId}/end`,
    );
    return res.data.data;
  },
};
