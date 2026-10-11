package com.mediturno.backend.pases;

import java.util.List;

import com.mediturno.backend.common.dto.CriteriosBusquedaDTO;
import com.mediturno.backend.common.exception.NoImplementadoException;
import com.mediturno.backend.pases.dto.EmitirPaseDTO;
import com.mediturno.backend.pases.dto.PaseResponseDTO;
import com.mediturno.backend.pases.dto.ReconsultaDTO;
import org.springframework.stereotype.Service;

/**
 * Lógica de negocio de: CU-8, CU-24, CU-25, CU-27.
 * Los métodos todavía no están implementados: lanzan NoImplementadoException (respuesta 501).
 */
@Service
public class PaseService {

    /** CU-24. */
    public PaseResponseDTO emitirPase(Long idMedico, Long idConsulta, EmitirPaseDTO datos) {
        throw new NoImplementadoException();
    }

    /** CU-25. */
    public PaseResponseDTO habilitarReconsulta(Long idMedico, Long idConsulta, ReconsultaDTO datos) {
        throw new NoImplementadoException();
    }

    /** CU-27. */
    public List<PaseResponseDTO> buscarPasesEmitidos(Long idMedico, CriteriosBusquedaDTO criterios) {
        throw new NoImplementadoException();
    }

    /** CU-8. Uso interno (lo usan TurnoSemanalService y ReservaService al validar una reserva). */
    public boolean tienePaseVigente(Long idPaciente, String codigoEspecialidad) {
        throw new NoImplementadoException();
    }
}
