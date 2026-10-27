import { useCallback, useEffect, useRef, useState } from 'react';
import { consultarHistoriaClinica, listarEspecialidades } from '../../../shared/src/api/endpoints';
import {
  FILTROS_VACIOS, comoError, criteriosDeConsulta, fechaIncompleta, opcionesDeFiltro, validarPeriodo,
} from './historiaClinica';

const ERROR_LISTADO = 'No fue posible consultar su historia clínica. Intente nuevamente.';

// Estado y acciones del CU-11 (GET /pacientes/me/historia-clinica?criterios).
// Cada cambio de criterios reenvía la misma solicitud; el detalle de una atención viene en el listado.
export function useHistoriaClinica() {
  const [atenciones, setAtenciones] = useState([]);
  // Si el paciente tiene alguna atención finalizada (sin filtros). null mientras no se sabe.
  // Sin ninguna se aplica el flujo alternativo 3.A (lista vacía, no es un error).
  const [tieneAtenciones, setTieneAtenciones] = useState(null);
  const [opciones, setOpciones] = useState({ medicos: [], especialidades: [] });
  const [filtros, setFiltros] = useState(FILTROS_VACIOS);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const [errorPeriodo, setErrorPeriodo] = useState(null);
  const [seleccionada, setSeleccionada] = useState(null);

  // Descarta respuestas viejas si se vuelve a consultar o se sale de la pantalla.
  const montado = useRef(false);
  const solicitud = useRef(0);

  async function consultar(criterios, pedir) {
    const actual = ++solicitud.current;
    setCargando(true);
    setError(null);
    try {
      const resultado = await pedir(criterios);
      if (!montado.current || actual !== solicitud.current) return;
      setAtenciones(resultado);
      // Si la atención abierta ya no está en el listado (por un filtro), se cierra su detalle.
      setSeleccionada((previa) => resultado.find((a) => a.idConsulta === previa?.idConsulta) ?? null);
    } catch (fallo) {
      if (montado.current && actual === solicitud.current) setError(comoError(fallo, ERROR_LISTADO));
    } finally {
      if (montado.current && actual === solicitud.current) setCargando(false);
    }
  }

  // Pasos 1 a 3: la historia completa (más recientes primero). Con ella se arman también los filtros.
  const cargarHistoria = useCallback(() => {
    setFiltros(FILTROS_VACIOS);
    setErrorPeriodo(null);
    return consultar(FILTROS_VACIOS, async (criterios) => {
      const [lista, especialidades] = await Promise.all([
        consultarHistoriaClinica(criteriosDeConsulta(criterios)),
        listarEspecialidades().catch(() => []),
      ]);
      if (montado.current) {
        setTieneAtenciones(lista.length > 0);
        setOpciones(opcionesDeFiltro(lista, especialidades));
      }
      return lista;
    });
  }, []);

  useEffect(() => {
    montado.current = true;
    cargarHistoria();
    return () => {
      montado.current = false;
      solicitud.current += 1;
    };
  }, [cargarHistoria]);

  // Paso 4: filtros por período, especialidad o médico y orden por fecha.
  function aplicar(nuevos) {
    setFiltros(nuevos);
    // Una fecha a medio escribir todavía no consulta; una completa e inválida avisa sin consultar.
    if (fechaIncompleta(nuevos.fechaDesde) || fechaIncompleta(nuevos.fechaHasta)) return;
    const problema = validarPeriodo(nuevos);
    setErrorPeriodo(problema);
    if (!problema) consultar(nuevos, (criterios) => consultarHistoriaClinica(criteriosDeConsulta(criterios)));
  }

  const cambiarFiltro = (clave, valor) => aplicar({ ...filtros, [clave]: valor });
  const limpiarFiltros = () => aplicar(FILTROS_VACIOS);

  // Pasos 5 y 6: el detalle registrado por el médico ya viene en el listado (sin una nueva invocación).
  const verDetalle = (atencion) => setSeleccionada(atencion);
  const cerrarDetalle = useCallback(() => setSeleccionada(null), []);

  const hayFiltros = Object.keys(FILTROS_VACIOS).some((clave) => filtros[clave] !== FILTROS_VACIOS[clave]);

  return {
    atenciones, tieneAtenciones, opciones, filtros, hayFiltros, cargando, error, errorPeriodo, seleccionada,
    cargarHistoria, cambiarFiltro, limpiarFiltros, verDetalle, cerrarDetalle,
    limpiarError: () => setError(null),
  };
}
