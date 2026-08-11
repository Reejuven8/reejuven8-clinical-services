import { createSlice, PayloadAction } from '@reduxjs/toolkit';

interface UiState {
  activeChildId: string | null;
  activePregnancyId: string | null;
}

const initialState: UiState = { activeChildId: null, activePregnancyId: null };

const uiSlice = createSlice({
  name: 'ui',
  initialState,
  reducers: {
    setActiveChildId: (state, action: PayloadAction<string>) => {
      state.activeChildId = action.payload;
    },
    setActivePregnancyId: (state, action: PayloadAction<string>) => {
      state.activePregnancyId = action.payload;
    },
  },
});

export const { setActiveChildId, setActivePregnancyId } = uiSlice.actions;
export default uiSlice.reducer;
