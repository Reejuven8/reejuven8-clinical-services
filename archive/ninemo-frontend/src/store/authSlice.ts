import { createSlice, PayloadAction } from '@reduxjs/toolkit';

interface AuthState {
  userId: string | null;
  role: 'PATIENT' | 'DOCTOR' | 'ADMIN' | null;
  isAuthenticated: boolean;
}

const initialState: AuthState = { userId: null, role: null, isAuthenticated: false };

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setCredentials: (
      state,
      action: PayloadAction<{ userId: string; role: AuthState['role'] }>,
    ) => {
      state.userId = action.payload.userId;
      state.role = action.payload.role;
      state.isAuthenticated = true;
    },
    clearCredentials: state => {
      state.userId = null;
      state.role = null;
      state.isAuthenticated = false;
    },
  },
});

export const { setCredentials, clearCredentials } = authSlice.actions;
export default authSlice.reducer;
