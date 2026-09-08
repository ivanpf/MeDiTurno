import React, { useState, useEffect, useCallback } from 'react';
import { View, FlatList, Text, TouchableOpacity, StyleSheet, Alert, ActivityIndicator, RefreshControl } from 'react-native';
import { listarReservas, crearReserva, actualizarReserva, eliminarReserva } from '../services/api';
import ReservaList from '../components/ReservaList';
import ReservaForm from '../components/ReservaForm';

const ReservasScreen = () => {
  const [reservas, setReservas] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [mostrarFormulario, setMostrarFormulario] = useState(false);
  const [reservaSeleccionada, setReservaSeleccionada] = useState(null);

  const cargarReservas = async () => {
    try {
      const data = await listarReservas();
      setReservas(data);
    } catch (error) {
      Alert.alert('Error', 'No se pudieron cargar las reservas');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    cargarReservas();
  }, []);

  const onRefresh = useCallback(() => {
    setRefreshing(true);
    cargarReservas();
  }, []);

  const handleGuardar = async (datosReserva) => {
    try {
      if (reservaSeleccionada) {
        await actualizarReserva(reservaSeleccionada.id, datosReserva);
        Alert.alert('Éxito', 'Reserva actualizada correctamente');
      } else {
        await crearReserva(datosReserva);
        Alert.alert('Éxito', 'Reserva creada correctamente');
      }
      setMostrarFormulario(false);
      setReservaSeleccionada(null);
      cargarReservas();
    } catch (error) {
      Alert.alert('Error', 'No se pudo guardar la reserva');
    }
  };

  const handleEliminar = async (id) => {
    Alert.alert(
      'Confirmar eliminación',
      '¿Estás seguro de que quieres eliminar esta reserva?',
      [
        { text: 'Cancelar', style: 'cancel' },
        {
          text: 'Eliminar',
          style: 'destructive',
          onPress: async () => {
            try {
              await eliminarReserva(id);
              Alert.alert('Éxito', 'Reserva eliminada');
              cargarReservas();
            } catch (error) {
              Alert.alert('Error', 'No se pudo eliminar la reserva');
            }
          },
        },
      ]
    );
  };

  const handleEditar = (reserva) => {
    setReservaSeleccionada(reserva);
    setMostrarFormulario(true);
  };

  const handleCancelar = () => {
    setMostrarFormulario(false);
    setReservaSeleccionada(null);
  };

  if (loading) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator size="large" color="#007bff" />
      </View>
    );
  }

  if (mostrarFormulario) {
    return (
      <View style={styles.container}>
        <Text style={styles.titulo}>
          {reservaSeleccionada ? 'Editar Reserva' : 'Nueva Reserva'}
        </Text>
        <ReservaForm
          reservaInicial={reservaSeleccionada}
          onGuardar={handleGuardar}
          onCancelar={handleCancelar}
        />
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <TouchableOpacity
        style={styles.botonNuevo}
        onPress={() => setMostrarFormulario(true)}
      >
        <Text style={styles.textoBotonNuevo}>+ Nueva Reserva</Text>
      </TouchableOpacity>

      {reservas.length === 0 ? (
        <View style={styles.vacio}>
          <Text style={styles.textoVacio}>No hay reservas registradas</Text>
        </View>
      ) : (
        <FlatList
          data={reservas}
          keyExtractor={(item) => item.id.toString()}
          renderItem={({ item }) => (
            <ReservaList
              reserva={item}
              onEditar={handleEditar}
              onEliminar={handleEliminar}
            />
          )}
          refreshControl={
            <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
          }
        />
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
    paddingTop: 16,
  },
  centered: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  titulo: {
    fontSize: 22,
    fontWeight: 'bold',
    textAlign: 'center',
    marginBottom: 16,
    color: '#333',
  },
  botonNuevo: {
    backgroundColor: '#007bff',
    marginHorizontal: 16,
    marginBottom: 16,
    padding: 14,
    borderRadius: 8,
    alignItems: 'center',
  },
  textoBotonNuevo: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
  vacio: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  textoVacio: {
    fontSize: 16,
    color: '#999',
  },
});

export default ReservasScreen;
