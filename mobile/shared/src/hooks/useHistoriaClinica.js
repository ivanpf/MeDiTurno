/**
 * Hook para historia clínica propia (CU-11).
 */

import { useState, useCallback } from 'react';
import { consultarHistoriaClinica } from '../api/endpoints';
import { isApiError } from '../api/client';

/**
 * @returns {Object}
 */
export function useHistoriaClinica() {
  const [atenciones, setAtenciones] = useState([]);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState(null);
  const [filtros, setFiltros] = useState({
    fechaDesde: '',
    fechaHasta: '',
    especialidadCodigo: '',
    medicoId: '',
    ordenPor: 'fecha',
    ordenDireccion: 'DESC',
  });

  /**
   * Carga la historia clínica con filtros.
   */
  const cargar = useCallback(async () => {
    setCargando(true);
    setError(null);
    try {
      const criterios = {
        pagina: 0,
        tamanio: 50,
        ...filtros,
      };
      // Eliminar vacíos
      Object.keys(criterios).forEach((k) => {
        if (criterios[k] === '' || criterios[k] === null) delete criterios[k];
      });
      const data = await consultarHistoriaClinica(criterios);
      setAtenciones(data || []);
    } catch (err) {
      setError(isApiError(err) ? err : new Error('No se pudo cargar la historia clínica'));
    } finally {
      setCargando(false);
    }
  }, [filtros]);

  /**
   * Actualiza filtros y recarga.
   * @param {Object} nuevosFiltros
   */
  const actualizarFiltros = useCallback((nuevosFiltros) => {
    setFiltros((prev) => ({ ...prev, ...nuevosFiltros }));
  }, []);

  /**
   * Limpia filtros.
   */
  const limpiarFiltros = useCallback(() => {
    setFiltros({
      fechaDesde: '',
      fechaHasta: '',
      especialidadCodigo: '',
      medicoId: '',
      ordenPor: 'fecha',
      ordenDireccion: 'DESC',
    });
  }, []);

  return {
    atenciones,
    cargando,
    error,
    filtros,
    cargar,
    actualizarFiltros,
    limpiarFiltros,
    setError,
  };
}