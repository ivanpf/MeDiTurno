import { useCallback, useEffect, useRef, useState } from 'react';
import { ANUNCIOS_DEMO } from './anunciosDemo';
import { anunciosVigentes, comoError, fotosDelCarrusel } from './cartelera';

const ERROR_CARTELERA = 'No fue posible cargar la cartelera de novedades. Intente nuevamente.';

// Fuente de los anuncios. El backend del CU-6 todavía no está: cuando GET /api/anuncios devuelva
// AnuncioResponseDTO, se reemplaza por `listarCartelera` de shared/src/api/endpoints.
export const obtenerAnuncios = async () => ANUNCIOS_DEMO;

// Estado del CU-6: los anuncios vigentes y las fotos que pasan en el carrusel.
export function useCarteleraNovedades(obtener = obtenerAnuncios) {
  const [anuncios, setAnuncios] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);

  // Evita actualizar el estado si se sale de la pantalla antes de que llegue la respuesta.
  const montado = useRef(false);

  const cargar = useCallback(async () => {
    setCargando(true);
    setError(null);
    try {
      const lista = await obtener();
      if (montado.current) setAnuncios(anunciosVigentes(lista ?? []));
    } catch (fallo) {
      if (montado.current) setError(comoError(fallo, ERROR_CARTELERA));
    } finally {
      if (montado.current) setCargando(false);
    }
  }, [obtener]);

  useEffect(() => {
    montado.current = true;
    cargar();
    return () => { montado.current = false; };
  }, [cargar]);

  return {
    anuncios,
    fotos: fotosDelCarrusel(anuncios),
    cargando,
    error,
    recargar: cargar,
    limpiarError: () => setError(null),
  };
}
