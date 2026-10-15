/**
 * Avatar component con iniciales y color por rol.
 */

import React from 'react';
import { View, Text, StyleSheet } from 'react-native';

const estilos = StyleSheet.create({
  base: {
    borderRadius: 9999,
    justifyContent: 'center',
    alignItems: 'center',
  },
  sizeSm: { width: 32, height: 32 },
  sizeMd: { width: 40, height: 40 },
  sizeLg: { width: 56, height: 56 },
  textoSm: { fontSize: 12, fontWeight: '600' },
  textoMd: { fontSize: 14, fontWeight: '600' },
  textoLg: { fontSize: 18, fontWeight: '600' },
});

const COLORES_ROL = {
  paciente: '#1d4f91',
  funcionario: '#e1ad01',
  medico: '#1e7b4a',
  admin: '#b42318',
};

/**
 * Obtiene iniciales del nombre.
 * @param {string} nombre
 * @param {string} [apellido]
 * @returns {string}
 */
function getIniciales(nombre, apellido) {
  const n = nombre?.trim()?.[0]?.toUpperCase() || '';
  const a = apellido?.trim()?.[0]?.toUpperCase() || '';
  return n + a || '?';
}

/**
 * @param {Object} props
 * @param {string} [props.nombre]
 * @param {string} [props.apellido]
 * @param {string} [props.rol] - paciente|funcionario|medico|admin
 * @param {'sm'|'md'|'lg'} [props.size='md']
 * @param {string} [props.className]
 * @param {Object} [props.style]
 */
export function Avatar({ nombre, apellido, rol = 'paciente', size = 'md', className, style }) {
  const iniciales = getIniciales(nombre, apellido);
  const color = COLORES_ROL[rol] || COLORES_ROL.paciente;

  const sizeStyles = {
    sm: [estilos.sizeSm, estilos.textoSm],
    md: [estilos.sizeMd, estilos.textoMd],
    lg: [estilos.sizeLg, estilos.textoLg],
  }[size];

  return (
    <View
      style={[
        estilos.base,
        sizeStyles[0],
        { backgroundColor: color },
        style,
      ]}
      className={className}
    >
      <Text style={[sizeStyles[1], { color: '#fff' }]}>{iniciales}</Text>
    </View>
  );
}