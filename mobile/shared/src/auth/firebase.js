/**
 * Configuración de Firebase para React Native.
 * Usa los datos públicos del proyecto (los mismos que la web), leídos de
 * app.json → extra vía expo-constants. Los valores de respaldo permiten que la
 * app funcione aunque no estén definidos en app.json.
 */

import Constants from 'expo-constants';
import { initializeApp, getApps } from 'firebase/app';
import { getAuth, initializeAuth, getReactNativePersistence } from 'firebase/auth';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Platform } from 'react-native';

const extra = Constants.expoConfig?.extra ?? {};

const firebaseConfig = {
  apiKey: extra.firebaseApiKey ?? 'AIzaSyBOnEVcO4U5g79lNqwF21idgrYM8M4ZjJM',
  authDomain: extra.firebaseAuthDomain ?? 'mediturno-auth-db915.firebaseapp.com',
  projectId: extra.firebaseProjectId ?? 'mediturno-auth-db915',
  appId: extra.firebaseAppId ?? '1:364481560232:web:80098cbaa6c85a09d28c9d',
};

// Inicializa la app de Firebase (solo una vez)
let app;
if (getApps().length === 0) {
  app = initializeApp(firebaseConfig);
} else {
  app = getApps()[0];
}

// Inicializa Auth con persistencia en AsyncStorage
let auth;
if (Platform.OS === 'web') {
  auth = getAuth(app);
} else {
  try {
    auth = initializeAuth(app, {
      persistence: getReactNativePersistence(AsyncStorage),
    });
  } catch (error) {
    // Si ya fue inicializado previamente (por Fast Refresh o re-evaluación), obtener la instancia activa
    auth = getAuth(app);
  }
}

export { app, auth, firebaseConfig };
