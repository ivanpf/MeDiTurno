import { Link } from 'react-router-dom'
import PantallaAcceso from '../../components/PantallaAcceso'

// CU-4 Recuperación de acceso.
export default function RecuperarAccesoPage() {
  return (
    <PantallaAcceso>
      <div className="tarjeta">
        <h1>Recuperar acceso</h1>
        <p className="pendiente"><strong>CU-4</strong> — pendiente de implementar. Se desarrolla en <code>src/features/auth/RecuperarAccesoPage.jsx</code>.</p>
        <Link className="enlace-volver" to="/login">Volver al inicio de sesión</Link>
      </div>
    </PantallaAcceso>
  )
}
