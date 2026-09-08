package com.mediturno.backend;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Listado de turnos, de a pedazos.
 *
 * El scroll infinito no puede pedir los turnos de una sola vez: con miles de
 * filas la app tardaria y consumiria memoria de mas. Se pide una pagina, y
 * cuando el usuario llega al final se pide la siguiente.
 */
@RestController
@RequestMapping("/api/turnos")
@CrossOrigin
public class TurnoController {

    /** Tope para que nadie pida 100000 turnos de un saque. */
    private static final int TAMANIO_MAXIMO = 50;

    private final TurnoRepository turnos;

    public TurnoController(TurnoRepository turnos) {
        this.turnos = turnos;
    }

    /**
     * Respuesta con forma propia en vez de devolver Page directamente.
     *
     * El JSON de Page cambia entre versiones de Spring; definiendo el record
     * acá, el contrato con la app queda fijo y no se rompe al actualizar.
     */
    public record PaginaTurnos(
            List<Turno> contenido,
            int pagina,
            int totalPaginas,
            long totalElementos,
            boolean ultima) {
    }

    @GetMapping
    public PaginaTurnos listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int pagina = Math.max(page, 0);
        int tamanio = Math.min(Math.max(size, 1), TAMANIO_MAXIMO);

        Page<Turno> resultado = turnos.findAll(
                PageRequest.of(pagina, tamanio, Sort.by("fechaHora").ascending()));

        return new PaginaTurnos(
                resultado.getContent(),
                resultado.getNumber(),
                resultado.getTotalPages(),
                resultado.getTotalElements(),
                resultado.isLast());
    }
}
