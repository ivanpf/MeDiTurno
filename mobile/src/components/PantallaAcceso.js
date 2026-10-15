// Estructura de las pantallas sin sesión (login, registro, recuperar acceso):
// panel de marca arriba y el formulario debajo.
// Equivalente a frontend/src/components/PantallaAcceso.jsx (versión celular).

import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import Logo from './Logo';
import { COLORS } from '../../shared/src/theme/colors';

const estilos = StyleSheet.create({
  marca: {
    alignItems: 'center',
    paddingVertical: 32,
    paddingHorizontal: 24,
    backgroundColor: COLORS.azulOscuro,
  },
  titulo: {
    marginTop: 20,
    fontSize: 22,
    fontWeight: '700',
    lineHeight: 28,
    textAlign: 'center',
    color: COLORS.blanco,
  },
  subrayado: {
    width: 40,
    height: 3,
    marginTop: 12,
    borderRadius: 2,
    backgroundColor: COLORS.mostaza,
  },
  texto: {
    marginTop: 12,
    fontSize: 14,
    lineHeight: 20,
    textAlign: 'center',
    color: '#c9dbf3',
  },
  formulario: {
    flex: 1,
    justifyContent: 'center',
    padding: 24,
  },
});

export default function PantallaAcceso({ children }) {
  return (
    <View style={{ flex: 1, backgroundColor: COLORS.fondo }}>
      <View style={estilos.marca}>
        <Logo variante="claro" grande />
        <Text style={estilos.titulo}>Sus turnos médicos, en un solo lugar</Text>
        <View style={estilos.subrayado} />
        <Text style={estilos.texto}>Reserve, consulte y gestione sus consultas de forma simple y segura.</Text>
      </View>
      <View style={estilos.formulario}>{children}</View>
    </View>
  );
}
