const { test } = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');
const { transformFileSync } = require('@babel/core');
const React = require('react');
const { create, act } = require('react-test-renderer');

globalThis.IS_REACT_ACT_ENVIRONMENT = true;
const carpeta = path.join(__dirname, '../src/features/cuenta');

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

function crearFirebase({ reauthError, updateError } = {}) {
  const llamadas = { credential: [], reauth: [], update: [] };
  const firebaseAuth = {
    EmailAuthProvider: {
      credential: (email, pass) => { llamadas.credential.push({ email, pass }); return { email, pass }; },
    },
    reauthenticateWithCredential: async (user, cred) => {
      llamadas.reauth.push({ user, cred });
      if (reauthError) throw reauthError;
    },
    updatePassword: async (user, nueva) => {
      llamadas.update.push({ user, nueva });
      if (updateError) throw updateError;
    },
  };
  return { firebaseAuth, llamadas };
}

async function montar(t, opciones = {}) {
  const { firebaseAuth, llamadas } = crearFirebase(opciones);
  const auth = opciones.auth || { currentUser: { email: 'ana@example.com' } };
  const { useCambiarContrasena } = cargarModulo('useCambiarContrasena.js', {
    'firebase/auth': firebaseAuth,
    '../../../shared/src/auth/firebase': { auth },
    '../../../shared/src/utils/validation': require(path.join(__dirname, '../shared/src/utils/validation')),
  });
  let actual;
  let raiz;
  function Escenario() { actual = useCambiarContrasena(); return null; }
  await act(async () => { raiz = create(React.createElement(Escenario)); });
  t.after(async () => { await act(async () => raiz.unmount()); });
  const hacer = async (accion) => { await act(async () => { await accion(actual); }); };
  return { get estado() { return actual; }, llamadas, hacer };
}

test('CP-C3-01: con datos válidos reautentica y actualiza la contraseña, y limpia el formulario', async (t) => {
  const escenario = await montar(t);
  const { llamadas, hacer } = escenario;
  await hacer((e) => e.cambiarCampo('actual')('actual123'));
  await hacer((e) => e.cambiarCampo('nueva')('nueva456'));
  await hacer((e) => e.cambiarCampo('repetirNueva')('nueva456'));
  await hacer((e) => e.guardar());

  assert.equal(llamadas.credential.length, 1);
  assert.equal(llamadas.credential[0].email, 'ana@example.com');
  assert.equal(llamadas.credential[0].pass, 'actual123');
  assert.equal(llamadas.reauth.length, 1);
  assert.equal(llamadas.update.length, 1);
  assert.equal(llamadas.update[0].nueva, 'nueva456');
  assert.equal(escenario.estado.modificada, true);
  assert.equal(escenario.estado.error, null);
  assert.equal(escenario.estado.datos.actual, '');
  assert.equal(escenario.estado.datos.nueva, '');
  assert.equal(escenario.estado.datos.repetirNueva, '');
});

test('CP-C3-02: la contraseña actual vacía no llama a Firebase y muestra el error de validación', async (t) => {
  const escenario = await montar(t);
  const { llamadas, hacer } = escenario;
  await hacer((e) => e.cambiarCampo('nueva')('nueva456'));
  await hacer((e) => e.cambiarCampo('repetirNueva')('nueva456'));
  await hacer((e) => e.guardar());

  assert.equal(llamadas.credential.length, 0);
  assert.equal(llamadas.reauth.length, 0);
  assert.equal(llamadas.update.length, 0);
  assert.equal(escenario.estado.modificada, false);
  assert.ok(escenario.estado.error);
  assert.match(escenario.estado.error.mensaje, /actual/i);
});

test('CP-C3-03: las contraseñas nuevas que no coinciden no llaman a Firebase', async (t) => {
  const escenario = await montar(t);
  const { llamadas, hacer } = escenario;
  await hacer((e) => e.cambiarCampo('actual')('actual123'));
  await hacer((e) => e.cambiarCampo('nueva')('nueva456'));
  await hacer((e) => e.cambiarCampo('repetirNueva')('otra789'));
  await hacer((e) => e.guardar());

  assert.equal(llamadas.credential.length, 0);
  assert.ok(escenario.estado.error);
  assert.match(escenario.estado.error.mensaje, /coinciden/i);
});

test('CP-C3-04: una nueva contraseña de menos de 6 caracteres no llama a Firebase', async (t) => {
  const escenario = await montar(t);
  const { llamadas, hacer } = escenario;
  await hacer((e) => e.cambiarCampo('actual')('actual123'));
  await hacer((e) => e.cambiarCampo('nueva')('abc'));
  await hacer((e) => e.cambiarCampo('repetirNueva')('abc'));
  await hacer((e) => e.guardar());

  assert.equal(llamadas.credential.length, 0);
  assert.ok(escenario.estado.error);
  assert.match(escenario.estado.error.mensaje, /6 caracteres/i);
});

test('CP-C3-05: si la reautenticación falla por contraseña incorrecta, no actualiza y muestra el mensaje correspondiente', async (t) => {
  const escenario = await montar(t, { reauthError: { code: 'auth/invalid-credential' } });
  const { llamadas, hacer } = escenario;
  await hacer((e) => e.cambiarCampo('actual')('mala'));
  await hacer((e) => e.cambiarCampo('nueva')('nueva456'));
  await hacer((e) => e.cambiarCampo('repetirNueva')('nueva456'));
  await hacer((e) => e.guardar());

  assert.equal(llamadas.reauth.length, 1);
  assert.equal(llamadas.update.length, 0);
  assert.equal(escenario.estado.modificada, false);
  assert.ok(escenario.estado.error);
  assert.equal(escenario.estado.error.mensaje, 'La contraseña actual no es correcta.');
});

test('CP-C3-06: si Firebase exige reautenticación reciente, muestra el mensaje correspondiente', async (t) => {
  const escenario = await montar(t, { reauthError: { code: 'auth/requires-recent-login' } });
  const { llamadas, hacer } = escenario;
  await hacer((e) => e.cambiarCampo('actual')('actual123'));
  await hacer((e) => e.cambiarCampo('nueva')('nueva456'));
  await hacer((e) => e.cambiarCampo('repetirNueva')('nueva456'));
  await hacer((e) => e.guardar());

  assert.equal(llamadas.update.length, 0);
  assert.equal(escenario.estado.error.mensaje, 'Por seguridad, cierre sesión e inicie sesión nuevamente antes de cambiar la contraseña.');
});

test('CP-C3-07: si updatePassword falla por contraseña débil, muestra el mensaje correspondiente', async (t) => {
  const escenario = await montar(t, { updateError: { code: 'auth/weak-password' } });
  const { llamadas, hacer } = escenario;
  await hacer((e) => e.cambiarCampo('actual')('actual123'));
  await hacer((e) => e.cambiarCampo('nueva')('nueva456'));
  await hacer((e) => e.cambiarCampo('repetirNueva')('nueva456'));
  await hacer((e) => e.guardar());

  assert.equal(llamadas.reauth.length, 1);
  assert.equal(llamadas.update.length, 1);
  assert.equal(escenario.estado.modificada, false);
  assert.equal(escenario.estado.error.mensaje, 'La nueva contraseña debe tener al menos 6 caracteres.');
});

test('CP-C3-08: sin sesión activa no llama a Firebase y avisa que la sesión venció', async (t) => {
  const escenario = await montar(t, { auth: { currentUser: null } });
  const { llamadas, hacer } = escenario;
  await hacer((e) => e.cambiarCampo('actual')('actual123'));
  await hacer((e) => e.cambiarCampo('nueva')('nueva456'));
  await hacer((e) => e.cambiarCampo('repetirNueva')('nueva456'));
  await hacer((e) => e.guardar());

  assert.equal(llamadas.credential.length, 0);
  assert.equal(llamadas.reauth.length, 0);
  assert.equal(llamadas.update.length, 0);
  assert.equal(escenario.estado.modificada, false);
  assert.match(escenario.estado.error.mensaje, /sesión venció/i);
});

test('CP-C3-09: "limpiar" vuelve al estado inicial sin modificar nada', async (t) => {
  const escenario = await montar(t);
  const { hacer } = escenario;
  await hacer((e) => e.cambiarCampo('actual')('actual123'));
  await hacer((e) => e.cambiarCampo('nueva')('nueva456'));
  await hacer((e) => e.limpiar());

  assert.equal(escenario.estado.datos.actual, '');
  assert.equal(escenario.estado.datos.nueva, '');
  assert.equal(escenario.estado.datos.repetirNueva, '');
  assert.equal(escenario.estado.modificada, false);
  assert.equal(escenario.estado.error, null);
});
