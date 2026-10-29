// Anuncios de prueba del CU-6 mientras GET /api/anuncios no está implementado en el backend
// (AnuncioResponseDTO todavía no tiene campos). La forma sigue las tablas anuncio e imagen_anuncio:
// cada anuncio trae sus imágenes con el orden y el texto alternativo.
// Las fotos son de muestra (picsum.photos); con el backend llegará la URL guardada en imagen_anuncio.imagen.

const foto = (semilla) => `https://picsum.photos/seed/mediturno-${semilla}/960/540`;

export const ANUNCIOS_DEMO = [
  {
    id: 1,
    titulo: 'Campaña de vacunación antigripal',
    contenido: 'Del 1 al 30 de noviembre se aplica la vacuna antigripal de lunes a viernes de 8:00 a 12:00 '
      + 'en el vacunatorio de planta baja. Presente su cédula de identidad; no hace falta reservar.',
    fechaPublicacion: '2026-10-26T09:00:00',
    fechaInicio: '2026-10-26',
    fechaFin: null,
    estado: 'PUBLICADO',
    autor: 'Martínez, Ana',
    imagenes: [
      { imagen: foto('vacunacion'), orden: 1, textoAlternativo: 'Enfermera preparando la vacuna antigripal' },
      { imagen: foto('vacunatorio'), orden: 2, textoAlternativo: 'Sala de espera del vacunatorio' },
    ],
  },
  {
    id: 2,
    titulo: 'Nuevo consultorio de cardiología',
    contenido: 'Desde noviembre se suma un consultorio de cardiología en el primer piso, con turnos de mañana '
      + 'y de tarde. Puede reservar desde la aplicación en «Reservar consulta».',
    fechaPublicacion: '2026-10-22T14:30:00',
    fechaInicio: null,
    fechaFin: null,
    estado: 'PUBLICADO',
    autor: 'Rodríguez, Pablo',
    imagenes: [
      { imagen: foto('cardiologia'), orden: 1, textoAlternativo: 'Consultorio de cardiología del primer piso' },
    ],
  },
  {
    id: 3,
    titulo: 'Horario especial por feriado',
    contenido: 'El lunes 2 de noviembre la policlínica atiende solo urgencias. Las reservas de ese día se '
      + 'reprogramaron y cada paciente recibió un aviso por correo.',
    fechaPublicacion: '2026-10-20T10:00:00',
    fechaInicio: null,
    fechaFin: '2026-11-02',
    estado: 'PUBLICADO',
    autor: 'Martínez, Ana',
    imagenes: [],
  },
  {
    id: 4,
    titulo: 'Charla sobre alimentación saludable',
    contenido: 'El jueves 12 de noviembre a las 18:00, en el salón de actos, la nutricionista del centro dará '
      + 'una charla abierta sobre alimentación saludable. La entrada es libre.',
    fechaPublicacion: '2026-10-18T11:15:00',
    fechaInicio: null,
    fechaFin: '2026-11-12',
    estado: 'PUBLICADO',
    autor: 'Rodríguez, Pablo',
    imagenes: [
      { imagen: foto('nutricion'), orden: 1, textoAlternativo: 'Frutas y verduras de estación' },
    ],
  },
  // Estos dos no se muestran: uno se eliminó (CU-18) y el otro ya terminó su período de publicación.
  {
    id: 5,
    titulo: 'Corte de agua programado',
    contenido: 'Anuncio eliminado.',
    fechaPublicacion: '2026-10-10T08:00:00',
    fechaInicio: null,
    fechaFin: null,
    estado: 'ELIMINADO',
    autor: 'Martínez, Ana',
    imagenes: [],
  },
  {
    id: 6,
    titulo: 'Jornada de donación de sangre',
    contenido: 'La jornada de donación de sangre se realizó el 15 de setiembre.',
    fechaPublicacion: '2026-09-01T09:00:00',
    fechaInicio: '2026-09-01',
    fechaFin: '2026-09-15',
    estado: 'PUBLICADO',
    autor: 'Rodríguez, Pablo',
    imagenes: [
      { imagen: foto('donacion'), orden: 1, textoAlternativo: 'Donante en el banco de sangre' },
    ],
  },
];
