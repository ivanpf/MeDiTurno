// CU-5: consulta del perfil real y edición de los datos de contacto.
import React from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
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
  const { perfil, datos, setDatos, cargando, error, exito, actualizar, descartar, setError, reintentar } = usePerfil();
  const cambiar = (campo, valor) => setDatos((d) => ({ ...d, [campo]: valor }));
  return (
    <ScrollView style={estilos.contenedor} contentContainerStyle={estilos.contenido}>
      <View style={estilos.panel}>
        <Text style={estilos.titulo}>Mi perfil</Text>
        <Text style={estilos.texto}>Sus datos de identidad y de contacto.</Text>
        {exito && <View style={estilos.exito} accessibilityLiveRegion="polite"><Text style={estilos.exitoTexto}>Perfil actualizado correctamente.</Text></View>}
        {error && <ErrorBanner mensaje={error.message ?? error.mensaje} onDismiss={() => setError(null)} />}
        {cargando && <Text accessibilityLiveRegion="polite">{perfil ? 'Guardando…' : 'Cargando perfil…'}</Text>}
        {!perfil && !cargando && <Button onPress={reintentar}>Reintentar</Button>}
        {perfil && <>
          <View style={estilos.datos}>
            {[['Nombre', perfil.nombre], ['Cédula', perfil.cedula], ['Perfil', 'Paciente']].map(([etiqueta, valor]) => (
              <View key={etiqueta} style={estilos.filaDato}><Text style={estilos.etiqueta}>{etiqueta}</Text><Text style={estilos.valor}>{valor}</Text></View>
            ))}
          </View>
          <Input label="Correo electrónico" value={datos.correo} onChangeText={(v) => cambiar('correo', v)} keyboardType="email-address" autoCapitalize="none" maxLength={255} disabled={cargando} />
          <Text style={estilos.texto}>Si cambia su correo, utilice el nuevo para su próximo inicio de sesión.</Text>
          <Input label="Teléfono (opcional)" value={datos.telefono} onChangeText={(v) => cambiar('telefono', v)} maxLength={30} disabled={cargando} />
          <Input label="Domicilio (opcional)" value={datos.domicilio} onChangeText={(v) => cambiar('domicilio', v)} maxLength={255} disabled={cargando} />
          <View style={estilos.filaBotones}>
            <Button variant="primary" disabled={cargando} onPress={actualizar} style={estilos.mitad}>Guardar cambios</Button>
            <Button variant="secondary" disabled={cargando} onPress={descartar} style={estilos.mitad}>Descartar</Button>
          </View>
        </>}
      </View>
    </ScrollView>
  );
}
