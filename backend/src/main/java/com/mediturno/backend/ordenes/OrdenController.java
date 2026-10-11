package com.mediturno.backend.ordenes;

import com.mediturno.backend.common.seguridad.UsuarioActual;
import com.mediturno.backend.ordenes.dto.OrdenResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de: CU-16.
 * Las rutas son las de los diagramas de secuencia; donde el diagrama no la indica, es una propuesta.
 */
@RestController
@RequestMapping("/api")
public class OrdenController {

    private final OrdenService ordenService;
    private final UsuarioActual usuarioActual;

    public OrdenController(OrdenService ordenService, UsuarioActual usuarioActual) {
        this.ordenService = ordenService;
        this.usuarioActual = usuarioActual;
    }

    @PostMapping("/reservas/{idReserva}/orden")
    @ResponseStatus(HttpStatus.CREATED)
    public OrdenResponseDTO emitirOrden(@PathVariable Long idReserva) {
        return ordenService.emitirOrden(idReserva, usuarioActual.obtenerIdFuncionarioAutenticado());
    }

    @GetMapping("/ordenes/{numeroOrden}/pdf")
    public ResponseEntity<byte[]> generarOrdenPDF(@PathVariable Long numeroOrden) {
        byte[] pdf = ordenService.generarOrdenPDF(numeroOrden);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).body(pdf);
    }
}
