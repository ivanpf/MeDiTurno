import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../../services/api'
import { comoError } from '../../utils/errores'
import { fecha, NOMBRE_ROL_INTERNO } from '../../utils/formatos'
import estilos from './BotonesEstado.module.css'
import ConfirmarCambioEstado from './ConfirmarCambioEstado'

const ORDENES = [
  ['apellido', 'Apellido'],
  ['nombre', 'Nombre'],
  ['rol', 'Rol'],
  ['fechaCreacion', 'Fecha de creación'],
]

const FILTROS_VACIOS = { texto: '', rol: '', estado: '', codigoEspecialidad: '', ordenarPor: 'apellido', descendente: false }

function Estado({ estado }) {
  return <span className={estado === 'ACTIVO' ? 'estado-activo' : 'estado-inactivo'}>{estado === 'ACTIVO' ? 'Activo' : 'Inactivo'}</span>
}

const aConsulta = (criterios) => {
  const consulta = new URLSearchParams()
  Object.entries(criterios).forEach(([clave, valor]) => {
    if (valor !== '' && valor !== false) consulta.set(clave, valor)
  })
  return consulta
}

// CU-36 Listado y búsqueda de usuarios internos (administradores, funcionarios y médicos), con su detalle. Desde el listado
// también se activan e inactivan las cuentas (CU-46) y se abre la modificación (CU-45).
export default function ListadoUsuariosPage() {
  const navigate = useNavigate()
  const [filtros, setFiltros] = useState(FILTROS_VACIOS)
  const [aplicados, setAplicados] = useState(FILTROS_VACIOS) // los filtros de la última búsqueda: con ellos se refresca el listado
  const [usuarios, setUsuarios] = useState(null)
  const [especialidades, setEspecialidades] = useState([])
  const [buscando, setBuscando] = useState(false)
  const [error, setError] = useState(null)
  const [aviso, setAviso] = useState(null)

  const [detalle, setDetalle] = useState(null)
  const [cargandoDetalle, setCargandoDetalle] = useState(false)
  const [errorDetalle, setErrorDetalle] = useState(null)
  const panelDetalle = useRef(null)
  const [cambiandoEstado, setCambiandoEstado] = useState(null)

  const buscar = async (criterios) => {
    setBuscando(true)
    setError(null)
    try {
      setUsuarios(await api.get(`/usuarios-internos?${aConsulta(criterios)}`))
      setAplicados(criterios)
    } catch (err) {
      setError(comoError(err, 'No fue posible consultar los usuarios. Intente nuevamente.'))
    } finally {
      setBuscando(false)
    }
  }

  useEffect(() => {
    // Listado inicial: todos los usuarios internos, más una lista de especialidades para filtrar.
    api.get('/usuarios-internos?ordenarPor=apellido')
      .then(setUsuarios)
      .catch((err) => setError(comoError(err, 'No fue posible consultar los usuarios. Intente nuevamente.')))
    api.get('/especialidades').then(setEspecialidades).catch(() => setEspecialidades([]))
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
    setAviso(null)
    buscar(filtros)
  }

  const limpiarFiltros = () => {
    setFiltros(FILTROS_VACIOS)
    setAviso(null)
    buscar(FILTROS_VACIOS)
  }

  const verDetalle = async (idUsuario) => {
    setCargandoDetalle(true)
    setErrorDetalle(null)
    try {
      setDetalle(await api.get(`/usuarios-internos/${idUsuario}`))
    } catch (err) {
      setDetalle(null)
      setErrorDetalle(comoError(err, 'No fue posible consultar el detalle del usuario. Intente nuevamente.'))
    } finally {
      setCargandoDetalle(false)
    }
  }

  const cerrarDetalle = () => {
    setDetalle(null)
    setErrorDetalle(null)
  }

  const modificar = (idUsuario) => navigate('/admin/usuarios/modificar', { state: { idUsuario } })

  const estadoCambiado = (resultado) => {
    const { apellido, nombre } = cambiandoEstado
    setCambiandoEstado(null)
    setAviso(`La cuenta de ${apellido}, ${nombre} fue ${resultado} correctamente.`)
    buscar(aplicados)
    if (detalle) verDetalle(detalle.idUsuario)
  }

  return (
    <section className="panel">
      <h1>Usuarios internos</h1>

      {aviso && <p className="mensaje-exito" role="status">{aviso}</p>}
      {error && <p className="mensaje-error" role="alert">{error.mensaje}</p>}

      <form className="listado-filtros" onSubmit={enviarBusqueda}>
        <div className="listado-campos">
          <label>
            Buscar por cédula, nombre, apellido o correo
            <input type="search" name="texto" value={filtros.texto} onChange={cambiarFiltro} maxLength={100} />
          </label>
          <label>
            Rol
            <select name="rol" value={filtros.rol} onChange={cambiarFiltro}>
              <option value="">Todos</option>
              {Object.entries(NOMBRE_ROL_INTERNO).map(([valor, etiqueta]) => <option key={valor} value={valor}>{etiqueta}</option>)}
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
            Especialidad (médicos)
            <select name="codigoEspecialidad" value={filtros.codigoEspecialidad} onChange={cambiarFiltro}>
              <option value="">Todas</option>
              {especialidades.map((e) => <option key={e.codigo} value={e.codigo}>{e.nombre}</option>)}
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
            <h2 id="titulo-detalle">{detalle.apellido}, {detalle.nombre}</h2>
            <dl className="detalle-datos">
              <div>
                <dt>Cédula</dt>
                <dd>{detalle.cedula}</dd>
              </div>
              <div>
                <dt>Fecha de nacimiento</dt>
                <dd>{fecha(detalle.fechaNacimiento)}</dd>
              </div>
              <div>
                <dt>Correo electrónico</dt>
                <dd>{detalle.correo}</dd>
              </div>
              <div>
                <dt>Rol</dt>
                <dd>{NOMBRE_ROL_INTERNO[detalle.rol]}</dd>
              </div>
              <div>
                <dt>Estado</dt>
                <dd><Estado estado={detalle.estado} /></dd>
              </div>
              <div>
                <dt>Fecha de creación</dt>
                <dd>{fecha(detalle.fechaCreacion)}</dd>
              </div>
              {detalle.rol === 'MEDICO' && (
                <>
                  <div>
                    <dt>Matrícula</dt>
                    <dd>{detalle.matricula}</dd>
                  </div>
                  <div className="detalle-ancho">
                    <dt>Especialidades</dt>
                    <dd>
                      {detalle.especialidades.map((e) => (e.estado === 'ACTIVO' ? e.nombre : `${e.nombre} (inactiva)`)).join(', ')}
                    </dd>
                  </div>
                </>
              )}
            </dl>
            <div className="listado-acciones">
              <button type="button" onClick={() => modificar(detalle.idUsuario)}>Modificar</button>
              <button type="button" className={detalle.estado === 'ACTIVO' ? estilos.inactivar : estilos.activar} onClick={() => setCambiandoEstado(detalle)}>
                {detalle.estado === 'ACTIVO' ? 'Inactivar cuenta' : 'Activar cuenta'}
              </button>
              <button type="button" onClick={cerrarDetalle}>Cerrar detalle</button>
            </div>
          </article>
        )}
      </div>

      {usuarios && usuarios.length === 0 && (
        <p className="pendiente" role="status">No se encontraron usuarios con los criterios seleccionados.</p>
      )}

      {usuarios && usuarios.length > 0 && (
        <div className="tabla-scroll">
          <table className="tabla">
            <thead>
              <tr>
                <th>Cédula</th>
                <th>Apellido</th>
                <th>Nombre</th>
                <th>Correo electrónico</th>
                <th>Rol</th>
                <th>Estado</th>
                <th>Creación</th>
                <th><span className="solo-lectores">Acciones</span></th>
              </tr>
            </thead>
            <tbody>
              {usuarios.map((u) => (
                <tr key={u.idUsuario}>
                  <td>{u.cedula}</td>
                  <td>{u.apellido}</td>
                  <td>{u.nombre}</td>
                  <td>{u.correo}</td>
                  <td>{NOMBRE_ROL_INTERNO[u.rol]}</td>
                  <td><Estado estado={u.estado} /></td>
                  <td>{fecha(u.fechaCreacion)}</td>
                  <td className="celda-botones">
                    <button type="button" onClick={() => verDetalle(u.idUsuario)} disabled={cargandoDetalle}>Ver detalle</button>
                    <button type="button" onClick={() => modificar(u.idUsuario)}>Modificar</button>
                    <button type="button" className={u.estado === 'ACTIVO' ? estilos.inactivar : estilos.activar} onClick={() => setCambiandoEstado(u)}>{u.estado === 'ACTIVO' ? 'Inactivar' : 'Activar'}</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {cambiandoEstado && (
        <ConfirmarCambioEstado usuario={cambiandoEstado} onTerminar={estadoCambiado} onCancelar={() => setCambiandoEstado(null)} />
      )}
    </section>
  )
}
