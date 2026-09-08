import { Pressable, StyleSheet } from 'react-native';

import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Spacing } from '@/constants/theme';
import type { Turno } from '@/lib/api';

/** Color del cartelito segun el estado del turno. */
const COLOR_ESTADO: Record<string, string> = {
  Confirmado: '#2f9e57',
  Pendiente: '#d68910',
  Reprogramado: '#8e6ac4',
};

/**
 * Formatea "2026-09-08T08:00:00" como "08/09 08:00".
 *
 * Se arma a mano en vez de usar toLocaleString porque el formato de fecha
 * depende del idioma del telefono, y en la demo conviene que siempre se vea
 * igual sin importar como este configurado el emulador.
 */
function formatearFecha(iso: string): string {
  const fecha = new Date(iso);

  // padStart agrega un cero adelante: 8 -> "08".
  const dia = String(fecha.getDate()).padStart(2, '0');
  const mes = String(fecha.getMonth() + 1).padStart(2, '0'); // getMonth va de 0 a 11
  const hora = String(fecha.getHours()).padStart(2, '0');
  const minutos = String(fecha.getMinutes()).padStart(2, '0');

  return `${dia}/${mes} ${hora}:${minutos}`;
}

export type TurnoItemProps = {
  turno: Turno;
  /** Que hacer al tocar la fila. */
  onPress: (turno: Turno) => void;
};

/**
 * Una fila de la lista. Es un componente aparte para que la pantalla no tenga
 * que ocuparse de como se ve cada turno, solo de cuales mostrar.
 */
export function TurnoItem({ turno, onPress }: TurnoItemProps) {
  const colorEstado = COLOR_ESTADO[turno.estado] ?? '#60646C';

  return (
    <Pressable onPress={() => onPress(turno)} style={({ pressed }) => pressed && styles.tocada}>
      <ThemedView type="backgroundElement" style={styles.fila}>
      <ThemedView type="backgroundElement" style={styles.encabezado}>
        <ThemedText type="smallBold">{turno.especialidad}</ThemedText>
        <ThemedText type="small" style={{ color: colorEstado }}>
          {turno.estado}
        </ThemedText>
      </ThemedView>

      <ThemedText type="default">{turno.paciente}</ThemedText>

      <ThemedView type="backgroundElement" style={styles.pie}>
        <ThemedText type="small" themeColor="textSecondary">
          {turno.profesional}
        </ThemedText>
        <ThemedText type="small" themeColor="textSecondary">
          {formatearFecha(turno.fechaHora)}
        </ThemedText>
        </ThemedView>
      </ThemedView>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  tocada: {
    opacity: 0.6,
  },
  fila: {
    gap: Spacing.one,
    padding: Spacing.three,
    borderRadius: Spacing.three,
  },
  encabezado: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  pie: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
});
