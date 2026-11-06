// Pantalla de inicio: va a la marcada como `inicio` en la tabla de rutas (cartelera),
// o a la primera del menú del rol. Se redirige con useEffect para evitar el warning
// de React de actualizar estado antes de que el componente esté montado.

import React, { useEffect } from 'react';
import { useRouter } from 'expo-router';
import { useAuth } from '../../shared/src/auth/useAuth';
import { rutaInicio } from '../../shared/src/navigation/rutas';

export default function Index() {
  const router = useRouter();
  const { rol, cargando } = useAuth();

  useEffect(() => {
    if (!cargando && rol) {
      router.replace(rutaInicio(rol));
    }
  }, [cargando, rol, router]);

  return null;
}
