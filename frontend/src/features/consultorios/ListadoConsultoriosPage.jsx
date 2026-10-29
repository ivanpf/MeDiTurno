import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../../services/api'
import { comoError } from '../../utils/errores'
import { hora, NOMBRE_DIA } from '../../utils/formatos'

const ORDENES = [
  ['nombre', 'Nombre'],
  ['codigo', 'Código'],
  ['ubicacion', 'Ubicación'],
]

const FILTROS_VACIOS = { texto: '', ubicacion: '', estado: '', ordenarPor: 'nombre', descendente: false }

function Estado({ estado }) {
  return <span className={estado === 'ACTIVO' ? 'estado-activo' : 'estado-inactivo'}>{estado === 'ACTIVO' ? 'Activo' : 'Inactivo'}</span>
}

// CU-39 Listado y búsqueda de consultorios, con el detalle de cada uno.
export default function ListadoConsultoriosPage() {
  const navigate = useNavigate()
  const [filtros, setFiltros] = useState(FILTROS_VACIOS)
  const [consultorios, setConsultorios] = useState(null)
  const [ubicaciones, setUbicaciones] = useState([]) // las ubicaciones que existen, para el filtro
  const [buscando, setBuscando] = useState(false)
  const [error, setError] = useState(null)

  const [detalle, setDetalle] = useState(null)
  const [cargandoDetalle, setCargandoDetalle] = useState(false)
  const [errorDetalle, setErrorDetalle] = useState(null)
  const panelDetalle = useRef(null)

  const consultar = (criterios) => {
    const consulta = new URLSearchParams()
    Object.entries(criterios).forEach(([clave, valor]) => {
      if (valor !== '' && valor !== false) consulta.set(clave, valor)
    })
    return api.get(`/consultorios?${consulta}`)
  }

  const buscar = async (criterios) => {
    setBuscando(true)
    setError(null)
    try {
      setConsultorios(await consultar(criterios))
    } catch (err) {
      setError(comoError(err, 'No fue posible consultar los consultorios. Intente nuevamente.'))
    } finally {
      setBuscando(false)
    }
  }

  useEffect(() => {
    // Listado inicial: todos los consultorios. De ahí salen las ubicaciones que ofrece el filtro.
    consultar(FILTROS_VACIOS)
      .then((lista) => {
        setConsultorios(lista)
        setUbicaciones([...new Set(lista.map((c) => c.ubicacion))].sort((a, b) => a.localeCompare(b, 'es')))
      })
      .catch((err) => setError(comoError(err, 'No fue posible consultar los consultorios. Intente nuevamente.')))
  }, [])

  // El detalle se abre arriba de la tabla: se lleva la vista hasta él para que se note.
  useEffect(() => {
    if (detalle || errorDetalle) panelDetalle.current?.scrollIntoView({ block: 'start' })
  }, [detalle, errorDetalle])

  const cambiarFiltro = (e) => {
    const { name, type, checked, value } = e.target
    setFiltros({ ...filtros, [name]: type === 'checkbox' ? checked : value })
  }

  const enviarBusqueda = (e) => {
    e.preventDefault()
    buscar(filtros)
  }

  const limpiarFiltros = () => {
    setFiltros(FILTROS_VACIOS)
    buscar(FILTROS_VACIOS)
  }

  const verDetalle = async (codigo) => {
    setCargandoDetalle(true)
    setErrorDetalle(null)
    try {
      setDetalle(await api.get(`/consultorios/${encodeURIComponent(codigo)}/detalle`))
    } catch (err) {
      setDetalle(null)
      setErrorDetalle(comoError(err, 'No fue posible consultar el detalle del consultorio. Intente nuevamente.'))
    } finally {
      setCargandoDetalle(false)
    }
  }

  const cerrarDetalle = () => {
    setDetalle(null)
    setErrorDetalle(null)
  }

  return (
    <section className="panel">
      <h1>Consultorios</h1>

      {error && <p className="mensaje-error" role="alert">{error.mensaje}</p>}

      <form className="listado-filtros" onSubmit={enviarBusqueda}>
        <div className="listado-campos">
          <label>
            Buscar por código o nombre
            <input type="search" name="texto" value={filtros.texto} onChange={cambiarFiltro} maxLength={100} />
          </label>
          <label>
            Ubicación
            <select name="ubicacion" value={filtros.ubicacion} onChange={cambiarFiltro}>
              <option value="">Todas</option>
              {ubicaciones.map((u) => <option key={u} value={u}>{u}</option>)}
            </select>
          </label>
          <label>
            Estado
            <select name="estado" value={filtros.estado} onChange={cambiarFiltro}>
              <option value="">Todos</option>
              <option value="ACTIVO">Activo</option>
              <option value="INACTIVO">Inactivo</option>
            </select>
          </label>
          <label>
            Ordenar por
            <select name="ordenarPor" value={filtros.ordenarPor} onChange={cambiarFiltro}>
              {ORDENES.map(([valor, etiqueta]) => <option key={valor} value={valor}>{etiqueta}</option>)}
            </select>
          </label>
        </div>
        <label className="casilla-inversa">
          <input type="checkbox" name="descendente" checked={filtros.descendente} onChange={cambiarFiltro} />
          Orden descendente
        </label>
        <div className="listado-acciones">
          <button type="submit" className="principal" disabled={buscando}>Buscar</button>
          <button type="button" onClick={limpiarFiltros} disabled={buscando}>Limpiar</button>
        </div>
      </form>

      <div ref={panelDetalle} className="detalle-ancla">
        {errorDetalle && (
          <div className="mensaje-error" role="alert">
            {errorDetalle.mensaje}
            <div className="listado-acciones">
              <button type="button" onClick={cerrarDetalle}>Cerrar</button>
            </div>
          </div>
        )}

        {detalle && (
          <article className="detalle-panel" aria-labelledby="titulo-detalle">
            <h2 id="titulo-detalle">{detalle.nombre}</h2>
            <dl className="detalle-datos">
              <div>
                <dt>Código</dt>
                <dd>{detalle.codigo}</dd>
              </div>
              <div>
                <dt>Estado</dt>
                <dd><Estado estado={detalle.estado} /></dd>
              </div>
              <div className="detalle-ancho">
                <dt>Ubicación</dt>
                <dd>{detalle.ubicacion}</dd>
              </div>
              <div className="detalle-ancho">
                <dt>Descripción</dt>
                <dd>{detalle.descripcion || 'Sin descripción'}</dd>
              </div>
            </dl>

            <h3>Turnos de atención vigentes</h3>
            {detalle.turnos.length === 0 ? (
              <p className="texto-vacio">No hay turnos de atención vigentes en este consultorio.</p>
            ) : (
              <div className="tabla-scroll">
                <table className="tabla">
                  <thead>
                    <tr><th>Día</th><th>Horario</th><th>Médico</th><th>Especialidad</th><th>Estado</th></tr>
                  </thead>
                  <tbody>
                    {detalle.turnos.map((t) => (
                      <tr key={t.idTurnoSemanal}>
                        <td>{NOMBRE_DIA[t.dia]}</td>
                        <td>{hora(t.horaInicio)}-{hora(t.horaFin)}</td>
                        <td>{t.nombreMedico}</td>
                        <td>{t.nombreEspecialidad}</td>
                        <td>
                          <span className={t.estado === 'ACTIVO' ? 'estado-activo' : 'estado-inactivo'}>
                            {t.estado === 'ACTIVO' ? 'Activo' : 'Suspendido'}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            <div className="listado-acciones">
              <button type="button" onClick={cerrarDetalle}>Cerrar detalle</button>
            </div>
          </article>
        )}
      </div>

      {consultorios && consultorios.length === 0 && (
        <p className="pendiente" role="status">No se encontraron consultorios con los criterios seleccionados.</p>
      )}

      {consultorios && consultorios.length > 0 && (
        <div className="tabla-scroll">
          <table className="tabla">
            <thead>
              <tr>
                <th>Código</th>
                <th>Nombre</th>
                <th>Ubicación</th>
                <th>Estado</th>
                <th>Turnos vigentes</th>
                <th><span className="solo-lectores">Acciones</span></th>
              </tr>
            </thead>
            <tbody>
              {consultorios.map((c) => (
                <tr key={c.codigo}>
                  <td>{c.codigo}</td>
                  <td>{c.nombre}</td>
                  <td>{c.ubicacion}</td>
                  <td><Estado estado={c.estado} /></td>
                  <td>{c.cantidadTurnos}</td>
                  <td className="celda-botones">
                    <button type="button" onClick={() => verDetalle(c.codigo)} disabled={cargandoDetalle}>Ver detalle</button>
                    <button type="button" onClick={() => navigate('/admin/consultorios/modificar', { state: { codigo: c.codigo } })}>
                      Modificar
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
