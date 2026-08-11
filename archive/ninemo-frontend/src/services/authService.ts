import { apiClient } from './apiClient';
import { ApiResponse, LoginRequest, RegisterRequest, TokenResponse } from '../types/api';

export const authService = {
  login: async (req: LoginRequest): Promise<TokenResponse> => {
    const res = await apiClient.post<ApiResponse<TokenResponse>>('/identity/auth/login', req);
    return res.data.data;
  },
  register: async (req: RegisterRequest): Promise<TokenResponse> => {
    const res = await apiClient.post<ApiResponse<TokenResponse>>('/identity/auth/register', req);
    return res.data.data;
  },
  refresh: async (refreshToken: string): Promise<TokenResponse> => {
    const res = await apiClient.post<ApiResponse<TokenResponse>>('/identity/auth/refresh', { refreshToken });
    return res.data.data;
  },
  logout: async (): Promise<void> => {
    await apiClient.post('/identity/auth/logout');
  },
};
