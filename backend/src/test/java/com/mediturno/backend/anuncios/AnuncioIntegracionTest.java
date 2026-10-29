package com.mediturno.backend.anuncios;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;

import com.mediturno.backend.BasePostgresEmbebida;
import com.mediturno.backend.anuncios.dto.AltaAnuncioDTO;
import com.mediturno.backend.anuncios.dto.AnuncioResponseDTO;
import com.mediturno.backend.anuncios.dto.ArchivoImagenDTO;
import com.mediturno.backend.anuncios.model.EstadoAnuncio;

/** CU-17 (publicación de anuncios) contra PostgreSQL. «Hoy» es el 14/10/2026. */
@SpringBootTest
class AnuncioIntegracionTest extends BasePostgresEmbebida {

    static final LocalDate HOY = LocalDate.of(2026, 10, 14);
    static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3};
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};

    @DynamicPropertySource
    static void base(DynamicPropertyRegistry registro) {
        registrarBaseDeDatos(registro, "anuncios");
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
        String codigo = UUID.randomUUID().toString().substring(0, 15);
        funcionario = jdbc.queryForObject("insert into usuario(correo,estado,fecha_creacion,cedula,nombre,apellido,fecha_nacimiento) "
            + "values (?, 'ACTIVO', CURRENT_TIMESTAMP, ?, 'Juan', 'Pérez', '1990-01-01') returning id_usuario",
            Long.class, codigo + "@prueba.com", codigo);
        jdbc.update("insert into funcionario(id_usuario) values (?)", funcionario);
    }

    AltaAnuncioDTO datos(LocalDate inicio, LocalDate fin) {
        return new AltaAnuncioDTO("Horario de verano", "Atendemos de 8 a 14 horas.", inicio, fin, null);
    }

    MockMultipartFile archivo(String nombre, byte[] contenido) {
        return new MockMultipartFile("imagen", nombre, "image/jpeg", contenido);
    }

    void rechazado(Runnable accion, String fragmento) {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, accion::run);
        assertEquals(400, e.getStatusCode().value());
        assertEquals(true, e.getReason().contains(fragmento), e.getReason());
    }

    long cantidadDeAnuncios() {
        return jdbc.queryForObject("select count(*) from anuncio", Long.class);
    }

    @Test
    void publicaUnAnuncioSinImagen() {
        AnuncioResponseDTO anuncio = anuncios.publicarAnuncio(funcionario, datos(null, HOY.plusDays(30)), null);

        assertEquals(EstadoAnuncio.PUBLICADO, anuncio.estado());
        assertEquals(HOY, anuncio.fechaInicio());
        assertEquals(HOY.plusDays(30), anuncio.fechaFin());
        assertNull(anuncio.imagen());
        assertEquals(funcionario, jdbc.queryForObject("select id_funcionario from anuncio where id_anuncio=?", Long.class, anuncio.idAnuncio()));
        assertEquals("PUBLICADO", jdbc.queryForObject("select anuncio_estado from anuncio where id_anuncio=?", String.class, anuncio.idAnuncio()));
    }

    @Test
    void sinFechaDeFinNoVenceYConFechaFuturaQuedaProgramado() {
        AnuncioResponseDTO anuncio = anuncios.publicarAnuncio(funcionario, datos(HOY.plusDays(5), null), null);

        assertEquals(HOY.plusDays(5), anuncio.fechaInicio());
        assertNull(anuncio.fechaFin());
    }

    @Test
    void guardaLaImagenYLaEntrega() {
        AltaAnuncioDTO alta = new AltaAnuncioDTO("Vacunación", "Abierta la campaña.", null, null, " Cartel azul ");

        AnuncioResponseDTO anuncio = anuncios.publicarAnuncio(funcionario, alta, archivo("a.jpg", JPG));

        assertEquals("a.jpg", anuncio.imagen().nombreArchivo());
        assertEquals("Cartel azul", anuncio.imagen().textoAlternativo());
        ArchivoImagenDTO guardada = anuncios.obtenerImagen(anuncio.imagen().idImagen());
        assertEquals("image/jpeg", guardada.tipoContenido());
        assertArrayEquals(JPG, guardada.datos());
    }

    @Test
    void aceptaPngYLoReconocePorElContenido() {
        AnuncioResponseDTO anuncio = anuncios.publicarAnuncio(funcionario, datos(null, null), archivo("foto.jpg", PNG));

        assertEquals("image/png", anuncios.obtenerImagen(anuncio.imagen().idImagen()).tipoContenido());
    }

    @Test
    void elFormatoSeDecidePorElContenidoNoPorElNombre() {
        rechazado(() -> anuncios.publicarAnuncio(funcionario, datos(null, null), archivo("falsa.jpg", "no soy una imagen".getBytes())),
            "debe ser JPG o PNG");
    }

    @Test
    void rechazaImagenesGrandesYVacias() {
        byte[] justa = new byte[5 * 1024 * 1024];
        System.arraycopy(JPG, 0, justa, 0, JPG.length);
        byte[] grande = new byte[5 * 1024 * 1024 + 1];
        System.arraycopy(JPG, 0, grande, 0, JPG.length);

        assertNotNull(anuncios.publicarAnuncio(funcionario, datos(null, null), archivo("justa.jpg", justa)).imagen());
        rechazado(() -> anuncios.publicarAnuncio(funcionario, datos(null, null), archivo("grande.jpg", grande)), "tamaño máximo de 5 MB");
        rechazado(() -> anuncios.publicarAnuncio(funcionario, datos(null, null), archivo("vacia.jpg", new byte[0])), "está vacía");
    }

    @Test
    void unaImagenInvalidaNoDejaElAnuncioGuardado() {
        long antes = cantidadDeAnuncios();

        assertThrows(ResponseStatusException.class, () -> anuncios.publicarAnuncio(funcionario, datos(null, null), archivo("x.jpg", new byte[] {1, 2})));

        assertEquals(antes, cantidadDeAnuncios());
    }

    @Test
    void validaLaVigencia() {
        rechazado(() -> anuncios.publicarAnuncio(funcionario, datos(HOY.minusDays(1), null), null), "anterior a hoy");
        rechazado(() -> anuncios.publicarAnuncio(funcionario, datos(HOY.plusDays(5), HOY.plusDays(4)), null), "fin no puede ser anterior");
        rechazado(() -> anuncios.publicarAnuncio(funcionario, datos(null, HOY.minusDays(1)), null), "fin no puede ser anterior");
    }

    @Test
    void elDiaDeInicioYElDeFinPuedenSerElMismo() {
        AnuncioResponseDTO anuncio = anuncios.publicarAnuncio(funcionario, datos(HOY, HOY), null);

        assertEquals(HOY, anuncio.fechaFin());
    }

    @Test
    void noEntregaLaImagenDeUnAnuncioEliminado() {
        AnuncioResponseDTO anuncio = anuncios.publicarAnuncio(funcionario, datos(null, null), archivo("a.jpg", JPG));
        Long idImagen = anuncio.imagen().idImagen();
        jdbc.update("update anuncio set anuncio_estado='ELIMINADO' where id_anuncio=?", anuncio.idAnuncio());

        assertEquals(404, assertThrows(ResponseStatusException.class, () -> anuncios.obtenerImagen(idImagen)).getStatusCode().value());
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> anuncios.obtenerImagen(-1L)).getStatusCode().value());
    }
}
