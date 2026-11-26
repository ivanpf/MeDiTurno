import { useState } from 'react';
import { EmailAuthProvider, reauthenticateWithCredential, updatePassword } from 'firebase/auth';
import { auth } from '../../../shared/src/auth/firebase';
import { validarCambioContrasena } from '../../../shared/src/utils/validation';

const MINIMO_CONTRASENA = 6;

const VACIO = { actual: '', nueva: '', repetirNueva: '' };

export const mensajeDeError = (codigo) => {
  switch (codigo) {
    case 'auth/invalid-credential':
    case 'auth/wrong-password':
      return 'La contraseña actual no es correcta.';
    case 'auth/weak-password':
      return `La nueva contraseña debe tener al menos ${MINIMO_CONTRASENA} caracteres.`;
    case 'auth/requires-recent-login':
      return 'Por seguridad, cierre sesión e inicie sesión nuevamente antes de cambiar la contraseña.';
    case 'auth/too-many-requests':
      return 'Se realizaron demasiados intentos. Espere un momento e inténtelo nuevamente.';
    case 'auth/network-request-failed':
      return 'No se pudo conectar con el servicio de autenticación. Verifique su conexión e inténtelo nuevamente.';
    default:
      return 'No fue posible cambiar la contraseña. Intente nuevamente.';
  }
};

// CU-3: cambio de contraseña directo con Firebase Auth (reautenticación + updatePassword).
export function useCambiarContrasena() {
  const [datos, setDatos] = useState(VACIO);
  const [error, setError] = useState(null); // { mensaje, detalles }
  const [modificada, setModificada] = useState(false);
  const [cargando, setCargando] = useState(false);

  const cambiarCampo = (campo) => (valor) => setDatos((prev) => ({ ...prev, [campo]: valor }));

  const limpiar = () => {
    setDatos(VACIO);
    setError(null);
    setModificada(false);
  };

  const guardar = async () => {
    setError(null);
    setModificada(false);
    const { valido, errores } = validarCambioContrasena(datos);
    if (!valido) {
      setError({ mensaje: errores[0], detalles: [] });
      return;
    }
    const usuario = auth.currentUser;
    if (!usuario?.email) {
      setError({ mensaje: 'Su sesión venció. Inicie sesión nuevamente para cambiar la contraseña.', detalles: [] });
      return;
    }
    setCargando(true);
    try {
      const credencial = EmailAuthProvider.credential(usuario.email, datos.actual);
      await reauthenticateWithCredential(usuario, credencial);
      await updatePassword(usuario, datos.nueva);
      setDatos(VACIO);
      setModificada(true);
    } catch (err) {
      setError({ mensaje: mensajeDeError(err?.code), detalles: [] });
    } finally {
      setCargando(false);
    }
  };

  return { datos, error, modificada, cargando, cambiarCampo, guardar, limpiar };
}
