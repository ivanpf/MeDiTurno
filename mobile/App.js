import React, { useEffect, useState } from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import ReservasScreen from './src/screens/ReservasScreen';
import { registerForPushNotificationsAsync, sendTokenToBackend, setupNotificationListeners } from './src/services/notifications';

const Stack = createNativeStackNavigator();

export default function App() {
  useEffect(() => {
    const initializeNotifications = async () => {
      try {
        const token = await registerForPushNotificationsAsync();
        if (token) {
          await sendTokenToBackend(token);
          console.log('Token registrado en el backend');
        }
      } catch (error) {
        console.error('Error initializing notifications:', error);
      }
    };

    initializeNotifications();

    const cleanup = setupNotificationListeners();
    return cleanup;
  }, []);

  return (
    <NavigationContainer>
      <Stack.Navigator initialRouteName="Reservas">
        <Stack.Screen
          name="Reservas"
          component={ReservasScreen}
          options={{ title: 'Sistema de Reservas' }}
        />
      </Stack.Navigator>
    </NavigationContainer>
  );
}
