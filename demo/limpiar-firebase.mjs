// Limpia las cuentas de Firebase Authentication que dejaron las demos y los ensayos, y conserva las del seed.
// Se ejecuta A MANO (nunca lo corre otra cosa) desde la raíz del repo:
//
//   node demo/limpiar-firebase.mjs --correos medico@x.com,paciente@x.com            → simulacro: lista qué se conserva y qué se borraría
//   node demo/limpiar-firebase.mjs --correos medico@x.com,paciente@x.com --borrar   → borra (pide escribir BORRAR para confirmar)
//   node demo/limpiar-firebase.mjs --correos ... --borrar --si                      → borra sin preguntar
//
// SE BORRAN SOLO las cuentas de Firebase cuyos correos se pasan en --correos (el del médico y el del paciente de la demo). Todo lo
// demás se conserva (las de otras personas del equipo, las del seed y las cuatro @mediturno.com). La base de datos no se toca.
// Credenciales: secrets/firebase-clave.json (o la ruta de la variable FIREBASE_CREDENCIALES). Sin dependencias: solo Node 18+.

import { createSign } from 'node:crypto'
import { existsSync, readFileSync, readdirSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { createInterface } from 'node:readline/promises'
import { fileURLToPath } from 'node:url'

const raiz = join(dirname(fileURLToPath(import.meta.url)), '..')
const args = process.argv.slice(2)
const borrar = args.includes('--borrar')
const sinPreguntar = args.includes('--si')
const correosDemo = (args[args.indexOf('--correos') + 1] ?? '').split(',').map((c) => c.trim().toLowerCase()).filter(Boolean)
if (correosDemo.length === 0) fallar('Falta --correos con los correos de la demo separados por coma (por ejemplo --correos medico@x.com,paciente@x.com).')

const CORREOS_FIJOS = ['admin@mediturno.com', 'funcionario@mediturno.com', 'medico@mediturno.com', 'paciente@mediturno.com']

function fallar(mensaje) {
  console.error(`ERROR: ${mensaje}`)
  process.exit(1)
}

// ---------------------------------------------------------------- qué se conserva
const carpetasSeed = ['seed', 'datos-prueba'].map((c) => join(raiz, 'backend', 'src', 'main', 'resources', 'db', c))
const uidsDelSeed = new Set()
for (const carpeta of carpetasSeed) {
  if (!existsSync(carpeta)) continue
  for (const archivo of readdirSync(carpeta).filter((f) => f.endsWith('.sql'))) {
    // Los UID de Firebase son cadenas de 28 caracteres alfanuméricos entre comillas simples.
    for (const [, uid] of readFileSync(join(carpeta, archivo), 'utf8').matchAll(/'([A-Za-z0-9]{28})'/g)) uidsDelSeed.add(uid)
  }
}
if (uidsDelSeed.size === 0) fallar('No se encontró ningún UID de Firebase en los scripts de población: no se sabe qué conservar. No se borra nada.')

// ---------------------------------------------------------------- acceso a Firebase (cuenta de servicio → token → API REST)
const rutaClave = process.env.FIREBASE_CREDENCIALES ?? join(raiz, 'secrets', 'firebase-clave.json')
if (!existsSync(rutaClave)) fallar(`No se encontró la clave de servicio en ${rutaClave}.`)
const clave = JSON.parse(readFileSync(rutaClave, 'utf8'))
const proyecto = clave.project_id

const b64 = (x) => Buffer.from(typeof x === 'string' ? x : JSON.stringify(x)).toString('base64url')

async function tokenDeAcceso() {
  const ahora = Math.floor(Date.now() / 1000)
  const cuerpo = `${b64({ alg: 'RS256', typ: 'JWT' })}.${b64({
    iss: clave.client_email,
    scope: 'https://www.googleapis.com/auth/identitytoolkit',
    aud: 'https://oauth2.googleapis.com/token',
    iat: ahora,
    exp: ahora + 3600,
  })}`
  const firma = createSign('RSA-SHA256').update(cuerpo).sign(clave.private_key, 'base64url')
  const respuesta = await fetch('https://oauth2.googleapis.com/token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer', assertion: `${cuerpo}.${firma}` }),
  })
  const datos = await respuesta.json()
  if (!respuesta.ok) fallar(`No se pudo obtener el token de acceso: ${datos.error_description ?? datos.error ?? respuesta.status}`)
  return datos.access_token
}

const token = await tokenDeAcceso()
const base = `https://identitytoolkit.googleapis.com/v1/projects/${proyecto}`
const cabeceras = { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }

async function listarUsuarios() {
  const usuarios = []
  let pagina = ''
  do {
    const respuesta = await fetch(`${base}/accounts:batchGet?maxResults=1000${pagina ? `&nextPageToken=${encodeURIComponent(pagina)}` : ''}`, { headers: cabeceras })
    const datos = await respuesta.json()
    if (!respuesta.ok) fallar(`No se pudo listar los usuarios: ${datos.error?.message ?? respuesta.status}`)
    usuarios.push(...(datos.users ?? []))
    pagina = datos.nextPageToken ?? ''
  } while (pagina)
  return usuarios
}

async function borrarUsuarios(uids) {
  for (let i = 0; i < uids.length; i += 500) {
    const respuesta = await fetch(`${base}/accounts:batchDelete`, {
      method: 'POST',
      headers: cabeceras,
      body: JSON.stringify({ localIds: uids.slice(i, i + 500), force: true }),
    })
    const datos = await respuesta.json()
    if (!respuesta.ok) fallar(`No se pudo borrar: ${datos.error?.message ?? respuesta.status}`)
    for (const e of datos.errors ?? []) console.error(`  no se pudo borrar ${e.localId}: ${e.message}`)
  }
}

// ---------------------------------------------------------------- clasificación
const usuarios = await listarUsuarios()
// Solo se borran las cuentas de los correos indicados en --correos; el resto, de otras personas, no se toca.
const esDeLaDemo = (u) => correosDemo.includes((u.email ?? '').toLowerCase()) && !uidsDelSeed.has(u.localId) && !CORREOS_FIJOS.includes((u.email ?? '').toLowerCase())
const seConserva = (u) => !esDeLaDemo(u)
const aConservar = usuarios.filter(seConserva)
const aBorrar = usuarios.filter((u) => !seConserva(u))
const etiqueta = (u) => `${(u.email ?? '(sin correo)').padEnd(38)} ${u.localId}  ${(u.providerUserInfo ?? []).map((p) => p.providerId).join(',')}`

console.log(`Proyecto de Firebase: ${proyecto} · ${usuarios.length} cuentas`)
console.log(`\nSe CONSERVAN (${aConservar.length}):`)
aConservar.forEach((u) => console.log(`  ${etiqueta(u)}`))
console.log(`\nSe BORRARÍAN (${aBorrar.length}):`)
aBorrar.forEach((u) => console.log(`  ${etiqueta(u)}`))

// Las cuentas fijas tienen que seguir ahí: si faltara alguna, algo no cuadra y es mejor no tocar nada.
const faltan = CORREOS_FIJOS.filter((c) => !aConservar.some((u) => (u.email ?? '').toLowerCase() === c))
if (faltan.length) console.log(`\nAVISO: no existen en Firebase las cuentas fijas: ${faltan.join(', ')}`)

if (!borrar) {
  console.log('\nSimulacro: no se borró nada. Para borrar, repetir con --borrar.')
  process.exit(0)
}
if (aBorrar.length === 0) {
  console.log('\nNo hay nada para borrar.')
  process.exit(0)
}
if (!sinPreguntar) {
  const lector = createInterface({ input: process.stdin, output: process.stdout })
  const respuesta = await lector.question(`\nSe borrarán ${aBorrar.length} cuentas de ${proyecto}. Escriba BORRAR para confirmar: `)
  lector.close()
  if (respuesta.trim() !== 'BORRAR') fallar('Cancelado: no se borró nada.')
}
await borrarUsuarios(aBorrar.map((u) => u.localId))
console.log(`\nListo: ${aBorrar.length} cuentas borradas. Quedan ${aConservar.length}.`)
