import React from 'react';
import { Slot } from 'expo-router';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { Providers } from '../src/providers';

export default function RootLayout() {
  return (
    <SafeAreaProvider>
      <Providers>
        <Slot />
      </Providers>
    </SafeAreaProvider>
  );
}
