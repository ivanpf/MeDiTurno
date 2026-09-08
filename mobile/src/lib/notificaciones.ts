import * as Notifications from 'expo-notifications';
import { Platform } from 'react-native';

import type { Turno } from '@/lib/api';

/**
 * Notificaciones LOCALES: las dispara la propia app, no un servidor.
 *
 * Se ven igual que una push remota (banner, sonido, icono), pero no necesitan
 * Firebase, ni cuenta de Expo, ni que el telefono este registrado en ningun
 * lado. Para una push de verdad haria falta un servidor que le hable al
 * dispositivo, y credenciales de FCM.
 */

/**
 * Que hacer cuando llega una notificacion y la app esta ABIERTA.
 *
 * Sin esto, Android la guarda en la bandeja pero no la muestra encima de la
 * app, y en una demo parece que no funciono.
 */
Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowBanner: true,
    shouldShowList: true,
    shouldPlaySound: true,
    shouldSetBadge: false,
  }),
});

/** Nombre del canal de Android donde se agrupan estas notificaciones. */
const CANAL = 'turnos';

/**
 * Android agrupa las notificaciones en "canales", y el usuario puede silenciar
 * cada uno por separado desde los ajustes del sistema. Si no se crea ninguno,
 * las notificaciones pueden no sonar.
 */
export async function prepararCanal(): Promise<void> {
  if (Platform.OS !== 'android') {
    return;
  }

  await Notifications.setNotificationChannelAsync(CANAL, {
    name: 'Turnos',
    importance: Notifications.AndroidImportance.DEFAULT,
  });
}

/**
 * Pide permiso para mostrar notificaciones.
 *
 * Desde Android 13 el permiso es obligatorio y lo tiene que aceptar el usuario;
 * antes se daba por sentado. Si ya fue concedido, no vuelve a preguntar.
 */
export async function pedirPermiso(): Promise<boolean> {
  const actual = await Notifications.getPermissionsAsync();

  if (actual.granted) {
    return true;
  }

  const pedido = await Notifications.requestPermissionsAsync();
  return pedido.granted;
}

/** Cuantos segundos despues del toque aparece el recordatorio. */
const SEGUNDOS = 5;

/**
 * Programa un recordatorio para un turno.
 *
 * Se demora unos segundos a proposito: asi se puede minimizar la app y ver la
 * notificacion llegar desde afuera, que es lo que se espera de una push.
 */
export async function recordarTurno(turno: Turno): Promise<boolean> {
  if (!(await pedirPermiso())) {
    return false;
  }

  await Notifications.scheduleNotificationAsync({
    content: {
      title: `Turno de ${turno.especialidad}`,
      body: `${turno.paciente} con ${turno.profesional}`,
      sound: true,
    },
    trigger: {
      type: Notifications.SchedulableTriggerInputTypes.TIME_INTERVAL,
      seconds: SEGUNDOS,
      channelId: CANAL,
    },
  });

  return true;
}
