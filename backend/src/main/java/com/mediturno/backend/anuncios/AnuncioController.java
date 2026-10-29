package com.mediturno.backend.anuncios;

import java.time.Duration;
import java.util.List;

import com.mediturno.backend.anuncios.dto.AltaAnuncioDTO;
import com.mediturno.backend.anuncios.dto.AnuncioResponseDTO;
import com.mediturno.backend.anuncios.dto.ArchivoImagenDTO;
import com.mediturno.backend.common.seguridad.RolesPermitidos;
import com.mediturno.backend.common.seguridad.UsuarioActual;
import com.mediturno.backend.usuarios.model.Funcionario;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Endpoints de: CU-6, CU-17, CU-18.
 * Las rutas son las de los diagramas de secuencia; donde el diagrama no la indica, es una propuesta.
 */
@RolesPermitidos(Funcionario.class)
@RestController
@RequestMapping("/api/anuncios")
public class AnuncioController {

    private final AnuncioService anuncioService;
    private final UsuarioActual usuarioActual;

    public AnuncioController(AnuncioService anuncioService, UsuarioActual usuarioActual) {
        this.anuncioService = anuncioService;
        this.usuarioActual = usuarioActual;
    }

    @RolesPermitidos
    @GetMapping
    public List<AnuncioResponseDTO> listarCartelera() {
        return anuncioService.listarCartelera();
    }

    /** Multipart: la parte «datos» lleva el anuncio en JSON y la parte «imagen», el archivo (opcional). */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AnuncioResponseDTO publicarAnuncio(@RequestPart("datos") @Valid AltaAnuncioDTO datos,
            @RequestPart(value = "imagen", required = false) MultipartFile imagen) {
        return anuncioService.publicarAnuncio(usuarioActual.obtenerIdFuncionarioAutenticado(), datos, imagen);
    }

    /** El archivo de una imagen. Lo usan la cartelera (CU-6) y la vista previa de un anuncio. */
    @RolesPermitidos
    @GetMapping("/imagenes/{idImagen}")
    public ResponseEntity<byte[]> obtenerImagen(@PathVariable Long idImagen) {
        ArchivoImagenDTO imagen = anuncioService.obtenerImagen(idImagen);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(imagen.tipoContenido()))
            .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePrivate())
            .body(imagen.datos());
    }

    /** CU-18: todos los anuncios que hubo (también los eliminados), con su estado y su situación respecto de hoy. */
    @GetMapping("/gestion")
    public List<AnuncioResponseDTO> listarAnunciosParaGestion() {
        return anuncioService.listarAnunciosParaGestion();
    }

    @DeleteMapping("/{idAnuncio}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarAnuncio(@PathVariable Long idAnuncio) {
        anuncioService.eliminarAnuncio(idAnuncio);
    }
}
