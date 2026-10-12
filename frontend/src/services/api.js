// Cliente HTTP único del front. Todas las llamadas al backend pasan por acá:
//   const reservas = await api.get('/reservas')
//   await api.post('/reservas', { turnoId: 3, numero: 5 })
// El backend responde los errores con { estado, mensaje, detalles, fecha }; acá se convierten en un ApiError
// (error.mensaje, error.estado y error.detalles).

import { auth } from './firebase'

const BASE = '/api'

export class ApiError extends Error {
  constructor(estado, mensaje, detalles = []) {
    super(mensaje)
    this.mensaje = mensaje
    this.estado = estado
    this.detalles = detalles
  }
}

// Token de Firebase de la persona con sesión (el SDK lo renueva solo). Sin sesión, no se manda nada.
async function obtenerToken() {
  await auth.authStateReady()
  return auth.currentUser?.getIdToken() ?? null
}

// Esta función existe para no repetir el mismo código en cada petición al backend. Hace tres cosas:
//  - Agrega el token de Firebase a la cabecera Authorization, para que el backend verifique quién llama.
//    Se pide en cada llamada porque vence cada hora y getIdToken() lo renueva solo.
//  - Manda el cuerpo como JSON y lee la respuesta.
//  - Si el backend responde un error, lanza un ApiError.
async function request(metodo, ruta, cuerpo) {
  const token = await obtenerToken()
  const respuesta = await fetch(BASE + ruta, {
    method: metodo,
    headers: {
      ...(cuerpo !== undefined && { 'Content-Type': 'application/json' }),
      ...(token && { Authorization: `Bearer ${token}` }),
    },
    body: cuerpo !== undefined ? JSON.stringify(cuerpo) : undefined,
  })

  const datos = respuesta.status === 204 ? null : await respuesta.json().catch(() => null)
  if (!respuesta.ok) {
    throw new ApiError(respuesta.status, datos?.mensaje ?? 'Ocurrió un error', datos?.detalles ?? [])
  }
  return datos
}

export const api = {
  get: (ruta) => request('GET', ruta),
  post: (ruta, cuerpo) => request('POST', ruta, cuerpo ?? {}),
  put: (ruta, cuerpo) => request('PUT', ruta, cuerpo ?? {}),
  patch: (ruta, cuerpo) => request('PATCH', ruta, cuerpo ?? {}),
  delete: (ruta) => request('DELETE', ruta),
}
