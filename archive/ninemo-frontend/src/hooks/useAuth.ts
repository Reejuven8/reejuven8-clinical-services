import { useMutation } from '@tanstack/react-query';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { useDispatch } from 'react-redux';
import { authService } from '../services/authService';
import { setCredentials, clearCredentials } from '../store/authSlice';
import { LoginRequest, RegisterRequest } from '../types/api';

export function useLogin() {
  const dispatch = useDispatch();
  return useMutation({
    mutationFn: (req: LoginRequest) => authService.login(req),
    onSuccess: async token => {
      await AsyncStorage.multiSet([
        ['accessToken', token.accessToken],
        ['refreshToken', token.refreshToken],
        ['userId', token.userId],
        ['role', token.role],
      ]);
      dispatch(setCredentials({ userId: token.userId, role: token.role }));
    },
  });
}

export function useRegister() {
  const dispatch = useDispatch();
  return useMutation({
    mutationFn: (req: RegisterRequest) => authService.register(req),
    onSuccess: async token => {
      await AsyncStorage.multiSet([
        ['accessToken', token.accessToken],
        ['refreshToken', token.refreshToken],
        ['userId', token.userId],
        ['role', token.role],
      ]);
      dispatch(setCredentials({ userId: token.userId, role: token.role }));
    },
  });
}

export function useLogout() {
  const dispatch = useDispatch();
  return useMutation({
    mutationFn: () => authService.logout(),
    onSettled: async () => {
      await AsyncStorage.multiRemove(['accessToken', 'refreshToken', 'userId', 'role']);
      dispatch(clearCredentials());
    },
  });
}
