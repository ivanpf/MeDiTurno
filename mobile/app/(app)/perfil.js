import React from 'react';
import { ScrollView, StyleSheet } from 'react-native';
import PaginaPendiente from '../../shared/src/components/PaginaPendiente';
import { COLORS } from '../../shared/src/theme/colors';

const estilos = StyleSheet.create({
  contenedor: { flex: 1, backgroundColor: COLORS.fondo },
  contenido: { padding: 16 },
});

export default function PerfilScreen() {
  return (
    <ScrollView style={estilos.contenedor} contentContainerStyle={estilos.contenido}>
      <PaginaPendiente cu="CU-5" titulo="Mi perfil" archivo="app/(app)/perfil.js" />
    </ScrollView>
  );
}
