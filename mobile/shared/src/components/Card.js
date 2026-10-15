/**
 * Card component - Panel con sombra y bordes redondeados.
 */

import React from 'react';
import { View, StyleSheet } from 'react-native';

const estilos = StyleSheet.create({
  base: {
    backgroundColor: '#fff',
    borderRadius: 12,
    shadowColor: '#12366b',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.12,
    shadowRadius: 3,
    elevation: 3,
  },
  padding: {
    padding: 24,
  },
});

/**
 * @param {Object} props
 * @param {React.ReactNode} props.children
 * @param {boolean} [props.withPadding=true]
 * @param {Object} [props.style]
 * @param {string} [props.className]
 */
export function Card({ children, withPadding = true, style, className }) {
  return (
    <View
      style={[
        estilos.base,
        withPadding && estilos.padding,
        style,
      ]}
      className={className}
    >
      {children}
    </View>
  );
}