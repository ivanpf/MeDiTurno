package com.mediturno.backend.anuncios;

import java.util.List;

import com.mediturno.backend.anuncios.dto.AltaAnuncioDTO;
import com.mediturno.backend.anuncios.dto.AnuncioResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.mediturno.backend.common.seguridad.RolesPermitidos;
import com.mediturno.backend.usuarios.model.Funcionario;

/**
 * Endpoints de: CU-6, CU-17, CU-18.
 * Las rutas son las de los diagramas de secuencia; donde el diagrama no la indica, es una propuesta.
 */
@RolesPermitidos(Funcionario.class)
@RestController
@RequestMapping("/api/anuncios")
public class AnuncioController {

    private final AnuncioService anuncioService;

    public AnuncioController(AnuncioService anuncioService) {
        this.anuncioService = anuncioService;
    }

    @RolesPermitidos
    @GetMapping
    public List<AnuncioResponseDTO> listarCartelera() {
        return anuncioService.listarCartelera();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AnuncioResponseDTO publicarAnuncio(@RequestBody AltaAnuncioDTO datos) {
        return anuncioService.publicarAnuncio(datos);
    }

    @DeleteMapping("/{idAnuncio}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarAnuncio(@PathVariable Long idAnuncio) {
        anuncioService.eliminarAnuncio(idAnuncio);
    }
}
