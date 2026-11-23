// Datos del seed (backend/src/main/resources/db/datos-prueba/R__zz_datos_de_prueba.sql) que usan las pruebas.
// Si el seed cambia, se cambian acá y no en cada test.
//
// El turno de la Dra. Gómez de HOY es de 18 a 21, Cardiología, 9 cupos: 180 min ÷ 9 = 20 min por número,
// así que la hora estimada del número N es 18:00 + (N − 1) × 20 min.

export const TURNO_DE_HOY = {
  // Texto del botón del turno en «Mis pacientes» (con guion largo, como lo muestra la pantalla).
  horario: '18:00–21:00',
  titulo: 'Pacientes de 18:00 a 21:00',
  especialidad: 'Cardiología',
}

// Pacientes del turno de hoy, como los muestra la pantalla («Apellido, Nombre»).
export const ANA = { nombre: 'López, Ana', numero: 1, hora: '~18:00', cedula: '9.000.001-1' } // orden emitida, 5 consultas previas
export const JOAQUIN = { nombre: 'Viera, Joaquín', numero: 3, hora: '~18:40' } // orden emitida, sin historia clínica
export const PEDRO = { nombre: 'Castro, Pedro', numero: 5, hora: '~19:20' } // reserva vigente, sin orden
export const VALENTINA = { nombre: 'Olivera, Valentina', numero: 7 } // reserva cancelada: no debe aparecer

// Consultas previas de Ana (CU-22): cinco atenciones con estos tres médicos.
export const CONSULTAS_PREVIAS_DE_ANA = 5
export const MEDICOS_DE_ANA = ['Gómez, Laura', 'Rossi, Diego', 'Cabrera, Andrés']

// Días de la semana en que atiende la Dra. Gómez, además del día en que se cargó el seed (0 = domingo … 6 = sábado).
export const DIAS_DE_LA_DRA_GOMEZ = [1, 3] // lunes (Cardiología 9–12) y miércoles (Clínica Médica 14–17)

export const DRA_GOMEZ = { cedula: '9.000.002-2', correo: 'medico@mediturno.com' }

// ------------------------------------------------------------------ paciente: Ana López (paciente@mediturno.com)

export const PERFIL_DE_ANA = {
  nombre: 'López, Ana', cedula: '9.000.001-1', nacimiento: '12/04/1988',
  correo: 'paciente@mediturno.com', telefono: '099 000 021', domicilio: 'Av. Italia 1234',
}

// Sus cinco consultas (CU-11), de la más reciente a la más antigua.
export const CONSULTAS_DE_ANA = [
  { especialidad: 'Clínica Médica', medico: 'Cabrera, Andrés', motivo: 'Fiebre, dolor de garganta y tos de tres días.' }, // hace una semana
  { especialidad: 'Cardiología', medico: 'Gómez, Laura' }, //       hace unas 2 semanas
  { especialidad: 'Clínica Médica', medico: 'Gómez, Laura' }, //    hace unas 3 semanas (pase vigente a Traumatología)
  { especialidad: 'Traumatología', medico: 'Rossi, Diego' }, //     hace unas 11 semanas (con el primer pase)
  { especialidad: 'Clínica Médica', medico: 'Gómez, Laura' }, //    hace unas 13 semanas (pases a Traumatología y Dermatología)
]

// Sus reservas de hoy y próximas (CU-9). Las fechas se calculan con utilidades/fechas.js.
export const RESERVAS_DE_ANA = {
  // Hoy, turno de la Dra. Gómez de 18 a 21: orden emitida (no se cancela; la atiende el médico en CU-23).
  hoy: { especialidad: 'Cardiología', horario: '18:00–21:00', numero: 1 },
  // Mañana, Dr. Cabrera de 18 a 20: faltan menos de 48 horas.
  manana: { especialidad: 'Clínica Médica', horario: '18:00–20:00', numero: 2 },
  // Dra. Gómez, el primer lunes a 3 días o más: se puede cancelar (CU-9). También reservaron los números 1 y 7.
  cancelable: { especialidad: 'Cardiología', medico: 'Gómez, Laura', horario: '09:00–12:00', dia: 1, numero: 5 },
}

// Pases de Ana: el de Traumatología está vigente; el de Dermatología venció (CU-8 no le deja reservar Dermatología).
export const PASE_VIGENTE = { especialidad: 'Traumatología', medico: 'Rossi, Diego' }
export const PASE_VENCIDO = { especialidad: 'Dermatología' }

// Turno del Dr. Cabrera de 13 a 17 de dentro de 7 días: todos sus números están reservados (CU-8 no lo ofrece).
// Ese mismo día también atiende de 8 a 12, con cupos.
export const TURNO_SIN_CUPOS = {
  especialidad: 'Clínica Médica', medico: 'Cabrera, Andrés', horario: '13:00–17:00', otroHorarioDelDia: '08:00–12:00', enDias: 7,
}

// Cartelera (CU-6): lo que se ve hoy y lo que no (programado, vencido y eliminado). Dos de los vigentes tienen imagen.
export const ANUNCIOS = {
  vigentes: ['Horario de verano', 'Campaña de vacunación', 'Traiga su cédula'],
  conImagen: ['Horario de verano', 'Campaña de vacunación'],
  noVisibles: ['Nuevos turnos de Dermatología', 'Cierre por feriado', 'Mantenimiento del consultorio 4'],
}
