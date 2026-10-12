import { useEffect, useState } from 'react'
import { signOut } from 'firebase/auth'
import { auth } from '../services/firebase'
import { api } from '../services/api'
import { AuthContext } from './useAuth'

// La identidad la maneja Firebase (correo y Google); el backend verifica el token y responde el rol y el nombre.
// Acá solo se guardan esos dos datos para dibujar el menú; si Firebase ya no tiene sesión, también se descartan.
const CLAVE_ROL = 'mediturno.rol'
const CLAVE_NOMBRE = 'mediturno.nombre'

export function AuthProvider({ children }) {
  const [rol, setRol] = useState(() => localStorage.getItem(CLAVE_ROL))
  const [nombre, setNombre] = useState(() => localStorage.getItem(CLAVE_NOMBRE))

  const limpiar = () => {
    localStorage.removeItem(CLAVE_ROL)
    localStorage.removeItem(CLAVE_NOMBRE)
    setRol(null)
    setNombre(null)
  }

  // Si la sesión de Firebase venció o se cerró en otra pestaña, no se conserva el rol guardado.
  useEffect(() => {
    auth.authStateReady().then(() => {
      if (!auth.currentUser) limpiar()
    })
  }, [])

  const iniciarSesion = (nuevoRol, nuevoNombre) => {
    localStorage.setItem(CLAVE_ROL, nuevoRol)
    localStorage.setItem(CLAVE_NOMBRE, nuevoNombre)
    setRol(nuevoRol)
    setNombre(nuevoNombre)
  }

  // CU-2: el backend revoca las sesiones en Firebase; aunque eso falle, la sesión local se cierra igual.
  const cerrarSesion = async () => {
    try {
      await api.post('/auth/logout')
    } catch {
      // se ignora: lo importante es cerrar la sesión en este navegador
    }
    await signOut(auth)
    limpiar()
  }

  return <AuthContext.Provider value={{ rol, nombre, iniciarSesion, cerrarSesion }}>{children}</AuthContext.Provider>
}
