/**
 * Utilidades de formateo de texto y datos de presentación.
 */

/**
 * Capitaliza la primera letra de cada palabra.
 * @param {string} texto
 * @returns {string}
 */
export function capitalizar(texto) {
  if (!texto) return '';
  return texto
    .toLowerCase()
    .split(' ')
    .map((p) => p.charAt(0).toUpperCase() + p.slice(1))
    .join(' ');
}

/**
 * Formatea nombre completo: "Apellido, Nombre"
 * @param {string} nombre
 * @param {string} apellido
 * @returns {string}
 */
export function formatearNombreCompleto(nombre, apellido) {
  const n = nombre?.trim() || '';
  const a = apellido?.trim() || '';
  if (!n && !a) return '';
  if (!n) return a;
  if (!a) return n;
  return `${a}, ${n}`;
}

/**
 * Formatea nombre para mostrar: "Dr. Apellido" o "Dra. Apellido"
 * @param {Object} medico - { nombre, apellido }
 * @returns {string}
 */
export function formatearMedico(medico) {
  if (!medico) return '';
  const { nombre, apellido } = medico;
  const prefijo = nombre?.toLowerCase().includes('dra') ? 'Dra.' : 'Dr.';
  return `${prefijo} ${apellido}`;
}

/**
 * Obtiene la clase CSS para el badge según el estado de la reserva.
 * @param {string} estado - VIGENTE|ORDEN_EMITIDA|ATENDIDA|CANCELADA|ANULADA|VENCIDA
 * @returns {string} - Clase Tailwind
 */
export function badgeClassForEstado(estado) {
  const clases = {
    VIGENTE: 'badge-vigente',
    ORDEN_EMITIDA: 'badge-orden-emitida',
    ATENDIDA: 'badge-atendida',
    CANCELADA: 'badge-cancelada',
    ANULADA: 'badge-anulada',
    VENCIDA: 'badge-vencida',
  };
  return clases[estado] || 'badge';
}

/**
 * Etiqueta legible para el estado de la reserva.
 * @param {string} estado
 * @returns {string}
 */
export function labelForEstado(estado) {
  const etiquetas = {
    VIGENTE: 'Vigente',
    ORDEN_EMITIDA: 'Orden emitida',
    ATENDIDA: 'Atendida',
    CANCELADA: 'Cancelada',
    ANULADA: 'Anulada',
    VENCIDA: 'Vencida',
  };
  return etiquetas[estado] || estado;
}

/**
 * Etiqueta legible para el rol.
 * @param {string} rol
 * @returns {string}
 */
export function labelForRol(rol) {
  const etiquetas = {
    paciente: 'Paciente',
    funcionario: 'Funcionario',
    medico: 'Médico',
    admin: 'Administrador',
  };
  return etiquetas[rol] || rol;
}

/**
 * Trunca texto largo con elipsis.
 * @param {string} texto
 * @param {number} max - Longitud máxima
 * @returns {string}
 */
export function truncar(texto, max = 100) {
  if (!texto) return '';
  if (texto.length <= max) return texto;
  return texto.slice(0, max - 3) + '...';
}

/**
 * Formatea número de reserva para mostrar: "RES-000123"
 * @param {number|string} id
 * @returns {string}
 */
export function formatearCodigoReserva(id) {
  const num = Number(id);
  if (isNaN(num)) return String(id);
  return `RES-${String(num).padStart(6, '0')}`;
}

/**
 * Genera array de slots para elegir número en reserva.
 * @param {number} cupoMaximo
 * @param {number[]} ocupados - Números ya reservados
 * @param {Function} calcularHora - fn(numero) => hora estimada
 * @returns {Array<{numero: number, hora: string, ocupado: boolean}>}
 */
export function generarSlots(cupoMaximo, ocupados = [], calcularHora) {
  const setOcupados = new Set(ocupados);
  const slots = [];
  for (let n = 1; n <= cupoMaximo; n++) {
    slots.push({
      numero: n,
      hora: calcularHora(n),
      ocupado: setOcupados.has(n),
    });
  }
  return slots;
}

/**
 * Parsea ApiError para obtener mensaje legible.
 * @param {Error} error
 * @returns {string}
 */
export function getErrorMessage(error) {
  if (error?.nombre === 'ApiError' || error?.estado) {
    return error.mensaje;
  }
  if (error?.message) return error.message;
  return 'Ocurrió un error inesperado';
}

/**
 * Parsea ApiError para obtener detalles como array de strings.
 * @param {Error} error
 * @returns {string[]}
 */
export function getErrorDetails(error) {
  if (error?.detalles && Array.isArray(error.detalles)) {
    return error.detalles;
  }
  return [];
}