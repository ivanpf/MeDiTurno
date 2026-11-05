// Cálculos de presentación del CU-6. Los anuncios vigentes ya los decide el backend en
// GET /api/anuncios; estos filtros repiten el mismo criterio como defensa en el cliente.

// «2026-10-28» (fecha local de hoy), para comparar con fechaInicio y fechaFin.
export function hoyISO(ahora = new Date()) {
  const dos = (n) => String(n).padStart(2, '0');
  return `${ahora.getFullYear()}-${dos(ahora.getMonth() + 1)}-${dos(ahora.getDate())}`;
}

// Publicados y dentro de su período (una fecha vacía no limita), más recientes primero.
export function anunciosVigentes(anuncios, hoy = hoyISO()) {
  return anuncios
    .filter((a) => a.estado === 'PUBLICADO')
    .filter((a) => (!a.fechaInicio || a.fechaInicio <= hoy) && (!a.fechaFin || hoy <= a.fechaFin))
    .sort((a, b) => b.fechaPublicacion.localeCompare(a.fechaPublicacion));
}

// Fotos del carrusel: la imagen de cada anuncio, con el título del anuncio al que pertenece.
// La uri ya viene armada por la fuente de datos, junto con las cabeceras de autenticación.
export function fotosDelCarrusel(anuncios) {
  return anuncios.flatMap((anuncio) => (anuncio.imagenes ?? [])
    .map((imagen) => ({
      clave: `${anuncio.id}-${imagen.idImagen ?? imagen.orden}`,
      uri: imagen.imagen,
      headers: imagen.headers,
      descripcion: imagen.textoAlternativo || anuncio.titulo,
      idAnuncio: anuncio.id,
      titulo: anuncio.titulo,
      fechaPublicacion: anuncio.fechaPublicacion,
    })));
}

// Después de la última foto vuelve a la primera (y antes de la primera, a la última).
export const indiceCircular = (indice, total) => (total ? ((indice % total) + total) % total : 0);

export function comoError(fallo, mensaje) {
  return {
    estado: fallo?.estado ?? 0,
    mensaje: fallo?.estado ? (fallo.mensaje || fallo.message || mensaje) : mensaje,
    detalles: fallo?.detalles ?? [],
  };
}
