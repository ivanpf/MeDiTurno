import { useCallback, useEffect, useRef, useState } from 'react';
import {
  listarEspecialidades, listarOcurrenciasDisponibles, reservarOrden,
} from '../../../shared/src/api/endpoints';
import {
  agruparPorSemana, comoError, esNumeroLibre, obtenerMedicos, ordenarOcurrencias,
} from './reservaConsulta';

// Un hook es una función que reúne estado y acciones reutilizables de React.
// La pantalla llama estas acciones; el hook consulta la API y actualiza el estado.
export function useReservaConsulta() {
  const [especialidades, setEspecialidades] = useState([]);
  const [codigoEspecialidad, setCodigoEspecialidad] = useState('');
  const [ocurrencias, setOcurrencias] = useState([]);
  const [idMedico, setIdMedico] = useState('');
  const [idOcurrencia, setIdOcurrencia] = useState(null);
  const [numero, setNumero] = useState(null);
  const [reserva, setReserva] = useState(null);
  const [cargandoEspecialidades, setCargandoEspecialidades] = useState(true);
  const [cargandoTurnos, setCargandoTurnos] = useState(false);
  const [turnosConsultados, setTurnosConsultados] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [errorEspecialidades, setErrorEspecialidades] = useState(null);
  const [errorTurnos, setErrorTurnos] = useState(null);
  const [errorReserva, setErrorReserva] = useState(null);

  // useRef conserva valores de control sin redibujar la pantalla.
  // Los contadores descartan respuestas viejas si cambia la selección o salimos.
  const montado = useRef(false);
  const solicitudEspecialidades = useRef(0);
  const solicitudTurnos = useRef(0);
  const envioEnCurso = useRef(false);
  const reservaCompletada = useRef(false);

  const cargarEspecialidades = useCallback(async () => {
    if (envioEnCurso.current) return;
    const solicitud = ++solicitudEspecialidades.current;
    solicitudTurnos.current += 1;
    setCargandoEspecialidades(true);
    setCargandoTurnos(false);
    setErrorEspecialidades(null);
    setCodigoEspecialidad('');
    setIdMedico('');
    setOcurrencias([]);
    setIdOcurrencia(null);
    setNumero(null);
    setTurnosConsultados(false);
    setErrorTurnos(null);
    setErrorReserva(null);
    try {
      const lista = await listarEspecialidades();
      if (montado.current && solicitud === solicitudEspecialidades.current) {
        setEspecialidades(lista.filter((item) => item.estado === 'ACTIVO'));
      }
    } catch (fallo) {
      if (montado.current && solicitud === solicitudEspecialidades.current) {
        setEspecialidades([]);
        setErrorEspecialidades(comoError(fallo, 'No fue posible cargar las especialidades. Intente nuevamente.'));
      }
    } finally {
      if (montado.current && solicitud === solicitudEspecialidades.current) setCargandoEspecialidades(false);
    }
  }, []);

  // Se carga al entrar. La limpieza invalida cualquier respuesta que llegue tras salir.
  useEffect(() => {
    montado.current = true;
    cargarEspecialidades();
    return () => {
      montado.current = false;
      solicitudEspecialidades.current += 1;
      solicitudTurnos.current += 1;
    };
  }, [cargarEspecialidades]);

  async function cargarTurnos(codigo, medicoAConservar = '') {
    const solicitud = ++solicitudTurnos.current;
    setCodigoEspecialidad(codigo);
    setIdMedico(medicoAConservar);
    setOcurrencias([]);
    setIdOcurrencia(null);
    setNumero(null);
    setErrorTurnos(null);
    setErrorReserva(null);
    setTurnosConsultados(false);
    setCargandoTurnos(!!codigo);
    if (!codigo) return;

    try {
      // El backend valida el pase y limita la ventana de fechas reservables.
      const lista = ordenarOcurrencias(await listarOcurrenciasDisponibles(codigo));
      if (!montado.current || solicitud !== solicitudTurnos.current) return;
      setOcurrencias(lista);
      setTurnosConsultados(true);
      if (!lista.some((item) => String(item.idMedico) === medicoAConservar)) setIdMedico('');
    } catch (fallo) {
      if (!montado.current || solicitud !== solicitudTurnos.current) return;
      setIdMedico('');
      setErrorTurnos(comoError(fallo, 'No fue posible consultar los turnos. Intente nuevamente.'));
    } finally {
      if (montado.current && solicitud === solicitudTurnos.current) setCargandoTurnos(false);
    }
  }

  function seleccionarEspecialidad(codigo) {
    if (envioEnCurso.current || cargandoEspecialidades) return;
    return cargarTurnos(codigo);
  }

  function actualizarDisponibilidad() {
    if (envioEnCurso.current || cargandoEspecialidades) return;
    return cargarTurnos(codigoEspecialidad, idMedico);
  }

  // Al cambiar un paso, las elecciones que dependían de él dejan de ser válidas.
  function seleccionarMedico(id) {
    if (envioEnCurso.current || cargandoTurnos) return;
    setIdMedico(id);
    setIdOcurrencia(null);
    setNumero(null);
    setErrorReserva(null);
  }

  function seleccionarTurno(turno) {
    if (envioEnCurso.current || String(turno.idMedico) !== idMedico) return;
    setIdOcurrencia(turno.idOcurrenciaTurno);
    setNumero(null);
    setErrorReserva(null);
  }

  const especialidad = especialidades.find((item) => item.codigo === codigoEspecialidad);
  const medicos = obtenerMedicos(ocurrencias);
  const turno = ocurrencias.find((item) => item.idOcurrenciaTurno === idOcurrencia) ?? null;
  const semanas = agruparPorSemana(ocurrencias.filter((item) => String(item.idMedico) === idMedico));

  function seleccionarNumero(valor) {
    if (envioEnCurso.current || !esNumeroLibre(turno, valor)) return;
    setNumero(valor);
    setErrorReserva(null);
  }

  function cambiarFecha() {
    if (envioEnCurso.current) return;
    setIdOcurrencia(null);
    setNumero(null);
    setErrorReserva(null);
  }

  async function confirmarReserva() {
    if (envioEnCurso.current || reservaCompletada.current || !esNumeroLibre(turno, numero)) return;
    // El ref se bloquea de inmediato, incluso antes del siguiente render: evita doble envío.
    envioEnCurso.current = true;
    setEnviando(true);
    setErrorReserva(null);
    try {
      const resultado = await reservarOrden({ idOcurrenciaTurno: turno.idOcurrenciaTurno, numero });
      reservaCompletada.current = true;
      if (!montado.current) return;
      // El ID, número y hora definitivos vienen del servidor. Guardamos también el contexto visible.
      setReserva({ ...resultado, especialidad: especialidad.nombre,
        nombreMedico: turno.nombreMedico, fecha: turno.fecha, consultorio: turno.consultorio });
      setIdOcurrencia(null);
      setNumero(null);
      setOcurrencias([]);
    } catch (fallo) {
      if (!montado.current) return;
      const error = comoError(fallo, 'No se pudo confirmar la reserva. Compruebe el resultado antes de volver a intentarlo.');
      if (error.estado === 409) {
        // Otro paciente pudo tomar el número: se vuelven a pedir los datos, sin reenviar la reserva.
        await cargarTurnos(codigoEspecialidad, idMedico);
        if (montado.current) setErrorReserva({ ...error,
          mensaje: 'El número o turno ya no está disponible. Actualizamos la disponibilidad; elija una fecha y un número libre.' });
      } else if (error.estado === 403) {
        setOcurrencias([]);
        setIdMedico('');
        setIdOcurrencia(null);
        setNumero(null);
        setTurnosConsultados(false);
        setErrorTurnos(error);
      } else {
        setErrorReserva(error);
      }
    } finally {
      envioEnCurso.current = false;
      if (montado.current) setEnviando(false);
    }
  }

  function iniciarOtraReserva() {
    reservaCompletada.current = false;
    setReserva(null);
    setCodigoEspecialidad('');
    setIdMedico('');
    setIdOcurrencia(null);
    setNumero(null);
    setOcurrencias([]);
    setTurnosConsultados(false);
    setErrorTurnos(null);
    setErrorReserva(null);
  }

  return {
    especialidades, especialidad, codigoEspecialidad, medicos, idMedico, semanas, turno, numero,
    reserva, cargandoEspecialidades, cargandoTurnos, turnosConsultados, enviando,
    errorEspecialidades, errorTurnos, errorReserva,
    cargarEspecialidades, seleccionarEspecialidad, actualizarDisponibilidad, seleccionarMedico,
    seleccionarTurno, seleccionarNumero, cambiarFecha, confirmarReserva, iniciarOtraReserva,
    limpiarErrorReserva: () => setErrorReserva(null),
  };
}
