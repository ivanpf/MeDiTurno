import React, { useEffect } from 'react';
import { AuthProvider } from '../shared/src/auth/AuthContext';
import { conectarEscuchaDeNotificaciones } from '../shared/src/push/notificacionesPush';

export function Providers({ children }) {
  // Los avisos del paciente que llegan con la app abierta se muestran con el listener de notificaciones.
  useEffect(() => conectarEscuchaDeNotificaciones(), []);

  return <AuthProvider>{children}</AuthProvider>;
}
