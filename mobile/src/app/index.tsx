import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, Pressable, StyleSheet, useColorScheme } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { LoginForm } from '@/components/login-form';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { BottomTabInset, Colors, MaxContentWidth, Spacing } from '@/constants/theme';
import { API_BASE_URL, pingBackend, type PingResult, type Usuario } from '@/lib/api';

export default function HomeScreen() {
  const scheme = useColorScheme();
  const colors = Colors[scheme === 'dark' ? 'dark' : 'light'];

  // Quien esta usando la app. null = todavia no ingreso, y por eso ve el login.
  // Por ahora solo guardamos el email; cuando exista el backend guardaremos el
  // usuario completo que devuelva el servidor.
  const [usuario, setUsuario] = useState<Usuario | null>(null);

  const [checking, setChecking] = useState(false);
  const [result, setResult] = useState<PingResult | null>(null);

  const check = useCallback(async () => {
    setChecking(true);
    setResult(await pingBackend());
    setChecking(false);
  }, []);

  // Consulta apenas se abre la pantalla, sin esperar a que toques el boton.
  useEffect(() => {
    check();
  }, [check]);

  // Los hooks de arriba se declaran siempre, pasen las cosas que pasen: React
  // exige que se llamen en el mismo orden en cada dibujado. Recien despues de
  // declararlos podemos cortar y mostrar otra pantalla.
  if (!usuario) {
    return (
      <ThemedView style={styles.container}>
        <SafeAreaView style={styles.safeArea}>
          <LoginForm onLogin={setUsuario} />
        </SafeAreaView>
      </ThemedView>
    );
  }

  return (
    <ThemedView style={styles.container}>
      <SafeAreaView style={styles.safeArea}>
        <ThemedView style={styles.header}>
          <ThemedText type="title">MeDiTurno</ThemedText>
          <ThemedText type="small" themeColor="textSecondary">
            Sesion iniciada como {usuario.nombre}
          </ThemedText>
        </ThemedView>

        <ThemedView type="backgroundElement" style={styles.card}>
          <ThemedText type="small" themeColor="textSecondary">
            Servidor
          </ThemedText>
          <ThemedText type="code">{API_BASE_URL}</ThemedText>

          <ThemedView style={styles.divider} />

          {checking && (
            <ThemedView style={styles.statusRow}>
              <ActivityIndicator />
              <ThemedText type="default">Consultando...</ThemedText>
            </ThemedView>
          )}

          {!checking && result?.ok && (
            <ThemedView style={styles.statusBlock}>
              <ThemedText type="subtitle">Conectado</ThemedText>
              <ThemedText type="default">{result.message}</ThemedText>
              <ThemedText type="small" themeColor="textSecondary">
                Respondio en {result.ms} ms
              </ThemedText>
            </ThemedView>
          )}

          {!checking && result && !result.ok && (
            <ThemedView style={styles.statusBlock}>
              <ThemedText type="subtitle">Sin conexion</ThemedText>
              <ThemedText type="default">{result.error}</ThemedText>
              <ThemedText type="small" themeColor="textSecondary">
                Reviso que los contenedores esten arriba con {'`podman compose ps`'}
              </ThemedText>
            </ThemedView>
          )}
        </ThemedView>

        <Pressable
          onPress={check}
          disabled={checking}
          style={({ pressed }) => [
            styles.button,
            { backgroundColor: colors.backgroundSelected },
            (pressed || checking) && styles.buttonPressed,
          ]}>
          <ThemedText type="smallBold">Volver a probar</ThemedText>
        </Pressable>

        <Pressable onPress={() => setUsuario(null)} style={styles.salir}>
          <ThemedText type="small" themeColor="textSecondary">
            Cerrar sesion
          </ThemedText>
        </Pressable>
      </SafeAreaView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    flexDirection: 'row',
    justifyContent: 'center',
  },
  safeArea: {
    flex: 1,
    paddingHorizontal: Spacing.four,
    paddingBottom: BottomTabInset + Spacing.three,
    gap: Spacing.four,
    justifyContent: 'center',
    maxWidth: MaxContentWidth,
  },
  header: {
    gap: Spacing.one,
  },
  card: {
    gap: Spacing.two,
    padding: Spacing.four,
    borderRadius: Spacing.four,
  },
  divider: {
    height: Spacing.half,
    marginVertical: Spacing.two,
  },
  statusRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.two,
  },
  statusBlock: {
    gap: Spacing.one,
  },
  button: {
    paddingVertical: Spacing.three,
    borderRadius: Spacing.three,
    alignItems: 'center',
  },
  buttonPressed: {
    opacity: 0.6,
  },
  salir: {
    alignItems: 'center',
  },
});
