const { test } = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');
const { transformFileSync } = require('@babel/core');
const React = require('react');
const { create, act } = require('react-test-renderer');

globalThis.IS_REACT_ACT_ENVIRONMENT = true;
const carpeta = path.join(__dirname, '../src/features/consultas');

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

const helpers = cargarModulo('historiaClinica.js');
const especialidades = [
  { codigo: 'GEN', nombre: 'Clínica', estado: 'ACTIVO' },
  { codigo: 'CARD', nombre: 'Cardiología', estado: 'ACTIVO' },
  { codigo: 'DERM', nombre: 'Dermatología', estado: 'ACTIVO' },
];
const atenciones = [
  { idConsulta: 5, numeroOrden: 15, idMedico: 7, nombreMedico: 'Pérez, Juan', especialidad: 'Clínica',
    fecha: '2026-09-02T09:00:00', motivo: 'Dolor de garganta.', observaciones: 'Faringe congestiva.', indicaciones: 'Reposo.' },
  { idConsulta: 3, numeroOrden: 11, idMedico: 8, nombreMedico: 'Gómez, Laura', especialidad: 'Cardiología',
    fecha: '2026-05-14T10:00:00', motivo: 'Control.', observaciones: 'Sin alteraciones.', indicaciones: null },
];

async function montar(t, consultar = async () => atenciones) {
  const pedidos = [];
  const api = {
    listarEspecialidades: async () => especialidades,
    consultarHistoriaClinica: async (criterios) => { pedidos.push(criterios); return consultar(criterios); },
  };
  const { useHistoriaClinica } = cargarModulo('useHistoriaClinica.js', {
    '../../../shared/src/api/endpoints': api, './historiaClinica': helpers,
  });
  let actual;
  let raiz;
  function Escenario() { actual = useHistoriaClinica(); return null; }
  await act(async () => { raiz = create(React.createElement(Escenario)); });
  t.after(async () => act(async () => raiz.unmount()));
  return { get estado() { return actual; }, pedidos,
    hacer: async (accion) => { await act(async () => { await accion(actual); }); } };
}

test('las fechas se escriben como DD/MM/AAAA y viajan como AAAA-MM-DD', () => {
  assert.equal(helpers.formatearEntradaFecha('02092026'), '02/09/2026');
  assert.equal(helpers.formatearEntradaFecha('0209'), '02/09');
  assert.equal(helpers.fechaISO('02/09/2026'), '2026-09-02');
  assert.equal(helpers.fechaISO('31/02/2026'), null);
  assert.equal(helpers.fechaVisible('2026-09-02T09:00:00'), '02/09/2026');
});

test('el período se valida antes de consultar', () => {
  assert.equal(helpers.validarPeriodo({ fechaDesde: '', fechaHasta: '' }), null);
  assert.match(helpers.validarPeriodo({ fechaDesde: '31/02/2026', fechaHasta: '' }), /Desde/);
  assert.match(helpers.validarPeriodo({ fechaDesde: '10/10/2026', fechaHasta: '01/10/2026' }), /posterior/);
});

test('los criterios usan los nombres de CriteriosBusquedaDTO y omiten los vacíos', () => {
  assert.deepEqual(helpers.criteriosDeConsulta(helpers.FILTROS_VACIOS), { ordenarPor: 'fecha', descendente: true });
  assert.deepEqual(
    helpers.criteriosDeConsulta({ fechaDesde: '01/05/2026', fechaHasta: '30/09/2026', codigoEspecialidad: 'GEN', idMedico: '7', descendente: false }),
    { ordenarPor: 'fecha', descendente: false, fechaDesde: '2026-05-01', fechaHasta: '2026-09-30', codigoEspecialidad: 'GEN', idMedico: '7' });
});

test('los filtros ofrecen solo los médicos y especialidades de las atenciones del paciente', () => {
  const opciones = helpers.opcionesDeFiltro(atenciones, especialidades);
  assert.deepEqual(opciones.medicos.map((m) => m.label), ['Gómez, Laura', 'Pérez, Juan']);
  assert.deepEqual(opciones.especialidades, [{ value: 'GEN', label: 'Clínica' }, { value: 'CARD', label: 'Cardiología' }]);
});

test('al entrar muestra el listado completo, más recientes primero', async (t) => {
  const escenario = await montar(t);
  assert.deepEqual(escenario.pedidos, [{ ordenarPor: 'fecha', descendente: true }]);
  assert.equal(escenario.estado.tieneAtenciones, true);
  assert.equal(escenario.estado.atenciones.length, 2);
  assert.equal(escenario.estado.cargando, false);
});

test('3.A: sin atenciones finalizadas informa la situación sin tratarla como error', async (t) => {
  const escenario = await montar(t, async () => []);
  assert.equal(escenario.estado.tieneAtenciones, false);
  assert.equal(escenario.estado.error, null);
});

test('cada cambio de criterios reenvía la solicitud; una fecha incompleta o inválida no consulta', async (t) => {
  const escenario = await montar(t);
  await escenario.hacer((e) => e.cambiarFiltro('codigoEspecialidad', 'CARD'));
  assert.deepEqual(escenario.pedidos.at(-1), { ordenarPor: 'fecha', descendente: true, codigoEspecialidad: 'CARD' });

  await escenario.hacer((e) => e.cambiarFiltro('fechaDesde', '02/09'));
  assert.equal(escenario.pedidos.length, 2);
  await escenario.hacer((e) => e.cambiarFiltro('fechaDesde', '31/02/2026'));
  assert.equal(escenario.pedidos.length, 2);
  assert.match(escenario.estado.errorPeriodo, /Desde/);

  await escenario.hacer((e) => e.cambiarFiltro('fechaDesde', '01/09/2026'));
  assert.equal(escenario.estado.errorPeriodo, null);
  assert.equal(escenario.pedidos.at(-1).fechaDesde, '2026-09-01');

  await escenario.hacer((e) => e.limpiarFiltros());
  assert.deepEqual(escenario.pedidos.at(-1), { ordenarPor: 'fecha', descendente: true });
  assert.equal(escenario.estado.hayFiltros, false);
});

test('el detalle sale del listado, sin una nueva invocación, y se cierra si un filtro lo deja afuera', async (t) => {
  let respuesta = atenciones;
  const escenario = await montar(t, async () => respuesta);
  await escenario.hacer((e) => e.verDetalle(atenciones[1]));
  assert.equal(escenario.estado.seleccionada.idConsulta, 3);
  assert.equal(escenario.pedidos.length, 1);
  assert.deepEqual(helpers.seccionesDelDetalle(escenario.estado.seleccionada).map((s) => s.texto),
    ['Control.', 'Sin indicaciones.', 'Sin alteraciones.']);

  respuesta = [atenciones[0]];
  await escenario.hacer((e) => e.cambiarFiltro('idMedico', '7'));
  assert.equal(escenario.estado.seleccionada, null);
});

test('un error del backend se muestra con su mensaje', async (t) => {
  const escenario = await montar(t, async () => { throw { estado: 500, mensaje: 'Error interno', detalles: [] }; });
  assert.equal(escenario.estado.error.mensaje, 'Error interno');
  assert.equal(escenario.estado.tieneAtenciones, null);
});
