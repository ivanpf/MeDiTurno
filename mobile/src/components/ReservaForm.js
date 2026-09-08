import React, { useState, useEffect } from 'react';
import { View, TextInput, TouchableOpacity, Text, StyleSheet, Alert } from 'react-native';
import { Picker } from '@react-native-picker/picker';

const ReservaForm = ({ reservaInicial, onGuardar, onCancelar }) => {
  const [nombreCliente, setNombreCliente] = useState('');
  const [fecha, setFecha] = useState('');
  const [hora, setHora] = useState('');
  const [cantidadPersonas, setCantidadPersonas] = useState('1');
  const [estado, setEstado] = useState('PENDIENTE');

  useEffect(() => {
    if (reservaInicial) {
      setNombreCliente(reservaInicial.nombreCliente || '');
      setFecha(reservaInicial.fecha || '');
      setHora(reservaInicial.hora || '');
      setCantidadPersonas(String(reservaInicial.cantidadPersonas || 1));
      setEstado(reservaInicial.estado || 'PENDIENTE');
    }
  }, [reservaInicial]);

  const handleGuardar = () => {
    if (!nombreCliente.trim()) {
      Alert.alert('Error', 'El nombre del cliente es obligatorio');
      return;
    }
    if (!fecha.trim()) {
      Alert.alert('Error', 'La fecha es obligatoria (formato: AAAA-MM-DD)');
      return;
    }
    if (!hora.trim()) {
      Alert.alert('Error', 'La hora es obligatoria (formato: HH:MM)');
      return;
    }

    onGuardar({
      nombreCliente: nombreCliente.trim(),
      fecha,
      hora,
      cantidadPersonas: parseInt(cantidadPersonas),
      estado,
    });
  };

  return (
    <View style={styles.container}>
      <Text style={styles.label}>Nombre del Cliente *</Text>
      <TextInput
        style={styles.input}
        value={nombreCliente}
        onChangeText={setNombreCliente}
        placeholder="Ej: Juan Perez"
      />

      <Text style={styles.label}>Fecha (AAAA-MM-DD) *</Text>
      <TextInput
        style={styles.input}
        value={fecha}
        onChangeText={setFecha}
        placeholder="Ej: 2026-09-15"
      />

      <Text style={styles.label}>Hora (HH:MM) *</Text>
      <TextInput
        style={styles.input}
        value={hora}
        onChangeText={setHora}
        placeholder="Ej: 20:00"
      />

      <Text style={styles.label}>Cantidad de Personas</Text>
      <TextInput
        style={styles.input}
        value={cantidadPersonas}
        onChangeText={setCantidadPersonas}
        placeholder="1"
        keyboardType="numeric"
      />

      <Text style={styles.label}>Estado</Text>
      <View style={styles.pickerContainer}>
        <Picker
          selectedValue={estado}
          onValueChange={(itemValue) => setEstado(itemValue)}
          style={styles.picker}
        >
          <Picker.Item label="PENDIENTE" value="PENDIENTE" />
          <Picker.Item label="CONFIRMADA" value="CONFIRMADA" />
          <Picker.Item label="CANCELADA" value="CANCELADA" />
        </Picker>
      </View>

      <View style={styles.botones}>
        <TouchableOpacity style={styles.botonCancelar} onPress={onCancelar}>
          <Text style={styles.textoBoton}>Cancelar</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.botonGuardar} onPress={handleGuardar}>
          <Text style={styles.textoBoton}>Guardar</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    padding: 16,
  },
  label: {
    fontSize: 14,
    fontWeight: '600',
    marginBottom: 4,
    marginTop: 12,
    color: '#333',
  },
  input: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 12,
    fontSize: 16,
    backgroundColor: '#f9f9f9',
  },
  pickerContainer: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    backgroundColor: '#f9f9f9',
  },
  picker: {
    height: 50,
  },
  botones: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: 24,
  },
  botonCancelar: {
    backgroundColor: '#6c757d',
    padding: 14,
    borderRadius: 8,
    flex: 1,
    marginRight: 8,
    alignItems: 'center',
  },
  botonGuardar: {
    backgroundColor: '#28a745',
    padding: 14,
    borderRadius: 8,
    flex: 1,
    marginLeft: 8,
    alignItems: 'center',
  },
  textoBoton: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
});

export default ReservaForm;
