import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, ApiError } from '../../services/api'
import estilos from './PacientesPorTurnoPage.module.css'

const hoy = () => new Intl.DateTimeFormat('sv-SE', { timeZone: 'America/Montevideo' }).format(new Date())
const hora = (valor) => valor.slice(0, 5)
const comoError = (err) => err instanceof ApiError ? err : new ApiError(0, 'No fue posible consultar los datos. Intente nuevamente.')
const estados = { HABILITADA: 'Habilitado', SUSPENDIDA: 'Suspendido', FINALIZADA: 'Finalizado' }
const estadoOrden = (paciente) => paciente.estadoOrden === 'UTILIZADA' ? 'Orden utilizada'
  : paciente.estadoOrden === 'EMITIDA' ? 'Orden emitida'
    : paciente.estado === 'ORDEN_EMITIDA' ? 'Orden emitida' : 'Sin emitir'

// CU-21: el backend limita los turnos y los pacientes al médico de la sesión.
export default function PacientesPorTurnoPage() {
  const [fecha, setFecha] = useState(hoy)
  const [turnos, setTurnos] = useState([])
  const [idTurno, setIdTurno] = useState('')
  const [pacientes, setPacientes] = useState([])
  const [orden, setOrden] = useState('numero')
  const [descendente, setDescendente] = useState(false)
  const [cargandoTurnos, setCargandoTurnos] = useState(true)
  const [cargandoPacientes, setCargandoPacientes] = useState(false)
  const [errorTurnos, setErrorTurnos] = useState(null)
  const [errorPacientes, setErrorPacientes] = useState(null)
  const [intento, setIntento] = useState(0)

  useEffect(() => {
    let vigente = true
    if (!fecha) return () => { vigente = false }
    api.get(`/turnos/mios?${new URLSearchParams({ fecha })}`)
      .then((lista) => {
        if (vigente) {
          setTurnos(lista)
          setIdTurno((actual) => lista.some((turno) => String(turno.idOcurrenciaTurno) === actual) ? actual : '')
        }
      })
      .catch((err) => { if (vigente) setErrorTurnos(comoError(err)) })
      .finally(() => { if (vigente) setCargandoTurnos(false) })
    return () => { vigente = false }
  }, [fecha, intento])

  useEffect(() => {
    let vigente = true
    if (!idTurno) return () => { vigente = false }
    api.get(`/turnos/${idTurno}/pacientes`)
      .then((lista) => { if (vigente) setPacientes(lista) })
      .catch((err) => { if (vigente) setErrorPacientes(comoError(err)) })
      .finally(() => { if (vigente) setCargandoPacientes(false) })
    return () => { vigente = false }
  }, [idTurno, intento])

  const elegirTurno = (id) => {
    setIdTurno(id)
    setPacientes([])
    setErrorPacientes(null)
    setCargandoPacientes(!!id)
    // Permite volver a consultar el mismo turno después de un error.
    if (id === idTurno) setIntento((valor) => valor + 1)
  }
  const cambiarFecha = (valor) => {
    setFecha(valor)
    setTurnos([])
    setIdTurno('')
    setPacientes([])
    setErrorTurnos(null)
    setErrorPacientes(null)
    setCargandoTurnos(!!valor)
    setCargandoPacientes(false)
    if (valor === fecha) setIntento((actual) => actual + 1)
  }
  const actualizar = () => {
    setCargandoTurnos(true)
    setCargandoPacientes(!!idTurno)
    setErrorTurnos(null)
    setErrorPacientes(null)
    setPacientes([])
    setIntento((valor) => valor + 1)
  }

  const listado = [...pacientes].sort((a, b) => {
    const comparacion = orden === 'nombre' ? a.nombrePaciente.localeCompare(b.nombrePaciente, 'es', { sensitivity: 'base' })
      : orden === 'horario' ? a.horaEstimada.localeCompare(b.horaEstimada) : a.numero - b.numero
    return (comparacion || a.numero - b.numero) * (descendente ? -1 : 1)
  })
  const seleccionado = turnos.find((turno) => String(turno.idOcurrenciaTurno) === idTurno)

  return (
    <section className={`panel ${estilos.pagina}`}>
      <h1>Mis pacientes</h1>
      <p>Seleccione una fecha y uno de sus turnos para consultar los pacientes con reserva vigente u orden emitida.</p>
      <div className={estilos.controles}>
        <label>Fecha de atención
          <input type="date" value={fecha} onChange={(e) => cambiarFecha(e.target.value)} required />
        </label>
        <button type="button" onClick={() => cambiarFecha(hoy())}>Hoy</button>
        <button type="button" disabled={cargandoTurnos || cargandoPacientes || !fecha} onClick={actualizar}>Actualizar</button>
      </div>
      {errorTurnos && <p className="mensaje-error" role="alert">{errorTurnos.mensaje}</p>}
      {cargandoTurnos && <p role="status">Cargando turnos…</p>}
      {!fecha && <p className="pendiente" role="status">Seleccione una fecha de atención.</p>}
      {fecha && !cargandoTurnos && !errorTurnos && !turnos.length && <p className="pendiente" role="status">No tiene turnos de atención registrados para esta fecha.</p>}
      {!cargandoTurnos && !errorTurnos && turnos.length > 0 && (
        <div className={estilos.turnos} role="group" aria-label="Turnos de atención">
          {turnos.map((turno) => (
            <button type="button" key={turno.idOcurrenciaTurno} aria-pressed={idTurno === String(turno.idOcurrenciaTurno)}
              className={idTurno === String(turno.idOcurrenciaTurno) ? estilos.seleccionado : ''}
              onClick={() => elegirTurno(String(turno.idOcurrenciaTurno))}>
              <strong>{hora(turno.horaInicio)}–{hora(turno.horaFin)}</strong>
              <span>{turno.nombreEspecialidad}</span>
              <span>{turno.nombreConsultorio} · {estados[turno.estado] ?? turno.estado}</span>
            </button>
          ))}
        </div>
      )}
      {!cargandoTurnos && turnos.length > 0 && !idTurno && <p role="status">Seleccione un turno para ver sus pacientes.</p>}
      {!cargandoTurnos && !errorTurnos && seleccionado && (
        <div aria-busy={cargandoPacientes}>
          <h2>Pacientes de {hora(seleccionado.horaInicio)} a {hora(seleccionado.horaFin)}</h2>
          {errorPacientes && <p className="mensaje-error" role="alert">{errorPacientes.mensaje}</p>}
          {cargandoPacientes && <p role="status">Cargando pacientes…</p>}
          {!cargandoPacientes && !errorPacientes && !listado.length && <p className="pendiente" role="status">No hay pacientes asociados al turno.</p>}
          {!cargandoPacientes && !errorPacientes && listado.length > 0 && (
            <>
              <div className={estilos.controles}>
                <label>Ordenar por
                  <select value={orden} onChange={(e) => setOrden(e.target.value)}>
                    <option value="numero">Número</option><option value="nombre">Nombre</option><option value="horario">Horario</option>
                  </select>
                </label>
                <label className={estilos.inversa}><input type="checkbox" checked={descendente} onChange={(e) => setDescendente(e.target.checked)} />Orden descendente</label>
              </div>
              <p role="status">{listado.length} {listado.length === 1 ? 'paciente' : 'pacientes'} en este turno.</p>
              <div className="tabla-scroll">
                <table className="tabla">
                  <caption className={estilos.leyenda}>Reservas vigentes y órdenes emitidas · {fecha.split('-').reverse().join('/')}</caption>
                  <thead><tr><th scope="col">Número</th><th scope="col">Paciente</th><th scope="col">Hora estimada</th><th scope="col">Estado de la orden</th><th scope="col">Orden</th><th scope="col">Historia clínica</th><th scope="col">Consulta</th></tr></thead>
                  <tbody>{listado.map((paciente) => (
                    <tr key={paciente.idReserva}>
                      <td>{paciente.numero}</td><td>{paciente.nombrePaciente}</td><td>~{hora(paciente.horaEstimada)}</td>
                      <td><span className={paciente.estado === 'ORDEN_EMITIDA' ? estilos.emitida : estilos.sinEmitir}>{estadoOrden(paciente)}</span></td>
                      <td>{paciente.numeroOrden ?? '—'}</td>
                      <td>{seleccionado.fecha === hoy() ? <Link to={`/medico/historia-clinica?${new URLSearchParams({ idPaciente: paciente.idPaciente })}`} state={paciente}>Ver historia clínica</Link> : 'Disponible el día del turno'}</td>
                      <td>{paciente.estadoOrden === 'EMITIDA' ? <Link to="/medico/consulta" state={paciente}>Registrar consulta</Link> : '—'}</td>
                    </tr>
                  ))}</tbody>
                </table>
              </div>
            </>
          )}
        </div>
      )}
    </section>
  )
}
