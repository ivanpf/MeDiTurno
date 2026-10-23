import { useEffect, useRef, useState } from 'react'
import { api, ApiError } from '../../services/api'
import estilos from './MiHistoriaClinicaPage.module.css'

const FILTROS_VACIOS = { fechaDesde: '', fechaHasta: '', codigoEspecialidad: '', idMedico: '', descendente: true }
const ERROR_LISTADO = 'No fue posible consultar su historia clínica. Intente nuevamente.'

// «2026-09-02T09:00:00» → «02/09/2026»
const fecha = (iso) => iso.slice(0, 10).split('-').reverse().join('/')

const comoError = (err, mensaje) => (err instanceof ApiError ? err : new ApiError(0, mensaje))

// GET /pacientes/me/historia-clinica?criterios (CriteriosBusquedaDTO): el backend solo ordena por fecha
// y los criterios vacíos no filtran.
const consultar = (filtros) => {
  const consulta = new URLSearchParams({ ordenarPor: 'fecha', descendente: filtros.descendente })
  ;['fechaDesde', 'fechaHasta', 'codigoEspecialidad', 'idMedico'].forEach((clave) => {
    if (filtros[clave]) consulta.set(clave, filtros[clave])
  })
  return api.get(`/pacientes/me/historia-clinica?${consulta}`)
}

// Las opciones de los filtros salen de las atenciones del paciente: solo los médicos y especialidades que lo atendieron.
// La consulta devuelve el nombre de la especialidad; el código para filtrar se toma del listado de especialidades.
function opcionesDe(atenciones, especialidades) {
  const medicos = new Map(atenciones.map((a) => [String(a.idMedico), a.nombreMedico]))
  const nombres = new Set(atenciones.map((a) => a.especialidad))
  return {
    medicos: [...medicos].map(([id, nombre]) => ({ id, nombre })).sort((a, b) => a.nombre.localeCompare(b.nombre, 'es')),
    especialidades: especialidades.filter((e) => nombres.has(e.nombre)),
  }
}

// Secciones del detalle, en el orden de la interfaz acordada. ConsultaResponseDTO solo trae estos tres textos
// clínicos; antecedentes, diagnóstico y tratamiento no forman parte de la respuesta del backend.
const seccionesDe = (atencion) => [
  { titulo: 'Motivo de consulta', texto: atencion.motivo },
  { titulo: 'Indicaciones', texto: atencion.indicaciones || 'Sin indicaciones.' },
  { titulo: 'Observaciones', texto: atencion.observaciones || 'Sin observaciones.' },
]

// CU-11 Consulta de la historia clínica propia: atenciones finalizadas del paciente de la sesión.
// Cada cambio de criterios reenvía la misma solicitud; el detalle de una atención ya viene en el listado.
export default function MiHistoriaClinicaPage() {
  const [opciones, setOpciones] = useState({ medicos: [], especialidades: [] })
  const [filtros, setFiltros] = useState(FILTROS_VACIOS)
  const [atenciones, setAtenciones] = useState([])
  // Si el paciente tiene alguna atención finalizada (sin filtros). null mientras no se sabe.
  // Sin ninguna se aplica el flujo alternativo 3.A (lista vacía, no es un error).
  const [tieneAtenciones, setTieneAtenciones] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [errorPeriodo, setErrorPeriodo] = useState(null)
  const [seleccionada, setSeleccionada] = useState(null)
  const solicitud = useRef(0) // descarta respuestas viejas si se vuelve a consultar
  const detalle = useRef(null)

  // Pasos 1 a 3: la historia completa (más recientes primero). Con ella se arman también los filtros.
  useEffect(() => {
    Promise.all([consultar(FILTROS_VACIOS), api.get('/especialidades').catch(() => [])])
      .then(([lista, especialidades]) => {
        setAtenciones(lista)
        setTieneAtenciones(lista.length > 0)
        setOpciones(opcionesDe(lista, especialidades))
      })
      .catch((err) => setError(comoError(err, ERROR_LISTADO)))
      .finally(() => setCargando(false))
  }, [])

  // Al elegir una atención se baja hasta su detalle, que queda debajo del listado.
  useEffect(() => {
    if (seleccionada) detalle.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }, [seleccionada])

  // Paso 4: filtros por período, especialidad o médico y orden por fecha.
  const aplicar = async (nuevos) => {
    setFiltros(nuevos)
    if (nuevos.fechaDesde && nuevos.fechaHasta && nuevos.fechaDesde > nuevos.fechaHasta) {
      setErrorPeriodo('La fecha «Desde» no puede ser posterior a la fecha «Hasta».')
      return
    }
    setErrorPeriodo(null)
    const actual = ++solicitud.current
    setCargando(true)
    setError(null)
    try {
      const lista = await consultar(nuevos)
      if (actual !== solicitud.current) return
      setAtenciones(lista)
      // Si la atención abierta ya no está en el listado (por un filtro), se cierra su detalle.
      setSeleccionada((previa) => lista.find((a) => a.idConsulta === previa?.idConsulta) ?? null)
    } catch (err) {
      if (actual === solicitud.current) setError(comoError(err, ERROR_LISTADO))
    } finally {
      if (actual === solicitud.current) setCargando(false)
    }
  }

  const cambiarFiltro = (e) => {
    const { name, value } = e.target
    aplicar({ ...filtros, [name]: name === 'descendente' ? value === 'true' : value })
  }

  const hayFiltros = Object.keys(FILTROS_VACIOS).some((clave) => filtros[clave] !== FILTROS_VACIOS[clave])

  return (
    <section className="panel">
      <h1>Mi historia clínica</h1>

      {error && <p className="mensaje-error" role="alert">{error.mensaje}</p>}
      {tieneAtenciones === null && cargando && <p role="status">Cargando su historia clínica…</p>}

      {/* Flujo alternativo 3.A: no existen atenciones finalizadas. */}
      {tieneAtenciones === false && (
        <p className="pendiente" role="status">No existen atenciones finalizadas para visualizar en su historia clínica.</p>
      )}

      {tieneAtenciones && (
        <>
          <div className={estilos.filtros}>
            <label>
              Desde
              <input type="date" name="fechaDesde" value={filtros.fechaDesde} max={filtros.fechaHasta || undefined} onChange={cambiarFiltro} />
            </label>
            <label>
              Hasta
              <input type="date" name="fechaHasta" value={filtros.fechaHasta} min={filtros.fechaDesde || undefined} onChange={cambiarFiltro} />
            </label>
            <label>
              Especialidad
              <select name="codigoEspecialidad" value={filtros.codigoEspecialidad} onChange={cambiarFiltro}>
                <option value="">Todas</option>
                {opciones.especialidades.map((e) => <option key={e.codigo} value={e.codigo}>{e.nombre}</option>)}
              </select>
            </label>
            <label>
              Médico
              <select name="idMedico" value={filtros.idMedico} onChange={cambiarFiltro}>
                <option value="">Todos</option>
                {opciones.medicos.map((m) => <option key={m.id} value={m.id}>{m.nombre}</option>)}
              </select>
            </label>
            <label>
              Ordenar por fecha
              <select name="descendente" value={String(filtros.descendente)} onChange={cambiarFiltro}>
                <option value="true">Más recientes</option>
                <option value="false">Más antiguas</option>
              </select>
            </label>
          </div>
          {errorPeriodo && <p className="mensaje-error" role="alert">{errorPeriodo}</p>}
          {hayFiltros && (
            <div className={estilos.acciones}>
              <button type="button" onClick={() => aplicar(FILTROS_VACIOS)} disabled={cargando}>Limpiar filtros</button>
            </div>
          )}

          {cargando && <p role="status">Buscando atenciones…</p>}
          {!cargando && atenciones.length === 0 && (
            <p className="pendiente" role="status">No se encontraron atenciones con los criterios seleccionados.</p>
          )}

          {!cargando && atenciones.length > 0 && (
            <div className="tabla-scroll">
              <table className={`tabla ${estilos.tabla}`}>
                <thead>
                  <tr>
                    <th scope="col">Fecha</th>
                    <th scope="col">Especialidad</th>
                    <th scope="col">Médico</th>
                    <th scope="col"><span className={estilos.soloLectores}>Acciones</span></th>
                  </tr>
                </thead>
                <tbody>
                  {atenciones.map((a) => {
                    const elegida = seleccionada?.idConsulta === a.idConsulta
                    return (
                      <tr key={a.idConsulta} className={elegida ? estilos.elegida : undefined}>
                        <td>{fecha(a.fecha)}</td>
                        <td>{a.especialidad}</td>
                        <td>{a.nombreMedico}</td>
                        <td>
                          {/* Pasos 5 y 6: el detalle registrado por el médico ya viene en el listado. */}
                          <button type="button" aria-pressed={elegida} className={elegida ? 'principal' : undefined}
                            onClick={() => setSeleccionada(a)}>
                            Ver detalle
                          </button>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          )}

          {seleccionada && (
            <div ref={detalle} className={estilos.detalle} aria-live="polite">
              <h2>Atención del {fecha(seleccionada.fecha)}</h2>
              <p className={estilos.subtitulo}>{seleccionada.especialidad} · {seleccionada.nombreMedico}</p>
              {seccionesDe(seleccionada).map((s) => (
                <div key={s.titulo} className={estilos.seccion}>
                  <h3>{s.titulo}:</h3>
                  <p>{s.texto}</p>
                </div>
              ))}
              <button type="button" onClick={() => setSeleccionada(null)}>Cerrar</button>
            </div>
          )}
        </>
      )}
    </section>
  )
}
