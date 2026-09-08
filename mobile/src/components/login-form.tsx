import { useState } from 'react';
import { ActivityIndicator, Pressable, StyleSheet, TextInput } from 'react-native';

import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { login, type Usuario } from '@/lib/api';

export type LoginFormProps = {
  /** Se llama con el usuario que devolvio el backend. */
  onLogin: (usuario: Usuario) => void;
};

export function LoginForm({ onLogin }: LoginFormProps) {
  const colors = useTheme();

  // Cada campo del formulario necesita su propio estado. En React el input no
  // "guarda" lo que escribis: vos le decis que mostrar (value) y el te avisa
  // cuando el usuario tipea (onChangeText). Por eso van de a pares.
  const [email, setEmail] = useState('');
  const [clave, setClave] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  const completo = email.trim().length > 0 && clave.length > 0;

  // async porque hay que esperar la respuesta del servidor. Mientras tanto
  // enviando queda en true y el boton muestra el indicador de carga.
  async function ingresar() {
    setEnviando(true);
    setError(null);

    const resultado = await login(email.trim().toLowerCase(), clave);

    setEnviando(false);

    if (!resultado.ok) {
      setError(resultado.error);
      return;
    }

    onLogin(resultado.usuario);
  }

  return (
    <ThemedView style={styles.contenedor}>
      <ThemedView style={styles.encabezado}>
        <ThemedText type="title">MeDiTurno</ThemedText>
        <ThemedText type="small" themeColor="textSecondary">
          Ingresa para continuar
        </ThemedText>
      </ThemedView>

      <ThemedView type="backgroundElement" style={styles.tarjeta}>
        <ThemedView type="backgroundElement" style={styles.campo}>
          <ThemedText type="smallBold">Email</ThemedText>
          <TextInput
            value={email}
            onChangeText={setEmail}
            placeholder="demo@mediturno.com"
            placeholderTextColor={colors.textSecondary}
            autoCapitalize="none"
            autoCorrect={false}
            keyboardType="email-address"
            editable={!enviando}
            style={[styles.input, { color: colors.text, backgroundColor: colors.background }]}
          />
        </ThemedView>

        <ThemedView type="backgroundElement" style={styles.campo}>
          <ThemedText type="smallBold">Clave</ThemedText>
          <TextInput
            value={clave}
            onChangeText={setClave}
            placeholder="****"
            placeholderTextColor={colors.textSecondary}
            // Muestra puntitos en vez del texto.
            secureTextEntry
            autoCapitalize="none"
            editable={!enviando}
            style={[styles.input, { color: colors.text, backgroundColor: colors.background }]}
          />
        </ThemedView>

        {/* El mensaje solo existe cuando hay error: si error es null, React no
            dibuja nada y el espacio no queda reservado. */}
        {error && (
          <ThemedText type="small" style={styles.error}>
            {error}
          </ThemedText>
        )}
      </ThemedView>

      <Pressable
        onPress={ingresar}
        disabled={!completo || enviando}
        style={({ pressed }) => [
          styles.boton,
          { backgroundColor: colors.backgroundSelected },
          (pressed || !completo || enviando) && styles.botonApagado,
        ]}>
        {enviando ? <ActivityIndicator /> : <ThemedText type="smallBold">Ingresar</ThemedText>}
      </Pressable>

      <ThemedText type="small" themeColor="textSecondary" style={styles.ayuda}>
        Datos de prueba: demo@mediturno.com / 1234
      </ThemedText>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  contenedor: {
    gap: Spacing.four,
  },
  encabezado: {
    gap: Spacing.one,
  },
  tarjeta: {
    gap: Spacing.three,
    padding: Spacing.four,
    borderRadius: Spacing.four,
  },
  campo: {
    gap: Spacing.one,
  },
  input: {
    paddingHorizontal: Spacing.three,
    paddingVertical: Spacing.two,
    borderRadius: Spacing.two,
    fontSize: 16,
  },
  error: {
    color: '#e5484d',
  },
  boton: {
    paddingVertical: Spacing.three,
    borderRadius: Spacing.three,
    alignItems: 'center',
  },
  botonApagado: {
    opacity: 0.6,
  },
  ayuda: {
    textAlign: 'center',
  },
});
