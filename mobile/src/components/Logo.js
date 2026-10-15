// Marca de MeDiTurno: el logo (assets/logo.png) junto al nombre.
// Equivalente a frontend/src/components/Logo.jsx.
//   variante="claro": para fondos azules (el logo va dentro de un círculo blanco)
//   grande:           versión más grande (pantallas de acceso)

import React from 'react';
import { View, Text, Image, StyleSheet } from 'react-native';
import { COLORS } from '../../shared/src/theme/colors';

const logo = require('../../assets/logo.png');

const estilos = StyleSheet.create({
  fila: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },
  marca: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  marcaClaro: {
    backgroundColor: COLORS.blanco,
    borderRadius: 9999,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.25,
    shadowRadius: 4,
    elevation: 2,
  },
  texto: {
    fontSize: 22,
    fontWeight: '700',
    letterSpacing: -0.3,
  },
  textoClaro: {
    color: COLORS.blanco,
  },
  textoOscuro: {
    color: COLORS.azulOscuro,
  },
  textoGrande: {
    fontSize: 30,
  },
});

export default function Logo({ variante = 'oscuro', grande = false }) {
  const claro = variante === 'claro';
  const lado = grande ? 72 : 46;

  return (
    <View style={estilos.fila}>
      <View style={[estilos.marca, { width: lado, height: lado }, claro && estilos.marcaClaro]}>
        <Image source={logo} style={{ width: lado * 0.78, height: lado * 0.78 }} resizeMode="contain" />
      </View>
      <Text style={[estilos.texto, claro ? estilos.textoClaro : estilos.textoOscuro, grande && estilos.textoGrande]}>
        MeDiTurno
      </Text>
    </View>
  );
}
