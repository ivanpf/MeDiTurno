import { Navigate } from 'react-router-dom'
import { useAuth } from './useAuth'
import { rutaInicio } from '../routes'

// Deja ver la pantalla solo si el rol del usuario está en `roles`.
export default function SoloRol({ roles, children }) {
  const { rol } = useAuth()
  if (!rol) return <Navigate to="/login" replace />
  if (!roles.includes(rol)) return <Navigate to={rutaInicio(rol)} replace />
  return children
}
