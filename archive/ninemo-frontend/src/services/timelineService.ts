import { apiClient } from './apiClient';
import { ApiResponse, TimelineResponse } from '../types/api';

export const timelineService = {
  getCurrent: async (): Promise<TimelineResponse> => {
    const res = await apiClient.get<ApiResponse<TimelineResponse>>('/ninemo/timeline/current');
    return res.data.data;
  },
  getWeek: async (week: number): Promise<TimelineResponse> => {
    const res = await apiClient.get<ApiResponse<TimelineResponse>>(`/ninemo/timeline/week/${week}`);
    return res.data.data;
  },
};
