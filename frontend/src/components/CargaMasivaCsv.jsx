import { useRef, useState } from 'react'
import Confirmacion from './Confirmacion'
import { api } from '../services/api'
import { comoError } from '../utils/errores'
import estilos from './CargaMasivaCsv.module.css'

// «1 especialidad», «2 especialidades»
const cantidad = (n, singular, plural) => `${n} ${n === 1 ? singular : plural}`

// Pantalla de alta masiva por CSV (CU-29, CU-32, CU-35 y CU-38). Se previsualiza el archivo y solo se importa si todas las filas son válidas.
// El backend de cada caso de uso expone el mismo par de endpoints bajo su ruta: POST {ruta}/csv/previsualizacion y POST {ruta}/csv.
//   <CargaMasivaCsv titulo="Alta masiva de consultorios" ruta="/consultorios" singular="consultorio" plural="consultorios"
//     ejemplo={{ archivo: 'consultorios-ejemplo.csv', contenido: 'código,nombre\nC1,Sala 1\n' }}>
//     Texto de ayuda con las columnas del archivo.
//   </CargaMasivaCsv>
export default function CargaMasivaCsv({ titulo, ruta, singular, plural, ejemplo, children }) {
  const [archivo, setArchivo] = useState(null)
  const [vistaPrevia, setVistaPrevia] = useState(null)
  const [error, setError] = useState(null)
  const [creados, setCreados] = useState(null)
  const [trabajando, setTrabajando] = useState(false)
  const [confirmando, setConfirmando] = useState(false)
  const selector = useRef(null)

  const reiniciar = () => {
    setArchivo(null)
    setVistaPrevia(null)
    if (selector.current) selector.current.value = ''
  }

  const elegirArchivo = (e) => {
    setArchivo(e.target.files[0] ?? null)
    setVistaPrevia(null)
    setError(null)
    setCreados(null)
  }

  const formulario = () => {
    const datos = new FormData()
    datos.append('archivo', archivo)
    return datos
  }

  const previsualizar = async (e) => {
    e.preventDefault()
    setError(null)
    setCreados(null)
    setTrabajando(true)
    try {
      setVistaPrevia(await api.postForm(`${ruta}/csv/previsualizacion`, formulario()))
    } catch (err) {
      setVistaPrevia(null)
      setError(comoError(err, 'No fue posible leer el archivo. Intente nuevamente.'))
    } finally {
      setTrabajando(false)
    }
  }

  const importar = async () => {
    setError(null)
    setTrabajando(true)
    try {
      const resultado = await api.postForm(`${ruta}/csv`, formulario())
      setCreados(resultado.registrosCreados)
      reiniciar()
    } catch (err) {
      setError(comoError(err, 'No fue posible importar el archivo. Intente nuevamente.'))
    } finally {
      setTrabajando(false)
      setConfirmando(false)
    }
  }

  const descargarEjemplo = () => {
    const url = URL.createObjectURL(new Blob(['\uFEFF' + ejemplo.contenido], { type: 'text/csv;charset=utf-8' }))
    const enlace = document.createElement('a')
    enlace.href = url
    enlace.download = ejemplo.archivo
    enlace.click()
    URL.revokeObjectURL(url)
  }

  const erroresDeArchivo = vistaPrevia?.errores ?? []

  return (
    <section className="panel">
      <h1>{titulo}</h1>

      <p className={estilos.ayuda}>
        {children} Antes de guardar se muestra una previsualización y solo se importa si todas las filas son válidas.
      </p>

      {creados !== null && (
        <p className="mensaje-exito" role="status">
          Importación finalizada: se {creados === 1 ? 'creó' : 'crearon'} {cantidad(creados, singular, plural)}.
        </p>
      )}
      {error && <p className="mensaje-error" role="alert">{error.mensaje}</p>}

      <form className={estilos.formulario} onSubmit={previsualizar}>
        <label>
          Archivo CSV
          <input ref={selector} type="file" accept=".csv,text/csv" onChange={elegirArchivo} />
        </label>
        <div className={estilos.acciones}>
          <button type="submit" className="principal" disabled={!archivo || trabajando}>Previsualizar</button>
          <button type="button" onClick={descargarEjemplo}>Descargar archivo de ejemplo</button>
        </div>
      </form>

      {erroresDeArchivo.length > 0 && (
        <div className="mensaje-error" role="alert">
          <ul className={estilos.lista}>
            {erroresDeArchivo.map((e) => <li key={e}>{e}</li>)}
          </ul>
          <p className={estilos.pie}>Corrija el archivo y vuelva a cargarlo.</p>
        </div>
      )}

      {vistaPrevia && vistaPrevia.filas.length > 0 && (
        <>
          <h2>Previsualización</h2>
          <p className={estilos.resumen}>
            Se detectaron {cantidad(vistaPrevia.totalFilas, 'fila', 'filas')}: {cantidad(vistaPrevia.filasValidas, 'válida', 'válidas')} y{' '}
            {cantidad(vistaPrevia.filasConErrores, 'con errores', 'con errores')}.
          </p>
          {vistaPrevia.valido ? (
            <p className="mensaje-exito" role="status">Todas las filas son válidas: puede confirmar la importación.</p>
          ) : (
            <p className="mensaje-error" role="alert">
              Hay filas con errores, por lo que no se puede importar. Cancele, corrija el archivo y vuelva a cargarlo.
            </p>
          )}

          <div className="tabla-scroll">
            <table className="tabla">
              <thead>
                <tr>
                  <th>Fila</th>
                  {vistaPrevia.columnas.map((c) => <th key={c}>{c}</th>)}
                  <th>Resultado</th>
                </tr>
              </thead>
              <tbody>
                {vistaPrevia.filas.map((f) => (
                  <tr key={f.numero} className={f.errores.length > 0 ? estilos.conError : undefined}>
                    <td>{f.numero}</td>
                    {f.valores.map((v, i) => <td key={vistaPrevia.columnas[i]} className={estilos.valor}>{v}</td>)}
                    <td>
                      {f.errores.length === 0 ? (
                        <span className={estilos.valida}>Válida</span>
                      ) : (
                        <ul className={estilos.lista}>
                          {f.errores.map((e) => <li key={e}>{e}</li>)}
                        </ul>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className={estilos.acciones}>
            <button type="button" className="principal" disabled={!vistaPrevia.valido || trabajando} onClick={() => setConfirmando(true)}>
              Confirmar importación
            </button>
            <button type="button" disabled={trabajando} onClick={() => { reiniciar(); setError(null) }}>Cancelar</button>
          </div>
        </>
      )}

      {confirmando && (
        <Confirmacion
          titulo="Confirmar importación"
          textoConfirmar={trabajando ? 'Importando…' : 'Importar'}
          ocupado={trabajando}
          onConfirmar={importar}
          onCancelar={() => setConfirmando(false)}
        >
          <p>Se crearán {cantidad(vistaPrevia?.totalFilas ?? 0, singular, plural)}.</p>
        </Confirmacion>
      )}
    </section>
  )
}
