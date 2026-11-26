import { useState } from 'react';
import { sendPasswordResetEmail } from 'firebase/auth';
import { auth } from '../../../shared/src/auth/firebase';
import { validarRecuperacion } from '../../../shared/src/utils/validation';

const MENSAJE_CONFIRMACION = 'Si existe una cuenta asociada a esa dirección, recibirá un correo con instrucciones para recuperar el acceso.';

export const mensajeDeError = (codigo) => {
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

// CU-4: Firebase envía el correo y procesa el cambio en su página segura; la app solo pide el correo.
export function useRecuperarAcceso() {
  const [correo, setCorreo] = useState('');
  const [enviada, setEnviada] = useState(false);
  const [error, setError] = useState(null); // Error | null
  const [cargando, setCargando] = useState(false);

  const solicitar = async () => {
    const correoNormalizado = correo.trim();
    const { valido, errores } = validarRecuperacion(correoNormalizado);
    if (!valido) {
      setError(new Error(errores[0]));
      return;
    }
    setCargando(true);
    setError(null);
    try {
      await sendPasswordResetEmail(auth, correoNormalizado);
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

  const reiniciar = () => {
    setEnviada(false);
    setError(null);
  };

  return { correo, setCorreo, enviada, error, cargando, solicitar, reiniciar };
}

export { MENSAJE_CONFIRMACION };
