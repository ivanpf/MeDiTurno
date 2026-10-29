package com.mediturno.backend.anuncios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;

import com.mediturno.backend.BasePostgresEmbebida;
import com.mediturno.backend.anuncios.dto.AnuncioResponseDTO;
import com.mediturno.backend.anuncios.model.EstadoAnuncio;
import com.mediturno.backend.anuncios.model.VigenciaAnuncio;

/** CU-6 (cartelera de novedades) y CU-18 (eliminar anuncios) contra PostgreSQL. «Hoy» es el 14/10/2026. */
@SpringBootTest
class CarteleraIntegracionTest extends BasePostgresEmbebida {

    static final LocalDate HOY = LocalDate.of(2026, 10, 14);

    @DynamicPropertySource
    static void base(DynamicPropertyRegistry registro) {
        registrarBaseDeDatos(registro, "cartelera");
        registro.add("spring.jpa.show-sql", () -> "false");
    }

    @TestConfiguration
    static class Configuracion {
        @Bean @Primary Clock relojPrueba() {
            return Clock.fixed(Instant.parse("2026-10-14T12:00:00Z"), ZoneId.of("America/Montevideo"));
        }
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired AnuncioService anuncios;

    Long funcionario;

    @BeforeEach
    void preparar() {
        jdbc.update("delete from imagen_anuncio");
        jdbc.update("delete from anuncio");
        String codigo = UUID.randomUUID().toString().substring(0, 15);
        funcionario = jdbc.queryForObject("insert into usuario(correo,estado,fecha_creacion,cedula,nombre,apellido,fecha_nacimiento) "
            + "values (?, 'ACTIVO', CURRENT_TIMESTAMP, ?, 'Juan', 'Pérez', '1990-01-01') returning id_usuario",
            Long.class, codigo + "@prueba.com", codigo);
        jdbc.update("insert into funcionario(id_usuario) values (?)", funcionario);
    }

    Long anuncio(String titulo, String estado, LocalDate inicio, LocalDate fin, LocalDateTime publicacion) {
        return jdbc.queryForObject("insert into anuncio(id_funcionario,titulo,contenido,fecha_creacion,fecha_publicacion,fecha_inicio,fecha_fin,anuncio_estado) "
            + "values (?,?,'Contenido de ' || ?,?,?,?,?,?) returning id_anuncio",
            Long.class, funcionario, titulo, titulo, publicacion, publicacion, inicio, fin, estado);
    }

    Long imagen(Long idAnuncio) {
        return jdbc.queryForObject("insert into imagen_anuncio(id_anuncio,datos,nombre_archivo,tipo_contenido,texto_alternativo) "
            + "values (?, decode('89504e470d0a1a0a', 'hex'), 'foto.png', 'image/png', 'Una foto') returning id_imagen_anuncio", Long.class, idAnuncio);
    }

    List<String> titulos() {
        return anuncios.listarCartelera().stream().map(AnuncioResponseDTO::titulo).toList();
    }

    @Test
    void muestraSoloLosPublicadosQueSeVenHoy() {
        LocalDateTime ayer = HOY.minusDays(1).atTime(10, 0);
        anuncio("Vigente", "PUBLICADO", HOY.minusDays(3), HOY.plusDays(10), ayer);
        anuncio("Programado", "PUBLICADO", HOY.plusDays(1), HOY.plusDays(10), ayer);
        anuncio("Vencido", "PUBLICADO", HOY.minusDays(10), HOY.minusDays(1), ayer);
        anuncio("Eliminado", "ELIMINADO", HOY.minusDays(3), HOY.plusDays(10), ayer);

        assertEquals(List.of("Vigente"), titulos());
    }

    @Test
    void elDiaDeInicioYElDeFinEstanIncluidos() {
        LocalDateTime ayer = HOY.minusDays(1).atTime(10, 0);
        anuncio("Empieza hoy", "PUBLICADO", HOY, HOY.plusDays(5), ayer);
        anuncio("Termina hoy", "PUBLICADO", HOY.minusDays(5), HOY, ayer);

        assertEquals(2, titulos().size());
    }

    @Test
    void sinFechaDeFinNoVence() {
        anuncio("Sin fin", "PUBLICADO", HOY.minusDays(100), null, HOY.minusDays(100).atTime(9, 0));

        assertEquals(List.of("Sin fin"), titulos());
    }

    @Test
    void elMasNuevoVaPrimero() {
        anuncio("Viejo", "PUBLICADO", HOY.minusDays(9), null, HOY.minusDays(9).atTime(9, 0));
        anuncio("Nuevo", "PUBLICADO", HOY.minusDays(1), null, HOY.minusDays(1).atTime(9, 0));
        anuncio("Medio", "PUBLICADO", HOY.minusDays(4), null, HOY.minusDays(4).atTime(9, 0));

        assertEquals(List.of("Nuevo", "Medio", "Viejo"), titulos());
    }

    @Test
    void traeLosDatosCompletosYLaImagenSoloSiLaTiene() {
        Long conImagen = anuncio("Con imagen", "PUBLICADO", HOY, null, HOY.minusDays(1).atTime(9, 30));
        anuncio("Sin imagen", "PUBLICADO", HOY, null, HOY.minusDays(2).atTime(9, 30));
        Long idImagen = imagen(conImagen);

        List<AnuncioResponseDTO> cartelera = anuncios.listarCartelera();

        AnuncioResponseDTO primero = cartelera.get(0);
        assertEquals("Con imagen", primero.titulo());
        assertEquals("Contenido de Con imagen", primero.contenido());
        assertEquals(HOY.minusDays(1).atTime(9, 30), primero.fechaPublicacion());
        assertEquals(idImagen, primero.imagen().idImagen());
        assertEquals("foto.png", primero.imagen().nombreArchivo());
        assertEquals("Una foto", primero.imagen().textoAlternativo());
        assertNull(cartelera.get(1).imagen());
    }

    @Test
    void sinAnunciosDevuelveUnaListaVacia() {
        assertEquals(List.of(), anuncios.listarCartelera());
    }

    @Test
    void siSePublicaAhoraSeVeEnLaCartelera() {
        anuncios.publicarAnuncio(funcionario,
            new com.mediturno.backend.anuncios.dto.AltaAnuncioDTO("Recién publicado", "Texto", null, null, null), null);

        assertEquals(List.of("Recién publicado"), titulos());
    }

    // ------------------------------------------------------------------ CU-18

    @Test
    void eliminarUnAnuncioLoSacaDeLaCartelera() {
        Long id = anuncio("Se elimina", "PUBLICADO", HOY, null, HOY.minusDays(1).atTime(9, 0));
        anuncio("Se queda", "PUBLICADO", HOY, null, HOY.minusDays(2).atTime(9, 0));

        anuncios.eliminarAnuncio(id);

        assertEquals(List.of("Se queda"), titulos());
        assertEquals("ELIMINADO", jdbc.queryForObject("select anuncio_estado from anuncio where id_anuncio=?", String.class, id));
    }

    @Test
    void eliminarConservaElRegistroYDejaDeEntregarLaImagen() {
        Long id = anuncio("Con imagen", "PUBLICADO", HOY, null, HOY.minusDays(1).atTime(9, 0));
        Long idImagen = imagen(id);
        assertEquals("image/png", anuncios.obtenerImagen(idImagen).tipoContenido());

        anuncios.eliminarAnuncio(id);

        assertEquals(1L, jdbc.queryForObject("select count(*) from anuncio where id_anuncio=?", Long.class, id));
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> anuncios.obtenerImagen(idImagen)).getStatusCode().value());
    }

    @Test
    void sePuedeEliminarUnAnuncioProgramadoOVencido() {
        Long programado = anuncio("Programado", "PUBLICADO", HOY.plusDays(3), null, HOY.minusDays(1).atTime(9, 0));
        Long vencido = anuncio("Vencido", "PUBLICADO", HOY.minusDays(9), HOY.minusDays(2), HOY.minusDays(9).atTime(9, 0));

        anuncios.eliminarAnuncio(programado);
        anuncios.eliminarAnuncio(vencido);

        assertEquals(List.of(), titulos());
        assertEquals(List.of(EstadoAnuncio.ELIMINADO, EstadoAnuncio.ELIMINADO),
            anuncios.listarAnunciosParaGestion().stream().map(AnuncioResponseDTO::estado).toList());
    }

    @Test
    void eliminarDosVecesOUnoInexistenteFalla() {
        Long id = anuncio("Uno", "PUBLICADO", HOY, null, HOY.minusDays(1).atTime(9, 0));
        anuncios.eliminarAnuncio(id);

        assertEquals(409, assertThrows(ResponseStatusException.class, () -> anuncios.eliminarAnuncio(id)).getStatusCode().value());
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> anuncios.eliminarAnuncio(-1L)).getStatusCode().value());
    }

    @Test
    void elListadoDeGestionTraeTodosLosAnunciosTambienLosEliminados() {
        LocalDateTime ayer = HOY.minusDays(1).atTime(10, 0);
        anuncio("Vigente", "PUBLICADO", HOY.minusDays(3), HOY.plusDays(10), ayer.minusHours(1));
        anuncio("Programado", "PUBLICADO", HOY.plusDays(1), null, ayer);
        anuncio("Vencido", "PUBLICADO", HOY.minusDays(10), HOY.minusDays(1), ayer.minusHours(2));
        anuncio("Eliminado", "ELIMINADO", HOY.minusDays(3), null, ayer);

        List<AnuncioResponseDTO> lista = anuncios.listarAnunciosParaGestion();

        assertEquals(List.of("Eliminado", "Programado", "Vigente", "Vencido"), lista.stream().map(AnuncioResponseDTO::titulo).toList());
        assertEquals(List.of(EstadoAnuncio.ELIMINADO, EstadoAnuncio.PUBLICADO, EstadoAnuncio.PUBLICADO, EstadoAnuncio.PUBLICADO),
            lista.stream().map(AnuncioResponseDTO::estado).toList());
        assertEquals(List.of(VigenciaAnuncio.PROGRAMADO, VigenciaAnuncio.VIGENTE, VigenciaAnuncio.VENCIDO),
            lista.stream().skip(1).map(AnuncioResponseDTO::vigencia).toList());
    }
}
