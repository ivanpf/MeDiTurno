import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { GoogleAuthProvider, signInWithEmailAndPassword, signInWithPopup, signOut } from 'firebase/auth'
import { useAuth } from '../../auth/useAuth'
import PantallaAcceso from '../../components/PantallaAcceso'
import { rutaInicio } from '../../routes'
import { api } from '../../services/api'
import { auth } from '../../services/firebase'

// Mensaje único para cualquier fallo: no revela si la cuenta existe ni su estado (CU-1).
const MENSAJE_ERROR = 'No fue posible iniciar sesión. Verifique sus datos e intente nuevamente.'

// CU-1 Inicio de sesión. Firebase valida las credenciales (correo o Google) y el backend responde el rol.
export default function LoginPage() {
  const { rol, iniciarSesion } = useAuth()
  const navigate = useNavigate()
  const [correo, setCorreo] = useState('')
  const [contrasena, setContrasena] = useState('')
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(false)

  if (rol) return <Navigate to={rutaInicio(rol)} replace />

  const ingresarCon = async (autenticar) => {
    setError('')
    setCargando(true)
    try {
      await autenticar()
      const sesion = await api.post('/auth/login')
      iniciarSesion(sesion.rol, sesion.nombre)
      navigate(rutaInicio(sesion.rol))
    } catch {
      await signOut(auth) // si el backend rechazó a la persona, no queda sesión abierta en Firebase
      setError(MENSAJE_ERROR)
    } finally {
      setCargando(false)
    }
  }

  const ingresar = (e) => {
    e.preventDefault()
    ingresarCon(() => signInWithEmailAndPassword(auth, correo, contrasena))
  }

  const ingresarConGoogle = () => ingresarCon(() => signInWithPopup(auth, new GoogleAuthProvider()))

  return (
    <PantallaAcceso>
      <form className="tarjeta" onSubmit={ingresar}>
        <h1>Iniciar sesión</h1>
        {error && <p className="mensaje-error" role="alert">{error}</p>}
        <label>
          Correo electrónico
          <input type="email" placeholder="correo@ejemplo.com" value={correo} onChange={(e) => setCorreo(e.target.value)} required />
        </label>
        <label>
          Contraseña
          <input type="password" value={contrasena} onChange={(e) => setContrasena(e.target.value)} required />
        </label>
        <button type="submit" className="principal" disabled={cargando}>Ingresar</button>
        <button type="button" onClick={ingresarConGoogle} disabled={cargando}>Iniciar sesión con Google</button>
        <p><Link to="/recuperar-acceso">¿Olvidó su contraseña? Recuperar acceso</Link></p>
        <p>¿Es paciente y no tiene cuenta? <Link to="/registro">Crear cuenta</Link></p>
      </form>
    </PantallaAcceso>
  )
}
