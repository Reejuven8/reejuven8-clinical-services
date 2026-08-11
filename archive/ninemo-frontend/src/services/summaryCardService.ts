import { apiClient } from './apiClient';
import { ApiResponse, SummaryCardResponse } from '../types/api';

export const summaryCardService = {
  get: async (patientId: string): Promise<SummaryCardResponse> => {
    const res = await apiClient.get<ApiResponse<SummaryCardResponse>>(`/ninemo/summary-card/${patientId}`);
    return res.data.data;
  },
};
