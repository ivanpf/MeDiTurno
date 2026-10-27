/**
 * Notificaciones push de la app del paciente (Expo Push, la misma mecánica de la POC del equipo).
 *
 * Flujo:
 *  1. Al iniciar sesión se pide permiso y, si lo concede, se obtiene el Expo Push Token del
 *     dispositivo y se registra en el backend (POST /api/notificaciones/push, autenticado:
 *     el token queda asociado al usuario de la sesión).
 *  2. Al cerrar sesión se elimina el token registrado (DELETE /api/notificaciones/push).
 *  3. Mientras la app está abierta, los avisos que llegan se muestran con los listeners.
 *
 * Las llamadas al backend son best-effort: las notificaciones nunca tienen que frenar ni
 * romper el uso de la app (si el registro falla, se reintenta en el próximo inicio de sesión).
 */

import * as Notifications from 'expo-notifications';
import Constants from 'expo-constants';
import { Platform } from 'react-native';
import { registrarPushToken, eliminarPushToken } from '../api/endpoints';
import { COLORS } from '../theme/colors';

// Identificador del proyecto EAS, requerido por getExpoPushTokenAsync (viene de app.json).
const PROJECT_ID = Constants.expoConfig?.extra?.eas?.projectId;

// Mientras la app esté abierta en primer plano, los avisos se muestran como banner y suena el tono por defecto.
Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowBanner: true,
    shouldShowList: true,
    shouldPlaySound: true,
    shouldSetBadge: false,
  }),
});

/**
 * Pide permisos de notificaciones y, si el dispositivo lo concede y no estaba registrado,
 * registra el token del dispositivo en el backend a nombre del usuario con sesión.
 * @returns {Promise<string|null>} el Expo Push Token registrado, o null si no se pudo.
 */
export async function pedirPermisoYRegistrarToken() {
  try {
    if (!PROJECT_ID) {
      return null;
    }

    // En Android hay que crear el canal antes de pedir los permisos.
    if (Platform.OS === 'android') {
      await Notifications.setNotificationChannelAsync('default', {
        name: 'MeDiTurno',
        importance: Notifications.AndroidImportance.HIGH,
        vibrationPattern: [0, 250, 250, 250],
        lightColor: COLORS.azul,
      });
    }

    const { status: existente } = await Notifications.getPermissionsAsync();
    let estado = existente;
    if (estado !== 'granted') {
      estado = (await Notifications.requestPermissionsAsync()).status;
    }
    if (estado !== 'granted') {
      // Sin permisos la app funciona igual; solo no llegan avisos al dispositivo.
      return null;
    }

    const token = (await Notifications.getExpoPushTokenAsync({ projectId: PROJECT_ID })).data;
    await registrarPushToken({ token });
    return token;
  } catch {
    // No se bloquea el inicio de sesión por el registro del dispositivo.
    return null;
  }
}

/**
 * Elimina el token del dispositivo registrado (se usa al cerrar sesión).
 * @returns {Promise<void>}
 */
export async function eliminarTokenRegistrado() {
  try {
    await eliminarPushToken();
  } catch {
    // Best-effort: el cierre de sesión sigue igual aunque la baja del token falle.
  }
}

/**
 * Suscribe los listeners de avisos mientras la app está abierta.
 * @returns {() => void} función para desuscribir.
 */
export function conectarEscuchaDeNotificaciones() {
  const alRecibir = Notifications.addNotificationReceivedListener((aviso) => {
    // Llegaron mientras la app abierta: el sistema muestra el banner (ver setNotificationHandler).
  });
  const alResponder = Notifications.addNotificationResponseReceivedListener(() => {
    // La persona tocó el aviso: por ahora la app abre en la pantalla de inicio.
  });
  return () => {
    Notifications.removeNotificationSubscription(alRecibir);
    Notifications.removeNotificationSubscription(alResponder);
  };
}
