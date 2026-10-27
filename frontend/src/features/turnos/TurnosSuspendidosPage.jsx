import { useEffect, useRef, useState } from 'react'
import Confirmacion from '../../components/Confirmacion'
import { api, ApiError } from '../../services/api'
import estilos from './TurnosSuspendidosPage.module.css'

const DIAS = [['LUNES', 'Lunes'], ['MARTES', 'Martes'], ['MIERCOLES', 'Miércoles'], ['JUEVES', 'Jueves'], ['VIERNES', 'Viernes'], ['SABADO', 'Sábado'], ['DOMINGO', 'Domingo']]
const NOMBRE_DIA = Object.fromEntries(DIAS)
const hora = (valor) => valor.slice(0, 5)
const fecha = (valor) => valor ? valor.split('-').reverse().join('/') : '—'
const comoError = (error) => error instanceof ApiError ? error : new ApiError(0, 'No fue posible completar la operación. Intente nuevamente.')

function Mensaje({ error }) {
  return <div className="mensaje-error" role="alert">
    {error.mensaje}
    {error.detalles?.length > 0 && <ul>{error.detalles.map((detalle) => <li key={detalle}>{detalle.split(': ').slice(1).join(': ') || detalle}</li>)}</ul>}
  </div>
}

function Suspension({ turno }) {
  const suspension = turno.suspension
  if (!suspension) return <span>Sin detalle de suspensión</span>
  return <span>{suspension.modalidad === 'TEMPORAL' ? 'Temporal' : 'Permanente'} desde {fecha(suspension.fechaDesde)}{suspension.fechaHasta && ` hasta ${fecha(suspension.fechaHasta)}`}</span>
}

export default function TurnosSuspendidosPage() {
  const [turnos, setTurnos] = useState([])
  const [consultorios, setConsultorios] = useState([])
  const [cargando, setCargando] = useState(true)
  const [errorCarga, setErrorCarga] = useState(null)
  const [intento, setIntento] = useState(0)
  const [elegido, setElegido] = useState(null)
  const [datos, setDatos] = useState(null)
  const [error, setError] = useState(null)
  const [confirmando, setConfirmando] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [rehabilitado, setRehabilitado] = useState(null)
  const envioEnCurso = useRef(false)

  useEffect(() => {
    let vigente = true
    Promise.all([api.get('/turnos-semanales'), api.get('/consultorios')])
      .then(([lista, salas]) => {
        if (!vigente) return
        setTurnos(lista.filter((turno) => turno.estado === 'SUSPENDIDO' || turno.suspension))
        setConsultorios(salas.filter((sala) => sala.estado === 'ACTIVO'))
      })
      .catch((err) => { if (vigente) setErrorCarga(comoError(err)) })
      .finally(() => { if (vigente) setCargando(false) })
    return () => { vigente = false }
  }, [intento])

  const actualizar = () => {
    setCargando(true)
    setErrorCarga(null)
    setElegido(null)
    setError(null)
    setRehabilitado(null)
    setIntento((actual) => actual + 1)
  }
  const abrir = (turno) => {
    setElegido(turno)
    setDatos({ dia: turno.dia, horaInicio: hora(turno.horaInicio), horaFin: hora(turno.horaFin), codigoConsultorio: turno.codigoConsultorio, cupoMaximo: String(turno.cupoMaximo) })
    setError(null)
    setRehabilitado(null)
  }
  const cambiar = (evento) => {
    setDatos((actuales) => ({ ...actuales, [evento.target.name]: evento.target.value }))
    setError(null)
  }
  const continuar = (evento) => {
    evento.preventDefault()
    setError(null)
    if (datos.horaFin <= datos.horaInicio) {
      setError(new ApiError(400, 'La hora de fin debe ser posterior a la hora de inicio.'))
      return
    }
    if (!consultorios.some((sala) => sala.codigo === datos.codigoConsultorio)) {
      setError(new ApiError(400, 'Seleccione un consultorio activo.'))
      return
    }
    setConfirmando(true)
  }
  const rehabilitar = async () => {
    if (envioEnCurso.current) return
    envioEnCurso.current = true
    setEnviando(true)
    setError(null)
    try {
      const respuesta = await api.post(`/turnos-semanales/${elegido.idTurnoSemanal}/rehabilitacion`, { ...datos, cupoMaximo: Number(datos.cupoMaximo) })
      setRehabilitado(respuesta)
      setTurnos((actuales) => actuales.filter((turno) => turno.idTurnoSemanal !== respuesta.idTurnoSemanal))
      setElegido(null)
    } catch (err) {
      setError(comoError(err))
    } finally {
      setConfirmando(false)
      setEnviando(false)
      envioEnCurso.current = false
    }
  }

  const consultorio = consultorios.find((sala) => sala.codigo === datos?.codigoConsultorio)

  return <section className={`panel ${estilos.pagina}`}>
    <h1>{elegido ? 'Rehabilitar turno' : 'Turnos suspendidos'}</h1>
    {cargando && <p role="status">Cargando turnos suspendidos…</p>}
    {errorCarga && <><Mensaje error={errorCarga} /><button type="button" className={estilos.actualizar} onClick={actualizar}>Reintentar</button></>}
    {rehabilitado && <>
      <p className="mensaje-exito" role="status">Turno rehabilitado correctamente. Las reservas anuladas no se restablecen.</p>
      <p>{rehabilitado.nombreMedico} · {rehabilitado.nombreEspecialidad} · {NOMBRE_DIA[rehabilitado.dia]} de {hora(rehabilitado.horaInicio)} a {hora(rehabilitado.horaFin)} · {rehabilitado.nombreConsultorio} · {rehabilitado.cupoMaximo} cupos.</p>
      <button type="button" onClick={() => setRehabilitado(null)}>Volver al listado</button>
    </>}
    {!cargando && !errorCarga && !rehabilitado && !elegido && <>
      <p>Se muestran las suspensiones vigentes y programadas. Seleccione un turno suspendido para revisar su configuración y rehabilitarlo.</p>
      <button type="button" className={estilos.actualizar} onClick={actualizar}>Actualizar listado</button>
      {turnos.length === 0 ? <p className="pendiente" role="status">No hay suspensiones vigentes ni programadas.</p> : <div className="tabla-scroll">
        <table className="tabla">
          <caption className={estilos.leyenda}>Turnos con suspensión vigente o programada</caption>
          <thead><tr><th scope="col">Médico</th><th scope="col">Especialidad</th><th scope="col">Día</th><th scope="col">Horario</th><th scope="col">Consultorio</th><th scope="col">Suspensión</th><th scope="col">Acciones</th></tr></thead>
          <tbody>{turnos.map((turno) => <tr key={turno.idTurnoSemanal}>
            <td>{turno.nombreMedico}</td><td>{turno.nombreEspecialidad}</td><td>{NOMBRE_DIA[turno.dia]}</td>
            <td>{hora(turno.horaInicio)}–{hora(turno.horaFin)}</td><td>{turno.nombreConsultorio}</td><td>
              <strong>{turno.estado === 'SUSPENDIDO' ? 'Vigente' : 'Programada'}</strong><br />
              <Suspension turno={turno} />
            </td>
            <td>{turno.estado === 'SUSPENDIDO'
              ? <button type="button" onClick={() => abrir(turno)} aria-label={`Rehabilitar turno de ${turno.nombreMedico}, ${NOMBRE_DIA[turno.dia]} a las ${hora(turno.horaInicio)}`}>Rehabilitar</button>
              : <span>El turno sigue activo. Podrá rehabilitarlo cuando la suspensión entre en vigencia.</span>}</td>
          </tr>)}</tbody>
        </table>
      </div>}
    </>}
    {elegido && <>
      <p className={estilos.resumen}>{elegido.nombreMedico} · {elegido.nombreEspecialidad}</p>
      <p><Suspension turno={elegido} /></p>
      <p className="pendiente">Las reservas anuladas no se restablecen. Los cambios se aplican a las nuevas fechas que se generen; las fechas ya existentes conservan su horario y cupos. Solo se reactivan las futuras que coincidan con el día elegido.</p>
      {error && <div className={estilos.mensaje}><Mensaje error={error} /></div>}
      <form className={estilos.formulario} onSubmit={continuar}>
        <h2>Configuración del turno</h2>
        <div className={estilos.campos}>
          <label>Día de la semana *<select name="dia" value={datos.dia} onChange={cambiar} disabled={enviando} required>{DIAS.map(([valor, texto]) => <option key={valor} value={valor}>{texto}</option>)}</select></label>
          <label>Hora de inicio *<input name="horaInicio" type="time" step="60" value={datos.horaInicio} onChange={cambiar} disabled={enviando} required /></label>
          <label>Hora de fin *<input name="horaFin" type="time" step="60" value={datos.horaFin} onChange={cambiar} disabled={enviando} required /></label>
          <label>Consultorio *<select name="codigoConsultorio" value={datos.codigoConsultorio} onChange={cambiar} disabled={enviando || !consultorios.length} required>
            {!consultorios.some((sala) => sala.codigo === elegido.codigoConsultorio) && <option value={elegido.codigoConsultorio} disabled>{elegido.nombreConsultorio} (no disponible)</option>}
            {consultorios.map((sala) => <option key={sala.codigo} value={sala.codigo}>{sala.nombre}</option>)}
          </select></label>
          <label>Máximo de pacientes *<input name="cupoMaximo" type="number" min="1" max="2147483647" step="1" value={datos.cupoMaximo} onChange={cambiar} disabled={enviando} required /></label>
        </div>
        {!consultorios.length && <p role="status">No hay consultorios activos disponibles. Actualice el listado después de habilitar uno.</p>}
        <div className={estilos.acciones}>
          <button type="submit" className="principal" disabled={enviando || !consultorios.length}>Continuar</button>
          <button type="button" disabled={enviando} onClick={() => { setElegido(null); setError(null) }}>Cancelar</button>
          <button type="button" disabled={enviando} onClick={actualizar}>Actualizar listado</button>
        </div>
      </form>
    </>}
    {confirmando && <Confirmacion titulo="Confirmar rehabilitación" textoConfirmar={enviando ? 'Rehabilitando…' : 'Rehabilitar turno'} ocupado={enviando} onConfirmar={rehabilitar} onCancelar={() => setConfirmando(false)}>
      <p>{elegido.nombreMedico} · {elegido.nombreEspecialidad}</p>
      <p>{NOMBRE_DIA[datos.dia]} de {datos.horaInicio} a {datos.horaFin} · {consultorio?.nombre} · {datos.cupoMaximo} cupos.</p>
      <p>Se cerrará la suspensión y el turno quedará activo. Las reservas anuladas no se restablecen.</p>
    </Confirmacion>}
  </section>
}
