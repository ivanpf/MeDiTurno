const { test } = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');
const { transformFileSync } = require('@babel/core');
const React = require('react');
const { create, act } = require('react-test-renderer');

globalThis.IS_REACT_ACT_ENVIRONMENT = true;
const carpeta = path.join(__dirname, '../src/features/reservas');

// Se usa React real para los estados y efectos. Solo se reemplaza la API, sin red ni Firebase.
function cargarModulo(nombre, reemplazos = {}) {
  const { code } = transformFileSync(path.join(carpeta, nombre), {
    configFile: false, babelrc: false,
    plugins: [require.resolve('@babel/plugin-transform-modules-commonjs')],
  });
  const modulo = { exports: {} };
  new Function('require', 'module', 'exports', code)(
    (id) => Object.hasOwn(reemplazos, id) ? reemplazos[id] : require(id), modulo, modulo.exports);
  return modulo.exports;
}

const helpers = cargarModulo('reservaConsulta.js');
const especialidades = [
  { codigo: 'GEN', nombre: 'Clínica', estado: 'ACTIVO', requierePase: false },
  { codigo: 'CARD', nombre: 'Cardiología', estado: 'ACTIVO', requierePase: true },
  { codigo: 'BAJA', nombre: 'Inactiva', estado: 'INACTIVO' },
];
const turno = { idOcurrenciaTurno: 41, idMedico: 7, nombreMedico: 'Laura Gómez',
  fecha: '2026-10-20', horaInicio: '09:00:00', horaFin: '10:00:00', consultorio: 'C1',
  cupos: 7, numerosNoDisponibles: [2] };
const fallo = (estado, mensaje) => ({ estado, mensaje, detalles: [] });
function diferida() {
  let resolver;
  const promesa = new Promise((resolve) => { resolver = resolve; });
  return { promesa, resolver };
}

async function montar(t, reemplazos = {}) {
  const api = {
    listarEspecialidades: async () => especialidades,
    listarOcurrenciasDisponibles: async () => [turno],
    reservarOrden: async (datos) => ({ idReserva: 92, numero: datos.numero, horaEstimada: '09:17:00' }),
    ...reemplazos,
  };
  const { useReservaConsulta } = cargarModulo('useReservaConsulta.js', {
    '../../../shared/src/api/endpoints': api, './reservaConsulta': helpers,
  });
  let actual;
  let raiz;
  function Escenario() { actual = useReservaConsulta(); return null; }
  await act(async () => { raiz = create(React.createElement(Escenario)); });
  let desmontado = false;
  const desmontar = async () => {
    if (!desmontado) { await act(async () => raiz.unmount()); desmontado = true; }
  };
  t.after(desmontar);
  return { get estado() { return actual; }, desmontar,
    hacer: async (accion) => { await act(async () => { await accion(actual); }); } };
}

async function elegirNumero(escenario, numero = 3) {
  await escenario.hacer((estado) => estado.seleccionarEspecialidad('GEN'));
  await escenario.hacer((estado) => estado.seleccionarMedico('7'));
  await escenario.hacer((estado) => estado.seleccionarTurno(turno));
  await escenario.hacer((estado) => estado.seleccionarNumero(numero));
}

test('la hora estimada coincide con la división entera del backend para cupos no divisibles', () => {
  assert.deepEqual(helpers.numerosDelTurno(turno).map((item) => item.hora),
    ['09:00', '09:08', '09:17', '09:25', '09:34', '09:42', '09:51']);
});

test('no se ofrecen números ocupados ni fuera del cupo', () => {
  for (const valor of [0, 2, 8, null, 1.5]) assert.equal(helpers.esNumeroLibre(turno, valor), false);
  assert.equal(helpers.esNumeroLibre(turno, 3), true);
  assert.equal(helpers.cuposLibres(turno), 6);
});

test('las semanas empiezan el lunes, incluyen el domingo y ordenan las fechas', () => {
  const lista = ['2026-10-25', '2026-10-18', '2026-10-19'].map((fecha) => ({ ...turno, fecha }));
  const semanas = helpers.agruparPorSemana(lista);
  assert.deepEqual(semanas.map((semana) => semana.inicio), ['2026-10-12', '2026-10-19']);
  assert.deepEqual(semanas[1].turnos.map((item) => item.fecha), ['2026-10-19', '2026-10-25']);
  assert.match(helpers.fechaVisible('2026-10-19'), /19\/10\/2026/);
  assert.equal(lista[0].fecha, '2026-10-25', 'no se modifica la respuesta original');
});

test('un médico aparece una sola vez aunque tenga varias ocurrencias', () => {
  assert.deepEqual(helpers.obtenerMedicos([turno, { ...turno, idOcurrenciaTurno: 42 }]),
    [{ value: '7', label: 'Laura Gómez' }]);
});

test('carga inicial automática y filtro de especialidades activas', async (t) => {
  const escenario = await montar(t);
  assert.deepEqual(escenario.estado.especialidades.map((item) => item.codigo), ['GEN', 'CARD']);
  assert.equal(escenario.estado.cargandoEspecialidades, false);
});

test('un error de carga permite reintentar sin mostrar una lista vacía como resultado exitoso', async (t) => {
  let intentos = 0;
  const escenario = await montar(t, { listarEspecialidades: async () => {
    if (++intentos === 1) throw new Error('sin conexión');
    return especialidades;
  } });
  assert.ok(escenario.estado.errorEspecialidades);
  await escenario.hacer((estado) => estado.cargarEspecialidades());
  assert.equal(escenario.estado.errorEspecialidades, null);
  assert.equal(escenario.estado.especialidades.length, 2);
});

test('cambiar médico o especialidad limpia fecha y número anteriores', async (t) => {
  const escenario = await montar(t);
  await elegirNumero(escenario);
  await escenario.hacer((estado) => estado.seleccionarMedico('7'));
  assert.equal(escenario.estado.turno, null);
  assert.equal(escenario.estado.numero, null);
  await elegirNumero(escenario);
  await escenario.hacer((estado) => estado.seleccionarEspecialidad('CARD'));
  assert.equal(escenario.estado.idMedico, '');
  assert.equal(escenario.estado.turno, null);
  assert.equal(escenario.estado.numero, null);
});

test('403 al consultar muestra el motivo del pase y no habilita fechas', async (t) => {
  const escenario = await montar(t, { listarOcurrenciasDisponibles: async () => {
    throw fallo(403, 'No posee un pase vigente');
  } });
  await escenario.hacer((estado) => estado.seleccionarEspecialidad('CARD'));
  assert.match(escenario.estado.errorTurnos.mensaje, /pase vigente/);
  assert.equal(escenario.estado.turnosConsultados, false);
  assert.deepEqual(escenario.estado.medicos, []);
});

test('una respuesta vieja no pisa la selección más reciente', async (t) => {
  const primera = diferida();
  const segunda = diferida();
  const escenario = await montar(t, { listarOcurrenciasDisponibles: (codigo) =>
    codigo === 'GEN' ? primera.promesa : segunda.promesa });
  let consultaPrimera;
  let consultaSegunda;
  await act(async () => { consultaPrimera = escenario.estado.seleccionarEspecialidad('GEN'); });
  await act(async () => { consultaSegunda = escenario.estado.seleccionarEspecialidad('CARD'); });
  await act(async () => { segunda.resolver([{ ...turno, idMedico: 8, nombreMedico: 'Otro médico' }]); await consultaSegunda; });
  await act(async () => { primera.resolver([turno]); await consultaPrimera; });
  assert.equal(escenario.estado.codigoEspecialidad, 'CARD');
  assert.deepEqual(escenario.estado.medicos, [{ value: '8', label: 'Otro médico' }]);
});

test('no se crea una reserva sin elegir un número libre', async (t) => {
  let envios = 0;
  const escenario = await montar(t, { reservarOrden: async () => { envios++; } });
  await elegirNumero(escenario, 2);
  await escenario.hacer((estado) => estado.confirmarReserva());
  assert.equal(escenario.estado.numero, null);
  assert.equal(envios, 0);
});

test('la confirmación envía solo ocurrencia y número y conserva los datos definitivos del servidor', async (t) => {
  let cuerpo;
  const escenario = await montar(t, { reservarOrden: async (datos) => {
    cuerpo = datos;
    return { idReserva: 92, numero: 3, horaEstimada: '09:17:00' };
  } });
  await elegirNumero(escenario);
  await escenario.hacer((estado) => estado.confirmarReserva());
  assert.deepEqual(cuerpo, { idOcurrenciaTurno: 41, numero: 3 });
  assert.equal(escenario.estado.reserva.idReserva, 92);
  assert.equal(escenario.estado.reserva.numero, 3);
  assert.equal(escenario.estado.reserva.horaEstimada, '09:17:00');
  assert.equal(escenario.estado.reserva.nombreMedico, 'Laura Gómez');
  assert.equal(escenario.estado.turno, null);
  await escenario.hacer((estado) => estado.iniciarOtraReserva());
  assert.equal(escenario.estado.reserva, null);
  assert.equal(escenario.estado.codigoEspecialidad, '');
});

test('el doble toque y un callback anterior a la confirmación no duplican el POST', async (t) => {
  const respuesta = diferida();
  let envios = 0;
  const escenario = await montar(t, { reservarOrden: () => { envios++; return respuesta.promesa; } });
  await elegirNumero(escenario);
  const confirmarAnterior = escenario.estado.confirmarReserva;
  let envio;
  await act(async () => { envio = confirmarAnterior(); confirmarAnterior(); });
  assert.equal(envios, 1);
  assert.equal(escenario.estado.enviando, true);
  await act(async () => { respuesta.resolver({ idReserva: 92, numero: 3, horaEstimada: '09:17:00' }); await envio; });
  await escenario.hacer(() => confirmarAnterior());
  assert.equal(envios, 1);
});

test('409 refresca disponibilidad, conserva médico válido y exige elegir fecha y número nuevamente', async (t) => {
  let consultas = 0;
  const escenario = await montar(t, {
    listarOcurrenciasDisponibles: async () => [{ ...turno, numerosNoDisponibles: ++consultas === 1 ? [2] : [2, 3] }],
    reservarOrden: async () => { throw fallo(409, 'Número ocupado'); },
  });
  await elegirNumero(escenario);
  await escenario.hacer((estado) => estado.confirmarReserva());
  assert.equal(consultas, 2);
  assert.equal(escenario.estado.idMedico, '7');
  assert.equal(escenario.estado.turno, null);
  assert.equal(escenario.estado.numero, null);
  assert.match(escenario.estado.errorReserva.mensaje, /Actualizamos/);
  await escenario.hacer((estado) => estado.seleccionarTurno(estado.semanas[0].turnos[0]));
  await escenario.hacer((estado) => estado.seleccionarNumero(3));
  assert.equal(escenario.estado.numero, null, 'el número recién ocupado ya no se puede elegir');
});

test('403 al confirmar bloquea la reserva y obliga a consultar nuevamente', async (t) => {
  const escenario = await montar(t, { reservarOrden: async () => { throw fallo(403, 'Pase vencido'); } });
  await elegirNumero(escenario);
  await escenario.hacer((estado) => estado.confirmarReserva());
  assert.match(escenario.estado.errorTurnos.mensaje, /Pase vencido/);
  assert.equal(escenario.estado.turno, null);
  assert.equal(escenario.estado.idMedico, '');
  assert.equal(escenario.estado.turnosConsultados, false);
});

test('una respuesta que llega después de salir de la pantalla no actualiza su estado', async (t) => {
  const pendiente = diferida();
  const escenario = await montar(t, { listarOcurrenciasDisponibles: () => pendiente.promesa });
  let consulta;
  await act(async () => { consulta = escenario.estado.seleccionarEspecialidad('GEN'); });
  await escenario.desmontar();
  await act(async () => { pendiente.resolver([turno]); await consulta; });
  assert.deepEqual(escenario.estado.medicos, []);
});
