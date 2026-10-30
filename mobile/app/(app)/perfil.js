// CU-5 Consulta y edición del perfil (app de pacientes).
// La identidad (nombre, correo) la maneja Firebase y no es editable; la edición de contacto
// usa el mismo contrato que la web. usePerfil (shared) ya valida y llama al endpoint.
// TODO (CU-5): el PerfilDTO del backend no definió aún sus campos; se asume { telefono, domicilio }
// (los mismos de CU-7). Si el backend los cambia, ajustar usePerfil y esta pantalla.
// TODO (CU-5): no hay endpoint GET de perfil, así que el teléfono y el domicilio actuales
// no se pueden precargar; los campos arrancan vacíos hasta que el backend lo provea.

import React, { useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { useAuth } from '../../shared/src/auth/useAuth';
import { auth } from '../../shared/src/auth/firebase';
import { usePerfil } from '../../shared/src/hooks/usePerfil';
import { ErrorBanner } from '../../shared/src/components/ErrorBanner';
import { Input } from '../../shared/src/components/Input';
import { Button } from '../../shared/src/components/Button';
import { COLORS } from '../../shared/src/theme/colors';

const estilos = StyleSheet.create({
  contenedor: { flex: 1, backgroundColor: COLORS.fondo },
  contenido: { alignItems: 'center', padding: 16, paddingBottom: 40 },
  panel: {
    backgroundColor: COLORS.blanco, borderColor: COLORS.borde, borderWidth: 1,
    borderRadius: 12, padding: 20, gap: 4, width: '100%', maxWidth: 520,
  },
  titulo: { fontSize: 24, fontWeight: '700', color: COLORS.azulOscuro },
  texto: { fontSize: 15, lineHeight: 22, color: COLORS.gris, marginBottom: 12 },
  // Consulta: datos de solo lectura, de a un dato por fila.
  datos: { borderWidth: 1, borderColor: COLORS.borde, borderRadius: 8, marginBottom: 16, overflow: 'hidden' },
  filaDato: { paddingVertical: 10, paddingHorizontal: 14, borderTopWidth: 1, borderTopColor: COLORS.borde },
  primera: { borderTopWidth: 0 },
  etiqueta: { fontSize: 13, fontWeight: '700', color: COLORS.gris },
  valor: { fontSize: 15, color: COLORS.texto, marginTop: 2 },
  aclaracion: { fontSize: 13, fontWeight: '700', color: COLORS.gris, marginVertical: 6 },
  exito: {
    backgroundColor: COLORS.exitoClaro, borderLeftWidth: 4, borderLeftColor: COLORS.exito,
    borderRadius: 8, padding: 12, marginBottom: 12,
  },
  exitoTexto: { fontSize: 14, lineHeight: 20, color: COLORS.exito },
  filaBotones: { flexDirection: 'row', gap: 12, marginTop: 8 },
  mitad: { flex: 1 },
});

export default function PerfilScreen() {
  const { nombre } = useAuth();
  const { cargando, error, exito, actualizar, setError } = usePerfil();
  const [telefono, setTelefono] = useState('');
  const [domicilio, setDomicilio] = useState('');

  const limpiar = () => {
    setTelefono('');
    setDomicilio('');
    setError(null);
  };

  const guardar = async () => {
    // usePerfil valida los límites (teléfono máx. 30, domicilio máx. 255) y llama al PUT.
    // De aquí sale true si el perfil quedó actualizado.
    const ok = await actualizar({ telefono: telefono.trim(), domicilio: domicilio.trim() });
    if (ok) {
      setTelefono('');
      setDomicilio('');
    }
  };

  return (
    <ScrollView style={estilos.contenedor} contentContainerStyle={estilos.contenido}>
      <View style={estilos.panel}>
        <Text style={estilos.titulo}>Mi perfil</Text>
        <Text style={estilos.texto}>Sus datos de identidad y de contacto.</Text>

        {exito && (
          <View style={estilos.exito} accessibilityLiveRegion="polite">
            <Text style={estilos.exitoTexto}>Perfil actualizado correctamente.</Text>
          </View>
        )}
        {error && <ErrorBanner mensaje={error.message ?? 'No se pudo actualizar el perfil'} onDismiss={() => setError(null)} />}

        {/* Consulta: la identidad no es editable (la maneja Firebase). */}
        <View style={estilos.datos}>
          <View style={[estilos.filaDato, estilos.primera]}>
            <Text style={estilos.etiqueta}>Nombre</Text>
            <Text style={estilos.valor}>{nombre}</Text>
          </View>
          <View style={estilos.filaDato}>
            <Text style={estilos.etiqueta}>Correo electrónico</Text>
            <Text style={estilos.valor}>{auth.currentUser?.email ?? ''}</Text>
          </View>
          <View style={estilos.filaDato}>
            <Text style={estilos.etiqueta}>Perfil</Text>
            <Text style={estilos.valor}>Paciente</Text>
          </View>
        </View>

        <Text style={estilos.aclaracion}>Datos de contacto (opcionales)</Text>
        <Input
          label="Teléfono"
          placeholder="099 123 456"
          value={telefono}
          onChangeText={setTelefono}
          keyboardType="default"
          maxLength={30}
          disabled={cargando}
        />
        <Input
          label="Domicilio"
          placeholder="Dirección"
          value={domicilio}
          onChangeText={setDomicilio}
          maxLength={255}
          disabled={cargando}
        />

        <View style={estilos.filaBotones}>
          <Button variant="primary" disabled={cargando} onPress={guardar} style={estilos.mitad}>
            {cargando ? 'Guardando…' : 'Guardar cambios'}
          </Button>
          <Button variant="secondary" disabled={cargando} onPress={limpiar} style={estilos.mitad}>
            Descartar
          </Button>
        </View>
      </View>
    </ScrollView>
  );
}
