import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import ReservasScreen from './src/screens/ReservasScreen';

const Stack = createNativeStackNavigator();

export default function App() {
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
