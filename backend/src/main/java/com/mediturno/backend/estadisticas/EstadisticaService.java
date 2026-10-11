package com.mediturno.backend.estadisticas;

import java.util.List;

import com.mediturno.backend.common.dto.PeriodoDTO;
import com.mediturno.backend.common.exception.NoImplementadoException;
import com.mediturno.backend.estadisticas.dto.EstadisticaOcupacionDTO;
import com.mediturno.backend.estadisticas.dto.EstadisticaReservaDTO;
import com.mediturno.backend.estadisticas.dto.EstadisticaTurnoDTO;
import org.springframework.stereotype.Service;

/**
 * Lógica de negocio de: CU-40, CU-41, CU-42.
 * Los métodos todavía no están implementados: lanzan NoImplementadoException (respuesta 501).
 */
@Service
public class EstadisticaService {

    /** CU-40. */
    public List<EstadisticaReservaDTO> estadisticaReservasPorEspecialidad(PeriodoDTO periodo) {
        throw new NoImplementadoException();
    }

    /** CU-41. */
    public List<EstadisticaTurnoDTO> estadisticaTurnosMasDemandados(PeriodoDTO periodo) {
        throw new NoImplementadoException();
    }

    /** CU-42. */
    public List<EstadisticaOcupacionDTO> estadisticaOcupacionTurnos(PeriodoDTO periodo) {
        throw new NoImplementadoException();
    }
}
