/**
 * Hook para gestionar turnos y ocurrencias (CU-8 - pasos 1 y 2).
 */

import { useState, useCallback } from 'react';
import { listarEspecialidades, listarOcurrenciasDisponibles } from '../api/endpoints';
import { isApiError } from '../api/client';
import { formatearFechaCorta, calcularHoraEstimada } from '../utils/date';

/**
 * @returns {Object}
 */
export function useTurnos() {
  const [especialidades, setEspecialidades] = useState([]);
  const [ocurrencias, setOcurrencias] = useState([]);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState(null);

  /**
   * Carga especialidades activas (Paso 1 de CU-8).
   */
  const cargarEspecialidades = useCallback(async () => {
    setCargando(true);
    setError(null);
    try {
      const data = await listarEspecialidades({ estado: 'ACTIVO' });
      setEspecialidades(data || []);
    } catch (err) {
      setError(isApiError(err) ? err : new Error('No se pudieron cargar las especialidades'));
    } finally {
      setCargando(false);
    }
  }, []);

  /**
   * Carga ocurrencias disponibles para una especialidad (Paso 2 de CU-8).
   * @param {string} codigoEspecialidad
   */
  const cargarOcurrencias = useCallback(async (codigoEspecialidad) => {
    if (!codigoEspecialidad) {
      setOcurrencias([]);
      return;
    }
    setCargando(true);
    setError(null);
    try {
      const data = await listarOcurrenciasDisponibles(codigoEspecialidad);
      // Enriquecer con fecha formateada y hora estimada base
      const enriquecidas = (data || []).map((o) => ({
        ...o,
        fechaFormateada: formatearFechaCorta(o.fecha),
        horaEstimadaBase: (numero) =>
          calcularHoraEstimada(o.horaInicio, o.horaFin, o.cupoMaximo, numero),
      }));
      setOcurrencias(enriquecidas);
    } catch (err) {
      setError(isApiError(err) ? err : new Error('No se pudieron cargar los turnos'));
    } finally {
      setCargando(false);
    }
  }, []);

  /**
   * Agrupa ocurrencias por semana para mostrar en UI.
   * @returns {Object[]} Array de { semana: string, items: OcurrenciaTurnoDTO[] }
   */
  const ocurrenciasPorSemana = useCallback(() => {
    const grupos = {};
    ocurrencias.forEach((o) => {
      const fecha = new Date(o.fecha + 'T00:00:00');
      const inicioSemana = new Date(fecha);
      inicioSemana.setDate(fecha.getDate() - fecha.getDay()); // Domingo inicio
      const key = inicioSemana.toISOString().split('T')[0];
      if (!grupos[key]) {
        grupos[key] = { semanaInicio: inicioSemana, items: [] };
      }
      grupos[key].items.push(o);
    });
    return Object.values(grupos)
      .sort((a, b) => a.semanaInicio - b.semanaInicio)
      .map((g) => ({
        semana: `Semana del ${formatearFechaCorta(g.semanaInicio.toISOString().split('T')[0]).split(' ')[1]}`,
        items: g.items,
      }));
  }, [ocurrencias]);

  return {
    especialidades,
    ocurrencias,
    ocurrenciasPorSemana,
    cargando,
    error,
    cargarEspecialidades,
    cargarOcurrencias,
    setError,
  };
}