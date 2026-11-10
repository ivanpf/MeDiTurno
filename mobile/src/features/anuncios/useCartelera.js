import { useCallback, useEffect, useRef, useState } from 'react';
import { getAuth } from 'firebase/auth';
import { listarCartelera } from '../../../shared/src/api/endpoints';
import { API_BASE } from '../../../shared/src/api/client';
import { anunciosVigentes, comoError, fotosDelCarrusel } from './cartelera';

const ERROR_CARTELERA = 'No fue posible cargar la cartelera de novedades. Intente nuevamente.';

// Token del usuario con sesión: los archivos de imagen del anuncio se piden autenticados.
async function tokenAutorizacion() {
  const usuario = getAuth().currentUser;
  if (!usuario) return null;
  try {
    return await usuario.getIdToken();
  } catch {
    return null;
  }
}

// El <Image> de React Native no puede enviar cabeceras (falla en la arquitectura nueva de
// Android, react-native#33131), así que bajamos el archivo con fetch autenticado — igual que
// hace la web — y lo entregamos al carrusel como data URI. Guardamos en memoria las fotos ya
// bajadas para no volver a pedirlas al retroceder y volver a la cartelera.
const fotosYaBajadas = new Map();

const BASE64_CHARS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';

function arrayBufferABase64(buffer) {
  const bytes = new Uint8Array(buffer);
  let resultado = '';
  let i = 0;
  const len = bytes.length;
  while (i < len) {
    const b1 = bytes[i++];
    const b2 = i < len ? bytes[i++] : -1;
    const b3 = i < len ? bytes[i++] : -1;
    const enc1 = b1 >> 2;
    const enc2 = ((b1 & 3) << 4) | (b2 === -1 ? 0 : b2 >> 4);
    const enc3 = b2 === -1 ? 64 : (b3 === -1 ? ((b2 & 15) << 2) : ((b2 & 15) << 2) | (b3 >> 6));
    const enc4 = b3 === -1 ? 64 : (b3 & 63);
    resultado += BASE64_CHARS.charAt(enc1) + BASE64_CHARS.charAt(enc2)
      + (enc3 === 64 ? '=' : BASE64_CHARS.charAt(enc3))
      + (enc4 === 64 ? '=' : BASE64_CHARS.charAt(enc4));
  }
  return resultado;
}

function bajarComoDataUri(uri, cabeceras) {
  return fetch(uri, { headers: cabeceras }).then(async (respuesta) => {
    if (!respuesta.ok) throw new Error(`imagen ${respuesta.status}`);
    const buffer = await respuesta.arrayBuffer();
    const tipo = respuesta.headers.get('content-type') || 'image/png';
    return `data:${tipo};base64,${arrayBufferABase64(buffer)}`;
  });
}

// AnuncioResponseDTO del backend → shape que consumen la pantalla y el carrusel.
// El backend entrega una imagen por anuncio (opcional); el archivo se baja autenticado
// con GET /api/anuncios/imagenes/{idImagen}.
function mapearAnuncio(anuncio, token) {
  const cabeceras = token ? { Authorization: `Bearer ${token}` } : undefined;
  return {
    id: anuncio.idAnuncio,
    titulo: anuncio.titulo,
    contenido: anuncio.contenido,
    fechaPublicacion: anuncio.fechaPublicacion,
    fechaInicio: anuncio.fechaInicio,
    fechaFin: anuncio.fechaFin,
    estado: anuncio.estado,
    imagenes: anuncio.imagen ? [{
      idImagen: anuncio.imagen.idImagen,
      orden: 0,
      imagen: `${API_BASE}/anuncios/imagenes/${anuncio.imagen.idImagen}`,
      textoAlternativo: anuncio.imagen.textoAlternativo,
      headers: cabeceras,
    }] : [],
  };
}

async function bajarFotos(anuncios) {
  await Promise.all(anuncios.flatMap((anuncio) => anuncio.imagenes.map(async (imagen) => {
    // Solo interesa bajar una vez por idImagen en esta sesión.
    if (fotosYaBajadas.has(imagen.idImagen)) {
      imagen.imagen = fotosYaBajadas.get(imagen.idImagen);
      return;
    }
    try {
      const dataUri = await bajarComoDataUri(imagen.imagen, imagen.headers);
      fotosYaBajadas.set(imagen.idImagen, dataUri);
      imagen.imagen = dataUri;
    } catch {
      // El archivo no se pudo bajar: la uri queda sin convertir y el carrusel
      // muestra su aviso "No se pudo cargar la imagen".
    }
  })));
}

// Fuente de los anuncios: la cartelera real que publica el backend (GET /api/anuncios).
export const obtenerAnuncios = async () => {
  const token = await tokenAutorizacion();
  const lista = await listarCartelera();
  const anuncios = (lista ?? []).map((anuncio) => mapearAnuncio(anuncio, token));
  await bajarFotos(anuncios);
  return anuncios;
};

// Estado del CU-6: los anuncios vigentes y las fotos que pasan en el carrusel.
export function useCarteleraNovedades(obtener = obtenerAnuncios) {
  const [anuncios, setAnuncios] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);

  // Evita actualizar el estado si se sale de la pantalla antes de que llegue la respuesta.
  const montado = useRef(false);

  const cargar = useCallback(async () => {
    setCargando(true);
    setError(null);
    try {
      const lista = await obtener();
      if (montado.current) setAnuncios(anunciosVigentes(lista ?? []));
    } catch (fallo) {
      if (montado.current) setError(comoError(fallo, ERROR_CARTELERA));
    } finally {
      if (montado.current) setCargando(false);
    }
  }, [obtener]);

  useEffect(() => {
    montado.current = true;
    cargar();
    return () => { montado.current = false; };
  }, [cargar]);

  return {
    anuncios,
    fotos: fotosDelCarrusel(anuncios),
    cargando,
    error,
    recargar: cargar,
    limpiarError: () => setError(null),
  };
}
