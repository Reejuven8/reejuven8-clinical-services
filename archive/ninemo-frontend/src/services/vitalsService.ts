import { apiClient } from './apiClient';
import { ApiResponse, VitalsLogRequest, VitalsLogResponse } from '../types/api';

export const vitalsService = {
  log: async (req: VitalsLogRequest): Promise<VitalsLogResponse> => {
    const res = await apiClient.post<ApiResponse<VitalsLogResponse>>('/ninemo/vitals', req);
    return res.data.data;
  },
  getByType: async (vitalType: string): Promise<VitalsLogResponse[]> => {
    const res = await apiClient.get<ApiResponse<VitalsLogResponse[]>>(`/ninemo/vitals/${vitalType}`);
    return res.data.data;
  },
};
