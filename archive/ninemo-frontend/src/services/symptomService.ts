import { apiClient } from './apiClient';
import { ApiResponse, SymptomLogRequest, SymptomLogResponse } from '../types/api';

export const symptomService = {
  log: async (req: SymptomLogRequest): Promise<SymptomLogResponse> => {
    const res = await apiClient.post<ApiResponse<SymptomLogResponse>>('/ninemo/symptoms', req);
    return res.data.data;
  },
  getHistory: async (): Promise<SymptomLogResponse[]> => {
    const res = await apiClient.get<ApiResponse<SymptomLogResponse[]>>('/ninemo/symptoms');
    return res.data.data;
  },
};
