import axios from 'axios';
import AsyncStorage from '@react-native-async-storage/async-storage';

const BASE_URL = __DEV__
  ? 'http://localhost:8080/api/v1'
  : 'https://api.reejuven8.com/api/v1';

export const apiClient = axios.create({ baseURL: BASE_URL, timeout: 15_000 });

apiClient.interceptors.request.use(async config => {
  const token = await AsyncStorage.getItem('accessToken');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

apiClient.interceptors.response.use(
  res => res,
  async error => {
    if (error.response?.status === 401) {
      await AsyncStorage.multiRemove(['accessToken', 'refreshToken', 'userId', 'role']);
    }
    return Promise.reject(error);
  },
);
