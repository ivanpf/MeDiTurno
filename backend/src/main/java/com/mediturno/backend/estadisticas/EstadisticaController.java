package com.mediturno.backend.estadisticas;

import java.util.List;

import com.mediturno.backend.common.dto.PeriodoDTO;
import com.mediturno.backend.estadisticas.dto.EstadisticaOcupacionDTO;
import com.mediturno.backend.estadisticas.dto.EstadisticaReservaDTO;
import com.mediturno.backend.estadisticas.dto.EstadisticaTurnoDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.mediturno.backend.common.seguridad.RolesPermitidos;
import com.mediturno.backend.usuarios.model.Administrador;

/**
 * Endpoints de: CU-40, CU-41, CU-42.
 * Las rutas son las de los diagramas de secuencia; donde el diagrama no la indica, es una propuesta.
 */
@RolesPermitidos(Administrador.class)
@RestController
@RequestMapping("/api/estadisticas")
public class EstadisticaController {

    private final EstadisticaService estadisticaService;

    public EstadisticaController(EstadisticaService estadisticaService) {
        this.estadisticaService = estadisticaService;
    }

    @GetMapping("/reservas-por-especialidad")
    public List<EstadisticaReservaDTO> estadisticaReservasPorEspecialidad(PeriodoDTO periodo) {
        return estadisticaService.estadisticaReservasPorEspecialidad(periodo);
    }

    @GetMapping("/turnos-mas-demandados")
    public List<EstadisticaTurnoDTO> estadisticaTurnosMasDemandados(PeriodoDTO periodo) {
        return estadisticaService.estadisticaTurnosMasDemandados(periodo);
    }

    @GetMapping("/ocupacion-turnos")
    public List<EstadisticaOcupacionDTO> estadisticaOcupacionTurnos(PeriodoDTO periodo) {
        return estadisticaService.estadisticaOcupacionTurnos(periodo);
    }
}
