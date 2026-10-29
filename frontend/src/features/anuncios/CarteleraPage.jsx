import { useEffect, useState } from 'react'
import { api, ApiError } from '../../services/api'
import ImagenAnuncio from './ImagenAnuncio'
import estilos from './CarteleraPage.module.css'

const formatearFecha = (fecha) => new Date(fecha).toLocaleDateString('es-UY')

// CU-6 Cartelera de novedades.
export default function CarteleraPage() {
  const [anuncios, setAnuncios] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelado = false
    api.get('/anuncios')
      .then((datos) => !cancelado && setAnuncios(datos))
      .catch((err) => !cancelado && setError(err instanceof ApiError ? err : new ApiError(0, 'No fue posible cargar la cartelera. Intente nuevamente.')))
    return () => {
      cancelado = true
    }
  }, [])

  return (
    <section className="panel">
      <h1>Cartelera de novedades</h1>

      {error && <p className="mensaje-error" role="alert">{error.mensaje}</p>}
      {!error && anuncios === null && <p role="status">Cargando…</p>}
      {anuncios?.length === 0 && <p className="pendiente">No hay anuncios vigentes por el momento.</p>}

      <div className={estilos.lista}>
        {anuncios?.map((anuncio) => (
          <article key={anuncio.idAnuncio} className={estilos.anuncio}>
            <h2>{anuncio.titulo}</h2>
            <p className={estilos.fecha}>Publicado el {formatearFecha(anuncio.fechaPublicacion)}</p>
            {anuncio.imagen && (
              <ImagenAnuncio
                idImagen={anuncio.imagen.idImagen}
                textoAlternativo={anuncio.imagen.textoAlternativo}
                className={estilos.imagen}
              />
            )}
            <p className={estilos.contenido}>{anuncio.contenido}</p>
          </article>
        ))}
      </div>
    </section>
  )
}
