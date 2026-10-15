// CU-8 - Reservar consulta (pendiente de implementar).
// Equivalente a frontend/src/features/reservas/ReservarConsultaPage.jsx.

import React from 'react';
import { ScrollView, StyleSheet } from 'react-native';
import PaginaPendiente from '../../shared/src/components/PaginaPendiente';
import { COLORS } from '../../shared/src/theme/colors';

const estilos = StyleSheet.create({
  contenedor: { flex: 1, backgroundColor: COLORS.fondo },
  contenido: { padding: 16 },
});

export default function ReservarScreen() {
  return (
    <ScrollView style={estilos.contenedor} contentContainerStyle={estilos.contenido}>
      <PaginaPendiente cu="CU-8" titulo="Reservar consulta" archivo="app/(app)/reservar.js" />
    </ScrollView>
  );
}
