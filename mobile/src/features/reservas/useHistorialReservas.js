import { useCallback, useEffect, useRef, useState } from 'react';
import { listarEspecialidades, listarMisReservas } from '../../../shared/src/api/endpoints';
import {
  FILTROS_HISTORIAL, criteriosDelHistorial, errorDelHistorial, validarPeriodoHistorial,
} from './historialReservas';

// CU-10: solo GET. Los errores del servidor se muestran sin simular resultados ni
// cambiar al listado de próximas reservas de CU-9 cuando falla el historial.
export function useHistorialReservas() {
  const [reservas, setReservas] = useState([]);
  const [filtros, setFiltros] = useState({ ...FILTROS_HISTORIAL });
  const [aplicados, setAplicados] = useState({ ...FILTROS_HISTORIAL });
  const [cargando, setCargando] = useState(true);
  const [consultado, setConsultado] = useState(false);
  const [error, setError] = useState(null);
  const [errorPeriodo, setErrorPeriodo] = useState(null);
  const [especialidades, setEspecialidades] = useState([]);
  const [cargandoEspecialidades, setCargandoEspecialidades] = useState(false);
  const [errorEspecialidades, setErrorEspecialidades] = useState(null);
  const [idSeleccionada, setIdSeleccionada] = useState(null);
  const activo = useRef(true);
  const solicitud = useRef(0);
  const catalogo = useRef(0);
  const filtrosAplicados = useRef({ ...FILTROS_HISTORIAL });

  const descartarConsultas = useCallback(() => {
    solicitud.current++;
    catalogo.current++;
  }, []);

  useEffect(() => {
    activo.current = true;
    return () => {
      activo.current = false;
      descartarConsultas();
    };
  }, [descartarConsultas]);

  const consultar = useCallback(async (criterios) => {
    if (!activo.current) return false;
    const problema = validarPeriodoHistorial(criterios);
    setErrorPeriodo(problema);
    if (problema) return false;
    const actual = ++solicitud.current;
    filtrosAplicados.current = { ...criterios };
    setAplicados({ ...criterios });
    setCargando(true);
    setConsultado(false);
    setError(null);
    setIdSeleccionada(null);
    try {
      const datos = await listarMisReservas(criteriosDelHistorial(criterios));
      if (!activo.current || actual !== solicitud.current) return false;
      if (!Array.isArray(datos) || !datos.every((reserva) => Number.isSafeInteger(reserva?.idReserva))) {
        throw new Error('El historial no devolvió una lista de reservas válida.');
      }
      setReservas(datos);
      setConsultado(true);
      return true;
    } catch (fallo) {
      if (activo.current && actual === solicitud.current) {
        setError(errorDelHistorial(fallo, 'No fue posible consultar su historial de reservas. Intente nuevamente.'));
      }
      return false;
    } finally {
      if (activo.current && actual === solicitud.current) setCargando(false);
    }
  }, []);

  const cargarEspecialidades = useCallback(async () => {
    const actual = ++catalogo.current;
    setCargandoEspecialidades(true);
    setErrorEspecialidades(null);
    try {
      const datos = await listarEspecialidades();
      if (!activo.current || actual !== catalogo.current) return;
      if (!Array.isArray(datos)) throw new Error('El catálogo no devolvió una lista válida.');
      // Un historial también puede contener especialidades que hoy están inactivas.
      setEspecialidades(datos.map((especialidad) => ({ value: especialidad.codigo, label: especialidad.nombre }))
        .sort((a, b) => a.label.localeCompare(b.label, 'es')));
    } catch (fallo) {
      if (activo.current && actual === catalogo.current) {
        setErrorEspecialidades(errorDelHistorial(fallo, 'No fue posible cargar las especialidades. Intente nuevamente.'));
      }
    } finally {
      if (activo.current && actual === catalogo.current) setCargandoEspecialidades(false);
    }
  }, []);

  // Al volver desde otra pantalla, renueva los resultados con los últimos filtros aplicados.
  const actualizar = useCallback(() => {
    consultar(filtrosAplicados.current);
    cargarEspecialidades();
  }, [consultar, cargarEspecialidades]);

  function cambiarFiltro(clave, valor) {
    setFiltros((anteriores) => ({ ...anteriores, [clave]: valor }));
    setErrorPeriodo(null);
    setIdSeleccionada(null);
  }

  function limpiarFiltros() {
    const vacios = { ...FILTROS_HISTORIAL };
    setFiltros(vacios);
    return consultar(vacios);
  }

  const cerrarDetalle = useCallback(() => setIdSeleccionada(null), []);
  const seleccionar = (idReserva) => setIdSeleccionada((anterior) => anterior === idReserva ? null : idReserva);
  const hayCambios = Object.keys(FILTROS_HISTORIAL).some((clave) => filtros[clave] !== aplicados[clave]);
  const hayFiltros = Object.keys(FILTROS_HISTORIAL).some((clave) => filtros[clave] !== FILTROS_HISTORIAL[clave]);

  return {
    reservas, filtros, cargando, consultado, error, errorPeriodo, especialidades, errorEspecialidades,
    cargandoEspecialidades, idSeleccionada, hayCambios, hayFiltros,
    actualizar, descartarConsultas, cambiarFiltro, limpiarFiltros, cargarEspecialidades,
    buscar: () => consultar(filtros), seleccionar, cerrarDetalle,
  };
}
