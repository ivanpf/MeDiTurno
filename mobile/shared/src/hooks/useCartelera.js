/**
 * Hook para cartelera de novedades (CU-6).
 */

import { useState, useCallback, useEffect } from 'react';
import { listarCartelera } from '../api/endpoints';
import { isApiError } from '../api/client';

/**
 * @returns {Object}
 */
export function useCartelera() {
  const [anuncios, setAnuncios] = useState([]);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState(null);

  const cargar = useCallback(async () => {
    setCargando(true);
    setError(null);
    try {
      const data = await listarCartelera();
      setAnuncios(data || []);
    } catch (err) {
      setError(isApiError(err) ? err : new Error('No se pudo cargar la cartelera'));
    } finally {
      setCargando(false);
    }
  }, []);

  useEffect(() => {
    cargar();
  }, [cargar]);

  return {
    anuncios,
    cargando,
    error,
    recargar: cargar,
    setError,
  };
}