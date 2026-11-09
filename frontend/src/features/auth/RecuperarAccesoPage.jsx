import { useState } from 'react'
import { Link } from 'react-router-dom'
import { sendPasswordResetEmail } from 'firebase/auth'
import PantallaAcceso from '../../components/PantallaAcceso'
import { auth } from '../../services/firebase'

const MENSAJE_CONFIRMACION = 'Si existe una cuenta asociada a esa dirección, recibirá un correo con instrucciones para recuperar el acceso.'

const mensajeDeError = (codigo) => {
  switch (codigo) {
    case 'auth/invalid-email':
      return 'Ingrese una dirección de correo electrónico válida.'
    case 'auth/too-many-requests':
      return 'Se realizaron demasiadas solicitudes. Espere un momento e inténtelo nuevamente.'
    case 'auth/network-request-failed':
      return 'No se pudo conectar con Firebase. Verifique su conexión e inténtelo nuevamente.'
    default:
      return 'No fue posible enviar las instrucciones. Intente nuevamente.'
  }
}

// CU-4: Firebase envía el enlace y procesa el cambio de contraseña en su página segura.
export default function RecuperarAccesoPage() {
  const [correo, setCorreo] = useState('')
  const [enviada, setEnviada] = useState(false)
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(false)

  const solicitar = async (evento) => {
    evento.preventDefault()
    setError('')
    setCargando(true)

    try {
      await sendPasswordResetEmail(auth, correo.trim())
      setEnviada(true)
    } catch (err) {
      if (err?.code === 'auth/user-not-found') {
        setEnviada(true)
      } else {
        setError(mensajeDeError(err?.code))
      }
    } finally {
      setCargando(false)
    }
  }

  const solicitarOtro = () => {
    setCorreo('')
    setError('')
    setEnviada(false)
  }

  return (
    <PantallaAcceso>
      <div className="tarjeta">
        <h1>Recuperar acceso</h1>
        {enviada ? (
          <>
            <p className="mensaje-exito" role="status">{MENSAJE_CONFIRMACION}</p>
            <p>Revise su correo y siga el enlace para elegir una contraseña nueva.</p>
            <Link className="enlace-volver" to="/login">Volver al inicio de sesión</Link>
            <p><button type="button" onClick={solicitarOtro}>Solicitar otro correo</button></p>
          </>
        ) : (
          <form onSubmit={solicitar}>
            <p>Ingrese su correo electrónico para recibir instrucciones y definir una contraseña nueva.</p>
            {error && <p className="mensaje-error" role="alert">{error}</p>}
            <label>
              Correo electrónico
              <input
                type="email"
                placeholder="correo@ejemplo.com"
                autoComplete="email"
                value={correo}
                onChange={(evento) => setCorreo(evento.target.value)}
                required
              />
            </label>
            <button type="submit" className="principal" disabled={cargando}>
              {cargando ? 'Enviando…' : 'Enviar enlace'}
            </button>
            <Link className="enlace-volver" to="/login">Volver al inicio de sesión</Link>
          </form>
        )}
      </div>
    </PantallaAcceso>
  )
}
