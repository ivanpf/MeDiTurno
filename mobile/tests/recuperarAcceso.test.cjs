const { test } = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');
const { transformFileSync } = require('@babel/core');
const React = require('react');
const { create, act } = require('react-test-renderer');

globalThis.IS_REACT_ACT_ENVIRONMENT = true;
const carpeta = path.join(__dirname, '../src/features/auth');

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

function crearFirebase({ sendError } = {}) {
  const llamadas = { send: [] };
  const firebaseAuth = {
    sendPasswordResetEmail: async (auth, correo) => {
      llamadas.send.push({ auth, correo });
      if (sendError) throw sendError;
    },
  };
  return { firebaseAuth, llamadas };
}

async function montar(t, opciones = {}) {
  const { firebaseAuth, llamadas } = crearFirebase(opciones);
  const auth = opciones.auth || {};
  const { useRecuperarAcceso } = cargarModulo('useRecuperarAcceso.js', {
    'firebase/auth': firebaseAuth,
    '../../../shared/src/auth/firebase': { auth },
    '../../../shared/src/utils/validation': require(path.join(__dirname, '../shared/src/utils/validation')),
  });
  let actual;
  let raiz;
  function Escenario() { actual = useRecuperarAcceso(); return null; }
  await act(async () => { raiz = create(React.createElement(Escenario)); });
  t.after(async () => { await act(async () => raiz.unmount()); });
  const hacer = async (accion) => { await act(async () => { await accion(actual); }); };
  return { get estado() { return actual; }, llamadas, auth, hacer };
}

test('CP-C4-01: con un correo válido envía el correo de recuperación y muestra la confirmación', async (t) => {
  const escenario = await montar(t);
  const { llamadas, hacer } = escenario;
  await hacer((e) => e.setCorreo('ana@example.com'));
  await hacer((e) => e.solicitar());

  assert.equal(llamadas.send.length, 1);
  assert.equal(llamadas.send[0].correo, 'ana@example.com');
  assert.equal(escenario.estado.enviada, true);
  assert.equal(escenario.estado.error, null);
  assert.equal(escenario.estado.cargando, false);
});

test('CP-C4-02: recorta los espacios del correo antes de enviarlo', async (t) => {
  const escenario = await montar(t);
  const { llamadas, hacer } = escenario;
  await hacer((e) => e.setCorreo('  ana@example.com  '));
  await hacer((e) => e.solicitar());

  assert.equal(llamadas.send.length, 1);
  assert.equal(llamadas.send[0].correo, 'ana@example.com');
  assert.equal(escenario.estado.enviada, true);
});

test('CP-C4-03: un correo inválido no envía nada y muestra el error de validación', async (t) => {
  const escenario = await montar(t);
  const { llamadas, hacer } = escenario;
  await hacer((e) => e.setCorreo('no-es-correo'));
  await hacer((e) => e.solicitar());

  assert.equal(llamadas.send.length, 0);
  assert.equal(escenario.estado.enviada, false);
  assert.ok(escenario.estado.error);
  assert.match(escenario.estado.error.message, /correo electrónico válido/i);
});

test('CP-C4-04: un correo vacío no envía nada y muestra el error de validación', async (t) => {
  const escenario = await montar(t);
  const { llamadas, hacer } = escenario;
  await hacer((e) => e.solicitar());

  assert.equal(llamadas.send.length, 0);
  assert.equal(escenario.estado.enviada, false);
  assert.ok(escenario.estado.error);
});

test('CP-C4-05: si Firebase responde user-not-found, igual muestra la confirmación (no revela si la cuenta existe)', async (t) => {
  const escenario = await montar(t, { sendError: { code: 'auth/user-not-found' } });
  const { llamadas, hacer } = escenario;
  await hacer((e) => e.setCorreo('inexistente@example.com'));
  await hacer((e) => e.solicitar());

  assert.equal(llamadas.send.length, 1);
  assert.equal(escenario.estado.enviada, true);
  assert.equal(escenario.estado.error, null);
});

test('CP-C4-06: si Firebase responde too-many-requests, muestra el mensaje correspondiente y no confirma', async (t) => {
  const escenario = await montar(t, { sendError: { code: 'auth/too-many-requests' } });
  const { llamadas, hacer } = escenario;
  await hacer((e) => e.setCorreo('ana@example.com'));
  await hacer((e) => e.solicitar());

  assert.equal(llamadas.send.length, 1);
  assert.equal(escenario.estado.enviada, false);
  assert.ok(escenario.estado.error);
  assert.match(escenario.estado.error.message, /demasiadas solicitudes/i);
});

test('CP-C4-07: si Firebase responde invalid-email, muestra el mensaje correspondiente', async (t) => {
  const escenario = await montar(t, { sendError: { code: 'auth/invalid-email' } });
  const { llamadas, hacer } = escenario;
  await hacer((e) => e.setCorreo('ana@example.com'));
  await hacer((e) => e.solicitar());

  assert.equal(llamadas.send.length, 1);
  assert.equal(escenario.estado.enviada, false);
  assert.ok(escenario.estado.error);
  assert.match(escenario.estado.error.message, /correo electrónico válid/i);
});

test('CP-C4-08: "reiniciar" vuelve al paso de solicitud y limpia el error', async (t) => {
  const escenario = await montar(t, { sendError: { code: 'auth/too-many-requests' } });
  const { hacer } = escenario;
  await hacer((e) => e.setCorreo('ana@example.com'));
  await hacer((e) => e.solicitar());
  assert.equal(escenario.estado.enviada, false);
  assert.ok(escenario.estado.error);

  await hacer((e) => e.reiniciar());
  assert.equal(escenario.estado.enviada, false);
  assert.equal(escenario.estado.error, null);
});
