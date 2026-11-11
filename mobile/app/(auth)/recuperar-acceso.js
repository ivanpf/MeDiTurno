/**
 * CU-4 - Recuperación de acceso.
 * Firebase envía el correo y procesa el cambio de contraseña en su página segura:
 * la app solo pide el correo y muestra la confirmación (igual que la web).
 */

import React, { useState } from 'react';
import { View, Text, StyleSheet, KeyboardAvoidingView, Platform, ScrollView, TouchableOpacity } from 'react-native';
import { Link, useRouter } from 'expo-router';
import { sendPasswordResetEmail } from 'firebase/auth';
import { auth } from '../../shared/src/auth/firebase';
import { Button, Input, Card, ErrorBanner, LoadingOverlay } from '../../shared/src/components';
import { validarRecuperacion } from '../../shared/src/utils/validation';

const MENSAJE_CONFIRMACION = 'Si existe una cuenta asociada a esa dirección, recibirá un correo con instrucciones para recuperar el acceso.';

const estilos = StyleSheet.create({
  contenedor: {
    flex: 1,
    justifyContent: 'center',
    padding: 24,
    backgroundColor: '#f4f7fb',
  },
  titulo: {
    fontSize: 22,
    fontWeight: '600',
    color: '#12366b',
    marginBottom: 8,
    textAlign: 'center',
  },
  subtitulo: {
    fontSize: 14,
    color: '#5b6b7c',
    textAlign: 'center',
    marginBottom: 24,
  },
  mensajeExito: {
    backgroundColor: '#e6f4ec',
    borderLeftWidth: 4,
    borderLeftColor: '#1e7b4a',
    padding: 12,
    borderRadius: 8,
    marginBottom: 20,
  },
  mensajeExitoTexto: {
    color: '#1e7b4a',
    fontSize: 14,
    lineHeight: 20,
  },
  mensajeError: {
    backgroundColor: '#fdecea',
    borderLeftWidth: 4,
    borderLeftColor: '#b42318',
    padding: 12,
    borderRadius: 8,
    marginBottom: 20,
  },
  mensajeErrorTexto: {
    color: '#b42318',
    fontSize: 14,
  },
  enlaceVolver: {
    marginTop: 16,
    textAlign: 'center',
  },
});

const mensajeDeError = (codigo) => {
  switch (codigo) {
    case 'auth/invalid-email':
      return 'Ingrese una dirección de correo electrónico válida.';
    case 'auth/too-many-requests':
      return 'Se realizaron demasiadas solicitudes. Espere un momento e inténtelo nuevamente.';
    case 'auth/network-request-failed':
      return 'No se pudo conectar con Firebase. Verifique su conexión e inténtelo nuevamente.';
    default:
      return 'No fue posible enviar las instrucciones. Intente nuevamente.';
  }
};

export default function RecuperarAccesoScreen() {
  const router = useRouter();
  const [correo, setCorreo] = useState('');
  const [enviada, setEnviada] = useState(false);
  const [error, setError] = useState(null);
  const [cargando, setCargando] = useState(false);

  const irLogin = () => router.replace('/(auth)/login');

  const solicitar = async () => {
    const { valido, errores } = validarRecuperacion(correo);
    if (!valido) {
      setError(new Error(errores[0]));
      return;
    }
    setCargando(true);
    setError(null);
    try {
      await sendPasswordResetEmail(auth, correo.trim());
      setEnviada(true);
    } catch (err) {
      if (err?.code === 'auth/user-not-found') {
        // Por seguridad, no revelamos si el correo existe.
        setEnviada(true);
      } else {
        setError(new Error(mensajeDeError(err?.code)));
      }
    } finally {
      setCargando(false);
    }
  };

  // Paso 1: solicitar correo
  if (!enviada) {
    return (
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        style={estilos.contenedor}
      >
        <ScrollView contentContainerStyle={{ flexGrow: 1, justifyContent: 'center' }}>
          <Card>
            <Text style={estilos.titulo}>Recuperar acceso</Text>
            <Text style={estilos.subtitulo}>
              Ingrese su correo electrónico para recibir instrucciones.
            </Text>

            {error && (
              <View style={estilos.mensajeError}>
                <Text style={estilos.mensajeErrorTexto}>{error.message}</Text>
              </View>
            )}

            <Input
              label="Correo electrónico *"
              placeholder="correo@ejemplo.com"
              value={correo}
              onChangeText={setCorreo}
              keyboardType="email-address"
              autoComplete="email"
              required
              disabled={cargando}
            />

            <Button
              className="principal"
              disabled={cargando || !correo}
              onPress={solicitar}
            >
              {cargando ? 'Enviando...' : 'Enviar enlace'}
            </Button>

            <TouchableOpacity style={estilos.enlaceVolver} onPress={irLogin}>
              <Text style={{ color: '#1d4f91', textDecorationLine: 'underline' }}>
                Volver al inicio de sesión
              </Text>
            </TouchableOpacity>
          </Card>
        </ScrollView>
        <LoadingOverlay visible={cargando} fullScreen />
      </KeyboardAvoidingView>
    );
  }

  // Paso 2: confirmación
  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      style={estilos.contenedor}
    >
      <ScrollView contentContainerStyle={{ flexGrow: 1, justifyContent: 'center' }}>
        <Card>
          <Text style={estilos.titulo}>Revise su correo</Text>
          <View style={estilos.mensajeExito}>
            <Text style={estilos.mensajeExitoTexto}>{MENSAJE_CONFIRMACION}</Text>
          </View>

          <Button className="principal" onPress={irLogin}>
            Volver al inicio de sesión
          </Button>

          <TouchableOpacity style={estilos.enlaceVolver} onPress={() => setEnviada(false)}>
            <Text style={{ color: '#1d4f91', textDecorationLine: 'underline' }}>
              Solicitar otro enlace
            </Text>
          </TouchableOpacity>
        </Card>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}
