import { useState } from 'react'
import { api } from '../../services/api'
import { comoError } from '../../utils/errores'
import { NOMBRE_ROL_INTERNO } from '../../utils/formatos'
import estilos from './BotonesEstado.module.css'
import ConfirmarCambioEstado from './ConfirmarCambioEstado'

const FILTROS_VACIOS = { texto: '', rol: '', estado: '' }

function Estado({ estado }) {
  return <span className={estado === 'ACTIVO' ? 'estado-activo' : 'estado-inactivo'}>{estado === 'ACTIVO' ? 'Activo' : 'Inactivo'}</span>
}

// CU-46 Activación e inactivación de usuarios internos. Se busca la cuenta y se confirma el cambio antes de aplicarlo: una cuenta
// inactiva no puede iniciar sesión, pero se conservan sus reservas, órdenes y registros clínicos. El backend no permite inactivar
// la propia cuenta, al último administrador activo ni a un médico con turnos de atención vigentes.
export default function EstadoUsuarioPage() {
  const [filtros, setFiltros] = useState(FILTROS_VACIOS)
  const [aplicados, setAplicados] = useState(null)
  const [usuarios, setUsuarios] = useState(null)
  const [buscando, setBuscando] = useState(false)
  const [error, setError] = useState(null)
  const [aviso, setAviso] = useState(null)
  const [cambiando, setCambiando] = useState(null)

  const buscar = async (criterios) => {
    setBuscando(true)
    setError(null)
    try {
      const consulta = new URLSearchParams()
      Object.entries(criterios).forEach(([clave, valor]) => {
        if (valor !== '') consulta.set(clave, valor)
      })
      setUsuarios(await api.get(`/usuarios-internos?${consulta}`))
      setAplicados(criterios)
    } catch (err) {
      setUsuarios(null)
      setError(comoError(err, 'No fue posible consultar los usuarios. Intente nuevamente.'))
    } finally {
      setBuscando(false)
    }
  }

  const cambiarFiltro = (e) => setFiltros({ ...filtros, [e.target.name]: e.target.value })

  const enviarBusqueda = (e) => {
    e.preventDefault()
    setAviso(null)
    buscar(filtros)
  }

  const terminado = (resultado) => {
    const { apellido, nombre } = cambiando
    setCambiando(null)
    setAviso(`La cuenta de ${apellido}, ${nombre} fue ${resultado} correctamente.`)
    buscar(aplicados)
  }

  return (
    <section className="panel">
      <h1>Activación e inactivación de usuarios</h1>

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
        </div>
        <div className="listado-acciones">
          <button type="submit" className="principal" disabled={buscando}>Buscar</button>
        </div>
      </form>

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
                  <td className="celda-botones">
                    <button type="button" className={u.estado === 'ACTIVO' ? estilos.inactivar : estilos.activar} onClick={() => setCambiando(u)}>{u.estado === 'ACTIVO' ? 'Inactivar' : 'Activar'}</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {cambiando && <ConfirmarCambioEstado usuario={cambiando} onTerminar={terminado} onCancelar={() => setCambiando(null)} />}
    </section>
  )
}
