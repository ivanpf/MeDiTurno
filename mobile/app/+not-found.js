/**
 * Pantalla 404 - No encontrada.
 */

import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { router } from 'expo-router';
import { Button } from '../shared/src/components';

const estilos = StyleSheet.create({
  contenedor: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 24,
    backgroundColor: '#f4f7fb',
  },
  titulo: {
    fontSize: 22,
    fontWeight: '600',
    color: '#12366b',
    marginBottom: 8,
    textAlign: 'center',
  },
  mensaje: {
    fontSize: 15,
    color: '#5b6b7c',
    textAlign: 'center',
    marginBottom: 24,
  },
});

export default function NotFoundScreen() {
  return (
    <View style={estilos.contenedor}>
      <Text style={estilos.titulo}>Página no encontrada</Text>
      <Text style={estilos.mensaje}>
        La pantalla que busca no existe o no está disponible.
      </Text>
      <Button className="principal" onPress={() => router.back()}>
        Volver atrás
      </Button>
    </View>
  );
}
