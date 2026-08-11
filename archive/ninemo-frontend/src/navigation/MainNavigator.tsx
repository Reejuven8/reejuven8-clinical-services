import React from 'react';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { Routes } from './routes';
import { TimelineScreen } from '../screens/TimelineScreen';
import { SummaryCardScreen } from '../screens/SummaryCardScreen';
import { DueDateClubScreen } from '../screens/DueDateClubScreen';
import { SymptomLogScreen } from '../screens/SymptomLogScreen';
import { VitalsWeightScreen } from '../screens/VitalsWeightScreen';
import { VitalsBPScreen } from '../screens/VitalsBPScreen';
import { KickCounterScreen } from '../screens/KickCounterScreen';
import { ContractionTimerScreen } from '../screens/ContractionTimerScreen';
import { GrowthChartScreen } from '../screens/GrowthChartScreen';
import { VaccinationScreen } from '../screens/VaccinationScreen';

const Tab = createBottomTabNavigator();
const Stack = createNativeStackNavigator();

function HomeStack() {
  return (
    <Stack.Navigator>
      <Stack.Screen name={Routes.Timeline} component={TimelineScreen} options={{ title: 'This Week' }} />
      <Stack.Screen name={Routes.SymptomLog} component={SymptomLogScreen} options={{ title: 'Log Symptoms' }} />
      <Stack.Screen name={Routes.VitalsWeight} component={VitalsWeightScreen} options={{ title: 'Weight' }} />
      <Stack.Screen name={Routes.VitalsBP} component={VitalsBPScreen} options={{ title: 'Blood Pressure' }} />
      <Stack.Screen name={Routes.KickCounter} component={KickCounterScreen} options={{ title: 'Kick Counter' }} />
      <Stack.Screen name={Routes.ContractionTimer} component={ContractionTimerScreen} options={{ title: 'Contractions' }} />
      <Stack.Screen name={Routes.GrowthChart} component={GrowthChartScreen} options={{ title: 'Growth Chart' }} />
      <Stack.Screen name={Routes.Vaccination} component={VaccinationScreen} options={{ title: 'Vaccinations' }} />
    </Stack.Navigator>
  );
}

export function MainNavigator() {
  return (
    <Tab.Navigator>
      <Tab.Screen name="Home" component={HomeStack} options={{ headerShown: false, tabBarLabel: 'Home' }} />
      <Tab.Screen name={Routes.SummaryCard} component={SummaryCardScreen} options={{ title: 'Summary' }} />
      <Tab.Screen name={Routes.DueDateClub} component={DueDateClubScreen} options={{ title: 'Community' }} />
    </Tab.Navigator>
  );
}
