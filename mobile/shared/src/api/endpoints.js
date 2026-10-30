/**
 * Funciones tipadas para cada endpoint del backend.
 * Centraliza las rutas y parámetros.
 */

import { api } from './client';

/** @type {import('../types/api').LoginResponseDTO} */
export async function login() {
  return api.post('/auth/login');
}

/** @returns {Promise<void>} */
export async function logout() {
  return api.post('/auth/logout');
}

/**
 * @param {import('../types/api').CambiarContrasenaDTO} data
 * @returns {Promise<boolean>}
 */
export async function cambiarContrasena(data) {
  return api.post('/auth/contrasena', data);
}

/**
 * @param {import('../types/api').SolicitarRecuperacionDTO} data
 * @returns {Promise<void>}
 */
export async function solicitarRecuperacion(data) {
  return api.post('/auth/recuperacion', data);
}

/**
 * @param {string} token
 * @returns {Promise<void>}
 */
export async function validarTokenRecuperacion(token) {
  return api.get(`/auth/recuperacion/${token}`);
}

/**
 * @param {string} token
 * @param {import('../types/api').RestablecerContrasenaDTO} data
 * @returns {Promise<boolean>}
 */
export async function restablecerContrasena(token, data) {
  return api.post(`/auth/recuperacion/${token}/contrasena`, data);
}

/**
 * @param {import('../types/api').RegistroPacienteDTO} data
 * @returns {Promise<import('../types/api').PacienteResponseDTO>}
 */
export async function registrarPaciente(data) {
  return api.post('/pacientes/registro', data);
}

/**
 * @param {import('../types/api').VerificacionRegistroDTO} data
 * @returns {Promise<void>}
 */
export async function verificarPacienteEnPadron(data) {
  return api.post('/pacientes/registro/verificacion', data);
}

/**
 * @param {import('../types/api').CriteriosBusquedaDTO} [criterios]
 * @returns {Promise<import('../types/api').EspecialidadResponseDTO[]>}
 */
export async function listarEspecialidades(criterios = {}) {
  const params = new URLSearchParams();
  Object.entries(criterios).forEach(([k, v]) => {
    if (v !== undefined && v !== null) params.append(k, v);
  });
  const query = params.toString() ? `?${params}` : '';
  return api.get(`/especialidades${query}`);
}

/**
 * @param {string} codigoEspecialidad
 * @returns {Promise<import('../types/api').OcurrenciaTurnoDTO[]>}
 */
export async function listarOcurrenciasDisponibles(codigoEspecialidad) {
  return api.get(`/turnos-semanales/ocurrencias-disponibles?codigoEspecialidad=${encodeURIComponent(codigoEspecialidad)}`);
}

/**
 * @param {import('../types/api').ReservaRequestDTO} data
 * @returns {Promise<import('../types/api').ReservaResponseDTO>}
 */
export async function reservarOrden(data) {
  return api.post('/reservas', data);
}

/**
 * @param {number} idReserva
 * @returns {Promise<void>}
 */
export async function cancelarReserva(idReserva) {
  return api.post(`/reservas/${idReserva}/cancelacion`);
}

// CU-9: listado de apoyo y comprobación previa; el historial de CU-10 conserva su endpoint.
/** @returns {Promise<import('../types/api').ReservaCancelacionDTO[]>} */
export async function listarProximasPropias() {
  return api.get('/reservas/mias/proximas');
}

/**
 * @param {number} idReserva
 * @returns {Promise<import('../types/api').ReservaCancelacionDTO>}
 */
export async function vistaPreviaCancelacion(idReserva) {
  return api.get(`/reservas/${idReserva}/cancelacion/vista-previa`);
}


/**
 * @param {import('../types/api').CriteriosBusquedaDTO} [criterios]
 * @returns {Promise<import('../types/api').ReservaResponseDTO[]>}
 */
// CU-10: historial completo. No usa el listado de próximas reservas ni aplica filtros locales.
export async function listarMisReservas(criterios = {}) {
  const params = new URLSearchParams();
  Object.entries(criterios).forEach(([k, v]) => {
    if (v !== undefined && v !== null) params.append(k, v);
  });
  const query = params.toString() ? `?${params}` : '';
  return api.get(`/reservas/mias${query}`);
}

/**
 * @param {import('../types/api').CriteriosBusquedaDTO} [criterios]
 * @returns {Promise<import('../types/api').ConsultaResponseDTO[]>}
 */
export async function consultarHistoriaClinica(criterios = {}) {
  const params = new URLSearchParams();
  Object.entries(criterios).forEach(([k, v]) => {
    if (v !== undefined && v !== null) params.append(k, v);
  });
  const query = params.toString() ? `?${params}` : '';
  return api.get(`/pacientes/me/historia-clinica${query}`);
}

/**
 * @param {import('../types/api').PerfilDTO} data
 * @returns {Promise<void>}
 */
export async function actualizarPerfil(data) {
  return api.put('/pacientes/me/perfil', data);
}

/**
 * @returns {Promise<import('../types/api').AnuncioResponseDTO[]>}
 */
export async function listarCartelera() {
  return api.get('/anuncios');
}

/**
 * Registra el token de push notifications (FCM) en el backend.
 * @param {string} token
 * @returns {Promise<void>}
 */
export async function registrarPushToken(token) {
  return api.post('/notificaciones/push', { token });
}

/**
 * Elimina el token de push notifications del backend.
 * @returns {Promise<void>}
 */
export async function eliminarPushToken() {
  return api.delete('/notificaciones/push');
}
