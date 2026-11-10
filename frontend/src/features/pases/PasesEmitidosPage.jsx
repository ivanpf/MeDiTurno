import { useEffect, useState } from 'react'
import { api } from '../../services/api'
import { comoError } from '../../utils/errores'
import { fecha } from '../../utils/formatos'

const FILTROS_VACIOS = { texto: '', fechaDesde: '', fechaHasta: '', codigoEspecialidad: '', estado: '', orden: 'recientes' }

// El estado guardado puede seguir en VIGENTE después de los 60 días: se muestra como vencido si la vigencia ya pasó.
function estadoVisible(pase) {
  if (pase.paseEstado === 'VIGENTE' && new Date(pase.fechaVigenciaCalculada) < new Date()) return 'Vencido'
  return { VIGENTE: 'Vigente', USADO: 'Usado', VENCIDO: 'Vencido' }[pase.paseEstado] ?? pase.paseEstado
}

// CU-27 Búsqueda y listado de los pases médicos emitidos por el médico con sesión.
export default function PasesEmitidosPage() {
  const [filtros, setFiltros] = useState(FILTROS_VACIOS)
  const [especialidades, setEspecialidades] = useState([])
  const [pases, setPases] = useState(null)
  const [buscando, setBuscando] = useState(false)
  const [error, setError] = useState(null)

  const nombreEspecialidad = (codigo) => especialidades.find((e) => e.codigo === codigo)?.nombre ?? codigo

  const consultar = (criterios) => {
    const consulta = new URLSearchParams()
    const { orden, ...resto } = criterios
    Object.entries(resto).forEach(([clave, valor]) => {
      if (valor !== '') consulta.set(clave, valor.trim())
    })
    if (orden === 'antiguos') consulta.set('descendente', 'false')
    return api.get(`/pases?${consulta}`)
  }

  const buscar = async (criterios) => {
    if (criterios.fechaDesde && criterios.fechaHasta && criterios.fechaDesde > criterios.fechaHasta) {
      setError(comoError(null, 'La fecha «Desde» no puede ser posterior a «Hasta».'))
      return
    }
    setBuscando(true)
    setError(null)
    try {
      setPases(await consultar(criterios))
    } catch (err) {
      setError(comoError(err, 'No fue posible consultar los pases. Intente nuevamente.'))
    } finally {
      setBuscando(false)
    }
  }

  useEffect(() => {
    // Listado inicial: todos los pases emitidos, y las especialidades para el filtro y para mostrar sus nombres.
    consultar(FILTROS_VACIOS)
      .then(setPases)
      .catch((err) => setError(comoError(err, 'No fue posible consultar los pases. Intente nuevamente.')))
    api.get('/especialidades').then(setEspecialidades).catch(() => {})
  }, [])

  const cambiarFiltro = (e) => setFiltros({ ...filtros, [e.target.name]: e.target.value })

  const enviarBusqueda = (e) => {
    e.preventDefault()
    buscar(filtros)
  }

  const limpiarFiltros = () => {
    setFiltros(FILTROS_VACIOS)
    buscar(FILTROS_VACIOS)
  }

  return (
    <section className="panel">
      <h1>Pases emitidos</h1>

      {error && <p className="mensaje-error" role="alert">{error.mensaje}</p>}

      <form className="listado-filtros" onSubmit={enviarBusqueda}>
        <div className="listado-campos">
          <label>
            Paciente (nombre o cédula)
            <input type="search" name="texto" value={filtros.texto} onChange={cambiarFiltro} maxLength={100} />
          </label>
          <label>
            Emitido desde
            <input type="date" name="fechaDesde" value={filtros.fechaDesde} onChange={cambiarFiltro} />
          </label>
          <label>
            Emitido hasta
            <input type="date" name="fechaHasta" value={filtros.fechaHasta} onChange={cambiarFiltro} />
          </label>
          <label>
            Especialidad de destino
            <select name="codigoEspecialidad" value={filtros.codigoEspecialidad} onChange={cambiarFiltro}>
              <option value="">Todas</option>
              {especialidades.map((e) => <option key={e.codigo} value={e.codigo}>{e.nombre}</option>)}
            </select>
          </label>
          <label>
            Estado
            <select name="estado" value={filtros.estado} onChange={cambiarFiltro}>
              <option value="">Todos</option>
              <option value="VIGENTE">Vigente</option>
              <option value="USADO">Usado</option>
              <option value="VENCIDO">Vencido</option>
            </select>
          </label>
          <label>
            Ordenar por fecha
            <select name="orden" value={filtros.orden} onChange={cambiarFiltro}>
              <option value="recientes">Más recientes primero</option>
              <option value="antiguos">Más antiguos primero</option>
            </select>
          </label>
        </div>
        <div className="listado-acciones">
          <button type="submit" className="principal" disabled={buscando}>Buscar</button>
          <button type="button" onClick={limpiarFiltros} disabled={buscando}>Limpiar</button>
        </div>
      </form>

      {pases && pases.length === 0 && <p className="pendiente">No hay pases que coincidan con la búsqueda.</p>}

      {pases && pases.length > 0 && (
        <div className="tabla-scroll">
          <table className="tabla">
            <thead>
              <tr>
                <th>Emitido</th>
                <th>Paciente</th>
                <th>Especialidad de origen</th>
                <th>Especialidad de destino</th>
                <th>Vence</th>
                <th>Estado</th>
              </tr>
            </thead>
            <tbody>
              {pases.map((pase) => (
                <tr key={pase.idPase}>
                  <td>{fecha(pase.fechaEmision)}</td>
                  <td>{pase.paciente}</td>
                  <td>{nombreEspecialidad(pase.especialidadOrigen)}</td>
                  <td>{nombreEspecialidad(pase.especialidadDestino)}</td>
                  <td>{fecha(pase.fechaVigenciaCalculada)}</td>
                  <td>{estadoVisible(pase)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
