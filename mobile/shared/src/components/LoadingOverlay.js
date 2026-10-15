/**
 * LoadingOverlay - Spinner full screen o inline.
 */

import React from 'react';
import { View, ActivityIndicator, StyleSheet, Text } from 'react-native';

const estilos = StyleSheet.create({
  overlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(255,255,255,0.8)',
    justifyContent: 'center',
    alignItems: 'center',
    zIndex: 100,
  },
  inline: {
    padding: 32,
    alignItems: 'center',
  },
  texto: {
    marginTop: 12,
    fontSize: 14,
    color: '#5b6b7c',
  },
});

/**
 * @param {Object} props
 * @param {boolean} [props.visible=true]
 * @param {string} [props.mensaje]
 * @param {boolean} [props.fullScreen=false]
 * @param {Object} [props.style]
 */
export function LoadingOverlay({ visible = true, mensaje, fullScreen = false, style }) {
  if (!visible) return null;

  return (
    <View
      style={[
        fullScreen ? estilos.overlay : estilos.inline,
        style,
      ]}
    >
      <ActivityIndicator size="large" color="#1d4f91" />
      {mensaje && <Text style={estilos.texto}>{mensaje}</Text>}
    </View>
  );
}