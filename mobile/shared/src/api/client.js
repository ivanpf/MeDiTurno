/**
 * Cliente HTTP único para la app móvil.
 * Equivalente a frontend/services/api.js pero adaptado a React Native (AsyncStorage + fetch nativo).
 */

import AsyncStorage from '@react-native-async-storage/async-storage';
import { Platform } from 'react-native';
import { getAuth } from 'firebase/auth';

const HOST = Platform.OS === 'android' ? '10.0.2.2' : 'localhost';
const API_BASE = __DEV__ ? `http://${HOST}:8080/api` : 'https://api.mediturno.com/api';

/**
 * @class ApiError
 * @extends Error
 */
export class ApiError extends Error {
  /**
   * @param {number} estado - HTTP status code
   * @param {string} mensaje - Mensaje legible para el usuario
   * @param {string[]} [detalles=[]] - Detalles técnicos (validation errors)
   */
  constructor(estado, mensaje, detalles = []) {
    super(mensaje);
    this.name = 'ApiError';
    this.estado = estado;
    this.mensaje = mensaje;
    this.detalles = detalles;
  }
}

/**
 * Obtiene el token de Firebase del usuario actual.
 * @returns {Promise<string|null>}
 */
async function obtenerToken() {
  const auth = getAuth();
  const user = auth.currentUser;
  if (!user) return null;
  return await user.getIdToken();
}

/**
 * Función interna para hacer requests al backend.
 * @param {string} metodo - HTTP method
 * @param {string} ruta - API path (ej: '/reservas')
 * @param {Object|null} [cuerpo] - Request body
 * @returns {Promise<any>}
 */
async function request(metodo, ruta, cuerpo = null) {
  const token = await obtenerToken();
  const headers = {
    ...(cuerpo && { 'Content-Type': 'application/json' }),
    ...(token && { Authorization: `Bearer ${token}` }),
  };

  const response = await fetch(`${API_BASE}${ruta}`, {
    method: metodo,
    headers,
    body: cuerpo ? JSON.stringify(cuerpo) : undefined,
  });

  const data = response.status === 204 ? null : await response.json().catch(() => null);

  if (!response.ok) {
    throw new ApiError(
      response.status,
      data?.mensaje ?? 'Ocurrió un error inesperado',
      data?.detalles ?? []
    );
  }
  return data;
}

/**
 * API client con métodos HTTP.
 * @type {Object}
 */
export const api = {
  get: (ruta) => request('GET', ruta),
  post: (ruta, cuerpo) => request('POST', ruta, cuerpo),
  put: (ruta, cuerpo) => request('PUT', ruta, cuerpo),
  patch: (ruta, cuerpo) => request('PATCH', ruta, cuerpo),
  delete: (ruta) => request('DELETE', ruta),
};

/**
 * Verifica si el error es un ApiError.
 * @param {unknown} error
 * @returns {error is ApiError}
 */
export function isApiError(error) {
  return error instanceof ApiError;
}
