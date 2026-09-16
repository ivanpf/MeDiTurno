import * as Notifications from 'expo-notifications';
import * as Constants from 'expo-constants';
import { Platform } from 'react-native';
import { API_BASE_URL } from './api';

Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowAlert: true,
    shouldPlaySound: true,
    shouldSetBadge: false,
  }),
});

export async function registerForPushNotificationsAsync() {
  const projectId = Constants.expoConfig?.extra?.eas?.projectId
    ?? Constants.easConfig?.projectId
    ?? '50046a95-963e-475a-ac63-5209e2658977';

  if (Platform.OS === 'android') {
    await Notifications.setNotificationChannelAsync('default', {
      name: 'MeDiTurno',
      importance: Notifications.AndroidImportance.HIGH,
      vibrationPattern: [0, 250, 250, 250],
      lightColor: '#007bff',
    });
  }

  const { status: existingStatus } = await Notifications.getPermissionsAsync();
  let finalStatus = existingStatus;

  if (existingStatus !== 'granted') {
    const { status } = await Notifications.requestPermissionsAsync();
    finalStatus = status;
  }

  if (finalStatus !== 'granted') {
    alert('No se obtuvieron permisos para notificaciones push');
    return null;
  }

  if (!projectId) {
    console.error('No se encontró projectId en app.json');
    return null;
  }

  const token = (await Notifications.getExpoPushTokenAsync({ projectId })).data;
  console.log('Expo Push Token:', token);

  return token;
}

export async function sendTokenToBackend(token) {
  try {
    const response = await fetch(`${API_BASE_URL}/notificar/register`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ token }),
    });

    if (!response.ok) {
      throw new Error('Error al registrar token');
    }

    return await response.json();
  } catch (error) {
    console.error('Error enviando token al backend:', error);
    throw error;
  }
}

export function setupNotificationListeners() {
  const notificationListener = Notifications.addNotificationReceivedListener(
    (notification) => {
      console.log('Notificación recibida:', notification);
    }
  );

  const responseListener = Notifications.addNotificationResponseReceivedListener(
    (response) => {
      console.log('Usuario tocó la notificación:', response);
    }
  );

  return () => {
    Notifications.removeNotificationSubscription(notificationListener);
    Notifications.removeNotificationSubscription(responseListener);
  };
}
