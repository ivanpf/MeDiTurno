import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import PantallaAcceso from '../../components/PantallaAcceso'
import { api } from '../../services/api'

// El backend responde estos códigos cuando el enlace ya no sirve (mismo criterio que la app mobile).
const CODIGOS_ENLACE_VENCIDO = [404, 410]
const MINIMO_CONTRASENA = 6

// CU-4 Recuperación de acceso. Mismo flujo que la mobile:
//   1. Se pide el correo y el backend envía un enlace con token.
//   2. El enlace aterriza acá con ?token=..., se valida y se permite definir una contraseña nueva.
// Los endpoints son públicos; mientras el backend no los implemente responde 501 y la pantalla
// muestra el mensaje de error previsto, sin cambios cuando se active el servicio.
export default function RecuperarAccesoPage() {
  const [parametros, definirParametros] = useSearchParams()
  const token = parametros.get('token')

  const [estado, setEstado] = useState(token ? 'validando' : 'solicitar') // solicitar | enviada | validando | definir | exito | vencido
  const [correo, setCorreo] = useState('')
  const [nuevaContrasena, setNuevaContrasena] = useState('')
  const [repetirContrasena, setRepetirContrasena] = useState('')
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(Boolean(token)) // con token, empieza validándolo

  // Con un token en la URL se valida al entrar: 204 habilita el paso de nueva contraseña,
  // 404/410 muestra «enlace vencido», otro error permite volver a solicitar.
  useEffect(() => {
    if (!token) return
    let vigente = true
    api.get(`/auth/recuperacion/${encodeURIComponent(token)}`)
      .then(() => vigente && setEstado('definir'))
      .catch((e) => {
        if (!vigente) return
        if (CODIGOS_ENLACE_VENCIDO.includes(e.estado)) setEstado('vencido')
        else { setEstado('solicitar'); setError(e.mensaje) }
      })
      .finally(() => { if (vigente) setCargando(false) })
    return () => { vigente = false }
  }, [token])

  const volverASolicitar = () => {
    definirParametros({}, { replace: true })
    setCorreo('')
    setNuevaContrasena('')
    setRepetirContrasena('')
    setError('')
    setEstado('solicitar')
  }

  const solicitar = async (e) => {
    e.preventDefault()
    const correoLimpio = correo.trim()
    if (!correoLimpio) return setError('Ingrese su correo electrónico.')
    setError('')
    setCargando(true)
    try {
      await api.post('/auth/recuperacion', { correo: correoLimpio })
      setEstado('enviada')
    } catch (e2) {
      setError(e2.mensaje)
    } finally {
      setCargando(false)
    }
  }

  const guardar = async (e) => {
    e.preventDefault()
    if (nuevaContrasena.length < MINIMO_CONTRASENA)
      return setError(`La contraseña debe tener al menos ${MINIMO_CONTRASENA} caracteres.`)
    if (nuevaContrasena !== repetirContrasena) return setError('Las contraseñas no coinciden.')
    setError('')
    setCargando(true)
    try {
      await api.post(`/auth/recuperacion/${encodeURIComponent(token)}/contrasena`, { nuevaContrasena })
      setEstado('exito')
    } catch (e2) {
      if (CODIGOS_ENLACE_VENCIDO.includes(e2.estado)) setEstado('vencido')
      else setError(e2.mensaje)
    } finally {
      setCargando(false)
    }
  }

  // Paso final: contraseña actualizada. Desde acá se vuelve al inicio de sesión.
  if (estado === 'exito') {
    return (
      <PantallaAcceso>
        <div className="tarjeta">
          <h1>Recuperar acceso</h1>
          <p className="mensaje-exito" role="status">Su contraseña fue modificada correctamente. Ya puede iniciar sesión con la nueva.</p>
          <Link className="enlace-volver" to="/login">Ir a iniciar sesión</Link>
        </div>
      </PantallaAcceso>
    )
  }

  // El enlace venció o no es válido: se puede pedir otro.
  if (estado === 'vencido') {
    return (
      <PantallaAcceso>
        <div className="tarjeta">
          <h1>Enlace vencido</h1>
          <p className="mensaje-error" role="alert">El enlace de recuperación venció o no es válido. Solicite uno nuevo.</p>
          <button type="button" className="principal" onClick={volverASolicitar}>Solicitar uno nuevo</button>
          <Link className="enlace-volver" to="/login">Volver al inicio de sesión</Link>
        </div>
      </PantallaAcceso>
    )
  }

  // Con el token recién validado: formulario de nueva contraseña.
  if (estado === 'definir') {
    return (
      <PantallaAcceso>
        <form className="tarjeta" onSubmit={guardar}>
          <h1>Nueva contraseña</h1>
          <p>Ingrese su nueva contraseña (mínimo {MINIMO_CONTRASENA} caracteres).</p>
          {error && <p className="mensaje-error" role="alert">{error}</p>}
          <label>
            Nueva contraseña
            <input type="password" minLength={MINIMO_CONTRASENA} maxLength={128} autoComplete="new-password" value={nuevaContrasena} onChange={(e) => setNuevaContrasena(e.target.value)} required />
          </label>
          <label>
            Confirmar contraseña
            <input type="password" maxLength={128} autoComplete="new-password" value={repetirContrasena} onChange={(e) => setRepetirContrasena(e.target.value)} required />
          </label>
          <button type="submit" className="principal" disabled={cargando}>{cargando ? 'Guardando…' : 'Guardar contraseña'}</button>
        </form>
      </PantallaAcceso>
    )
  }

  // Después de solicitar: el enlace va por correo.
  if (estado === 'enviada') {
    return (
      <PantallaAcceso>
        <div className="tarjeta">
          <h1>Recuperar acceso</h1>
          <p className="mensaje-exito" role="status">Si existe una cuenta asociada a esa dirección, recibirá un correo con instrucciones para recuperar el acceso. El enlace vale 10 minutos.</p>
          <Link className="enlace-volver" to="/login">Volver al inicio de sesión</Link>
          <p><button type="button" onClick={volverASolicitar}>Solicitar otro enlace</button></p>
        </div>
      </PantallaAcceso>
    )
  }

  // Estado inicial (y «validando» mientras se comprueba un token de la URL): pedir el correo.
  return (
    <PantallaAcceso>
      <form className="tarjeta" onSubmit={solicitar}>
        <h1>Recuperar acceso</h1>
        <p>Ingrese su correo electrónico para recibir instrucciones y definir una contraseña nueva.</p>
        {error && <p className="mensaje-error" role="alert">{error}</p>}
        <label>
          Correo electrónico
          <input type="email" placeholder="correo@ejemplo.com" autoComplete="email" value={correo} onChange={(e) => setCorreo(e.target.value)} required />
        </label>
        <button type="submit" className="principal" disabled={cargando}>{cargando ? 'Enviando…' : 'Enviar enlace'}</button>
        <Link className="enlace-volver" to="/login">Volver al inicio de sesión</Link>
      </form>
    </PantallaAcceso>
  )
}
