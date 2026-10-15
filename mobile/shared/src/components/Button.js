/**
 * Button component con variantes.
 */

import React from 'react';
import { Text, TouchableOpacity, StyleSheet } from 'react-native';

const estilos = StyleSheet.create({
  base: {
    borderRadius: 8,
    paddingVertical: 12,
    paddingHorizontal: 16,
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: 48,
    flexDirection: 'row',
    gap: 8,
  },
  primary: {
    backgroundColor: '#1d4f91',
  },
  secondary: {
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: '#1d4f91',
  },
  danger: {
    backgroundColor: '#b42318',
  },
  ghost: {
    backgroundColor: 'transparent',
  },
  disabled: {
    opacity: 0.5,
  },
  textoPrimary: { color: '#fff', fontWeight: '600', fontSize: 15 },
  textoSecondary: { color: '#1d4f91', fontWeight: '600', fontSize: 15 },
  textoDanger: { color: '#fff', fontWeight: '600', fontSize: 15 },
  textoGhost: { color: '#1d4f91', fontWeight: '600', fontSize: 15 },
});

/**
 * @param {Object} props
 * @param {'primary'|'secondary'|'danger'|'ghost'} [props.variant='primary']
 * @param {boolean} [props.disabled=false]
 * @param {React.ReactNode} props.children
 * @param {() => void} [props.onPress]
 * @param {React.ReactNode} [props.icon]
 * @param {string} [props.className] - Clases Tailwind adicionales (via className)
 * @param {Object} [props.style] - Estilos adicionales
 */
export function Button({
  variant = 'primary',
  disabled = false,
  children,
  onPress,
  icon,
  className = '',
  style,
  ...rest
}) {
  const isPrimary = variant === 'primary';
  const isSecondary = variant === 'secondary';
  const isDanger = variant === 'danger';
  const isGhost = variant === 'ghost';

  const textoStyle = isPrimary
    ? estilos.textoPrimary
    : isSecondary
    ? estilos.textoSecondary
    : isDanger
    ? estilos.textoDanger
    : estilos.textoGhost;

  const baseStyle = [
    estilos.base,
    isPrimary && estilos.primary,
    isSecondary && estilos.secondary,
    isDanger && estilos.danger,
    isGhost && estilos.ghost,
    disabled && estilos.disabled,
    style,
  ];

  return (
    <TouchableOpacity
      {...rest}
      style={baseStyle}
      onPress={disabled ? undefined : onPress}
      activeOpacity={0.8}
      disabled={disabled}
    >
      {icon}
      <Text style={textoStyle}>{children}</Text>
    </TouchableOpacity>
  );
}