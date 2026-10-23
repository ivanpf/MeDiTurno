// Reservas de mobile: CU-8 (crear) y CU-9 (cancelar), separados en los bloques siguientes.
import { useState, useCallback, useEffect, useRef } from 'react';
import { reservarOrden, cancelarReserva, listarMisReservas } from '../api/endpoints';
import { motivoNoCancelable } from '../../../src/features/reservas/cancelacionReserva';

function describirError(error, accion) {
  if (error?.estado === 501) return {
    estado: 501, detalles: [],
    mensaje: accion === 'listar'
      ? 'La consulta de sus reservas todavía no está disponible. Intente nuevamente más tarde.'
      : 'La cancelación de reservas todavía no está disponible. Su reserva no se modificó.',
  };
  return { estado: error?.estado, detalles: error?.detalles || [],
    mensaje: error?.mensaje || (accion === 'listar' ? 'No se pudieron cargar sus reservas. Intente nuevamente.'
      : 'No se pudo confirmar el resultado de la cancelación. Actualice sus reservas antes de intentarlo nuevamente.') };
}

export function useReservas() {
  const [reservas, setReservas] = useState([]);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState(null);
  const [errorCancelacion, setErrorCancelacion] = useState(null);
  const [cancelando, setCancelando] = useState(null);
  const [requiereActualizar, setRequiereActualizar] = useState(false);
  const activo = useRef(true);
  const consulta = useRef(0);
  const listaActual = useRef([]);
  const envio = useRef(null);
  const consultando = useRef(false);
  const listaVigente = useRef(false);

  useEffect(() => {
    activo.current = true;
    return () => { activo.current = false; consulta.current++; };
  }, []);

  // Listado de apoyo del CU-9 mediante el endpoint existente del CU-10.
  const cargarReservas = useCallback(async (criterios = {}) => {
    if (envio.current !== null) return false;
    const solicitud = ++consulta.current;
    consultando.current = true;
    listaVigente.current = false;
    setCargando(true);
    setError(null);
    setErrorCancelacion(null);
    try {
      const datos = await listarMisReservas(criterios);
      if (!Array.isArray(datos)) throw new Error('Respuesta de reservas inválida');
      if (!activo.current || solicitud !== consulta.current) return false;
      listaActual.current = datos;
      listaVigente.current = true;
      setReservas(datos);
      setRequiereActualizar(false);
      return true;
    } catch (fallo) {
      if (activo.current && solicitud === consulta.current) setError(describirError(fallo, 'listar'));
      return false;
    } finally {
      if (activo.current && solicitud === consulta.current) {
        consultando.current = false;
        setCargando(false);
      }
    }
  }, []);

  // ---------------- CU-8: creación de una reserva ----------------
  // La pantalla reservar.js usa su propio useReservaConsulta; se conserva esta función compartida.
  const crearReserva = useCallback(async (datos) => {
    setError(null);
    try {
      const reserva = await reservarOrden(datos);
      if (activo.current) {
        listaActual.current = [reserva, ...listaActual.current];
        setReservas(listaActual.current);
      }
      return reserva;
    } catch (fallo) {
      if (activo.current) setError(fallo);
      throw fallo;
    }
  }, []);

  // ---------------- CU-9: cancelación de una reserva ----------------
  const cancelar = useCallback(async (idReserva) => {
    // Bloqueo inmediato: protege frente al doble toque y callbacks de un render anterior.
    if (!activo.current || envio.current !== null || consultando.current || !listaVigente.current) return false;
    const reserva = listaActual.current.find((item) => item.idReserva === idReserva);
    const motivo = motivoNoCancelable(reserva);
    if (motivo) {
      setErrorCancelacion({ mensaje: motivo, detalles: [] });
      return false;
    }
    envio.current = idReserva;
    setCancelando(idReserva);
    setErrorCancelacion(null);
    try {
      await cancelarReserva(idReserva);
      if (!activo.current) return false;
      // Se actualiza únicamente después de la confirmación del servidor (204).
      listaActual.current = listaActual.current.map((item) => item.idReserva === idReserva
        ? { ...item, estado: 'CANCELADA' } : item);
      setReservas(listaActual.current);
      return true;
    } catch (fallo) {
      if (activo.current) {
        setErrorCancelacion(describirError(fallo, 'cancelar'));
        // Tras un rechazo o una respuesta incierta se exige consultar otra vez; nunca se repite el POST solo.
        listaVigente.current = false;
        setRequiereActualizar(true);
      }
      return false;
    } finally {
      envio.current = null;
      if (activo.current) setCancelando(null);
    }
  }, []);

  const puedeCancelar = useCallback((reserva) => !motivoNoCancelable(reserva), []);
  const limpiarErrorCancelacion = useCallback(() => setErrorCancelacion(null), []);
  return { reservas, cargando, error, cargarReservas, crearReserva, cancelar, puedeCancelar,
    cancelando, errorCancelacion, requiereActualizar, limpiarErrorCancelacion, setError };
}
