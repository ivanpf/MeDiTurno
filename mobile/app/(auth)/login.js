/**
 * CU-1 - Inicio de sesión.
 * Email/password + Google (Firebase). Solo se permite el rol paciente.
 */

import React, { useEffect, useState } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, KeyboardAvoidingView, Platform, ScrollView } from 'react-native';
import { useRouter } from 'expo-router';
import { signInWithEmailAndPassword, signInWithPopup, GoogleAuthProvider, signOut } from 'firebase/auth';
import { auth } from '../../shared/src/auth/firebase';
import { useAuth } from '../../shared/src/auth/useAuth';
import { ROL_NO_PERMITIDO } from '../../shared/src/auth/AuthContext';
import { rutaInicio } from '../../shared/src/navigation/rutas';
import { Button, Input, Card, ErrorBanner, LoadingOverlay } from '../../shared/src/components';
import { COLORS } from '../../shared/src/theme/colors';
import PantallaAcceso from '../../src/components/PantallaAcceso';

const MENSAJE_ERROR = 'No fue posible iniciar sesión. Verifique sus datos e intente nuevamente.';

const estilos = StyleSheet.create({
  titulo: {
    fontSize: 22,
    fontWeight: '600',
    color: COLORS.azulOscuro,
    marginBottom: 8,
    textAlign: 'center',
  },
  separador: {
    flexDirection: 'row',
    alignItems: 'center',
    marginVertical: 20,
    gap: 12,
  },
  linea: {
    flex: 1,
    height: 1,
    backgroundColor: COLORS.borde,
  },
  separadorTexto: {
    fontSize: 13,
    color: COLORS.gris,
    fontWeight: '600',
  },
  enlaces: {
    marginTop: 24,
    alignItems: 'center',
    gap: 12,
  },
  enlace: {
    color: COLORS.azul,
    fontSize: 14,
    textDecorationLine: 'underline',
  },
});

export default function LoginScreen() {
  const router = useRouter();
  const { iniciarSesion, rol } = useAuth();
  const [correo, setCorreo] = useState('');
  const [contrasena, setContrasena] = useState('');
  const [error, setError] = useState(null);
  const [cargando, setCargando] = useState(false);

  useEffect(() => {
    if (rol) router.replace(rutaInicio(rol));
  }, [rol]);

  if (rol) return null;

  const autenticarEmail = async () => {
    await signInWithEmailAndPassword(auth, correo, contrasena);
  };

  const autenticarGoogle = async () => {
    const provider = new GoogleAuthProvider();
    await signInWithPopup(auth, provider);
  };

  const manejarLogin = async (autenticar) => {
    setError(null);
    setCargando(true);
    try {
      const sesion = await iniciarSesion({ autenticar });
      router.replace(rutaInicio(sesion.rol));
    } catch (err) {
      await signOut(auth); // Limpiar Firebase si el backend rechazó
      if (err?.codigo === ROL_NO_PERMITIDO) {
        setError(err);
      } else {
        setError(new Error(MENSAJE_ERROR));
      }
    } finally {
      setCargando(false);
    }
  };

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      style={{ flex: 1 }}
      keyboardVerticalOffset={0}
    >
      <ScrollView contentContainerStyle={{ flexGrow: 1 }}>
        <PantallaAcceso>
          <Card>
            <Text style={estilos.titulo}>Iniciar sesión</Text>

            {error && (
              <ErrorBanner
                mensaje={error.mensaje || MENSAJE_ERROR}
                detalles={error.detalles || []}
                onDismiss={() => setError(null)}
              />
            )}

            <Input
              label="Correo electrónico"
              placeholder="correo@ejemplo.com"
              value={correo}
              onChangeText={setCorreo}
              keyboardType="email-address"
              autoComplete="email"
              required
              error={error?.mensaje && !correo ? 'Requerido' : undefined}
              disabled={cargando}
            />

            <Input
              label="Contraseña"
              placeholder="••••••••"
              value={contrasena}
              onChangeText={setContrasena}
              secureTextEntry
              autoComplete="password"
              required
              error={error?.mensaje && !contrasena ? 'Requerido' : undefined}
              disabled={cargando}
            />

            <Button
              className="principal"
              disabled={cargando || !correo || !contrasena}
              onPress={() => manejarLogin(autenticarEmail)}
            >
              {cargando ? 'Ingresando...' : 'Ingresar'}
            </Button>

            <View style={estilos.separador}>
              <View style={estilos.linea} />
              <Text style={estilos.separadorTexto}>o</Text>
              <View style={estilos.linea} />
            </View>

            <Button
              variant="secondary"
              disabled={cargando}
              onPress={() => manejarLogin(autenticarGoogle)}
            >
              Iniciar sesión con Google
            </Button>

            <View style={estilos.enlaces}>
              <TouchableOpacity onPress={() => router.push('/(auth)/recuperar-acceso')}>
                <Text style={estilos.enlace}>¿Olvidó su contraseña? Recuperar acceso</Text>
              </TouchableOpacity>
              <TouchableOpacity onPress={() => router.push('/(auth)/registro')}>
                <Text style={estilos.enlace}>¿Es paciente y no tiene cuenta? Crear cuenta</Text>
              </TouchableOpacity>
            </View>
          </Card>
        </PantallaAcceso>
      </ScrollView>

      <LoadingOverlay visible={cargando} mensaje="Iniciando sesión..." fullScreen />
    </KeyboardAvoidingView>
  );
}
