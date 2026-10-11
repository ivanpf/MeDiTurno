package com.mediturno.backend.turnos;

import java.util.List;

import com.mediturno.backend.common.dto.CriteriosBusquedaDTO;
import com.mediturno.backend.common.exception.NoImplementadoException;
import com.mediturno.backend.turnos.dto.AltaTurnoSemanalDTO;
import com.mediturno.backend.turnos.dto.OcurrenciaTurnoDTO;
import com.mediturno.backend.turnos.dto.RehabilitacionTurnoDTO;
import com.mediturno.backend.turnos.dto.SuspensionTurnoDTO;
import com.mediturno.backend.turnos.dto.SuspensionTurnoResponseDTO;
import com.mediturno.backend.turnos.dto.TurnoSemanalResponseDTO;
import org.springframework.stereotype.Service;

/**
 * Lógica de negocio de: CU-8, CU-12, CU-13, CU-14, CU-20.
 * Los métodos todavía no están implementados: lanzan NoImplementadoException (respuesta 501).
 */
@Service
public class TurnoSemanalService {

    /** CU-12. */
    public TurnoSemanalResponseDTO altaTurnoSemanal(AltaTurnoSemanalDTO datos) {
        throw new NoImplementadoException();
    }

    /** CU-13. */
    public SuspensionTurnoResponseDTO suspenderTurno(Long idTurnoSemanal, SuspensionTurnoDTO datos) {
        throw new NoImplementadoException();
    }

    /** CU-14. */
    public TurnoSemanalResponseDTO rehabilitarTurno(Long idTurnoSemanal, RehabilitacionTurnoDTO datos) {
        throw new NoImplementadoException();
    }

    /** CU-20. */
    public List<TurnoSemanalResponseDTO> buscarTurnosSemanales(CriteriosBusquedaDTO criterios) {
        throw new NoImplementadoException();
    }

    /** CU-8. */
    public List<OcurrenciaTurnoDTO> listarOcurrenciasDisponibles(String codigoEspecialidad, Long idPaciente) {
        throw new NoImplementadoException();
    }
}
