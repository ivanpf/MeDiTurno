import { Link } from 'react-router-dom'
import PantallaAcceso from '../../components/PantallaAcceso'

// CU-7 Registro de cuenta de paciente.
export default function RegistroPage() {
  return (
    <PantallaAcceso>
      <div className="tarjeta">
        <h1>Crear cuenta</h1>
        <p className="pendiente"><strong>CU-7</strong> — pendiente de implementar. Se desarrolla en <code>src/features/auth/RegistroPage.jsx</code>.</p>
        <Link className="enlace-volver" to="/login">Volver al inicio de sesión</Link>
      </div>
    </PantallaAcceso>
  )
}
