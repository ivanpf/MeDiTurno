/* oxlint-disable react/set-state-in-effect -- sincroniza carga, errores y selección con la consulta al servidor */
import { useEffect, useRef, useState } from 'react'
import { api } from '../../services/api'
import { comoError } from '../../utils/errores'
import { fecha } from '../../utils/formatos'
import estilos from './HistorialAtencionesPage.module.css'

const VACIOS = { fechaDesde: '', fechaHasta: '', texto: '', codigoEspecialidad: '', descendente: 'true' }
export default function HistorialAtencionesPage() {
  const [filtros, setFiltros] = useState(VACIOS)
  const [aplicados, setAplicados] = useState(VACIOS)
  const [especialidades, setEspecialidades] = useState([])
  const [lista, setLista] = useState([])
  const [detalle, setDetalle] = useState(null)
  const [error, setError] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [abriendo, setAbriendo] = useState(false)
  const [intento, setIntento] = useState(0)
  const solicitud = useRef(0)
  const ancla = useRef(null)

  useEffect(() => {
    let vigente = true
    api.get('/especialidades').then((l) => { if (vigente) setEspecialidades(l) })
      .catch((err) => { if (vigente) setError(comoError(err, 'No fue posible cargar las especialidades.')) })
    return () => { vigente = false }
  }, [intento])
  useEffect(() => {
    let vigente = true
    ++solicitud.current
    setDetalle(null)
    setAbriendo(false)
    setCargando(true)
    setError(null)
    const parametros = new URLSearchParams(Object.entries(aplicados).filter(([, v]) => v !== ''))
    api.get(`/consultas/mias?${parametros}`)
      .then((l) => { if (vigente) setLista(l) })
      .catch((err) => { if (vigente) { setLista([]); setError(comoError(err, 'No fue posible consultar sus atenciones.')) } })
      .finally(() => { if (vigente) setCargando(false) })
    return () => { vigente = false }
  }, [aplicados, intento])
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
    const actual = ++solicitud.current
    setAbriendo(true)
    setDetalle(null)
    setError(null)
    try {
      const d = await api.get(`/consultas/mias/${id}`)
      if (actual === solicitud.current) setDetalle(d)
    } catch (err) { if (actual === solicitud.current) setError(comoError(err, 'No fue posible abrir la atención.')) }
    finally { if (actual === solicitud.current) setAbriendo(false) }
  }
  return <section className="panel">
    <h1>Historial de atenciones</h1><p>Consulte las atenciones que usted registró en MeDiTurno.</p>
    <form className="listado-filtros" onSubmit={buscar}>
      <div className="listado-campos">
        <label>Desde<input type="date" name="fechaDesde" value={filtros.fechaDesde} onChange={cambiar} /></label>
        <label>Hasta<input type="date" name="fechaHasta" value={filtros.fechaHasta} onChange={cambiar} /></label>
        <label>Nombre o cédula del paciente<input name="texto" value={filtros.texto} onChange={cambiar} maxLength={200} /></label>
        <label>Especialidad<select name="codigoEspecialidad" value={filtros.codigoEspecialidad} onChange={cambiar}><option value="">Todas</option>{especialidades.map((e) => <option key={e.codigo} value={e.codigo}>{e.nombre}</option>)}</select></label>
        <label>Orden por fecha<select name="descendente" value={filtros.descendente} onChange={cambiar}><option value="true">Más recientes primero</option><option value="false">Más antiguas primero</option></select></label>
      </div>
      <div className="listado-acciones"><button className="principal" disabled={cargando}>Buscar</button><button type="button" disabled={cargando} onClick={() => { setFiltros(VACIOS); setAplicados({ ...VACIOS }) }}>Limpiar filtros</button><button type="button" disabled={cargando} onClick={() => setIntento(intento + 1)}>Actualizar</button></div>
    </form>
    {error && <p className="mensaje-error" role="alert">{error.mensaje}</p>}
    {cargando && <p role="status">Cargando atenciones…</p>}
    {!cargando && !error && <p role="status">{lista.length ? `${lista.length} atenciones encontradas.` : 'No se encontraron atenciones para los criterios indicados.'}</p>}
    {!cargando && lista.length > 0 && <div className="tabla-scroll"><table className="tabla"><caption>Atenciones propias</caption>
      <thead><tr><th>Paciente</th><th>Fecha</th><th>Especialidad</th><th>Estado</th><th>Acciones</th></tr></thead>
      <tbody>{lista.map((a) => <tr key={a.idConsulta}><td>{a.nombrePaciente}</td><td>{fecha(a.fecha)} · {a.fecha.slice(11, 16)}</td><td>{a.especialidad}</td><td>Registrada</td><td><button disabled={abriendo} onClick={() => abrir(a.idConsulta)}>Ver detalle</button></td></tr>)}</tbody>
    </table></div>}
    {abriendo && <p role="status">Cargando detalle…</p>}
    {detalle && <article className="detalle-panel detalle-ancla" ref={ancla} aria-label="Detalle de la atención">
      <h2>Atención de {detalle.nombrePaciente}</h2>
      <dl className="detalle-datos"><div><dt>Fecha</dt><dd>{fecha(detalle.fecha)} · {detalle.fecha.slice(11, 16)}</dd></div><div><dt>Especialidad</dt><dd>{detalle.especialidad}</dd></div><div><dt>Estado</dt><dd>Registrada</dd></div><div><dt>Orden</dt><dd>#{detalle.numeroOrden}</dd></div></dl>
      {[['Motivo de consulta', detalle.motivo], ['Observaciones', detalle.observaciones], ['Indicaciones', detalle.indicaciones]].map(([titulo, texto]) => <div key={titulo}><h3>{titulo}</h3><p className={estilos.texto}>{texto || 'Sin información registrada.'}</p></div>)}
      <button onClick={() => setDetalle(null)}>Cerrar detalle</button>
    </article>}
  </section>
}
