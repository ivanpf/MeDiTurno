import { lazy } from 'react'
import { ROLES } from './auth/roles'
const ReservarConsultaPage = lazy(() => import('./features/reservas/ReservarConsultaPage'))
const MisReservasPage = lazy(() => import('./features/reservas/MisReservasPage'))
const MiHistoriaClinicaPage = lazy(() => import('./features/consultas/MiHistoriaClinicaPage'))
const TurnosSemanalesPage = lazy(() => import('./features/turnos/TurnosSemanalesPage'))
const AltaTurnoPage = lazy(() => import('./features/turnos/AltaTurnoPage'))
const TurnosSuspendidosPage = lazy(() => import('./features/turnos/TurnosSuspendidosPage'))
const ConsultaReservaPage = lazy(() => import('./features/reservas/ConsultaReservaPage'))
const ReservasParaAtencionPage = lazy(() => import('./features/reservas/ReservasParaAtencionPage'))
const PublicarAnuncioPage = lazy(() => import('./features/anuncios/PublicarAnuncioPage'))
const EliminarAnuncioPage = lazy(() => import('./features/anuncios/EliminarAnuncioPage'))
const PacientesPorTurnoPage = lazy(() => import('./features/consultas/PacientesPorTurnoPage'))
const HistoriaClinicaAtencionPage = lazy(() => import('./features/consultas/HistoriaClinicaAtencionPage'))
const RegistrarConsultaPage = lazy(() => import('./features/consultas/RegistrarConsultaPage'))
const EmitirPasePage = lazy(() => import('./features/pases/EmitirPasePage'))
const HabilitarReconsultaPage = lazy(() => import('./features/pases/HabilitarReconsultaPage'))
const HistorialAtencionesPage = lazy(() => import('./features/consultas/HistorialAtencionesPage'))
const PasesEmitidosPage = lazy(() => import('./features/pases/PasesEmitidosPage'))
const AltaEspecialidadPage = lazy(() => import('./features/especialidades/AltaEspecialidadPage'))
const AltaMasivaEspecialidadesPage = lazy(() => import('./features/especialidades/AltaMasivaEspecialidadesPage'))
const ListadoEspecialidadesPage = lazy(() => import('./features/especialidades/ListadoEspecialidadesPage'))
const ModificarEspecialidadPage = lazy(() => import('./features/especialidades/ModificarEspecialidadPage'))
const AltaPacientePage = lazy(() => import('./features/pacientes/AltaPacientePage'))
const AltaMasivaPacientesPage = lazy(() => import('./features/pacientes/AltaMasivaPacientesPage'))
const ListadoPacientesPage = lazy(() => import('./features/pacientes/ListadoPacientesPage'))
const ModificarPacientePage = lazy(() => import('./features/pacientes/ModificarPacientePage'))
const AltaUsuarioInternoPage = lazy(() => import('./features/usuarios/AltaUsuarioInternoPage'))
const AltaMasivaUsuariosPage = lazy(() => import('./features/usuarios/AltaMasivaUsuariosPage'))
const ListadoUsuariosPage = lazy(() => import('./features/usuarios/ListadoUsuariosPage'))
const ModificarUsuarioPage = lazy(() => import('./features/usuarios/ModificarUsuarioPage'))
const EstadoUsuarioPage = lazy(() => import('./features/usuarios/EstadoUsuarioPage'))
const AltaConsultorioPage = lazy(() => import('./features/consultorios/AltaConsultorioPage'))
const AltaMasivaConsultoriosPage = lazy(() => import('./features/consultorios/AltaMasivaConsultoriosPage'))
const ListadoConsultoriosPage = lazy(() => import('./features/consultorios/ListadoConsultoriosPage'))
const ModificarConsultorioPage = lazy(() => import('./features/consultorios/ModificarConsultorioPage'))
const ReservasPorEspecialidadPage = lazy(() => import('./features/estadisticas/ReservasPorEspecialidadPage'))
const TurnosMasDemandadosPage = lazy(() => import('./features/estadisticas/TurnosMasDemandadosPage'))
const OcupacionTurnosPage = lazy(() => import('./features/estadisticas/OcupacionTurnosPage'))
const CarteleraPage = lazy(() => import('./features/anuncios/CarteleraPage'))
const PerfilPage = lazy(() => import('./features/cuenta/PerfilPage'))
const CambiarContrasenaPage = lazy(() => import('./features/cuenta/CambiarContrasenaPage'))

// Fuente única de las pantallas autenticadas: de acá salen el menú lateral y las rutas.
//   roles:  quiénes ven la pantalla      grupo: título que agrupa ítems del menú (opcional)
//   cu:     caso(s) de uso que cubre     Componente: la pantalla
// Para agregar una pantalla: crear el componente en su carpeta de `features/` y sumar una línea acá.
// Las pantallas comunes a todos los perfiles van al final, así la primera de cada rol es su pantalla de inicio.
export const RUTAS = [
  { roles: [ROLES.PACIENTE], path: '/paciente/reservar', etiqueta: 'Reservar consulta', cu: 'CU-8', Componente: ReservarConsultaPage },
  { roles: [ROLES.PACIENTE], path: '/paciente/reservas', etiqueta: 'Mis reservas', cu: 'CU-9 / CU-10', Componente: MisReservasPage },
  { roles: [ROLES.PACIENTE], path: '/paciente/historia-clinica', etiqueta: 'Historia clínica', cu: 'CU-11', Componente: MiHistoriaClinicaPage },
  { roles: [ROLES.FUNCIONARIO], path: '/funcionario/turnos', etiqueta: 'Turnos semanales', cu: 'CU-20 / CU-13', Componente: TurnosSemanalesPage },
  { roles: [ROLES.FUNCIONARIO], path: '/funcionario/turnos/alta', etiqueta: 'Alta de turno', cu: 'CU-12', Componente: AltaTurnoPage },
  { roles: [ROLES.FUNCIONARIO], path: '/funcionario/turnos/suspendidos', etiqueta: 'Turnos suspendidos', cu: 'CU-14', Componente: TurnosSuspendidosPage },
  { roles: [ROLES.FUNCIONARIO], path: '/funcionario/reservas', etiqueta: 'Consulta de reserva y orden', cu: 'CU-15 / CU-16', Componente: ConsultaReservaPage },
  { roles: [ROLES.FUNCIONARIO], path: '/funcionario/reservas/atencion', etiqueta: 'Reservas para atención', cu: 'CU-19', Componente: ReservasParaAtencionPage },
  { roles: [ROLES.FUNCIONARIO], path: '/funcionario/anuncios/publicar', etiqueta: 'Publicar anuncio', cu: 'CU-17', grupo: 'Gestión de cartelera', Componente: PublicarAnuncioPage },
  { roles: [ROLES.FUNCIONARIO], path: '/funcionario/anuncios/eliminar', etiqueta: 'Eliminar anuncios', cu: 'CU-18', grupo: 'Gestión de cartelera', Componente: EliminarAnuncioPage },
  { roles: [ROLES.MEDICO], path: '/medico/pacientes', etiqueta: 'Mis pacientes', cu: 'CU-21', Componente: PacientesPorTurnoPage },
  { roles: [ROLES.MEDICO], path: '/medico/historia-clinica', etiqueta: 'Historia clínica', cu: 'CU-22', Componente: HistoriaClinicaAtencionPage },
  { roles: [ROLES.MEDICO], path: '/medico/consulta', etiqueta: 'Registrar consulta', cu: 'CU-23', Componente: RegistrarConsultaPage },
  { roles: [ROLES.MEDICO], path: '/medico/pase', etiqueta: 'Emitir pase', cu: 'CU-24', Componente: EmitirPasePage },
  { roles: [ROLES.MEDICO], path: '/medico/reconsulta', etiqueta: 'Habilitar reconsulta', cu: 'CU-25', Componente: HabilitarReconsultaPage },
  { roles: [ROLES.MEDICO], path: '/medico/atenciones', etiqueta: 'Historial de atenciones', cu: 'CU-26', Componente: HistorialAtencionesPage },
  { roles: [ROLES.MEDICO], path: '/medico/pases', etiqueta: 'Pases emitidos', cu: 'CU-27', Componente: PasesEmitidosPage },
  { roles: [ROLES.ADMIN], path: '/admin/especialidades', etiqueta: 'Alta', cu: 'CU-28', grupo: 'Especialidades', Componente: AltaEspecialidadPage },
  { roles: [ROLES.ADMIN], path: '/admin/especialidades/csv', etiqueta: 'Alta masiva (CSV)', cu: 'CU-29', grupo: 'Especialidades', Componente: AltaMasivaEspecialidadesPage },
  { roles: [ROLES.ADMIN], path: '/admin/especialidades/listado', etiqueta: 'Listado y búsqueda', cu: 'CU-30', grupo: 'Especialidades', Componente: ListadoEspecialidadesPage },
  { roles: [ROLES.ADMIN], path: '/admin/especialidades/modificar', etiqueta: 'Modificación', cu: 'CU-43', grupo: 'Especialidades', Componente: ModificarEspecialidadPage },
  { roles: [ROLES.ADMIN], path: '/admin/pacientes', etiqueta: 'Alta', cu: 'CU-31', grupo: 'Pacientes', Componente: AltaPacientePage },
  { roles: [ROLES.ADMIN], path: '/admin/pacientes/csv', etiqueta: 'Alta masiva (CSV)', cu: 'CU-32', grupo: 'Pacientes', Componente: AltaMasivaPacientesPage },
  { roles: [ROLES.ADMIN], path: '/admin/pacientes/listado', etiqueta: 'Listado y búsqueda', cu: 'CU-33', grupo: 'Pacientes', Componente: ListadoPacientesPage },
  { roles: [ROLES.ADMIN], path: '/admin/pacientes/modificar', etiqueta: 'Modificación', cu: 'CU-44', grupo: 'Pacientes', Componente: ModificarPacientePage },
  { roles: [ROLES.ADMIN], path: '/admin/usuarios', etiqueta: 'Alta', cu: 'CU-34', grupo: 'Usuarios internos', Componente: AltaUsuarioInternoPage },
  { roles: [ROLES.ADMIN], path: '/admin/usuarios/csv', etiqueta: 'Alta masiva (CSV)', cu: 'CU-35', grupo: 'Usuarios internos', Componente: AltaMasivaUsuariosPage },
  { roles: [ROLES.ADMIN], path: '/admin/usuarios/listado', etiqueta: 'Listado y búsqueda', cu: 'CU-36', grupo: 'Usuarios internos', Componente: ListadoUsuariosPage },
  { roles: [ROLES.ADMIN], path: '/admin/usuarios/modificar', etiqueta: 'Modificación', cu: 'CU-45', grupo: 'Usuarios internos', Componente: ModificarUsuarioPage },
  { roles: [ROLES.ADMIN], path: '/admin/usuarios/estado', etiqueta: 'Activación e inactivación', cu: 'CU-46', grupo: 'Usuarios internos', Componente: EstadoUsuarioPage },
  { roles: [ROLES.ADMIN], path: '/admin/consultorios', etiqueta: 'Alta', cu: 'CU-37', grupo: 'Consultorios', Componente: AltaConsultorioPage },
  { roles: [ROLES.ADMIN], path: '/admin/consultorios/csv', etiqueta: 'Alta masiva (CSV)', cu: 'CU-38', grupo: 'Consultorios', Componente: AltaMasivaConsultoriosPage },
  { roles: [ROLES.ADMIN], path: '/admin/consultorios/listado', etiqueta: 'Listado y búsqueda', cu: 'CU-39', grupo: 'Consultorios', Componente: ListadoConsultoriosPage },
  { roles: [ROLES.ADMIN], path: '/admin/consultorios/modificar', etiqueta: 'Modificación', cu: 'CU-47', grupo: 'Consultorios', Componente: ModificarConsultorioPage },
  { roles: [ROLES.ADMIN], path: '/admin/estadisticas/reservas', etiqueta: 'Reservas por especialidad', cu: 'CU-40', grupo: 'Estadísticas', Componente: ReservasPorEspecialidadPage },
  { roles: [ROLES.ADMIN], path: '/admin/estadisticas/turnos', etiqueta: 'Turnos más demandados', cu: 'CU-41', grupo: 'Estadísticas', Componente: TurnosMasDemandadosPage },
  { roles: [ROLES.ADMIN], path: '/admin/estadisticas/ocupacion', etiqueta: 'Ocupación de turnos', cu: 'CU-42', grupo: 'Estadísticas', Componente: OcupacionTurnosPage },
  { roles: [ROLES.PACIENTE, ROLES.FUNCIONARIO, ROLES.MEDICO, ROLES.ADMIN], path: '/cartelera', etiqueta: 'Cartelera de novedades', cu: 'CU-6', Componente: CarteleraPage },
  { roles: [ROLES.PACIENTE, ROLES.FUNCIONARIO, ROLES.MEDICO, ROLES.ADMIN], path: '/perfil', etiqueta: 'Mi perfil', cu: 'CU-5', grupo: 'Mi cuenta', Componente: PerfilPage },
  { roles: [ROLES.PACIENTE, ROLES.FUNCIONARIO, ROLES.MEDICO, ROLES.ADMIN], path: '/cambiar-contrasena', etiqueta: 'Cambiar contraseña', cu: 'CU-3', grupo: 'Mi cuenta', Componente: CambiarContrasenaPage },
]

// Pantalla a la que se va después de iniciar sesión: la primera del menú del rol.
export const rutaInicio = (rol) => RUTAS.find((r) => r.roles.includes(rol))?.path ?? '/login'
