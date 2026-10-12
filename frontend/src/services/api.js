// Cliente HTTP único del front. Todas las llamadas al backend pasan por acá:
//   const reservas = await api.get('/reservas')
//   await api.post('/reservas', { turnoId: 3, numero: 5 })
// El backend responde los errores con { estado, mensaje, detalles }.

import { auth } from './firebase'

const BASE = '/api'

export class ApiError extends Error {
  constructor(estado, mensaje, detalles = []) {
    super(mensaje)
    this.estado = estado
    this.detalles = detalles
  }
}

// Token de Firebase de la persona con sesión (el SDK lo renueva solo). Sin sesión, no se manda nada.
async function obtenerToken() {
  await auth.authStateReady()
  return auth.currentUser?.getIdToken() ?? null
}

async function pedir(metodo, ruta, cuerpo) {
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
  get: (ruta) => pedir('GET', ruta),
  post: (ruta, cuerpo) => pedir('POST', ruta, cuerpo ?? {}),
  put: (ruta, cuerpo) => pedir('PUT', ruta, cuerpo ?? {}),
  patch: (ruta, cuerpo) => pedir('PATCH', ruta, cuerpo ?? {}),
  delete: (ruta) => pedir('DELETE', ruta),
}
