import { apiClient } from './apiClient';
import { ApiResponse, VaccinationRecordResponse } from '../types/api';

export const vaccinationService = {
  getSchedule: async (childId: string): Promise<VaccinationRecordResponse[]> => {
    const res = await apiClient.get<ApiResponse<VaccinationRecordResponse[]>>(
      `/ninemo/vaccinations/children/${childId}/schedule`,
    );
    return res.data.data;
  },
  markCompleted: async (
    vaccinationId: string,
    administeredDate: string,
    administeredBy: string,
  ): Promise<VaccinationRecordResponse> => {
    const res = await apiClient.put<ApiResponse<VaccinationRecordResponse>>(
      `/ninemo/vaccinations/${vaccinationId}/mark-completed`,
      null,
      { params: { administeredDate, administeredBy } },
    );
    return res.data.data;
  },
};
