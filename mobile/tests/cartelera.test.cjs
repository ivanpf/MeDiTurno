const { test } = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');
const { transformFileSync } = require('@babel/core');
const React = require('react');
const { create, act } = require('react-test-renderer');

globalThis.IS_REACT_ACT_ENVIRONMENT = true;
const carpeta = path.join(__dirname, '../src/features/anuncios');

// Se usa React real para los estados y efectos. Solo se reemplazan los módulos propios por los ya cargados.
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

const helpers = cargarModulo('cartelera.js');
const demo = cargarModulo('anunciosDemo.js');
const HOY = '2026-10-28';

const anuncio = (id, datos = {}) => ({
  id, titulo: `Anuncio ${id}`, contenido: '…', fechaPublicacion: `2026-10-${10 + id}T09:00:00`,
  fechaInicio: null, fechaFin: null, estado: 'PUBLICADO', autor: 'Martínez, Ana', imagenes: [], ...datos,
});

test('solo se muestran los publicados dentro de su período, más recientes primero', () => {
  const lista = [
    anuncio(1),
    anuncio(2, { estado: 'ELIMINADO' }),
    anuncio(3, { fechaFin: '2026-10-27' }),
    anuncio(4, { fechaInicio: '2026-10-29' }),
    anuncio(5, { fechaInicio: HOY, fechaFin: HOY }),
  ];
  assert.deepEqual(helpers.anunciosVigentes(lista, HOY).map((a) => a.id), [5, 1]);
  assert.equal(helpers.hoyISO(new Date(2026, 9, 28, 23, 30)), HOY);
});

test('el carrusel junta las fotos de todos los anuncios respetando su orden', () => {
  const fotos = helpers.fotosDelCarrusel([
    anuncio(1, { imagenes: [{ imagen: 'b.jpg', orden: 2, textoAlternativo: null }, { imagen: 'a.jpg', orden: 1, textoAlternativo: 'Foto A' }] }),
    anuncio(2),
    anuncio(3, { imagenes: [{ imagen: 'c.jpg', orden: 1, textoAlternativo: 'Foto C' }] }),
  ]);
  assert.deepEqual(fotos.map((f) => f.uri), ['a.jpg', 'b.jpg', 'c.jpg']);
  assert.equal(fotos[1].descripcion, 'Anuncio 1');
  assert.equal(fotos[2].idAnuncio, 3);
  assert.equal(new Set(fotos.map((f) => f.clave)).size, 3);
});

test('las fotos pasan en círculo: después de la última vuelve a la primera', () => {
  assert.equal(helpers.indiceCircular(3, 3), 0);
  assert.equal(helpers.indiceCircular(-1, 3), 2);
  assert.equal(helpers.indiceCircular(1, 3), 1);
  assert.equal(helpers.indiceCircular(5, 0), 0);
});

test('los datos de prueba tienen anuncios vigentes con fotos y otros que no se muestran', () => {
  const vigentes = helpers.anunciosVigentes(demo.ANUNCIOS_DEMO, HOY);
  assert.ok(vigentes.length >= 3);
  assert.ok(vigentes.length < demo.ANUNCIOS_DEMO.length);
  assert.ok(helpers.fotosDelCarrusel(vigentes).length >= 3);
});

async function montar(t, obtener) {
  const { useCarteleraNovedades } = cargarModulo('useCartelera.js', {
    './anunciosDemo': demo, './cartelera': helpers,
  });
  let actual;
  let raiz;
  function Escenario() { actual = useCarteleraNovedades(obtener); return null; }
  await act(async () => { raiz = create(React.createElement(Escenario)); });
  t.after(async () => act(async () => raiz.unmount()));
  return { get estado() { return actual; } };
}

test('al entrar carga los anuncios vigentes y arma las fotos', async (t) => {
  const escenario = await montar(t, async () => [anuncio(1, { imagenes: [{ imagen: 'a.jpg', orden: 1 }] }), anuncio(2, { estado: 'ELIMINADO' })]);
  assert.equal(escenario.estado.cargando, false);
  assert.deepEqual(escenario.estado.anuncios.map((a) => a.id), [1]);
  assert.equal(escenario.estado.fotos.length, 1);
});

test('sin anuncios publicados la lista queda vacía sin tratarlo como error', async (t) => {
  const escenario = await montar(t, async () => []);
  assert.deepEqual(escenario.estado.anuncios, []);
  assert.equal(escenario.estado.error, null);
});

test('un error al cargar se muestra con su mensaje', async (t) => {
  const escenario = await montar(t, async () => { throw { estado: 500, mensaje: 'Error interno', detalles: [] }; });
  assert.equal(escenario.estado.error.mensaje, 'Error interno');
  assert.equal(escenario.estado.cargando, false);
});
