/* oxlint-disable react/set-state-in-effect -- sincroniza carga, errores y selección con la consulta al servidor */
import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../services/api'
import { comoError } from '../../utils/errores'
import { fecha, hora, NOMBRE_DIA } from '../../utils/formatos'

const ESTADOS = { VIGENTE: 'Vigente', CANCELADA: 'Cancelada', ANULADA: 'Anulada', ORDEN_EMITIDA: 'Orden emitida', ATENDIDA: 'Atendida', VENCIDA: 'Vencida' }
const hoy = () => new Intl.DateTimeFormat('sv-SE', { timeZone: 'America/Montevideo' }).format(new Date())
const iniciales = (funcionario) => ({ fechaDesde: funcionario ? hoy() : '', fechaHasta: funcionario ? hoy() : '', idMedico: '', codigoEspecialidad: '', estado: '', ordenarPor: funcionario ? 'numero' : 'fecha', descendente: funcionario ? 'false' : 'true', idTurnoSemanal: '' })
const consulta = (filtros) => new URLSearchParams(Object.entries(filtros).filter(([, v]) => v !== '')).toString()

// CU-10 y CU-19 comparten la presentación de reservas; el servidor aplica los permisos y los filtros.
export default function ListadoReservas({ funcionario = false, revision = 0 }) {
  const ruta = funcionario ? '/reservas/para-atencion' : '/reservas/mias'
  const [filtros, setFiltros] = useState(() => iniciales(funcionario))
  const [aplicados, setAplicados] = useState(() => iniciales(funcionario))
  const [lista, setLista] = useState([])
  const [opciones, setOpciones] = useState({ medicos: [], especialidades: [], turnos: [] })
  const [error, setError] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [detalle, setDetalle] = useState(null)
  const [abriendo, setAbriendo] = useState(false)
  const [intento, setIntento] = useState(0)
  const solicitudDetalle = useRef(0)
  const ancla = useRef(null)

  useEffect(() => {
    let vigente = true
    const cargarOpciones = async () => {
      if (funcionario) {
        const [especialidades, turnos] = await Promise.all([api.get('/especialidades'), api.get('/turnos-semanales')])
        const medicos = [...new Map(turnos.map((t) => [t.idMedico, { id: t.idMedico, nombre: t.nombreMedico }])).values()]
        return { medicos, especialidades, turnos }
      }
      const todas = await api.get('/reservas/mias')
      return {
        medicos: [...new Map(todas.map((r) => [r.idMedico, { id: r.idMedico, nombre: r.nombreMedico }])).values()],
        especialidades: [...new Map(todas.map((r) => [r.codigoEspecialidad, { codigo: r.codigoEspecialidad, nombre: r.especialidad }])).values()],
        turnos: [],
      }
    }
    cargarOpciones().then((o) => { if (vigente) setOpciones(o) })
      .catch((err) => { if (vigente) setError(comoError(err, 'No fue posible cargar las opciones de búsqueda.')) })
    return () => { vigente = false }
  }, [funcionario, revision, intento])

  useEffect(() => {
    let vigente = true
    ++solicitudDetalle.current
    setCargando(true)
    setError(null)
    setDetalle(null)
    setAbriendo(false)
    api.get(`${ruta}?${consulta(aplicados)}`)
      .then((l) => { if (vigente) setLista(l) })
      .catch((err) => { if (vigente) { setLista([]); setError(comoError(err, 'No fue posible consultar las reservas.')) } })
      .finally(() => { if (vigente) setCargando(false) })
    return () => { vigente = false }
  }, [ruta, aplicados, revision, intento])

  useEffect(() => { if (detalle) ancla.current?.scrollIntoView({ behavior: 'smooth', block: 'start' }) }, [detalle])
  const cambiar = (e) => setFiltros({ ...filtros, [e.target.name]: e.target.value })
  const buscar = (e) => {
    e.preventDefault()
    if (filtros.fechaDesde && filtros.fechaHasta && filtros.fechaDesde > filtros.fechaHasta) {
      setError(comoError(null, 'La fecha Desde no puede ser posterior a Hasta.'))
      return
    }
    setAplicados({ ...filtros })
  }
  const abrir = async (id) => {
    const solicitud = ++solicitudDetalle.current
    setAbriendo(true)
    setError(null)
    setDetalle(null)
    try {
      const r = await api.get(funcionario ? `/reservas/${id}` : `/reservas/mias/${id}`)
      if (solicitud === solicitudDetalle.current) setDetalle(r)
    } catch (err) { if (solicitud === solicitudDetalle.current) setError(comoError(err, 'No fue posible abrir la reserva.')) }
    finally { if (solicitud === solicitudDetalle.current) setAbriendo(false) }
  }

  return <div>
    <form className="listado-filtros" onSubmit={buscar}>
      <div className="listado-campos">
        <label>Desde<input type="date" name="fechaDesde" value={filtros.fechaDesde} onChange={cambiar} /></label>
        <label>Hasta<input type="date" name="fechaHasta" value={filtros.fechaHasta} onChange={cambiar} /></label>
        <label>Médico<select name="idMedico" value={filtros.idMedico} onChange={cambiar}><option value="">Todos</option>{opciones.medicos.map((m) => <option key={m.id} value={m.id}>{m.nombre}</option>)}</select></label>
        <label>Especialidad<select name="codigoEspecialidad" value={filtros.codigoEspecialidad} onChange={cambiar}><option value="">Todas</option>{opciones.especialidades.map((e) => <option key={e.codigo} value={e.codigo}>{e.nombre}</option>)}</select></label>
        <label>Estado<select name="estado" value={filtros.estado} onChange={cambiar}><option value="">Todos</option>{Object.entries(ESTADOS).map(([v, n]) => <option key={v} value={v}>{n}</option>)}</select></label>
        {funcionario && <label>Turno semanal<select name="idTurnoSemanal" value={filtros.idTurnoSemanal} onChange={cambiar}><option value="">Todos</option>{opciones.turnos.map((t) => <option key={t.idTurnoSemanal} value={t.idTurnoSemanal}>#{t.idTurnoSemanal} · {t.nombreMedico} · {NOMBRE_DIA[t.dia]} {hora(t.horaInicio)}</option>)}</select></label>}
        <label>Ordenar por<select name="ordenarPor" value={filtros.ordenarPor} onChange={cambiar}>{(funcionario ? [['numero', 'Número'], ['paciente', 'Paciente']] : [['fecha', 'Fecha'], ['medico', 'Médico'], ['especialidad', 'Especialidad'], ['estado', 'Estado'], ['numero', 'Número']]).map(([v, n]) => <option key={v} value={v}>{n}</option>)}</select></label>
        <label>Sentido<select name="descendente" value={filtros.descendente} onChange={cambiar}><option value="false">Ascendente</option><option value="true">Descendente</option></select></label>
      </div>
      <div className="listado-acciones"><button className="principal" disabled={cargando}>Buscar</button><button type="button" disabled={cargando} onClick={() => { const f = iniciales(funcionario); setFiltros(f); setAplicados(f) }}>Restablecer</button><button type="button" disabled={cargando} onClick={() => setIntento(intento + 1)}>Actualizar</button></div>
    </form>
    {error && <p className="mensaje-error" role="alert">{error.mensaje}</p>}
    {cargando && <p role="status">Cargando reservas…</p>}
    {abriendo && <p role="status">Cargando detalle…</p>}
    {!cargando && !error && <p role="status">{lista.length ? `${lista.length} reservas encontradas.` : 'No se encontraron reservas para los criterios indicados.'}</p>}
    {!cargando && lista.length > 0 && <div className="tabla-scroll"><table className="tabla">
      <caption>{funcionario ? 'Reservas para la atención' : 'Historial completo de reservas'}</caption>
      <thead><tr><th>Reserva / número</th>{funcionario && <th>Paciente</th>}<th>Fecha y horario</th><th>Especialidad y médico</th><th>Consultorio</th><th>Estado</th><th>Acciones</th></tr></thead>
      <tbody>{lista.map((r) => <tr key={r.idReserva}>
        <td>#{r.idReserva}<br />Número {r.numero}</td>{funcionario && <td>{r.nombrePaciente}<br />CI {r.cedulaPaciente}</td>}
        <td>{fecha(r.fecha)}<br />{hora(r.horaInicio)}–{hora(r.horaFin)}<br />Estimada: ~{hora(r.horaEstimada)}</td>
        <td>{r.especialidad}<br />{r.nombreMedico}</td><td>{r.consultorio}</td><td>{ESTADOS[r.estado]}{r.numeroOrden && <><br />Orden #{r.numeroOrden}: {r.estadoOrden?.toLowerCase()}</>}</td>
        <td><button disabled={abriendo} onClick={() => abrir(r.idReserva)}>Ver detalle</button>{funcionario && <><br /><Link to={`/funcionario/reservas?idReserva=${r.idReserva}`}>Verificar o emitir orden</Link></>}</td>
      </tr>)}</tbody>
    </table></div>}
    {detalle && <article className="detalle-panel detalle-ancla" ref={ancla} aria-label="Detalle de la reserva">
      <h3>Reserva #{detalle.idReserva}</h3>
      <dl className="detalle-datos">{[
        ['Paciente', `${detalle.nombrePaciente} · CI ${detalle.cedulaPaciente}`], ['Especialidad', detalle.especialidad], ['Médico', detalle.nombreMedico],
        ['Fecha', fecha(detalle.fecha)], ['Horario del turno', `${hora(detalle.horaInicio)}–${hora(detalle.horaFin)}`], ['Hora estimada', `~${hora(detalle.horaEstimada)}`],
        ['Consultorio', detalle.consultorio], ['Número', detalle.numero], ['Estado', ESTADOS[detalle.estado]], ['Orden', detalle.numeroOrden ? `#${detalle.numeroOrden}` : 'Sin orden emitida'],
      ].map(([n, v]) => <div key={n}><dt>{n}</dt><dd>{v}</dd></div>)}</dl>
      <button onClick={() => setDetalle(null)}>Cerrar detalle</button>
    </article>}
  </div>
}
