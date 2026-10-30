import { useState } from 'react'
import { api, ApiError } from '../../services/api'
import estilos from './CambiarContrasenaPage.module.css'

const MINIMO_CONTRASENA = 6
const VACIO = { actual: '', nueva: '', repetirNueva: '' }

// El backend informa los datos inválidos como «campo: mensaje»; a la persona solo le interesa el mensaje.
const soloMensaje = (detalle) => detalle.split(': ').slice(1).join(': ') || detalle

// CU-3 Cambio de contraseña (con sesión iniciada).
// Las contraseñas las guarda Firebase: el backend valida la actual y le pide el cambio al
// ProveedorIdentidad. Al terminar, la sesión sigue abierta: no hace falta volver a ingresar.
// TODO (CU-3): el backend responde 501 hasta que se implemente AuthService.cambiarContrasena;
// la pantalla queda lista para cuando se active (mismo contrato, sin cambios acá).
export default function CambiarContrasenaPage() {
  const [datos, setDatos] = useState(VACIO)
  const [error, setError] = useState(null)
  const [modificada, setModificada] = useState(false)
  const [enviando, setEnviando] = useState(false)

  const cambiar = (e) => {
    setDatos({ ...datos, [e.target.name]: e.target.value })
    setModificada(false)
  }

  const limpiar = () => {
    setDatos(VACIO)
    setError(null)
    setModificada(false)
  }

  const guardar = async (e) => {
    e.preventDefault()
    setError(null)
    setModificada(false)
    setEnviando(true)
    try {
      if (!datos.actual) throw new ApiError(0, 'Ingrese la contraseña actual.')
      if (datos.nueva.length < MINIMO_CONTRASENA)
        throw new ApiError(0, `La nueva contraseña debe tener al menos ${MINIMO_CONTRASENA} caracteres.`)
      if (datos.nueva !== datos.repetirNueva) throw new ApiError(0, 'Las contraseñas no coinciden.')

      await api.post('/auth/contrasena', { actual: datos.actual, nueva: datos.nueva })
      setDatos(VACIO)
      setModificada(true)
    } catch (err) {
      // Con un error del backend el formulario conserva los datos ingresados.
      setError(err instanceof ApiError ? err : new ApiError(0, 'No fue posible cambiar la contraseña. Intente nuevamente.'))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <section className="panel">
      <h1>Cambiar contraseña</h1>

      <div className={estilos.mensajes}>
        {modificada && <p className="mensaje-exito" role="status">La contraseña fue modificada correctamente.</p>}
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

      <form className={estilos.formulario} onSubmit={guardar}>
        <label>
          Contraseña actual
          <input name="actual" type="password" autoComplete="current-password" value={datos.actual} onChange={cambiar} required />
        </label>
        <label>
          Nueva contraseña
          <input name="nueva" type="password" autoComplete="new-password" minLength={MINIMO_CONTRASENA} maxLength={128} value={datos.nueva} onChange={cambiar} required />
        </label>
        <label>
          Repetir nueva contraseña
          <input name="repetirNueva" type="password" autoComplete="new-password" maxLength={128} value={datos.repetirNueva} onChange={cambiar} required />
        </label>
        <div className={estilos.acciones}>
          <button type="submit" className="principal" disabled={enviando}>Guardar</button>
          <button type="button" onClick={limpiar} disabled={enviando}>Cancelar</button>
        </div>
      </form>
    </section>
  )
}
