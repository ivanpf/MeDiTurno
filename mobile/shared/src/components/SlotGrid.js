/**
 * SlotGrid - Grid de números para elegir en reserva (CU-8 paso 3).
 * Muestra números 1..cupoMaximo con hora estimada, marcando ocupados.
 */

import React, { useMemo } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, FlatList } from 'react-native';

const estilos = StyleSheet.create({
  contenedor: {
    marginTop: 16,
  },
  titulo: {
    fontSize: 13,
    fontWeight: '600',
    color: '#5b6b7c',
    marginBottom: 12,
  },
  grid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'center',
    gap: 8,
  },
  slot: {
    width: 72,
    height: 72,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#d3dce8',
    backgroundColor: '#fff',
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 4,
  },
  slotOcupado: {
    backgroundColor: '#f4f7fb',
    borderColor: '#d3dce8',
  },
  slotSeleccionado: {
    backgroundColor: '#e8f0fa',
    borderColor: '#1d4f91',
    borderWidth: 2,
  },
  slotNumero: {
    fontSize: 14,
    fontWeight: '600',
    color: '#1c2733',
  },
  slotNumeroOcupado: {
    color: '#5b6b7c',
    textDecorationLine: 'line-through',
  },
  slotHora: {
    fontSize: 11,
    color: '#1d4f91',
    marginTop: 2,
  },
  slotHoraOcupado: {
    color: '#5b6b7c',
  },
  leyenda: {
    flexDirection: 'row',
    justifyContent: 'center',
    gap: 16,
    marginTop: 16,
    flexWrap: 'wrap',
  },
  leyendaItem: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  leyendaColor: {
    width: 16,
    height: 16,
    borderRadius: 4,
  },
  leyendaTexto: {
    fontSize: 12,
    color: '#5b6b7c',
  },
});

/**
 * @param {Object} props
 * @param {number} props.cupoMaximo
 * @param {number[]} [props.ocupados=[]]
 * @param {Function} props.calcularHora - (numero: number) => string (HH:mm)
 * @param {number} [props.seleccionado]
 * @param {(numero: number) => void} props.onSeleccionar
 * @param {boolean} [props.deshabilitado=false]
 */
export function SlotGrid({
  cupoMaximo,
  ocupados = [],
  calcularHora,
  seleccionado,
  onSeleccionar,
  deshabilitado = false,
}) {
  const setOcupados = useMemo(() => new Set(ocupados), [ocupados]);

  const slots = useMemo(() => {
    const arr = [];
    for (let n = 1; n <= cupoMaximo; n++) {
      arr.push({
        numero: n,
        hora: calcularHora(n),
        ocupado: setOcupados.has(n),
      });
    }
    return arr;
  }, [cupoMaximo, setOcupados, calcularHora]);

  const renderItem = ({ item }) => {
    const esOcupado = item.ocupado;
    const esSeleccionado = seleccionado === item.numero;

    return (
      <TouchableOpacity
        style={[
          estilos.slot,
          esOcupado && estilos.slotOcupado,
          esSeleccionado && !esOcupado && estilos.slotSeleccionado,
        ]}
        onPress={() => !esOcupado && !deshabilitado && onSeleccionar(item.numero)}
        disabled={esOcupado || deshabilitado}
        activeOpacity={0.7}
      >
        <Text style={[
          estilos.slotNumero,
          esOcupado && estilos.slotNumeroOcupado,
        ]}>
          Nº {item.numero}
        </Text>
        <Text style={[
          estilos.slotHora,
          esOcupado && estilos.slotHoraOcupado,
        ]}>
          ~{item.hora}
        </Text>
      </TouchableOpacity>
    );
  };

  return (
    <View style={estilos.contenedor}>
      <Text style={estilos.titulo}>
        Elija su número ({cupoMaximo - ocupados.length} disponibles)
      </Text>
      <View style={estilos.grid}>
        {slots.map((slot) => renderItem({ item: slot }))}
      </View>
      <View style={estilos.leyenda}>
        <View style={estilos.leyendaItem}>
          <View style={[estilos.leyendaColor, { backgroundColor: '#fff', borderWidth: 1, borderColor: '#d3dce8' }]} />
          <Text style={estilos.leyendaTexto}>Disponible</Text>
        </View>
        <View style={estilos.leyendaItem}>
          <View style={[estilos.leyendaColor, { backgroundColor: '#e8f0fa', borderWidth: 2, borderColor: '#1d4f91' }]} />
          <Text style={estilos.leyendaTexto}>Seleccionado</Text>
        </View>
        <View style={estilos.leyendaItem}>
          <View style={[estilos.leyendaColor, { backgroundColor: '#f4f7fb' }]} />
          <Text style={estilos.leyendaTexto}>Ocupado</Text>
        </View>
      </View>
    </View>
  );
}