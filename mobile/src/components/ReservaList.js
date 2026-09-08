import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';

const ReservaList = ({ reserva, onEditar, onEliminar }) => {
  const getEstadoColor = (estado) => {
    switch (estado) {
      case 'CONFIRMADA': return '#28a745';
      case 'CANCELADA': return '#dc3545';
      default: return '#ffc107';
    }
  };

  return (
    <View style={styles.card}>
      <View style={styles.header}>
        <Text style={styles.nombre}>{reserva.nombreCliente}</Text>
        <View style={[styles.estadoBadge, { backgroundColor: getEstadoColor(reserva.estado) }]}>
          <Text style={styles.estadoTexto}>{reserva.estado}</Text>
        </View>
      </View>

      <View style={styles.info}>
        <Text style={styles.infoItem}>Fecha: {reserva.fecha}</Text>
        <Text style={styles.infoItem}>Hora: {reserva.hora}</Text>
        <Text style={styles.infoItem}>Personas: {reserva.cantidadPersonas}</Text>
      </View>

      <View style={styles.acciones}>
        <TouchableOpacity style={styles.botonEditar} onPress={() => onEditar(reserva)}>
          <Text style={styles.textoAccion}>Editar</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.botonEliminar} onPress={() => onEliminar(reserva.id)}>
          <Text style={styles.textoAccion}>Eliminar</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  card: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    marginHorizontal: 16,
    marginVertical: 8,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
  },
  nombre: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
    flex: 1,
  },
  estadoBadge: {
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 12,
  },
  estadoTexto: {
    color: '#fff',
    fontSize: 12,
    fontWeight: '600',
  },
  info: {
    marginBottom: 12,
  },
  infoItem: {
    fontSize: 14,
    color: '#666',
    marginBottom: 4,
  },
  acciones: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    borderTopWidth: 1,
    borderTopColor: '#eee',
    paddingTop: 12,
  },
  botonEditar: {
    backgroundColor: '#007bff',
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 6,
    marginRight: 8,
  },
  botonEliminar: {
    backgroundColor: '#dc3545',
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 6,
  },
  textoAccion: {
    color: '#fff',
    fontSize: 14,
    fontWeight: '600',
  },
});

export default ReservaList;
