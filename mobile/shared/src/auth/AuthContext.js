/**
 * AuthContext - Maneja el estado de autenticación global.
 * Equivalente a frontend/src/auth/AuthContext.jsx pero con AsyncStorage.
 *
 * Esta app es SOLO para el rol PACIENTE. El backend responde el rol al iniciar
 * sesión; si no es paciente, se rechaza el ingreso (los demás perfiles usan la web).
 */

import React, { createContext, useContext, useEffect, useState } from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { onAuthStateChanged, signOut } from 'firebase/auth';
import { auth } from './firebase';
import { login as apiLogin, logout } from '../api/endpoints';
import { ROLES } from '../navigation/rutas';

const AuthContext = createContext(null);

const ROL_KEY = 'mediturno.rol';
const NOMBRE_KEY = 'mediturno.nombre';
const ID_USUARIO_KEY = 'mediturno.idUsuario';

// Código de error propio para que la pantalla de login muestre el mensaje correcto.
export const ROL_NO_PERMITIDO = 'ROL_NO_PERMITIDO';

/**
 * Provider de autenticación.
 * @param {{children: React.ReactNode}} props
 */
export function AuthProvider({ children }) {
  const [rol, setRol] = useState(null);
  const [nombre, setNombre] = useState(null);
  const [idUsuario, setIdUsuario] = useState(null);
  const [cargando, setCargando] = useState(true);

  // Carga sesión guardada al iniciar (solo si es de paciente).
  useEffect(() => {
    const cargarSesion = async () => {
      try {
        const [r, n, id] = await Promise.all([
          AsyncStorage.getItem(ROL_KEY),
          AsyncStorage.getItem(NOMBRE_KEY),
          AsyncStorage.getItem(ID_USUARIO_KEY),
        ]);
        if (r === ROLES.PACIENTE && n && id) {
          setRol(r);
          setNombre(n);
          setIdUsuario(Number(id));
        } else if (r || n || id) {
          // Sesión guardada de otro rol (o incompleta): no se conserva.
          await AsyncStorage.multiRemove([ROL_KEY, NOMBRE_KEY, ID_USUARIO_KEY]);
        }
      } catch (e) {
        // Se ignora: no hay sesión que restaurar.
      } finally {
        setCargando(false);
      }
    };
    cargarSesion();
  }, []);

  // Si la sesión de Firebase venció o se cerró, no se conserva el rol guardado.
  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (usuario) => {
      if (!usuario) limpiarSesionLocal();
    });
    return unsubscribe;
  }, []);

  const guardarSesion = async (nuevoRol, nuevoNombre, nuevoId) => {
    await AsyncStorage.multiSet([
      [ROL_KEY, nuevoRol],
      [NOMBRE_KEY, nuevoNombre],
      [ID_USUARIO_KEY, String(nuevoId)],
    ]);
    setRol(nuevoRol);
    setNombre(nuevoNombre);
    setIdUsuario(nuevoId);
  };

  const limpiarSesionLocal = async () => {
    await AsyncStorage.multiRemove([ROL_KEY, NOMBRE_KEY, ID_USUARIO_KEY]);
    setRol(null);
    setNombre(null);
    setIdUsuario(null);
  };

  /**
   * Inicia sesión: Firebase valida las credenciales y el backend responde el rol.
   * Solo se acepta el rol PACIENTE.
   * @param {{autenticar: () => Promise<any>}} props
   * @returns {Promise<{rol: string, nombre: string, idUsuario: number}>}
   */
  const iniciarSesion = async ({ autenticar }) => {
    await autenticar();
    const sesion = await apiLogin();
    if (sesion.rol !== ROLES.PACIENTE) {
      await signOut(auth);
      const error = new Error(
        'Esta aplicación es solo para pacientes. Los demás perfiles ingresan desde la versión web.'
      );
      error.codigo = ROL_NO_PERMITIDO;
      throw error;
    }
    await guardarSesion(sesion.rol, sesion.nombre, sesion.idUsuario);
    return sesion;
  };

  // CU-2: el backend revoca la sesión en Firebase; la sesión local se cierra igual.
  const cerrarSesion = async () => {
    try {
      await logout();
    } catch {
      // Se ignora: lo importante es cerrar la sesión en el dispositivo.
    }
    await signOut(auth);
    await limpiarSesionLocal();
  };

  const value = {
    rol,
    nombre,
    idUsuario,
    cargando,
    autenticado: !!rol,
    iniciarSesion,
    cerrarSesion,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

/**
 * Hook para acceder al contexto de autenticación.
 * @returns {Object}
 */
export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth debe usarse dentro de AuthProvider');
  }
  return context;
}

export { AuthContext };
