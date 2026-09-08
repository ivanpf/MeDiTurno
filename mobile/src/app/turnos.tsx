import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, FlatList, StyleSheet } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { TurnoItem } from '@/components/turno-item';
import { BottomTabInset, MaxContentWidth, Spacing } from '@/constants/theme';
import { getTurnos, type Turno } from '@/lib/api';
import { prepararCanal, recordarTurno } from '@/lib/notificaciones';

/** Cuantos turnos se piden por vez. */
const POR_PAGINA = 20;

export default function TurnosScreen() {
  // Los turnos que ya se descargaron. Cada pagina nueva se SUMA a esta lista,
  // no la reemplaza: por eso el scroll parece infinito.
  const [turnos, setTurnos] = useState<Turno[]>([]);

  // Cual es la proxima pagina a pedir.
  const [proximaPagina, setProximaPagina] = useState(0);

  // Si el backend ya dijo que no quedan mas paginas.
  const [hayMas, setHayMas] = useState(true);

  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Mensaje corto que confirma que se programo el recordatorio.
  const [aviso, setAviso] = useState<string | null>(null);

  const cargarPagina = useCallback(
    async (pagina: number, reemplazar: boolean) => {
      setCargando(true);
      setError(null);

      const resultado = await getTurnos(pagina, POR_PAGINA);

      if (!resultado.ok) {
        setError(resultado.error);
        setCargando(false);
        return;
      }

      const { contenido, ultima } = resultado.pagina;

      // Al refrescar se reemplaza la lista; al scrollear se agrega al final.
      setTurnos((previos) => (reemplazar ? contenido : [...previos, ...contenido]));
      setProximaPagina(pagina + 1);
      setHayMas(!ultima);
      setCargando(false);
    },
    [],
  );

  // Primera carga, apenas se abre la pantalla.
  useEffect(() => {
    cargarPagina(0, true);
  }, [cargarPagina]);

  // El canal de Android se crea una sola vez, al abrir la pantalla.
  useEffect(() => {
    prepararCanal();
  }, []);

  /** Al tocar un turno se programa una notificacion local. */
  async function alTocarTurno(turno: Turno) {
    const listo = await recordarTurno(turno);

    setAviso(
      listo
        ? 'Te avisamos en 5 segundos. Podes minimizar la app.'
        : 'Falta el permiso de notificaciones',
    );

    // El aviso se borra solo para no quedar pegado en pantalla.
    setTimeout(() => setAviso(null), 6000);
  }

  /**
   * Se dispara cuando el usuario se acerca al final de la lista.
   *
   * Los dos guardas son imprescindibles: FlatList llama a onEndReached varias
   * veces seguidas mientras se scrollea, y sin ellos se pedirian la misma
   * pagina cinco veces y apareceria todo duplicado.
   */
  function cargarMas() {
    if (cargando || !hayMas) {
      return;
    }

    cargarPagina(proximaPagina, false);
  }

  function refrescar() {
    setHayMas(true);
    cargarPagina(0, true);
  }

  return (
    <ThemedView style={styles.contenedor}>
      <SafeAreaView style={styles.safeArea}>
        <ThemedView style={styles.encabezado}>
          <ThemedText type="subtitle">Turnos</ThemedText>
          <ThemedText type="small" themeColor="textSecondary">
            {turnos.length} cargados
          </ThemedText>
        </ThemedView>

        {error && (
          <ThemedText type="small" style={styles.error}>
            {error}
          </ThemedText>
        )}

        {aviso && (
          <ThemedText type="small" style={styles.aviso}>
            {aviso}
          </ThemedText>
        )}

        <FlatList
          data={turnos}
          // Identifica cada fila. React lo usa para no redibujar todo cuando se
          // agregan turnos nuevos al final.
          keyExtractor={(turno) => String(turno.id)}
          renderItem={({ item }) => <TurnoItem turno={item} onPress={alTocarTurno} />}
          contentContainerStyle={styles.lista}
          // Cuando falta el 30% final de la lista, pide la pagina siguiente.
          onEndReached={cargarMas}
          onEndReachedThreshold={0.3}
          // Tirar hacia abajo vuelve a empezar desde la pagina 0.
          refreshing={cargando && turnos.length === 0}
          onRefresh={refrescar}
          ListFooterComponent={
            <ThemedView style={styles.pie}>
              {cargando && turnos.length > 0 && <ActivityIndicator />}
              {!hayMas && turnos.length > 0 && (
                <ThemedText type="small" themeColor="textSecondary">
                  No hay mas turnos
                </ThemedText>
              )}
            </ThemedView>
          }
        />
      </SafeAreaView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  contenedor: {
    flex: 1,
    flexDirection: 'row',
    justifyContent: 'center',
  },
  safeArea: {
    flex: 1,
    paddingHorizontal: Spacing.four,
    gap: Spacing.three,
    maxWidth: MaxContentWidth,
    width: '100%',
  },
  encabezado: {
    gap: Spacing.one,
    paddingTop: Spacing.four,
  },
  lista: {
    gap: Spacing.two,
    paddingBottom: BottomTabInset + Spacing.four,
  },
  pie: {
    paddingVertical: Spacing.four,
    alignItems: 'center',
  },
  error: {
    color: '#e5484d',
  },
  aviso: {
    color: '#2f9e57',
  },
});
