// CU-3 Cambio de contraseña (con sesión iniciada, app de pacientes).
// Las contraseñas las guarda Firebase: el cambio se hace directo con el SDK de Firebase Auth
// (reautenticación con la contraseña actual + updatePassword), igual que en la web.
// Al terminar, la sesión sigue abierta: no hace falta volver a ingresar.

import React from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { useCambiarContrasena } from '../../src/features/cuenta/useCambiarContrasena';
import { ErrorBanner } from '../../shared/src/components/ErrorBanner';
import { Input } from '../../shared/src/components/Input';
import { Button } from '../../shared/src/components/Button';
import { COLORS } from '../../shared/src/theme/colors';

const MINIMO_CONTRASENA = 6;

const estilos = StyleSheet.create({
  contenedor: { flex: 1, backgroundColor: COLORS.fondo },
  contenido: { alignItems: 'center', padding: 16, paddingBottom: 40 },
  panel: {
    backgroundColor: COLORS.blanco, borderColor: COLORS.borde, borderWidth: 1,
    borderRadius: 12, padding: 20, gap: 4, width: '100%', maxWidth: 520,
  },
  titulo: { fontSize: 24, fontWeight: '700', color: COLORS.azulOscuro },
  texto: { fontSize: 15, lineHeight: 22, color: COLORS.gris, marginBottom: 12 },
  exito: {
    backgroundColor: COLORS.exitoClaro, borderLeftWidth: 4, borderLeftColor: COLORS.exito,
    borderRadius: 8, padding: 12, marginBottom: 12,
  },
  exitoTexto: { fontSize: 14, lineHeight: 20, color: COLORS.exito },
  filaBotones: { flexDirection: 'row', gap: 12, marginTop: 8 },
  mitad: { flex: 1 },
});

export default function CambiarContrasenaScreen() {
  const { datos, error, modificada, cargando, cambiarCampo, guardar, limpiar, limpiarError } = useCambiarContrasena();

  return (
    <ScrollView style={estilos.contenedor} contentContainerStyle={estilos.contenido}>
      <View style={estilos.panel}>
        <Text style={estilos.titulo}>Cambiar contraseña</Text>
        <Text style={estilos.texto}>
          Complete la contraseña actual y la nueva (mínimo {MINIMO_CONTRASENA} caracteres).
        </Text>

        {modificada && (
          <View style={estilos.exito} accessibilityLiveRegion="polite">
            <Text style={estilos.exitoTexto}>La contraseña fue modificada correctamente.</Text>
          </View>
        )}
        {error && <ErrorBanner mensaje={error.mensaje} detalles={error.detalles} onDismiss={limpiarError} />}

        <Input
          label="Contraseña actual"
          placeholder="••••••"
          value={datos.actual}
          onChangeText={cambiarCampo('actual')}
          secureTextEntry
          autoComplete="current-password"
          required
          disabled={cargando}
        />
        <Input
          label="Nueva contraseña"
          placeholder="••••••"
          value={datos.nueva}
          onChangeText={cambiarCampo('nueva')}
          secureTextEntry
          autoComplete="new-password"
          minLength={MINIMO_CONTRASENA}
          maxLength={128}
          required
          disabled={cargando}
        />
        <Input
          label="Repetir nueva contraseña"
          placeholder="••••••"
          value={datos.repetirNueva}
          onChangeText={cambiarCampo('repetirNueva')}
          secureTextEntry
          autoComplete="new-password"
          maxLength={128}
          required
          disabled={cargando}
        />

        <View style={estilos.filaBotones}>
          <Button variant="primary" disabled={cargando} onPress={guardar} style={estilos.mitad}>
            {cargando ? 'Guardando…' : 'Guardar'}
          </Button>
          <Button variant="secondary" disabled={cargando} onPress={limpiar} style={estilos.mitad}>
            Cancelar
          </Button>
        </View>
      </View>
    </ScrollView>
  );
}
