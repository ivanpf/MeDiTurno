/**
 * CU-7 - Registro de cuenta de paciente.
 * Con correo y contraseña, o con cuenta de Google.
 */

import React, { useState } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, KeyboardAvoidingView, Platform, ScrollView } from 'react-native';
import { useRouter } from 'expo-router';
import { signInWithPopup, GoogleAuthProvider, signOut } from 'firebase/auth';
import { auth } from '../../shared/src/auth/firebase';
import { registrarPaciente, verificarPacienteEnPadron } from '../../shared/src/api/endpoints';
import { Button, Input, Card, ErrorBanner, LoadingOverlay } from '../../shared/src/components';
import { validarRegistro, normalizarCedula } from '../../shared/src/utils/validation';
import { COLORS } from '../../shared/src/theme/colors';
import PantallaAcceso from '../../src/components/PantallaAcceso';

const FORMULARIO_INICIAL = {
  cedula: '',
  correo: '',
  telefono: '',
  domicilio: '',
  contrasena: '',
  repetirContrasena: '',
  proveedor: 'PASSWORD',
};

const estilos = StyleSheet.create({
  titulo: {
    fontSize: 22,
    fontWeight: '600',
    color: COLORS.azulOscuro,
    marginBottom: 8,
    textAlign: 'center',
  },
  subtitulo: {
    fontSize: 14,
    color: COLORS.gris,
    textAlign: 'center',
    marginBottom: 24,
  },
  metodoGrupo: {
    marginBottom: 16,
  },
  metodoLabel: {
    fontSize: 13,
    fontWeight: '600',
    color: COLORS.gris,
    marginBottom: 8,
  },
  metodoOpcion: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    padding: 12,
    borderWidth: 1,
    borderColor: COLORS.borde,
    borderRadius: 8,
    marginBottom: 8,
  },
  metodoOpcionActiva: {
    borderColor: COLORS.azul,
    backgroundColor: COLORS.azulClaro,
  },
  radio: {
    width: 20,
    height: 20,
    borderRadius: 10,
    borderWidth: 2,
    borderColor: COLORS.borde,
    justifyContent: 'center',
    alignItems: 'center',
  },
  radioActivo: {
    borderColor: COLORS.azul,
  },
  radioInner: {
    width: 10,
    height: 10,
    borderRadius: 5,
    backgroundColor: COLORS.azul,
  },
  metodoTexto: {
    fontSize: 14,
    color: COLORS.texto,
    flex: 1,
  },
  notaGoogle: {
    fontSize: 12,
    color: COLORS.gris,
    textAlign: 'center',
    marginTop: 8,
  },
  enlaceVolver: {
    marginTop: 16,
    alignSelf: 'center',
  },
  enlace: {
    color: COLORS.azul,
    fontSize: 14,
    textDecorationLine: 'underline',
  },
  exito: {
    alignItems: 'center',
  },
  exitoTitulo: {
    fontSize: 22,
    fontWeight: '600',
    color: COLORS.azulOscuro,
    marginBottom: 16,
  },
  mensajeExito: {
    alignSelf: 'stretch',
    paddingVertical: 16,
    paddingHorizontal: 18,
    backgroundColor: COLORS.exitoClaro,
    borderLeftWidth: 5,
    borderLeftColor: COLORS.exito,
    borderRadius: 8,
    marginBottom: 24,
  },
  mensajeExitoTexto: {
    fontSize: 15,
    lineHeight: 22,
    color: COLORS.exito,
  },
});

export default function RegistroScreen() {
  const router = useRouter();
  const [datos, setDatos] = useState(FORMULARIO_INICIAL);
  const [error, setError] = useState(null);
  const [enviando, setEnviando] = useState(false);
  const [creada, setCreada] = useState(false);

  const cambiar = (campo, valor) => {
    if (campo === 'cedula') {
      valor = normalizarCedula(valor);
    }
    setDatos((prev) => ({ ...prev, [campo]: valor }));
    if (error) setError(null);
  };

  const validar = () => {
    const { valido, errores } = validarRegistro(datos);
    if (!valido) {
      setError(new Error(errores.join('\n')));
      return false;
    }
    return true;
  };

  const registrarConCorreo = async () => {
    if (!validar()) return;
    setEnviando(true);
    try {
      await registrarPaciente({
        ...datos,
        proveedor: 'PASSWORD',
        contrasena: datos.contrasena,
      });
      setCreada(true);
    } catch (err) {
      setError(err);
    } finally {
      setEnviando(false);
    }
  };

  const registrarConGoogle = async () => {
    if (!datos.cedula.trim() || !datos.correo.trim()) {
      setError(new Error('Ingrese su cédula de identidad y su correo electrónico.'));
      return;
    }
    setEnviando(true);
    try {
      // 1. Verificar padrón antes de abrir Google
      await verificarPacienteEnPadron({ cedula: datos.cedula, correo: datos.correo });
      let tokenAcceso;
      try {
        const resultado = await signInWithPopup(auth, new GoogleAuthProvider());
        tokenAcceso = await resultado.user.getIdToken();
      } catch {
        throw new Error('No fue posible iniciar sesión con Google. Intente nuevamente.');
      }
      await registrarPaciente({ ...datos, proveedor: 'GOOGLE', tokenAcceso });
      setCreada(true);
    } catch (err) {
      setError(err);
    } finally {
      // Con Google queda una sesión abierta en Firebase; la persona entra desde el inicio de sesión.
      await signOut(auth);
      setEnviando(false);
    }
  };

  if (creada) {
    return (
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : 'height'} style={{ flex: 1 }}>
        <ScrollView contentContainerStyle={{ flexGrow: 1 }}>
          <PantallaAcceso>
            <Card>
              <View style={estilos.exito}>
                <Text style={estilos.exitoTitulo}>Crear cuenta</Text>
                <View style={estilos.mensajeExito}>
                  <Text style={estilos.mensajeExitoTexto}>Cuenta creada correctamente. Ya puede iniciar sesión.</Text>
                </View>
                <Button className="principal" onPress={() => router.replace('/(auth)/login')}>
                  Ir al inicio de sesión
                </Button>
              </View>
            </Card>
          </PantallaAcceso>
        </ScrollView>
      </KeyboardAvoidingView>
    );
  }

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      style={{ flex: 1 }}
    >
      <ScrollView contentContainerStyle={{ flexGrow: 1 }}>
        <PantallaAcceso>
          <Card>
            <Text style={estilos.titulo}>Crear cuenta</Text>
            <Text style={estilos.subtitulo}>Complete sus datos para registrarse como paciente.</Text>

            {error && (
              <ErrorBanner
                mensaje={error.message || 'No fue posible crear la cuenta. Intente nuevamente.'}
                detalles={error.detalles || []}
                onDismiss={() => setError(null)}
              />
            )}

            <Input
              label="Cédula de identidad *"
              placeholder="1.234.567-8"
              value={datos.cedula}
              onChangeText={(v) => cambiar('cedula', v)}
              maxLength={20}
              required
              disabled={enviando}
            />

            <Input
              label="Correo electrónico registrado en el padrón *"
              placeholder="correo@ejemplo.com"
              value={datos.correo}
              onChangeText={(v) => cambiar('correo', v)}
              keyboardType="email-address"
              autoComplete="email"
              maxLength={255}
              required
              disabled={enviando}
            />

            <Input
              label="Teléfono"
              placeholder="099 123 456"
              value={datos.telefono}
              onChangeText={(v) => cambiar('telefono', v)}
              keyboardType="phone-pad"
              maxLength={30}
              disabled={enviando}
            />

            <Input
              label="Domicilio"
              placeholder="Calle 1234, Ciudad"
              value={datos.domicilio}
              onChangeText={(v) => cambiar('domicilio', v)}
              maxLength={255}
              disabled={enviando}
            />

            <View style={estilos.metodoGrupo}>
              <Text style={estilos.metodoLabel}>Método de registro</Text>

              <TouchableOpacity
                style={[estilos.metodoOpcion, datos.proveedor === 'PASSWORD' && estilos.metodoOpcionActiva]}
                onPress={() => !enviando && setDatos((p) => ({ ...p, proveedor: 'PASSWORD' }))}
                disabled={enviando}
              >
                <View style={[estilos.radio, datos.proveedor === 'PASSWORD' && estilos.radioActivo]}>
                  {datos.proveedor === 'PASSWORD' && <View style={estilos.radioInner} />}
                </View>
                <Text style={estilos.metodoTexto}>Correo y contraseña</Text>
              </TouchableOpacity>

              {datos.proveedor === 'PASSWORD' && (
                <>
                  <Input
                    label="Contraseña * (mínimo 6 caracteres)"
                    placeholder="••••••"
                    value={datos.contrasena}
                    onChangeText={(v) => cambiar('contrasena', v)}
                    secureTextEntry
                    autoComplete="new-password"
                    minLength={6}
                    maxLength={128}
                    required
                    disabled={enviando}
                  />
                  <Input
                    label="Repita la contraseña *"
                    placeholder="••••••"
                    value={datos.repetirContrasena}
                    onChangeText={(v) => cambiar('repetirContrasena', v)}
                    secureTextEntry
                    autoComplete="new-password"
                    minLength={6}
                    maxLength={128}
                    required
                    disabled={enviando}
                  />
                </>
              )}

              <TouchableOpacity
                style={[estilos.metodoOpcion, datos.proveedor === 'GOOGLE' && estilos.metodoOpcionActiva]}
                onPress={() => !enviando && setDatos((p) => ({ ...p, proveedor: 'GOOGLE' }))}
                disabled={enviando}
              >
                <View style={[estilos.radio, datos.proveedor === 'GOOGLE' && estilos.radioActivo]}>
                  {datos.proveedor === 'GOOGLE' && <View style={estilos.radioInner} />}
                </View>
                <Text style={estilos.metodoTexto}>Cuenta de Google</Text>
              </TouchableOpacity>

              {datos.proveedor === 'GOOGLE' && (
                <Text style={estilos.notaGoogle}>
                  Con Google no hace falta la contraseña: use la cuenta de Google que tenga ese mismo correo electrónico.
                </Text>
              )}
            </View>

            {datos.proveedor === 'PASSWORD' ? (
              <Button className="principal" disabled={enviando} onPress={registrarConCorreo}>
                {enviando ? 'Creando cuenta...' : 'Crear cuenta'}
              </Button>
            ) : (
              <Button variant="secondary" disabled={enviando} onPress={registrarConGoogle}>
                Registrarme con Google
              </Button>
            )}

            <TouchableOpacity style={estilos.enlaceVolver} onPress={() => router.replace('/(auth)/login')}>
              <Text style={estilos.enlace}>Volver al inicio de sesión</Text>
            </TouchableOpacity>
          </Card>
        </PantallaAcceso>
      </ScrollView>

      <LoadingOverlay
        visible={enviando}
        mensaje={datos.proveedor === 'GOOGLE' ? 'Verificando padrón y abriendo Google...' : 'Creando cuenta...'}
        fullScreen
      />
    </KeyboardAvoidingView>
  );
}
