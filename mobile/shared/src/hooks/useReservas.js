/**
 * Hook para gestionar reservas (CU-8, CU-9, CU-10).
 */

import { useState, useCallback } from 'react';
import {
  reservarOrden,
  cancelarReserva,
  listarMisReservas,
} from '../api/endpoints';
import { isApiError } from '../api/client';
import { sePuedeCancelar } from '../utils/date';

/**
 * @returns {Object}
 */
export function useReservas() {
  const [reservas, setReservas] = useState([]);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState(null);

  /**
   * Carga el historial de reservas del paciente.
   * @param {Object} [criterios]
   */
  const cargarReservas = useCallback(async (criterios = {}) => {
    setCargando(true);
    setError(null);
    try {
      const data = await listarMisReservas(criterios);
      setReservas(data || []);
    } catch (err) {
      setError(isApiError(err) ? err : new Error('No se pudieron cargar las reservas'));
    } finally {
      setCargando(false);
    }
  }, []);

  /**
   * Crea una nueva reserva (CU-8).
   * @param {Object} data - { idOcurrenciaTurno, numero }
   * @returns {Promise<Object|null>} Reserva creada o null si error
   */
  const crearReserva = useCallback(async (data) => {
    setError(null);
    try {
      const reserva = await reservarOrden(data);
      // Agregar a la lista local
      setReservas((prev) => [reserva, ...prev]);
      return reserva;
    } catch (err) {
      const apiError = isApiError(err) ? err : new Error('No se pudo crear la reserva');
      setError(apiError);
      throw apiError;
    }
  }, []);

  /**
   * Cancela una reserva (CU-9).
   * @param {number} idReserva
   * @param {Object} reserva - Objeto reserva completo para validar >48h
   * @returns {Promise<boolean>}
   */
  const cancelar = useCallback(async (idReserva, reserva) => {
    if (reserva && !sePuedeCancelar(reserva.fechaOcurrencia?.split('T')[0], reserva.horaInicio)) {
      const err = new Error('No se puede cancelar: faltan menos de 48 horas');
      err.estado = 422;
      setError(err);
      throw err;
    }
    setError(null);
    try {
      await cancelarReserva(idReserva);
      // Actualizar estado local
      setReservas((prev) =>
        prev.map((r) => (r.id === idReserva ? { ...r, estado: 'CANCELADA' } : r))
      );
      return true;
    } catch (err) {
      const apiError = isApiError(err) ? err : new Error('No se pudo cancelar la reserva');
      setError(apiError);
      throw apiError;
    }
  }, []);

  /**
   * Verifica si una reserva se puede cancelar.
   * @param {Object} reserva
   * @returns {boolean}
   */
  const puedeCancelar = useCallback((reserva) => {
    return reserva.estado === 'VIGENTE' && sePuedeCancelar(reserva.fechaOcurrencia?.split('T')[0], reserva.horaInicio);
  }, []);

  return {
    reservas,
    cargando,
    error,
    cargarReservas,
    crearReserva,
    cancelar,
    puedeCancelar,
    setError,
  };
}