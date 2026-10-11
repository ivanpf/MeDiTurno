package com.mediturno.backend.consultas;

import java.util.List;

import com.mediturno.backend.common.dto.CriteriosBusquedaDTO;
import com.mediturno.backend.common.exception.NoImplementadoException;
import com.mediturno.backend.consultas.dto.ConsultaResponseDTO;
import com.mediturno.backend.consultas.dto.RegistrarConsultaDTO;
import com.mediturno.backend.reservas.dto.ReservaResponseDTO;
import org.springframework.stereotype.Service;

/**
 * Lógica de negocio de: CU-11, CU-21, CU-22, CU-23, CU-26.
 * Los métodos todavía no están implementados: lanzan NoImplementadoException (respuesta 501).
 */
@Service
public class ConsultaService {

    /** CU-23. */
    public ConsultaResponseDTO registrarConsulta(Long idMedico, Long numeroOrden, RegistrarConsultaDTO datos) {
        throw new NoImplementadoException();
    }

    /** CU-11. */
    public List<ConsultaResponseDTO> consultarHistoriaClinicaPropia(Long idPaciente, CriteriosBusquedaDTO criterios) {
        throw new NoImplementadoException();
    }

    /** CU-22. */
    public List<ConsultaResponseDTO> consultarHistoriaClinicaDuranteAtencion(Long idMedico, Long idPaciente) {
        throw new NoImplementadoException();
    }

    /** CU-21. */
    public List<ReservaResponseDTO> consultarPacientesPorTurno(Long idMedico, Long idOcurrenciaTurno) {
        throw new NoImplementadoException();
    }

    /** CU-26. */
    public List<ConsultaResponseDTO> consultarHistorialAtencionesPropias(Long idMedico, CriteriosBusquedaDTO criterios) {
        throw new NoImplementadoException();
    }
}
