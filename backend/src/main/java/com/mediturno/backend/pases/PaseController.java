package com.mediturno.backend.pases;

import java.util.List;

import com.mediturno.backend.common.dto.CriteriosBusquedaDTO;
import com.mediturno.backend.common.seguridad.UsuarioActual;
import com.mediturno.backend.pases.dto.EmitirPaseDTO;
import com.mediturno.backend.pases.dto.PaseResponseDTO;
import com.mediturno.backend.pases.dto.ReconsultaDTO;
import jakarta.validation.Valid; // <-- Agregamos el import de la validación
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.mediturno.backend.common.seguridad.RolesPermitidos;
import com.mediturno.backend.usuarios.model.Medico;

/**
 * Endpoints de: CU-24, CU-25, CU-27.
 * Las rutas son las de los diagramas de secuencia; donde el diagrama no la indica, es una propuesta.
 */
@RolesPermitidos(Medico.class)
@RestController
@RequestMapping("/api")
public class PaseController {

    private final PaseService paseService;
    private final UsuarioActual usuarioActual;

    public PaseController(PaseService paseService, UsuarioActual usuarioActual) {
        this.paseService = paseService;
        this.usuarioActual = usuarioActual;
    }

    @PostMapping("/consultas/{idConsulta}/pases")
    @ResponseStatus(HttpStatus.CREATED)
    public PaseResponseDTO emitirPase(
            @PathVariable Long idConsulta, 
            @Valid @RequestBody EmitirPaseDTO datos) { // <-- Agregamos @Valid aquí
        
        return paseService.emitirPase(usuarioActual.obtenerIdMedicoAutenticado(), idConsulta, datos);
    }

    @PostMapping("/consultas/{idConsulta}/reconsultas")
    @ResponseStatus(HttpStatus.CREATED)
    public PaseResponseDTO habilitarReconsulta(@PathVariable Long idConsulta, @RequestBody ReconsultaDTO datos) {
        return paseService.habilitarReconsulta(usuarioActual.obtenerIdMedicoAutenticado(), idConsulta, datos);
    }

    @GetMapping("/pases")
    public List<PaseResponseDTO> buscarPasesEmitidos(CriteriosBusquedaDTO criterios) {
        return paseService.buscarPasesEmitidos(usuarioActual.obtenerIdMedicoAutenticado(), criterios);
    }
}