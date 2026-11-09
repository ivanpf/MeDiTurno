import { useState } from 'react'
import { EmailAuthProvider, reauthenticateWithCredential, updatePassword } from 'firebase/auth'
import { auth } from '../../services/firebase'
import estilos from './CambiarContrasenaPage.module.css'

const MINIMO_CONTRASENA = 6
const VACIO = { actual: '', nueva: '', repetirNueva: '' }

const mensajeDeError = (codigo) => {
  switch (codigo) {
    case 'auth/invalid-credential':
    case 'auth/wrong-password':
      return 'La contraseña actual no es correcta.'
    case 'auth/weak-password':
      return `La nueva contraseña debe tener al menos ${MINIMO_CONTRASENA} caracteres.`
    case 'auth/requires-recent-login':
      return 'Por seguridad, cierre sesión e inicie sesión nuevamente antes de cambiar la contraseña.'
    case 'auth/too-many-requests':
      return 'Se realizaron demasiados intentos. Espere un momento e inténtelo nuevamente.'
    case 'auth/network-request-failed':
      return 'No se pudo conectar con el servicio de autenticación. Verifique su conexión e inténtelo nuevamente.'
    default:
      return 'No fue posible cambiar la contraseña. Intente nuevamente.'
  }
}

// CU-3: Firebase Auth valida la contraseña actual y actualiza la nueva sin enviar contraseñas al backend.
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

    if (datos.nueva.length < MINIMO_CONTRASENA) {
      setError({ mensaje: `La nueva contraseña debe tener al menos ${MINIMO_CONTRASENA} caracteres.`, detalles: [] })
      return
    }
    if (datos.nueva !== datos.repetirNueva) {
      setError({ mensaje: 'Las contraseñas no coinciden.', detalles: [] })
      return
    }

    const usuario = auth.currentUser
    if (!usuario?.email) {
      setError({ mensaje: 'Su sesión venció. Inicie sesión nuevamente para cambiar la contraseña.', detalles: [] })
      return
    }

    setEnviando(true)
    try {
      const credencial = EmailAuthProvider.credential(usuario.email, datos.actual)
      await reauthenticateWithCredential(usuario, credencial)
      await updatePassword(usuario, datos.nueva)
      setDatos(VACIO)
      setModificada(true)
    } catch (err) {
      setError({ mensaje: mensajeDeError(err?.code), detalles: [] })
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
