import { useEffect, useRef, useState } from 'react'
import { api, ApiError } from '../../services/api'
import estilos from './ListadoEspecialidadesPage.module.css'

const ORDENES = [
  ['nombre', 'Nombre'],
  ['codigo', 'Código'],
]

const NOMBRE_DIA = {
  LUNES: 'Lunes',
  MARTES: 'Martes',
  MIERCOLES: 'Miércoles',
  JUEVES: 'Jueves',
  VIERNES: 'Viernes',
  SABADO: 'Sábado',
  DOMINGO: 'Domingo',
}

const FILTROS_VACIOS = { texto: '', estado: '', requierePase: '', ordenarPor: 'nombre', descendente: false }

const hora = (texto) => texto.slice(0, 5)

const comoError = (err, mensaje) => (err instanceof ApiError ? err : new ApiError(0, mensaje))

function Estado({ estado }) {
  return <span className={estado === 'ACTIVO' ? estilos.activa : estilos.inactiva}>{estado === 'ACTIVO' ? 'Activa' : 'Inactiva'}</span>
}

// CU-30 Listado y búsqueda de especialidades médicas, con el detalle de cada una.
export default function ListadoEspecialidadesPage() {
  const [filtros, setFiltros] = useState(FILTROS_VACIOS)
  const [especialidades, setEspecialidades] = useState(null)
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
    return api.get(`/especialidades?${consulta}`)
  }

  const buscar = async (criterios) => {
    setBuscando(true)
    setError(null)
    try {
      setEspecialidades(await consultar(criterios))
    } catch (err) {
      setError(comoError(err, 'No fue posible consultar las especialidades. Intente nuevamente.'))
    } finally {
      setBuscando(false)
    }
  }

  useEffect(() => {
    // Listado inicial: todas las especialidades.
    consultar(FILTROS_VACIOS)
      .then(setEspecialidades)
      .catch((err) => setError(comoError(err, 'No fue posible consultar las especialidades. Intente nuevamente.')))
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
      setDetalle(await api.get(`/especialidades/${encodeURIComponent(codigo)}/detalle`))
    } catch (err) {
      setDetalle(null)
      setErrorDetalle(comoError(err, 'No fue posible consultar el detalle de la especialidad. Intente nuevamente.'))
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
      <h1>Especialidades</h1>

      {error && <p className="mensaje-error" role="alert">{error.mensaje}</p>}

      <form className={estilos.filtros} onSubmit={enviarBusqueda}>
        <div className={estilos.camposDeFiltro}>
          <label>
            Buscar por código o nombre
            <input type="search" name="texto" value={filtros.texto} onChange={cambiarFiltro} maxLength={100} />
          </label>
          <label>
            Estado
            <select name="estado" value={filtros.estado} onChange={cambiarFiltro}>
              <option value="">Todos</option>
              <option value="ACTIVO">Activa</option>
              <option value="INACTIVO">Inactiva</option>
            </select>
          </label>
          <label>
            Pase previo
            <select name="requierePase" value={filtros.requierePase} onChange={cambiarFiltro}>
              <option value="">Todas</option>
              <option value="true">Requiere pase</option>
              <option value="false">No requiere pase</option>
            </select>
          </label>
          <label>
            Ordenar por
            <select name="ordenarPor" value={filtros.ordenarPor} onChange={cambiarFiltro}>
              {ORDENES.map(([valor, etiqueta]) => <option key={valor} value={valor}>{etiqueta}</option>)}
            </select>
          </label>
        </div>
        <label className={estilos.inversa}>
          <input type="checkbox" name="descendente" checked={filtros.descendente} onChange={cambiarFiltro} />
          Orden descendente
        </label>
        <div className={estilos.acciones}>
          <button type="submit" className="principal" disabled={buscando}>Buscar</button>
          <button type="button" onClick={limpiarFiltros} disabled={buscando}>Limpiar</button>
        </div>
      </form>

      <div ref={panelDetalle} className={estilos.anclaDetalle}>
        {errorDetalle && (
          <div className="mensaje-error" role="alert">
            {errorDetalle.mensaje}
            <div className={estilos.acciones}>
              <button type="button" onClick={cerrarDetalle}>Cerrar</button>
            </div>
          </div>
        )}

        {detalle && (
          <article className={estilos.detalle} aria-labelledby="titulo-detalle">
            <h2 id="titulo-detalle">{detalle.nombre}</h2>
            <dl className={estilos.datos}>
              <div>
                <dt>Código</dt>
                <dd>{detalle.codigo}</dd>
              </div>
              <div>
                <dt>Estado</dt>
                <dd><Estado estado={detalle.estado} /></dd>
              </div>
              <div>
                <dt>Pase previo</dt>
                <dd>{detalle.requierePase ? 'Requiere pase' : 'No requiere pase'}</dd>
              </div>
              <div className={estilos.descripcion}>
                <dt>Descripción</dt>
                <dd>{detalle.descripcion || 'Sin descripción'}</dd>
              </div>
            </dl>

            <h3>Médicos activos que la atienden</h3>
            {detalle.medicos.length === 0 ? (
              <p className={estilos.vacio}>Ningún médico activo atiende esta especialidad.</p>
            ) : (
              <div className="tabla-scroll">
                <table className="tabla">
                  <thead>
                    <tr><th>Médico</th><th>Matrícula</th></tr>
                  </thead>
                  <tbody>
                    {detalle.medicos.map((m) => (
                      <tr key={m.idMedico}>
                        <td>{m.nombreCompleto}</td>
                        <td>{m.matricula}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            <h3>Turnos de atención vigentes</h3>
            {detalle.turnos.length === 0 ? (
              <p className={estilos.vacio}>No hay turnos de atención vigentes para esta especialidad.</p>
            ) : (
              <div className="tabla-scroll">
                <table className="tabla">
                  <thead>
                    <tr><th>Médico</th><th>Día</th><th>Horario</th><th>Consultorio</th><th>Estado</th></tr>
                  </thead>
                  <tbody>
                    {detalle.turnos.map((t) => (
                      <tr key={t.idTurnoSemanal}>
                        <td>{t.nombreMedico}</td>
                        <td>{NOMBRE_DIA[t.dia]}</td>
                        <td>{hora(t.horaInicio)}-{hora(t.horaFin)}</td>
                        <td>{t.nombreConsultorio}</td>
                        <td>
                          <span className={t.estado === 'ACTIVO' ? estilos.activa : estilos.inactiva}>
                            {t.estado === 'ACTIVO' ? 'Activo' : 'Suspendido'}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            <div className={estilos.acciones}>
              <button type="button" onClick={cerrarDetalle}>Cerrar detalle</button>
            </div>
          </article>
        )}
      </div>

      {especialidades && especialidades.length === 0 && (
        <p className="pendiente" role="status">No se encontraron especialidades con los criterios seleccionados.</p>
      )}

      {especialidades && especialidades.length > 0 && (
        <div className="tabla-scroll">
          <table className="tabla">
            <thead>
              <tr>
                <th>Código</th>
                <th>Nombre</th>
                <th>Descripción</th>
                <th>Pase previo</th>
                <th>Estado</th>
                <th><span className={estilos.soloLectores}>Acciones</span></th>
              </tr>
            </thead>
            <tbody>
              {especialidades.map((e) => (
                <tr key={e.codigo}>
                  <td>{e.codigo}</td>
                  <td>{e.nombre}</td>
                  <td className={estilos.descripcionTabla}>{e.descripcion}</td>
                  <td>{e.requierePase ? 'Sí' : 'No'}</td>
                  <td><Estado estado={e.estado} /></td>
                  <td>
                    <button type="button" onClick={() => verDetalle(e.codigo)} disabled={cargandoDetalle}>Ver detalle</button>
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
