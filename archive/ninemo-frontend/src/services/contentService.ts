import { apiClient } from './apiClient';
import { ApiResponse, ContentArticle } from '../types/api';

export const contentService = {
  listPublished: async (): Promise<ContentArticle[]> => {
    const res = await apiClient.get<ApiResponse<ContentArticle[]>>('/community/content');
    return res.data.data;
  },
  listByWeek: async (week: number): Promise<ContentArticle[]> => {
    const res = await apiClient.get<ApiResponse<ContentArticle[]>>(`/community/content/week/${week}`);
    return res.data.data;
  },
};
