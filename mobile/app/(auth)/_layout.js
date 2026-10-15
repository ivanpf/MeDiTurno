import React from 'react';
import { Stack } from 'expo-router';
import { View, StyleSheet } from 'react-native';

const estilos = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: '#f4f7fb',
  },
});

export default function AuthLayout() {
  return (
    <Stack
      screenOptions={{
        headerShown: false,
        contentStyle: estilos.screen,
        animation: 'fade',
      }}
    >
      <Stack.Screen name="login" />
      <Stack.Screen name="registro" />
      <Stack.Screen name="recuperar-acceso" />
    </Stack>
  );
}
