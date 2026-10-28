// Reservas de mobile: CU-8 (crear) y CU-9 (cancelar), separados en los bloques siguientes.
import { useState, useCallback, useEffect, useRef } from 'react';
import { reservarOrden, cancelarReserva, listarProximasPropias, vistaPreviaCancelacion } from '../api/endpoints';
import { motivoNoCancelable } from '../../../src/features/reservas/cancelacionReserva';

function describirError(error, accion) {
  if (error?.estado === 501) return {
    estado: 501, detalles: [],
    mensaje: 'Esta función de reservas todavía no está disponible en el servidor. Intente nuevamente más tarde.',
  };
  const mensajes = {
    listar: 'No se pudieron actualizar sus reservas. Intente nuevamente.',
    verificar: 'No se pudo verificar la reserva. Intente nuevamente antes de confirmar.',
    cancelar: 'No se pudo confirmar el resultado de la cancelación. Revise el estado actualizado de sus reservas antes de intentarlo nuevamente.',
  };
  return { estado: error?.estado, detalles: error?.detalles || [],
    mensaje: error?.mensaje || mensajes[accion] };
}

// Tanto el listado como la vista previa devuelven ReservaCancelacionDTO, con la reserva anidada.
function esDatoCancelacion(dato) {
  return Number.isSafeInteger(dato?.reserva?.idReserva) && typeof dato.puedeCancelar === 'boolean';
}

export function useReservas() {
  const [reservas, setReservas] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const [errorCancelacion, setErrorCancelacion] = useState(null);
  const [preparando, setPreparando] = useState(null);
  const [cancelando, setCancelando] = useState(null);
  const [confirmacion, setConfirmacion] = useState(null);
  const [cancelada, setCancelada] = useState(null);
  const [idSeleccionada, setIdSeleccionada] = useState(null);
  const [requiereActualizar, setRequiereActualizar] = useState(false);
  const activo = useRef(true);
  const consulta = useRef(0);
  const listaActual = useRef([]);
  const listaVigente = useRef(false);
  const consultando = useRef(false);
  const operacion = useRef(null);
  const confirmacionActual = useRef(null);

  useEffect(() => {
    activo.current = true;
    return () => {
      activo.current = false;
      consulta.current++;
      confirmacionActual.current = null;
    };
  }, []);

  // CU-9: listado de hoy y fechas próximas. El historial completo de CU-10 usa otro endpoint.
  const cargarReservas = useCallback(async ({ conservarAvisos = false } = {}) => {
    if (!activo.current || operacion.current !== null) return false;
    const solicitud = ++consulta.current;
    consultando.current = true;
    listaVigente.current = false;
    confirmacionActual.current = null;
    setConfirmacion(null);
    setIdSeleccionada(null);
    setCargando(true);
    setError(null);
    if (!conservarAvisos) {
      setErrorCancelacion(null);
      setCancelada(null);
    }
    try {
      const datos = await listarProximasPropias();
      if (!Array.isArray(datos) || !datos.every(esDatoCancelacion)) throw new Error('Respuesta de reservas inválida');
      if (!activo.current || solicitud !== consulta.current) return false;
      listaActual.current = datos;
      listaVigente.current = true;
      setReservas(datos);
      setRequiereActualizar(false);
      return true;
    } catch (fallo) {
      if (activo.current && solicitud === consulta.current) {
        setError(describirError(fallo, 'listar'));
        setRequiereActualizar(true);
      }
      return false;
    } finally {
      if (activo.current && solicitud === consulta.current) {
        consultando.current = false;
        setCargando(false);
      }
    }
  }, []);

  // ---------------- CU-8: creación de una reserva ----------------
  // reservar.js usa useReservaConsulta. Esta función compartida se conserva e invalida el listado de CU-9.
  const crearReserva = useCallback(async (datos) => {
    setError(null);
    try {
      const reserva = await reservarOrden(datos);
      if (activo.current) {
        listaVigente.current = false;
        confirmacionActual.current = null;
        setConfirmacion(null);
        setRequiereActualizar(true);
      }
      return reserva;
    } catch (fallo) {
      if (activo.current) setError(fallo);
      throw fallo;
    }
  }, []);

  // ---------------- CU-9: selección, vista previa y cancelación ----------------
  const seleccionarReserva = useCallback((idReserva) => {
    if (operacion.current !== null || consultando.current || !listaVigente.current) return;
    if (!listaActual.current.some((item) => item.reserva.idReserva === idReserva)) return;
    confirmacionActual.current = null;
    setConfirmacion(null);
    setIdSeleccionada(idReserva);
    setCancelada(null);
    setErrorCancelacion(null);
  }, []);

  const descartarConfirmacion = useCallback(() => {
    if (operacion.current !== null) return;
    confirmacionActual.current = null;
    setConfirmacion(null);
  }, []);

  const prepararCancelacion = useCallback(async (idReserva) => {
    if (!activo.current || operacion.current !== null || consultando.current || !listaVigente.current) return false;
    const item = listaActual.current.find((dato) => dato.reserva.idReserva === idReserva);
    const motivo = motivoNoCancelable(item);
    if (motivo) {
      setErrorCancelacion({ mensaje: motivo, detalles: [] });
      return false;
    }
    // Bloqueo inmediato también para la vista previa, antes del siguiente render.
    operacion.current = 'vista-previa';
    confirmacionActual.current = null;
    setConfirmacion(null);
    setPreparando(idReserva);
    setErrorCancelacion(null);
    setCancelada(null);
    let actualizar = false;
    try {
      const datos = await vistaPreviaCancelacion(idReserva);
      if (!activo.current) return false;
      if (!esDatoCancelacion(datos) || datos.reserva.idReserva !== idReserva) {
        throw new Error('Respuesta de vista previa inválida');
      }
      const rechazo = motivoNoCancelable(datos);
      if (rechazo) throw { estado: 409, mensaje: rechazo };
      listaActual.current = listaActual.current.map((dato) => dato.reserva.idReserva === idReserva ? datos : dato);
      setReservas(listaActual.current);
      confirmacionActual.current = datos;
      setConfirmacion(datos);
      return true;
    } catch (fallo) {
      if (activo.current) {
        setErrorCancelacion(describirError(fallo, 'verificar'));
        actualizar = fallo?.estado === 404 || fallo?.estado === 409;
      }
    } finally {
      operacion.current = null;
      if (activo.current) setPreparando(null);
    }
    if (actualizar && activo.current) await cargarReservas({ conservarAvisos: true });
    return false;
  }, [cargarReservas]);

  const cancelar = useCallback(async () => {
    const datos = confirmacionActual.current;
    if (!activo.current || operacion.current !== null || consultando.current || !listaVigente.current || !datos) return false;
    const idReserva = datos.reserva.idReserva;
    // Una vista previa habilita un solo envío. Un callback viejo no puede repetirlo.
    operacion.current = 'cancelacion';
    confirmacionActual.current = null;
    setCancelando(idReserva);
    setErrorCancelacion(null);
    let exito = false;
    try {
      await cancelarReserva(idReserva);
      if (!activo.current) return false;
      exito = true;
      setCancelada(idReserva);
    } catch (fallo) {
      if (activo.current) setErrorCancelacion(describirError(fallo, 'cancelar'));
    } finally {
      operacion.current = null;
      if (activo.current) {
        listaVigente.current = false;
        setConfirmacion(null);
        setCancelando(null);
        setRequiereActualizar(true);
      }
    }
    // Solo se repite el GET: devuelve el estado y la fecha de cancelación reales, incluso tras una respuesta incierta.
    if (activo.current) await cargarReservas({ conservarAvisos: true });
    return exito;
  }, [cargarReservas]);

  const puedeCancelar = useCallback((item) => !motivoNoCancelable(item), []);
  const limpiarErrorCancelacion = useCallback(() => setErrorCancelacion(null), []);
  return { reservas, cargando, error, cargarReservas, crearReserva, cancelar, puedeCancelar,
    preparando, cancelando, confirmacion, cancelada, idSeleccionada, seleccionarReserva,
    prepararCancelacion, descartarConfirmacion, errorCancelacion, requiereActualizar,
    limpiarErrorCancelacion, setError };
}
