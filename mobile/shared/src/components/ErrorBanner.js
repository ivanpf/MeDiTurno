/**
 * ErrorBanner - Banner de error dismissible.
 */

import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';

const estilos = StyleSheet.create({
  base: {
    backgroundColor: '#fdecea',
    borderLeftWidth: 4,
    borderLeftColor: '#b42318',
    padding: 12,
    borderRadius: 8,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    gap: 12,
  },
  mensaje: {
    flex: 1,
    fontSize: 14,
    color: '#b42318',
    lineHeight: 20,
  },
  cerrar: {
    padding: 4,
    marginLeft: 8,
  },
  detalles: {
    marginTop: 8,
    paddingTop: 8,
    borderTopWidth: 1,
    borderTopColor: 'rgba(180,35,24,0.2)',
  },
  detalleItem: {
    fontSize: 12,
    color: '#b42318',
    marginBottom: 4,
  },
});

/**
 * @param {Object} props
 * @param {string} props.mensaje
 * @param {string[]} [props.detalles=[]]
 * @param {() => void} props.onDismiss
 */
export function ErrorBanner({ mensaje, detalles = [], onDismiss }) {
  return (
    <View style={estilos.base}>
      <Text style={estilos.mensaje}>{mensaje}</Text>
      {detalles.length > 0 && (
        <View style={estilos.detalles}>
          {detalles.map((d, i) => (
            <Text key={i} style={estilos.detalleItem}>• {d}</Text>
          ))}
        </View>
      )}
      <TouchableOpacity onPress={onDismiss} style={estilos.cerrar} accessibilityLabel="Cerrar error">
        <Text style={{ fontSize: 20, color: '#b42318', lineHeight: 20 }}>✕</Text>
      </TouchableOpacity>
    </View>
  );
}