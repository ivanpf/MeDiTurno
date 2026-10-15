// Pantalla de inicio: va a la primera del menú del rol (igual que rutaInicio en la web).

import React from 'react';
import { Redirect } from 'expo-router';
import { useAuth } from '../../shared/src/auth/useAuth';
import { rutaInicio } from '../../shared/src/navigation/rutas';

export default function Index() {
  const { rol, cargando } = useAuth();
  if (cargando) return null;
  return <Redirect href={rutaInicio(rol)} />;
}
