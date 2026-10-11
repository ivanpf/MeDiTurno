import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/useAuth'
import { ETIQUETA_ROL, ROLES } from '../../auth/roles'
import PantallaAcceso from '../../components/PantallaAcceso'
import { rutaInicio } from '../../routes'
import estilos from './LoginPage.module.css'

// CU-1 Inicio de sesión. Por ahora solo la interfaz: ninguna credencial se valida.
export default function LoginPage() {
  const { rol, iniciarSesion } = useAuth()
  const navigate = useNavigate()
  const [rolDev, setRolDev] = useState(ROLES.PACIENTE) // solo desarrollo

  if (rol) return <Navigate to={rutaInicio(rol)} replace />

  const ingresar = (e) => {
    e?.preventDefault()
    // TODO(CU-1): enviar las credenciales al backend y usar el rol de la respuesta.
    iniciarSesion(rolDev)
    navigate(rutaInicio(rolDev))
  }

  return (
    <PantallaAcceso>
      <form className="tarjeta" onSubmit={ingresar}>
        <h1>Iniciar sesión</h1>
        <label>
          Correo electrónico
          <input type="email" placeholder="correo@ejemplo.com" />
        </label>
        <label>
          Contraseña
          <input type="password" />
        </label>
        <button type="submit" className="principal">Ingresar</button>
        <button type="button" onClick={ingresar}>Iniciar sesión con Google</button>
        <p><Link to="/recuperar-acceso">¿Olvidó su contraseña? Recuperar acceso</Link></p>
        <p>¿Es paciente y no tiene cuenta? <Link to="/registro">Crear cuenta</Link></p>

        <fieldset className={estilos.dev}>
          <legend>Solo desarrollo</legend>
          <label>
            Ingresar como
            <select value={rolDev} onChange={(e) => setRolDev(e.target.value)}>
              {Object.values(ROLES).map((r) => (
                <option key={r} value={r}>{ETIQUETA_ROL[r]}</option>
              ))}
            </select>
          </label>
        </fieldset>
      </form>
    </PantallaAcceso>
  )
}
