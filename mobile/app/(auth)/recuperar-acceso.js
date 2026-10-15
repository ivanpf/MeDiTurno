/**
 * CU-3/4 - Recuperación de acceso.
 * 3 pasos: solicitar email → validar token → nueva contraseña.
 */

import React, { useState, useEffect } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, KeyboardAvoidingView, Platform, ScrollView, Alert } from 'react-native';
import { Link, useRouter, useLocalSearchParams } from 'expo-router';
import { solicitarRecuperacion, validarTokenRecuperacion, restablecerContrasena } from '../../shared/src/api/endpoints';
import { Button, Input, Card, ErrorBanner, LoadingOverlay } from '../../shared/src/components';
import { validarRecuperacion, validarCambioContrasena } from '../../shared/src/utils/validation';

const ESTADOS = {
  SOLICITAR: 'solicitar',
  EMAIL_ENVIADO: 'email_enviado',
  NUEVA_CONTRASENA: 'nueva_contrasena',
  ENLACE_VENCIDO: 'enlace_vencido',
  EXITO: 'exito',
};

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

export default function RecuperarAccesoScreen() {
  const router = useRouter();
  const params = useLocalSearchParams();
  const [estado, setEstado] = useState(ESTADOS.SOLICITAR);
  const [correo, setCorreo] = useState('');
  const [nuevaContrasena, setNuevaContrasena] = useState('');
  const [repetirContrasena, setRepetirContrasena] = useState('');
  const [error, setError] = useState(null);
  const [cargando, setCargando] = useState(false);

  // Si viene token en URL (deep link), validar y pasar a paso 2
  useEffect(() => {
    if (params.token) {
      validarToken(params.token);
    }
  }, [params.token]);

  const validarToken = async (token) => {
    setCargando(true);
    setError(null);
    try {
      await validarTokenRecuperacion(token);
      setEstado(ESTADOS.NUEVA_CONTRASENA);
    } catch (err) {
      if (err.estado === 404 || err.estado === 410) {
        setEstado(ESTADOS.ENLACE_VENCIDO);
      } else {
        setError(err);
        setEstado(ESTADOS.SOLICITAR);
      }
    } finally {
      setCargando(false);
    }
  };

  const solicitar = async () => {
    const { valido, errores } = validarRecuperacion(correo);
    if (!valido) {
      setError(new Error(errores[0]));
      return;
    }
    setCargando(true);
    setError(null);
    try {
      await solicitarRecuperacion({ correo });
      setEstado(ESTADOS.EMAIL_ENVIADO);
    } catch (err) {
      setError(err);
    } finally {
      setCargando(false);
    }
  };

  const guardarNuevaContrasena = async () => {
    const { valido, errores } = validarCambioContrasena({
      actual: '', // No se valida actual en recuperación
      nueva: nuevaContrasena,
      repetirNueva: repetirContrasena,
    });
    if (!valido) {
      setError(new Error(errores[0]));
      return;
    }
    setCargando(true);
    setError(null);
    try {
      await restablecerContrasena(params.token, { nuevaContrasena });
      setEstado(ESTADOS.EXITO);
    } catch (err) {
      if (err.estado === 404 || err.estado === 410) {
        setEstado(ESTADOS.ENLACE_VENCIDO);
      } else {
        setError(err);
      }
    } finally {
      setCargando(false);
    }
  };

  const solicitarNuevo = () => {
    setEstado(ESTADOS.SOLICITAR);
    setCorreo('');
  };

  const irLogin = () => router.replace('/(auth)/login');

  // Paso 1: Solicitar correo
  if (estado === ESTADOS.SOLICITAR) {
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

  // Paso 2: Email enviado
  if (estado === ESTADOS.EMAIL_ENVIADO) {
    return (
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        style={estilos.contenedor}
      >
        <ScrollView contentContainerStyle={{ flexGrow: 1, justifyContent: 'center' }}>
          <Card>
            <Text style={estilos.titulo}>Revise su correo</Text>
            <View style={estilos.mensajeExito}>
              <Text style={estilos.mensajeExitoTexto}>
                Si existe una cuenta asociada a esa dirección, recibirá instrucciones para recuperar el acceso. El enlace vale 10 minutos.
              </Text>
            </View>

            <Button className="principal" onPress={irLogin}>
              Volver al inicio de sesión
            </Button>

            <TouchableOpacity style={estilos.enlaceVolver} onPress={() => setEstado(ESTADOS.SOLICITAR)}>
              <Text style={{ color: '#1d4f91', textDecorationLine: 'underline' }}>
                Solicitar otro enlace
              </Text>
            </TouchableOpacity>
          </Card>
        </ScrollView>
      </KeyboardAvoidingView>
    );
  }

  // Paso 3: Nueva contraseña (token válido)
  if (estado === ESTADOS.NUEVA_CONTRASENA) {
    return (
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        style={estilos.contenedor}
      >
        <ScrollView contentContainerStyle={{ flexGrow: 1, justifyContent: 'center' }}>
          <Card>
            <Text style={estilos.titulo}>Nueva contraseña</Text>
            <Text style={estilos.subtitulo}>
              Ingrese su nueva contraseña (mín. 6 caracteres).
            </Text>

            {error && (
              <View style={estilos.mensajeError}>
                <Text style={estilos.mensajeErrorTexto}>{error.message}</Text>
              </View>
            )}

            <Input
              label="Nueva contraseña *"
              placeholder="••••••"
              value={nuevaContrasena}
              onChangeText={setNuevaContrasena}
              secureTextEntry
              autoComplete="new-password"
              minLength={6}
              maxLength={128}
              required
              disabled={cargando}
            />

            <Input
              label="Confirmar contraseña *"
              placeholder="••••••"
              value={repetirContrasena}
              onChangeText={setRepetirContrasena}
              secureTextEntry
              autoComplete="new-password"
              minLength={6}
              maxLength={128}
              required
              disabled={cargando}
            />

            <Button
              className="principal"
              disabled={cargando}
              onPress={guardarNuevaContrasena}
            >
              {cargando ? 'Guardando...' : 'Guardar contraseña'}
            </Button>
          </Card>
        </ScrollView>
        <LoadingOverlay visible={cargando} fullScreen />
      </KeyboardAvoidingView>
    );
  }

  // Enlace vencido
  if (estado === ESTADOS.ENLACE_VENCIDO) {
    return (
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        style={estilos.contenedor}
      >
        <ScrollView contentContainerStyle={{ flexGrow: 1, justifyContent: 'center' }}>
          <Card>
            <Text style={estilos.titulo}>Enlace vencido</Text>
            <View style={estilos.mensajeError}>
              <Text style={estilos.mensajeErrorTexto}>
                El enlace de recuperación ha vencido o es inválido.
              </Text>
            </View>

            <Button className="principal" onPress={solicitarNuevo}>
              Solicitar uno nuevo
            </Button>

            <TouchableOpacity style={estilos.enlaceVolver} onPress={irLogin}>
              <Text style={{ color: '#1d4f91', textDecorationLine: 'underline' }}>
                Volver al inicio de sesión
              </Text>
            </TouchableOpacity>
          </Card>
        </ScrollView>
      </KeyboardAvoidingView>
    );
  }

  // Éxito
  if (estado === ESTADOS.EXITO) {
    return (
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        style={estilos.contenedor}
      >
        <ScrollView contentContainerStyle={{ flexGrow: 1, justifyContent: 'center' }}>
          <Card>
            <Text style={estilos.titulo}>Contraseña actualizada</Text>
            <View style={estilos.mensajeExito}>
              <Text style={estilos.mensajeExitoTexto}>
                Su contraseña fue modificada correctamente.
              </Text>
            </View>

            <Button className="principal" onPress={irLogin}>
              Ir a iniciar sesión
            </Button>
          </Card>
        </ScrollView>
      </KeyboardAvoidingView>
    );
  }

  return null;
}