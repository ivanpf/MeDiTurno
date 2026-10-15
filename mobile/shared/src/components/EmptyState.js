/**
 * EmptyState - Estado vacío para listas.
 */

import React from 'react';
import { View, Text, StyleSheet, Image } from 'react-native';

const estilos = StyleSheet.create({
  contenedor: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 32,
  },
  icono: {
    width: 80,
    height: 80,
    marginBottom: 16,
    opacity: 0.6,
  },
  iconoVacio: {
    width: 64,
    height: 64,
    borderRadius: 32,
    borderWidth: 3,
    borderColor: '#d3dce8',
    backgroundColor: '#f4f7fb',
    marginBottom: 16,
  },
  titulo: {
    fontSize: 18,
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
    lineHeight: 22,
  },
  accion: {
    marginTop: 8,
  },
});

/**
 * @param {Object} props
 * @param {string} props.titulo
 * @param {string} props.mensaje
 * @param {React.ReactNode} [props.icono] - Componente icono personalizado
 * @param {React.ReactNode} [props.accion] - Botón de acción
 */
export function EmptyState({ titulo, mensaje, icono, accion }) {
  return (
    <View style={estilos.contenedor}>
      {icono ? (
        <View style={estilos.icono}>{icono}</View>
      ) : (
        <View style={estilos.iconoVacio} />
      )}
      <Text style={estilos.titulo}>{titulo}</Text>
      <Text style={estilos.mensaje}>{mensaje}</Text>
      {accion && <View style={estilos.accion}>{accion}</View>}
    </View>
  );
}

/**
 * EmptyState predefinidos para casos comunes.
 */
export const EmptyStates = {
  reservas: () => (
    <EmptyState
      titulo="No tiene reservas"
      mensaje="Cuando reserve una consulta, aparecerá aquí."
      accion={<Text style={{ color: '#1d4f91', fontWeight: '600' }}>Reservar consulta →</Text>}
    />
  ),
  historia: () => (
    <EmptyState
      titulo="Sin atenciones registradas"
      mensaje="No existen atenciones finalizadas para visualizar."
    />
  ),
  cartelera: () => (
    <EmptyState
      titulo="Sin novedades"
      mensaje="No hay anuncios publicados en este momento."
    />
  ),
};