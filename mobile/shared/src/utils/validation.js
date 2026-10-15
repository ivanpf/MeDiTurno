/**
 * Utilidades de validación.
 * Reglas coherentes con el backend (DTOs con jakarta.validation).
 */

/**
 * Valida formato de cédula uruguaya (básico).
 * Acepta: 1.234.567-8, 12345678, 1.234.5678, etc.
 * @param {string} cedula
 * @returns {boolean}
 */
export function validarCedula(cedula) {
  if (!cedula) return false;
  // Quitar puntos y guiones
  const limpia = cedula.replace(/[.\-]/g, '');
  // Debe tener 7 u 8 dígitos
  return /^\d{7,8}$/.test(limpia);
}

/**
 * Normaliza cédula a formato estándar: 1.234.567-8
 * @param {string} cedula
 * @returns {string}
 */
export function normalizarCedula(cedula) {
  if (!cedula) return '';
  const limpia = cedula.replace(/[.\-]/g, '');
  if (limpia.length <= 3) return limpia;
  const sinDigito = limpia.slice(0, -1);
  const digito = limpia.slice(-1);
  // Agregar puntos cada 3 dígitos desde la derecha
  const conPuntos = sinDigito.replace(/\B(?=(\d{3})+(?!\d))/g, '.');
  return `${conPuntos}-${digito}`;
}

/**
 * Valida email básico.
 * @param {string} email
 * @returns {boolean}
 */
export function validarEmail(email) {
  if (!email) return false;
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

/**
 * Valida contraseña (mínimo 6 chars para Firebase).
 * @param {string} password
 * @returns {boolean}
 */
export function validarPassword(password) {
  return password && password.length >= 6;
}

/**
 * Valida que dos contraseñas coincidan.
 * @param {string} pass1
 * @param {string} pass2
 * @returns {boolean}
 */
export function passwordsCoinciden(pass1, pass2) {
  return pass1 === pass2 && pass1.length >= 6;
}

/**
 * Valida teléfono (opcional, hasta 30 chars).
 * @param {string} telefono
 * @returns {boolean}
 */
export function validarTelefono(telefono) {
  if (!telefono) return true; // Opcional
  return telefono.length <= 30;
}

/**
 * Valida domicilio (opcional, hasta 255 chars).
 * @param {string} domicilio
 * @returns {boolean}
 */
export function validarDomicilio(domicilio) {
  if (!domicilio) return true; // Opcional
  return domicilio.length <= 255;
}

/**
 * Valida formulario de registro completo.
 * @param {Object} data - { cedula, correo, telefono, domicilio, contrasena, repetirContrasena, proveedor }
 * @returns {Object} { valido: boolean, errores: string[] }
 */
export function validarRegistro(data) {
  const errores = [];

  if (!data.cedula || !validarCedula(data.cedula)) {
    errores.push('Cédula inválida');
  }
  if (!data.correo || !validarEmail(data.correo)) {
    errores.push('Correo electrónico inválido');
  }
  if (data.telefono && !validarTelefono(data.telefono)) {
    errores.push('Teléfono muy largo (máx. 30 caracteres)');
  }
  if (data.domicilio && !validarDomicilio(data.domicilio)) {
    errores.push('Domicilio muy largo (máx. 255 caracteres)');
  }
  if (data.proveedor === 'PASSWORD') {
    if (!data.contrasena || !validarPassword(data.contrasena)) {
      errores.push('La contraseña debe tener al menos 6 caracteres');
    }
    if (!passwordsCoinciden(data.contrasena, data.repetirContrasena)) {
      errores.push('Las contraseñas no coinciden');
    }
  }

  return {
    valido: errores.length === 0,
    errores,
  };
}

/**
 * Valida formulario de cambio de contraseña.
 * @param {Object} data - { actual, nueva, repetirNueva }
 * @returns {Object} { valido: boolean, errores: string[] }
 */
export function validarCambioContrasena(data) {
  const errores = [];

  if (!data.actual) {
    errores.push('Contraseña actual requerida');
  }
  if (!data.nueva || !validarPassword(data.nueva)) {
    errores.push('La nueva contraseña debe tener al menos 6 caracteres');
  }
  if (!passwordsCoinciden(data.nueva, data.repetirNueva)) {
    errores.push('Las contraseñas no coinciden');
  }

  return {
    valido: errores.length === 0,
    errores,
  };
}

/**
 * Valida email para recuperación.
 * @param {string} correo
 * @returns {Object} { valido: boolean, errores: string[] }
 */
export function validarRecuperacion(correo) {
  const errores = [];
  if (!correo || !validarEmail(correo)) {
    errores.push('Ingrese un correo electrónico válido');
  }
  return { valido: errores.length === 0, errores };
}