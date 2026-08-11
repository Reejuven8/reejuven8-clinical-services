import React from 'react';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { Routes } from './routes';
import { LoginScreen } from '../screens/auth/LoginScreen';
import { RegisterScreen } from '../screens/auth/RegisterScreen';

const Stack = createNativeStackNavigator();

export function AuthNavigator() {
  return (
    <Stack.Navigator>
      <Stack.Screen name={Routes.Login} component={LoginScreen} options={{ title: 'Sign In' }} />
      <Stack.Screen name={Routes.Register} component={RegisterScreen} options={{ title: 'Create Account' }} />
    </Stack.Navigator>
  );
}
