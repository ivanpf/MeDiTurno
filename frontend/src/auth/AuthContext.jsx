import { useState } from 'react'
import { AuthContext } from './useAuth'

// TODO(CU-1): por ahora la "sesión" es solo el rol elegido en la pantalla de login (modo desarrollo),
// con un nombre de ejemplo. Cuando se implemente el inicio de sesión real, el rol, el nombre y el token
// vendrán de la respuesta del backend, y solo hay que cambiar este archivo: el resto de la app usa useAuth().
const CLAVE_ROL = 'mediturno.rol'
const CLAVE_NOMBRE = 'mediturno.nombre'

const NOMBRE_DEV = {
  paciente: 'Ana López',
  funcionario: 'Juan Pérez',
  medico: 'Laura Gómez',
  admin: 'Carlos Méndez',
}

export function AuthProvider({ children }) {
  const [rol, setRol] = useState(() => localStorage.getItem(CLAVE_ROL))
  const [nombre, setNombre] = useState(() => localStorage.getItem(CLAVE_NOMBRE))

  const iniciarSesion = (nuevoRol, nuevoNombre = NOMBRE_DEV[nuevoRol]) => {
    localStorage.setItem(CLAVE_ROL, nuevoRol)
    localStorage.setItem(CLAVE_NOMBRE, nuevoNombre)
    setRol(nuevoRol)
    setNombre(nuevoNombre)
  }

  const cerrarSesion = () => {
    localStorage.removeItem(CLAVE_ROL)
    localStorage.removeItem(CLAVE_NOMBRE)
    setRol(null)
    setNombre(null)
  }

  return <AuthContext.Provider value={{ rol, nombre, iniciarSesion, cerrarSesion }}>{children}</AuthContext.Provider>
}
