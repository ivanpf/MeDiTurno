/**
 * Badge component para estados (reservas, turnos, etc.).
 */

import React from 'react';
import { View, Text, StyleSheet } from 'react-native';

const estilos = StyleSheet.create({
  base: {
    borderRadius: 9999,
    paddingHorizontal: 10,
    paddingVertical: 4,
    alignSelf: 'flex-start',
  },
  vigente: { backgroundColor: '#e6f4ec' },
  orden_emitida: { backgroundColor: '#e8f0fa' },
  atendida: { backgroundColor: '#d3dce8' },
  cancelada: { backgroundColor: '#fdecea' },
  anulada: { backgroundColor: '#fff6d6' },
  vencida: { backgroundColor: '#d3dce8' },
  textoVigente: { color: '#1e7b4a' },
  textoOrden: { color: '#1d4f91' },
  textoAtendida: { color: '#5b6b7c' },
  textoCancelada: { color: '#b42318' },
  textoAnulada: { color: '#b88700' },
  textoVencida: { color: '#5b6b7c' },
});

const ETIQUETAS = {
  VIGENTE: 'Vigente',
  ORDEN_EMITIDA: 'Orden emitida',
  ATENDIDA: 'Atendida',
  CANCELADA: 'Cancelada',
  ANULADA: 'Anulada',
  VENCIDA: 'Vencida',
};

const ESTILOS_ESTADO = {
  VIGENTE: { bg: estilos.vigente, texto: estilos.textoVigente },
  ORDEN_EMITIDA: { bg: estilos.orden_emitida, texto: estilos.textoOrden },
  ATENDIDA: { bg: estilos.atendida, texto: estilos.textoAtendida },
  CANCELADA: { bg: estilos.cancelada, texto: estilos.textoCancelada },
  ANULADA: { bg: estilos.anulada, texto: estilos.textoAnulada },
  VENCIDA: { bg: estilos.vencida, texto: estilos.textoVencida },
};

/**
 * @param {Object} props
 * @param {string} props.estado - VIGENTE|ORDEN_EMITIDA|ATENDIDA|CANCELADA|ANULADA|VENCIDA
 * @param {string} [props.className]
 * @param {Object} [props.style]
 */
export function Badge({ estado, className, style }) {
  const config = ESTILOS_ESTADO[estado] || ESTILOS_ESTADO.VIGENTE;
  const etiqueta = ETIQUETAS[estado] || estado;

  return (
    <View style={[estilos.base, config.bg, style]} className={className}>
      <Text style={[config.texto, { fontSize: 12, fontWeight: '600' }]}>{etiqueta}</Text>
    </View>
  );
}

/**
 * Badge genérico para cualquier texto.
 * @param {Object} props
 * @param {string} props.children
 * @param {Object} [props.style]
 * @param {string} [props.className]
 * @param {string} [props.bgColor]
 * @param {string} [props.textColor]
 */
export function BadgeCustom({ children, style, className, bgColor = '#e8f0fa', textColor = '#1d4f91' }) {
  return (
    <View
      style={[
        estilos.base,
        { backgroundColor: bgColor },
        style,
      ]}
      className={className}
    >
      <Text style={{ color: textColor, fontSize: 12, fontWeight: '600' }}>{children}</Text>
    </View>
  );
}