import { apiClient } from './apiClient';
import { ApiResponse, GrowthMeasurementRequest, GrowthMeasurementResponse } from '../types/api';

export const growthService = {
  record: async (childId: string, req: GrowthMeasurementRequest): Promise<GrowthMeasurementResponse> => {
    const res = await apiClient.post<ApiResponse<GrowthMeasurementResponse>>(
      `/ninemo/growth/children/${childId}/measurements`,
      req,
    );
    return res.data.data;
  },
  getHistory: async (childId: string): Promise<GrowthMeasurementResponse[]> => {
    const res = await apiClient.get<ApiResponse<GrowthMeasurementResponse[]>>(
      `/ninemo/growth/children/${childId}/measurements`,
    );
    return res.data.data;
  },
};
