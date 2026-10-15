import React from 'react';
import { ScrollView, StyleSheet } from 'react-native';
import PaginaPendiente from '../../shared/src/components/PaginaPendiente';
import { COLORS } from '../../shared/src/theme/colors';

const estilos = StyleSheet.create({
  contenedor: { flex: 1, backgroundColor: COLORS.fondo },
  contenido: { padding: 16 },
});

export default function ReservasScreen() {
  return (
    <ScrollView style={estilos.contenedor} contentContainerStyle={estilos.contenido}>
      <PaginaPendiente cu="CU-9 / CU-10" titulo="Mis reservas" archivo="app/(app)/reservas.js" />
    </ScrollView>
  );
}
