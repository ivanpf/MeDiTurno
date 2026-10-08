# Mocks de MeDiTurno

Mocks HTML de los casos de uso marcados con `*` en el documento de casos de uso.
**Objetivo:** definir dónde va cada componente y cómo es, más o menos, la UX. No son un diseño final.

## Reglas generales

- **Mantenelo básico.** Nada de diseño "lindo": sin colores de marca, sin sombras, sin animaciones, sin íconos, sin tipografías externas.
- **Sin frameworks ni librerías.** Solo HTML, un CSS compartido y JavaScript plano. Nada de npm, build ni CDN.
- **Todo en español, tratando al usuario de "usted"** ("tiene", "elija", "ingrese"; no "tienes", "elige", "ingresa" ni "tenés", "elegí", "ingresá"), igual que el resto de los mocks.
- **No agregar textos explicativos del caso de uso** en las pantallas (cajas azules, notas, aclaraciones sobre el flujo). Las pantallas muestran solo lo que vería el usuario.
- **Los datos son de ejemplo y deben verse reales** (nombres, fechas, textos clínicos creíbles), no "Lorem ipsum" ni "…".
- Si cambiás algo de una pantalla, **copiá el estilo de las que ya existen** en vez de inventar uno nuevo.

## Estructura

| Archivo | Para qué sirve |
|---|---|
| `index.html` | Índice con un link a cada mock |
| `cuNN.html` | Un mock por caso de uso (ej. `cu08.html` = CU-8) |
| `app.css` | Único CSS, compartido por todos los mocks |
| `app.js` | Encabezado, menú lateral y funciones auxiliares |

## Cómo se arma una pantalla

Cada mock empieza así y el contenido va dentro de `<body>`:

```html
<!doctype html><meta charset="utf-8"><title>CU-8</title><link rel="stylesheet" href="app.css"><script src="app.js"></script>
<body data-role="paciente" data-cu="CU-8" data-title="Reserva de una orden de consulta" data-active="Reservar consulta">
  ...contenido...
```

`app.js` rodea el contenido con el encabezado y el menú según estos atributos:

- `data-role`: `paciente`, `funcionario`, `medico`, `admin` o `public` (sin menú, para login/registro).
- `data-active`: ítem del menú que se resalta. Tiene que coincidir con el texto de `MENUS` en `app.js`.
- `data-cu` y `data-title`: aparecen en la barra superior.

Para agregar una pantalla nueva al menú, editar `MENUS` en `app.js` y sumarla a `index.html`.

## Componentes (clases de `app.css`)

- **Pantalla centrada** (login, registro): `<div class="center">`.
- **Formulario:** `label` + `input`/`select`/`textarea`. Para campos en fila: `<div class="row"><div>…</div><div>…</div></div>`.
- **Botones:** `<button>` común; `class="pri"` para la acción principal, `class="danger"` para acciones destructivas. Botones de acciones: `<div class="actions">`.
- **Tablas:** `<table>` sin clases.
- **Mensajes:** `<div class="msg err|ok|warn">`.
- **Cajas:** `<div class="box">`.
- **Etiquetas de estado:** `<span class="tag">Vigente</span>`.
- **Popups de confirmación:** `<div class="modal-bg hide"><div class="modal">…</div></div>`.
- **Texto secundario:** `class="muted"`.

## Mostrar y ocultar

- Ocultar un elemento: `class="hide"`. Mostrarlo/ocultarlo con `toggle('id', true|false)`.
- Pantallas por pasos dentro de un mismo mock: cada paso es un `<div data-state id="...">` (los siguientes con `class="hide"`) y se cambia con `show('id')`.

## Botones "Simular"

Los flujos alternativos (errores, casos vacíos) se muestran con botones dentro de `<div class="scenarios">Simular: …</div>`.
Hay una variable global en `app.js` para mostrarlos u ocultarlos en **todos** los mocks a la vez:

```js
const SHOW_SIMULAR = false;
```
