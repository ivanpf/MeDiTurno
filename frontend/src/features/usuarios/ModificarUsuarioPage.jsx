import { useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'
import { api, ApiError } from '../../services/api'
import { comoError, soloMensaje } from '../../utils/errores'
import { NOMBRE_ROL_INTERNO } from '../../utils/formatos'
import estilos from './ModificarUsuarioPage.module.css'

const aFormulario = (u) => ({
  nombre: u.nombre,
  apellido: u.apellido,
  correo: u.correo,
  rol: u.rol,
  matricula: u.matricula ?? '',
  especialidadIds: u.especialidades.map((e) => e.codigo),
})

// CU-45 Modificación de usuarios internos. La cédula no se puede modificar. Las reglas (turnos vigentes, último administrador,
// actividad registrada con el rol actual) las aplica el backend y se informan acá sin cambiar nada.
export default function ModificarUsuarioPage() {
  // Desde el listado (CU-36) se llega con el usuario ya elegido.
  const { state } = useLocation()
  const [texto, setTexto] = useState('')
  const [resultados, setResultados] = useState(null)
  const [buscando, setBuscando] = useState(false)
  const [usuario, setUsuario] = useState(null) // los datos guardados del usuario elegido
  const [borrador, setBorrador] = useState(null) // lo escrito en el formulario; vacío = los datos guardados
  const [especialidades, setEspecialidades] = useState([])
  const [error, setError] = useState(null)
  const [modificado, setModificado] = useState(false)
  const [enviando, setEnviando] = useState(false)

  useEffect(() => {
    api.get('/especialidades').then(setEspecialidades).catch(() => setEspecialidades([]))
  }, [])

  const cargarUsuario = async (idUsuario) => {
    setError(null)
    setModificado(false)
    setBorrador(null)
    try {
      setUsuario(await api.get(`/usuarios-internos/${idUsuario}`))
    } catch (err) {
      setUsuario(null)
      setError(comoError(err, 'No fue posible consultar al usuario. Intente nuevamente.'))
    }
  }

  useEffect(() => {
    if (!state?.idUsuario) return
    api.get(`/usuarios-internos/${state.idUsuario}`)
      .then(setUsuario)
      .catch((err) => setError(comoError(err, 'No fue posible consultar al usuario. Intente nuevamente.')))
  }, [state])

  const buscar = async (e) => {
    e.preventDefault()
    setBuscando(true)
    setError(null)
    try {
      setResultados(await api.get(`/usuarios-internos?${new URLSearchParams({ texto: texto.trim() })}`))
    } catch (err) {
      setResultados(null)
      setError(comoError(err, 'No fue posible buscar usuarios. Intente nuevamente.'))
    } finally {
      setBuscando(false)
    }
  }

  const elegir = (e) => {
    if (e.target.value) {
      cargarUsuario(e.target.value)
    } else {
      setUsuario(null)
      setBorrador(null)
    }
  }

  // Los datos del formulario son los guardados del usuario elegido, salvo lo que la persona haya escrito.
  const datos = borrador ?? (usuario ? aFormulario(usuario) : null)

  const cambiar = (e) => {
    setBorrador({ ...datos, [e.target.name]: e.target.value })
    setModificado(false)
  }

  const cambiarEspecialidad = (e) => {
    const { value, checked } = e.target
    setBorrador({
      ...datos,
      especialidadIds: checked ? [...datos.especialidadIds, value] : datos.especialidadIds.filter((codigo) => codigo !== value),
    })
    setModificado(false)
  }

  const restablecer = () => {
    setBorrador(null)
    setError(null)
    setModificado(false)
  }

  const esMedico = datos?.rol === 'MEDICO'

  const guardar = async (e) => {
    e.preventDefault()
    if (enviando) return
    setError(null)
    setModificado(false)
    if (esMedico && !datos.matricula.trim()) {
      setError(new ApiError(400, 'La matrícula es obligatoria para los médicos.'))
      return
    }
    if (esMedico && datos.especialidadIds.length === 0) {
      setError(new ApiError(400, 'Seleccione al menos una especialidad médica.'))
      return
    }
    setEnviando(true)
    try {
      const actualizado = await api.put(`/usuarios-internos/${usuario.idUsuario}`, {
        nombre: datos.nombre.trim(),
        apellido: datos.apellido.trim(),
        correo: datos.correo.trim(),
        rol: datos.rol,
        ...(esMedico && { matricula: datos.matricula.trim(), especialidadIds: datos.especialidadIds }),
      })
      setUsuario(actualizado)
      setBorrador(null)
      setModificado(true)
    } catch (err) {
      // Con un error del backend el formulario conserva lo que se escribió.
      setError(comoError(err, 'No fue posible modificar al usuario. Intente nuevamente.'))
    } finally {
      setEnviando(false)
    }
  }

  const cambiaCorreo = usuario && datos && datos.correo.trim().toLowerCase() !== usuario.correo.toLowerCase()
  const cambiaRol = usuario && datos && datos.rol !== usuario.rol
  // Se ofrecen las especialidades activas y, si el médico ya tenía alguna que después se inactivó, esa también (para no perderla sin querer).
  const asignadas = new Set(usuario?.especialidades.map((e) => e.codigo))
  const opciones = especialidades.filter((e) => e.estado === 'ACTIVO' || asignadas.has(e.codigo))

  return (
    <section className="panel">
      <h1>Modificación de usuario interno</h1>

      <div className={estilos.mensajes}>
        {modificado && <p className="mensaje-exito" role="status">Usuario modificado correctamente.</p>}
        {error && (
          <div className="mensaje-error" role="alert">
            {error.mensaje}
            {error.detalles.length > 0 && (
              <ul>
                {error.detalles.map((detalle) => <li key={detalle}>{soloMensaje(detalle)}</li>)}
              </ul>
            )}
          </div>
        )}
      </div>

      <form className={estilos.formulario} onSubmit={buscar}>
        <label>
          Buscar usuario por cédula, nombre, apellido o correo
          <input type="search" value={texto} onChange={(e) => setTexto(e.target.value)} maxLength={100} />
        </label>
        <div className={estilos.acciones}>
          <button type="submit" disabled={buscando || !texto.trim()}>Buscar</button>
        </div>
      </form>

      {resultados && resultados.length === 0 && (
        <p className="pendiente" role="status">No se encontraron usuarios con ese criterio.</p>
      )}

      {resultados && resultados.length > 0 && (
        <div className={estilos.formulario}>
          <label>
            Usuario
            <select value={usuario?.idUsuario ?? ''} onChange={elegir}>
              <option value="">Seleccione un usuario</option>
              {resultados.map((u) => (
                <option key={u.idUsuario} value={u.idUsuario}>
                  {u.apellido}, {u.nombre} ({u.cedula}) · {NOMBRE_ROL_INTERNO[u.rol]}
                </option>
              ))}
            </select>
          </label>
        </div>
      )}

      {datos && (
        <form className={estilos.formulario} onSubmit={guardar}>
          <fieldset className={estilos.campos} disabled={enviando}>
            <label>
              Cédula de identidad
              <input value={usuario.cedula} disabled />
            </label>
            <div className={estilos.fila}>
              <label>
                Nombre *
                <input name="nombre" value={datos.nombre} onChange={cambiar} maxLength={100} required />
              </label>
              <label>
                Apellido *
                <input name="apellido" value={datos.apellido} onChange={cambiar} maxLength={100} required />
              </label>
            </div>
            <label>
              Correo electrónico *
              <input type="email" name="correo" value={datos.correo} onChange={cambiar} maxLength={255} required />
            </label>
            {cambiaCorreo && (
              <p className="pendiente">
                El cambio de correo también se aplica a su cuenta de acceso: deberá usar el nuevo correo para iniciar sesión.
              </p>
            )}
            <label>
              Rol *
              <select name="rol" value={datos.rol} onChange={cambiar} required>
                {Object.entries(NOMBRE_ROL_INTERNO).map(([valor, etiqueta]) => <option key={valor} value={valor}>{etiqueta}</option>)}
              </select>
            </label>
            {cambiaRol && (
              <p className="pendiente">
                Al cambiar el rol la persona conserva su cuenta y su contraseña, pero accede con las opciones del nuevo rol. No se
                puede si tiene actividad registrada con el rol actual (turnos, órdenes, consultas u otros registros): en ese caso
                inactive la cuenta.
              </p>
            )}
            {esMedico && (
              <fieldset className={estilos.especialidades}>
                <legend>Datos del médico</legend>
                <label>
                  Matrícula médica *
                  <input name="matricula" value={datos.matricula} onChange={cambiar} maxLength={30} />
                </label>
                <p className={estilos.ayuda}>Seleccione al menos una especialidad.</p>
                {opciones.map((e) => (
                  <label key={e.codigo} className={estilos.opcion}>
                    <input type="checkbox" value={e.codigo} checked={datos.especialidadIds.includes(e.codigo)} onChange={cambiarEspecialidad} />
                    {e.estado === 'ACTIVO' ? e.nombre : `${e.nombre} (inactiva)`}
                  </label>
                ))}
              </fieldset>
            )}
            <p className={estilos.ayuda}>
              La cédula no se puede modificar. El correo y la matrícula no pueden pertenecer a otra persona. No se puede quitar una
              especialidad en la que el médico tiene turnos vigentes.
            </p>
            <div className={estilos.acciones}>
              <button type="submit" className="principal" disabled={enviando}>Guardar cambios</button>
              <button type="button" onClick={restablecer} disabled={enviando}>Deshacer cambios</button>
            </div>
          </fieldset>
        </form>
      )}
    </section>
  )
}
