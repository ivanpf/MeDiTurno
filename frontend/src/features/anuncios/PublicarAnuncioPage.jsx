import { useEffect, useMemo, useState } from 'react'
import { api, ApiError } from '../../services/api'
import estilos from './PublicarAnuncioPage.module.css'

// Los mismos límites que el backend (mediturno.anuncios en application.yaml); el backend vuelve a comprobarlos.
const MAXIMO_BYTES = 5 * 1024 * 1024
const TIPOS = ['image/jpeg', 'image/png']
const soloMensaje = (detalle) => detalle.split(': ').slice(1).join(': ') || detalle

// La fecha de hoy en Montevideo (aaaa-mm-dd), como la usa el backend.
const hoy = () => new Date().toLocaleDateString('en-CA', { timeZone: 'America/Montevideo' })
const formularioVacio = () => ({ titulo: '', contenido: '', fechaInicio: hoy(), fechaFin: '' })

// CU-17 Publicar anuncio.
export default function PublicarAnuncioPage() {
  const [datos, setDatos] = useState(formularioVacio)
  const [archivo, setArchivo] = useState(null)
  const [textoAlternativo, setTextoAlternativo] = useState('')
  const [error, setError] = useState(null)
  const [publicado, setPublicado] = useState(false)
  const [enviando, setEnviando] = useState(false)

  // Libera la vista previa (URL temporal del navegador) cuando cambia la imagen o se sale de la pantalla.
  const vista = useMemo(() => (archivo ? URL.createObjectURL(archivo) : null), [archivo])
  useEffect(() => (vista ? () => URL.revokeObjectURL(vista) : undefined), [vista])

  const cambiar = (e) => {
    setDatos({ ...datos, [e.target.name]: e.target.value })
    setPublicado(false)
  }

  const elegirImagen = (e) => {
    const elegido = e.target.files[0]
    e.target.value = '' // permite volver a elegir el mismo archivo
    setPublicado(false)
    if (!elegido) return
    if (!TIPOS.includes(elegido.type)) {
      setError(new ApiError(400, `La imagen «${elegido.name}» debe ser JPG o PNG.`))
      return
    }
    if (elegido.size > MAXIMO_BYTES) {
      setError(new ApiError(400, `La imagen «${elegido.name}» supera el tamaño máximo de 5 MB.`))
      return
    }
    setError(null)
    setArchivo(elegido)
    setTextoAlternativo('')
  }

  const quitarImagen = () => {
    setArchivo(null)
    setTextoAlternativo('')
  }

  const limpiar = () => {
    setDatos(formularioVacio())
    quitarImagen()
    setError(null)
    setPublicado(false)
  }

  const publicar = async (e) => {
    e.preventDefault()
    if (enviando) return
    setError(null)
    setPublicado(false)
    if (!datos.titulo.trim() || !datos.contenido.trim()) {
      setError(new ApiError(400, 'Complete los campos obligatorios.'))
      return
    }
    if (datos.fechaFin && datos.fechaFin < datos.fechaInicio) {
      setError(new ApiError(400, 'La fecha de fin no puede ser anterior a la de inicio.'))
      return
    }
    const formulario = new FormData()
    const anuncio = {
      titulo: datos.titulo.trim(),
      contenido: datos.contenido.trim(),
      fechaInicio: datos.fechaInicio || null,
      fechaFin: datos.fechaFin || null,
      textoAlternativo: textoAlternativo.trim() || null,
    }
    formulario.append('datos', new Blob([JSON.stringify(anuncio)], { type: 'application/json' }))
    if (archivo) formulario.append('imagen', archivo)

    setEnviando(true)
    try {
      await api.postForm('/anuncios', formulario)
      limpiar()
      setPublicado(true)
    } catch (err) {
      // Conserva lo escrito para que pueda corregirlo.
      setError(err instanceof ApiError ? err : new ApiError(0, 'No fue posible publicar el anuncio. Intente nuevamente.'))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <section className="panel">
      <h1>Publicar anuncio</h1>

      <div className={estilos.mensajes}>
        {publicado && <p className="mensaje-exito" role="status">Anuncio publicado correctamente.</p>}
        {error && (
          <div className="mensaje-error" role="alert">
            {error.mensaje}
            {error.detalles.length > 0 && (
              <ul>
                {error.detalles.map((detalle) => <li key={detalle}>{soloMensaje(detalle)}</li>)}
              </ul>
            )}
          </div>
        )}
      </div>

      <form className={estilos.formulario} onSubmit={publicar}>
        <fieldset className={estilos.campos} disabled={enviando}>
          <label>
            Título *
            <input name="titulo" value={datos.titulo} onChange={cambiar} maxLength={200} required />
          </label>
          <label>
            Contenido *
            <textarea name="contenido" value={datos.contenido} onChange={cambiar} maxLength={5000} rows={6} required />
          </label>
          <div className={estilos.fila}>
            <label>
              Visible desde
              <input type="date" name="fechaInicio" value={datos.fechaInicio} min={hoy()} onChange={cambiar} required />
            </label>
            <label>
              Visible hasta (opcional)
              <input type="date" name="fechaFin" value={datos.fechaFin} min={datos.fechaInicio} onChange={cambiar} />
            </label>
          </div>
          <p className={estilos.ayuda}>Si la fecha de inicio es posterior a hoy, el anuncio queda programado. Sin fecha de fin, no vence.</p>

          <label>
            Imagen (opcional)
            <input type="file" accept="image/jpeg,image/png" onChange={elegirImagen} />
          </label>
          <p className={estilos.ayuda}>JPG o PNG, de hasta 5 MB.</p>

          {archivo && (
            <div className={estilos.imagen}>
              <img src={vista} alt="" className={estilos.miniatura} />
              <div className={estilos.detalleImagen}>
                <span className={estilos.nombre}>{archivo.name}</span>
                <label>
                  Texto alternativo (describe la imagen)
                  <input value={textoAlternativo} onChange={(e) => setTextoAlternativo(e.target.value)} maxLength={255} />
                </label>
              </div>
              <button type="button" onClick={quitarImagen}>Quitar</button>
            </div>
          )}

          <div className={estilos.acciones}>
            <button type="submit" className="principal">{enviando ? 'Publicando…' : 'Publicar'}</button>
            <button type="button" onClick={limpiar}>Cancelar</button>
          </div>
        </fieldset>
      </form>
    </section>
  )
}
