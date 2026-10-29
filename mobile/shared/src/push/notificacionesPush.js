/**
 * Notificaciones push de la app del paciente (Firebase Cloud Messaging; el backend envía directo por FCM).
 *
 * Flujo:
 *  1. Al iniciar sesión se pide permiso y, si lo concede, se obtiene el token de FCM del
 *     dispositivo y se registra en el backend (POST /api/notificaciones/push, autenticado:
 *     el token queda asociado al usuario de la sesión).
 *  2. Al cerrar sesión se elimina el token registrado (DELETE /api/notificaciones/push).
 *  3. Mientras la app está abierta, los avisos que llegan se muestran con los listeners.
 *
 * Las llamadas al backend son best-effort: las notificaciones nunca tienen que frenar ni
 * romper el uso de la app (si el registro falla, se reintenta en el próximo inicio de sesión).
 */

import * as Notifications from 'expo-notifications';
import { Platform } from 'react-native';
import { registrarPushToken, eliminarPushToken } from '../api/endpoints';
import { COLORS } from '../theme/colors';

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
 * @returns {Promise<string|null>} el token de FCM registrado, o null si no se pudo.
 */
export async function pedirPermisoYRegistrarToken() {
  try {
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

    // Token nativo de Firebase Cloud Messaging: el backend envía directo por FCM, sin pasar por Expo.
    const token = (await Notifications.getDevicePushTokenAsync()).data;
    await registrarPushToken(token);
    return token;
  } catch (error) {
    // No se bloquea el inicio de sesión por el registro del dispositivo.
    console.warn('[push] No se pudo registrar el token:', error?.message ?? error);
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
    alRecibir.remove();
    alResponder.remove();
  };
}
