import { useEffect, useState } from 'react'
import { api } from '../../services/api'

// Muestra la imagen de un anuncio. Un <img src="/api/..."> no manda el token de sesión, así que el archivo se pide con
// api.descargar y se muestra con una URL temporal del navegador, que se libera al cambiar o salir.
export default function ImagenAnuncio({ idImagen, textoAlternativo, className }) {
  const [vista, setVista] = useState(null)

  useEffect(() => {
    let cancelado = false
    let url = null
    api.descargar(`/anuncios/imagenes/${idImagen}`)
      .then((archivo) => {
        if (cancelado) return
        url = URL.createObjectURL(archivo)
        setVista(url)
      })
      .catch(() => {}) // si no se pudo cargar, el anuncio se ve igual, sin la imagen
    return () => {
      cancelado = true
      if (url) URL.revokeObjectURL(url)
    }
  }, [idImagen])

  if (!vista) return null
  return <img src={vista} alt={textoAlternativo ?? ''} className={className} />
}
