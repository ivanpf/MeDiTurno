// Pantalla temporal de un caso de uso que todavía no se implementó.
// Equivalente a frontend/src/components/PaginaPendiente.jsx.
// Al desarrollar el caso de uso, reemplazar el contenido de la pantalla que la usa.

import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { COLORS } from '../theme/colors';

const estilos = StyleSheet.create({
  panel: {
    padding: 24,
    backgroundColor: COLORS.blanco,
    borderRadius: 12,
    shadowColor: COLORS.azulOscuro,
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.12,
    shadowRadius: 3,
    elevation: 3,
  },
  titulo: {
    marginBottom: 12,
    fontSize: 26,
    fontWeight: '600',
    lineHeight: 32,
    color: COLORS.azulOscuro,
  },
  subrayado: {
    width: 40,
    height: 3,
    marginBottom: 20,
    borderRadius: 2,
    backgroundColor: COLORS.mostaza,
  },
  pendiente: {
    paddingVertical: 16,
    paddingHorizontal: 18,
    backgroundColor: COLORS.mostazaClaro,
    borderLeftWidth: 5,
    borderLeftColor: COLORS.mostaza,
    borderRadius: 8,
  },
  pendienteTexto: {
    fontSize: 15,
    lineHeight: 22,
    color: COLORS.mostazaOscuro,
  },
  negrita: {
    fontWeight: '700',
  },
});

export default function PaginaPendiente({ cu, titulo, archivo }) {
  return (
    <View style={estilos.panel}>
      <Text style={estilos.titulo}>{titulo}</Text>
      <View style={estilos.subrayado} />
      <View style={estilos.pendiente}>
        <Text style={estilos.pendienteTexto}>
          <Text style={estilos.negrita}>{cu}</Text>
          {' — pendiente de implementar.'}
          {archivo ? ` Se desarrolla en ${archivo}.` : ''}
        </Text>
      </View>
    </View>
  );
}
