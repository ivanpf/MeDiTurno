// Fuente única de las pantallas, portada de frontend/src/routes.jsx.
// De acá salen el menú y las rutas; cada pantalla vive en app/(app)/<archivo>.
//   roles: quiénes ven la pantalla   grupo: título que agrupa ítems del menú (opcional)
//   cu:    caso de uso que cubre     path:  ruta de expo-router
export const ROLES = {
  PACIENTE: 'paciente',
  FUNCIONARIO: 'funcionario',
  MEDICO: 'medico',
  ADMIN: 'admin',
};

export const ETIQUETA_ROL = {
  paciente: 'Paciente',
  funcionario: 'Funcionario',
  medico: 'Médico',
  admin: 'Administrador',
};

export const RUTAS = [
  { roles: [ROLES.PACIENTE], path: '/(app)/reservar', etiqueta: 'Reservar consulta', cu: 'CU-8' },
  { roles: [ROLES.PACIENTE], path: '/(app)/reservas', etiqueta: 'Mis reservas', cu: 'CU-9 / CU-10' },
  { roles: [ROLES.PACIENTE], path: '/(app)/historia-clinica', etiqueta: 'Historia clínica', cu: 'CU-11' },
  { roles: [ROLES.PACIENTE, ROLES.FUNCIONARIO, ROLES.MEDICO, ROLES.ADMIN], path: '/(app)/cartelera', etiqueta: 'Cartelera de novedades', cu: 'CU-6', inicio: true },
  { roles: [ROLES.PACIENTE, ROLES.FUNCIONARIO, ROLES.MEDICO, ROLES.ADMIN], path: '/(app)/perfil', etiqueta: 'Mi perfil', cu: 'CU-5', grupo: 'Mi cuenta' },
  { roles: [ROLES.PACIENTE, ROLES.FUNCIONARIO, ROLES.MEDICO, ROLES.ADMIN], path: '/(app)/cambiar-contrasena', etiqueta: 'Cambiar contraseña', cu: 'CU-3', grupo: 'Mi cuenta' },
];

// Pantalla inicial del rol: la marcada con `inicio: true`; si ninguna, la primera del menú.
export const rutaInicio = (rol) =>
  RUTAS.find((r) => r.roles.includes(rol) && r.inicio)?.path
  ?? RUTAS.find((r) => r.roles.includes(rol))?.path
  ?? '/(auth)/login';
