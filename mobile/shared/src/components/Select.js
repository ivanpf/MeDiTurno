/**
 * Select component - Dropdown simple sin dependencias nativas.
 * Usa un Modal con lista de opciones.
 */

import React, { useState } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, Modal, FlatList } from 'react-native';

const estilos = StyleSheet.create({
  contenedor: {
    width: '100%',
    marginBottom: 16,
  },
  label: {
    fontSize: 13,
    fontWeight: '600',
    color: '#5b6b7c',
    marginBottom: 8,
  },
  control: {
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: '#d3dce8',
    borderRadius: 8,
    height: 48,
    paddingHorizontal: 16,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  controlDisabled: {
    backgroundColor: '#f4f7fb',
  },
  controlError: {
    borderColor: '#b42318',
  },
  controlTexto: {
    flex: 1,
    fontSize: 15,
    color: '#1c2733',
  },
  placeholder: {
    color: '#9aa7b5',
  },
  flecha: {
    fontSize: 12,
    color: '#5b6b7c',
    marginLeft: 8,
  },
  errorTexto: {
    marginTop: 6,
    fontSize: 13,
    color: '#b42318',
  },
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.4)',
    justifyContent: 'center',
    paddingHorizontal: 32,
  },
  dropdown: {
    backgroundColor: '#fff',
    borderRadius: 12,
    maxHeight: 360,
    overflow: 'hidden',
  },
  opcion: {
    paddingVertical: 14,
    paddingHorizontal: 20,
    borderBottomWidth: 1,
    borderBottomColor: '#eef2f7',
  },
  opcionTexto: {
    fontSize: 15,
    color: '#1c2733',
  },
  opcionSeleccionada: {
    color: '#1d4f91',
    fontWeight: '600',
  },
});

/**
 * @param {Object} props
 * @param {string} [props.label]
 * @param {string} [props.placeholder] - Texto para opción vacía
 * @param {string} [props.value]
 * @param {(value: string) => void} [props.onValueChange]
 * @param {Array<{value: string, label: string}>} props.options
 * @param {string} [props.error]
 * @param {boolean} [props.disabled=false]
 * @param {boolean} [props.required=false]
 * @param {string} [props.className]
 * @param {Object} [props.style]
 */
export function Select({
  label,
  placeholder = 'Seleccionar',
  value,
  onValueChange,
  options = [],
  error,
  disabled = false,
  required = false,
  className,
  style,
}) {
  const [abierto, setAbierto] = useState(false);
  const seleccionada = options.find((o) => String(o.value) === String(value));
  const texto = seleccionada ? seleccionada.label : placeholder;

  return (
    <View style={[estilos.contenedor, style]} className={className}>
      {label && (
        <Text style={estilos.label}>
          {label} {required && <Text style={{ color: '#b42318' }}>*</Text>}
        </Text>
      )}
      <TouchableOpacity
        style={[
          estilos.control,
          disabled && estilos.controlDisabled,
          error && estilos.controlError,
        ]}
        onPress={() => !disabled && setAbierto(true)}
        activeOpacity={0.8}
        disabled={disabled}
      >
        <Text
          style={[estilos.controlTexto, !seleccionada && estilos.placeholder]}
          numberOfLines={1}
        >
          {texto}
        </Text>
        <Text style={estilos.flecha}>▼</Text>
      </TouchableOpacity>
      {error && <Text style={estilos.errorTexto}>{error}</Text>}

      <Modal
        visible={abierto}
        transparent
        animationType="fade"
        onRequestClose={() => setAbierto(false)}
      >
        <TouchableOpacity
          style={estilos.overlay}
          activeOpacity={1}
          onPress={() => setAbierto(false)}
        >
          <View style={estilos.dropdown}>
            <FlatList
              data={options}
              keyExtractor={(item) => String(item.value)}
              renderItem={({ item }) => (
                <TouchableOpacity
                  style={estilos.opcion}
                  onPress={() => {
                    setAbierto(false);
                    onValueChange?.(item.value);
                  }}
                >
                  <Text
                    style={[
                      estilos.opcionTexto,
                      String(item.value) === String(value) && estilos.opcionSeleccionada,
                    ]}
                  >
                    {item.label}
                  </Text>
                </TouchableOpacity>
              )}
            />
          </View>
        </TouchableOpacity>
      </Modal>
    </View>
  );
}
