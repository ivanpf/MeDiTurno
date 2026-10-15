import { useEffect, useRef, useState } from 'react'
import { api, ApiError } from '../../services/api'
import estilos from './ReservarConsultaPage.module.css'

const fechaLocal = (fecha) => `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, '0')}-${String(fecha.getDate()).padStart(2, '0')}`
const fechaVisible = (fecha) => new Date(`${fecha}T12:00:00`).toLocaleDateString('es-UY', { weekday: 'short', day: '2-digit', month: '2-digit' })
const minutos = (hora) => Number(hora.slice(0, 2)) * 60 + Number(hora.slice(3, 5))
const horaEstimada = (turno, numero) => {
  const inicio = minutos(turno.horaInicio)
  const valor = Math.floor(inicio + (numero - 1) * (minutos(turno.horaFin) - inicio) / turno.cupos)
  return `${String(Math.floor(valor / 60)).padStart(2, '0')}:${String(valor % 60).padStart(2, '0')}`
}
const comoError = (err, mensaje) => err instanceof ApiError ? err : new ApiError(0, mensaje)

// CU-8 Reserva de una orden de consulta. La ventana de reserva (cuántas semanas hacia adelante) la define el backend: acá no se repite.
export default function ReservarConsultaPage() {
  const [especialidades, setEspecialidades] = useState([])
  const [codigo, setCodigo] = useState('')
  const [medico, setMedico] = useState('')
  const [ocurrencias, setOcurrencias] = useState([])
  const [mostrarTurnos, setMostrarTurnos] = useState(false)
  const [turno, setTurno] = useState(null)
  const [numero, setNumero] = useState(null)
  const [reserva, setReserva] = useState(null)
  const [error, setError] = useState(null)
  const [errorInicial, setErrorInicial] = useState(null)
  const [cargandoInicial, setCargandoInicial] = useState(true)
  const [cargando, setCargando] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [intento, setIntento] = useState(0)
  const solicitud = useRef(0)

  useEffect(() => {
    let vigente = true
    api.get('/especialidades').then((lista) => {
      if (vigente) setEspecialidades(lista.filter((item) => item.estado === 'ACTIVO'))
    }).catch((err) => {
      if (vigente) setErrorInicial(comoError(err, 'No fue posible cargar las especialidades. Intente nuevamente.'))
    }).finally(() => {
      if (vigente) setCargandoInicial(false)
    })
    return () => { vigente = false; solicitud.current += 1 }
  }, [intento])

  const cargarTurnos = async (especialidad, conservarMedico = false) => {
    const actual = ++solicitud.current
    setCodigo(especialidad)
    if (!conservarMedico) setMedico('')
    setOcurrencias([])
    setMostrarTurnos(false)
    setTurno(null)
    setNumero(null)
    setReserva(null)
    setError(null)
    setCargando(!!especialidad)
    if (!especialidad) return
    try {
      const lista = await api.get(`/turnos-semanales/ocurrencias-disponibles?${new URLSearchParams({ codigoEspecialidad: especialidad })}`)
      if (actual === solicitud.current) {
        setOcurrencias(lista.sort((a, b) => `${a.fecha} ${a.horaInicio}`.localeCompare(`${b.fecha} ${b.horaInicio}`)))
      }
    } catch (err) {
      if (actual === solicitud.current) setError(comoError(err, 'No fue posible consultar los turnos. Intente nuevamente.'))
    } finally {
      if (actual === solicitud.current) setCargando(false)
    }
  }

  // El endpoint disponible devuelve el médico de cada ocurrencia: no se consulta un listado de usuarios internos.
  const medicos = [...new Map(ocurrencias.map((item) => [String(item.idMedico), { id: String(item.idMedico), nombre: item.nombreMedico }])).values()]
  const turnos = ocurrencias.filter((item) => String(item.idMedico) === medico)
  const semanas = new Map()
  turnos.forEach((item) => {
    const fecha = new Date(`${item.fecha}T12:00:00`)
    fecha.setDate(fecha.getDate() - (fecha.getDay() + 6) % 7)
    const semana = fechaLocal(fecha)
    if (!semanas.has(semana)) semanas.set(semana, [])
    semanas.get(semana).push(item)
  })
  const ocupados = (item) => item.numerosNoDisponibles ?? []
  const libres = (item) => item.cupos - ocupados(item).length

  const reservar = async (e) => {
    e.preventDefault()
    if (enviando || !turno || numero === null || ocupados(turno).includes(numero)) return
    setEnviando(true)
    setError(null)
    try {
      const resultado = await api.post('/reservas', { idOcurrenciaTurno: turno.idOcurrenciaTurno, numero })
      setReserva(resultado)
      setTurno(null)
      setNumero(null)
      setMostrarTurnos(false)
      setOcurrencias([])
      setMedico('')
    } catch (err) {
      const fallo = comoError(err, 'No fue posible realizar la reserva. Intente nuevamente.')
      if (fallo.estado === 409) {
        await cargarTurnos(codigo, true)
        setError(new ApiError(409, 'El número o turno ya no está disponible. Actualice la selección y elija un número libre.'))
      } else {
        setError(fallo)
        if (fallo.estado === 403) { setTurno(null); setNumero(null); setMostrarTurnos(false) }
      }
    } finally {
      setEnviando(false)
    }
  }

  return (
    <section className="panel">
      <h1>Reservar consulta</h1>
      {reserva && (
        <div className="mensaje-exito" role="status">
          <strong>Reserva realizada correctamente.</strong>
          <p>Identificador: {reserva.idReserva} · Número: {reserva.numero} · Hora estimada: ~{reserva.horaEstimada?.slice(0, 5)}</p>
        </div>
      )}
      {(errorInicial || error) && (
        <div className="mensaje-error" role="alert">
          {(errorInicial || error).mensaje}
          {(errorInicial || error).detalles.length > 0 && <ul>{(errorInicial || error).detalles.map((detalle) => <li key={detalle}>{detalle}</li>)}</ul>}
          {errorInicial && <button type="button" onClick={() => { setCargandoInicial(true); setErrorInicial(null); setIntento((valor) => valor + 1) }}>Reintentar</button>}
        </div>
      )}
      <h2>1. Especialidad y médico</h2>
      {cargandoInicial && <p role="status">Cargando especialidades…</p>}
      {!cargandoInicial && !errorInicial && !especialidades.length && <p className="pendiente" role="status">No hay especialidades activas disponibles.</p>}
      <form onSubmit={(e) => { e.preventDefault(); setMostrarTurnos(true); setReserva(null) }}>
        <fieldset className={estilos.campos} disabled={enviando || cargandoInicial || !!errorInicial}>
          <div className={estilos.filtros}>
            <label>Especialidad *
              <select value={codigo} onChange={(e) => cargarTurnos(e.target.value)} required>
                <option value="">Seleccione una especialidad</option>
                {especialidades.map((item) => <option key={item.codigo} value={item.codigo}>{item.nombre}</option>)}
              </select>
            </label>
            <label>Médico *
              <select value={medico} disabled={cargando || !medicos.length} onChange={(e) => { setMedico(e.target.value); setMostrarTurnos(false); setTurno(null); setNumero(null); setReserva(null) }} required>
                <option value="">Seleccione un médico</option>
                {medicos.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}
              </select>
            </label>
            <button type="submit" disabled={cargando || !medico}>Ver turnos</button>
          </div>
          {codigo && <button type="button" disabled={cargando} onClick={() => cargarTurnos(codigo, true)}>Actualizar disponibilidad</button>}
        </fieldset>
      </form>
      {cargando && <p role="status">Consultando turnos disponibles…</p>}
      {codigo && !cargando && !error && !reserva && !ocurrencias.length && <p className="pendiente" role="status">No hay turnos disponibles para esta especialidad en las próximas ocho semanas.</p>}
      {mostrarTurnos && !reserva && (
        <div>
          <h2>2. Turnos disponibles (próximas ocho semanas)</h2>
          {turnos.length === 0 && <p className="pendiente" role="status">No hay turnos disponibles para el médico seleccionado.</p>}
          {[...semanas].map(([semana, lista]) => (
            <div key={semana}>
              <h3 className={estilos.semana}>Semana del {fechaVisible(semana)}</h3>
              <div className={estilos.turnos}>
                {lista.map((item) => (
                  <button type="button" key={item.idOcurrenciaTurno} className={estilos.turno} aria-pressed={turno?.idOcurrenciaTurno === item.idOcurrenciaTurno} disabled={enviando || libres(item) <= 0} onClick={() => { setTurno(item); setNumero(null); setError(null) }}>
                    <strong>{fechaVisible(item.fecha)}</strong>
                    <span>{item.horaInicio.slice(0, 5)}–{item.horaFin.slice(0, 5)}</span>
                    <span>Consultorio: {item.consultorio}</span>
                    <span>{libres(item) > 0 ? `${libres(item)} cupos libres` : 'Sin cupos'}</span>
                  </button>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}
      {turno && (
        <form onSubmit={reservar}>
          <h2>3. Elija su número</h2>
          <p>Los números ocupados no se pueden seleccionar. Las horas son estimadas.</p>
          <div className={estilos.numeros}>
            {Array.from({ length: turno.cupos }, (_, indice) => indice + 1).map((valor) => (
              <button type="button" key={valor} aria-pressed={numero === valor} disabled={enviando || ocupados(turno).includes(valor)} onClick={() => setNumero(valor)}>
                <strong>N.º {valor}</strong>
                <span>~{horaEstimada(turno, valor)}</span>
                {ocupados(turno).includes(valor) && <span>Ocupado</span>}
              </button>
            ))}
          </div>
          {numero !== null && <p role="status">Número {numero} · {fechaVisible(turno.fecha)} · Hora estimada: ~{horaEstimada(turno, numero)} · Consultorio: {turno.consultorio}</p>}
          <div className={estilos.acciones}>
            <button type="submit" className="principal" disabled={enviando || numero === null}>{enviando ? 'Reservando…' : 'Reservar'}</button>
            <button type="button" disabled={enviando} onClick={() => { setTurno(null); setNumero(null); setError(null) }}>Cancelar</button>
          </div>
        </form>
      )}
    </section>
  )
}
