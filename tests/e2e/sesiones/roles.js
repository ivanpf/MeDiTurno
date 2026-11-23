// Roles con los que se prueba la web. Cada uno tiene:
// - las variables de entorno (.env) con su correo y su contraseña de prueba;
// - una carpeta de tests propia (tests/<rol>/), que corre con la sesión de ese rol ya iniciada.
// Para sumar los tests de otro rol alcanza con crear su carpeta en tests/: la sesión ya está configurada acá.
export const ROLES = {
  medico: { correo: 'E2E_MEDICO_CORREO', clave: 'E2E_MEDICO_CLAVE' },
  paciente: { correo: 'E2E_PACIENTE_CORREO', clave: 'E2E_PACIENTE_CLAVE' },
  funcionario: { correo: 'E2E_FUNCIONARIO_CORREO', clave: 'E2E_FUNCIONARIO_CLAVE' },
  admin: { correo: 'E2E_ADMIN_CORREO', clave: 'E2E_ADMIN_CLAVE' },
}

// Dónde queda guardada la sesión de cada rol (ignorado por Git: contiene tokens).
export const archivoSesion = (rol) => `sesiones/.guardadas/${rol}.json`
