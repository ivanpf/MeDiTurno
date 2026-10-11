package com.mediturno.backend.pacientes;

import java.util.List;

import com.mediturno.backend.common.dto.CriteriosBusquedaDTO;
import com.mediturno.backend.common.dto.PerfilDTO;
import com.mediturno.backend.common.dto.ResultadoImportacionDTO;
import com.mediturno.backend.common.exception.NoImplementadoException;
import com.mediturno.backend.pacientes.dto.AltaPacienteDTO;
import com.mediturno.backend.pacientes.dto.ModificarPacienteDTO;
import com.mediturno.backend.pacientes.dto.PacienteResponseDTO;
import com.mediturno.backend.pacientes.dto.RegistroPacienteDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Lógica de negocio de: CU-5, CU-7, CU-31, CU-32, CU-33, CU-44.
 * Los métodos todavía no están implementados: lanzan NoImplementadoException (respuesta 501).
 */
@Service
public class PacienteService {

    /** CU-7. */
    public PacienteResponseDTO registrarCuentaPaciente(RegistroPacienteDTO datos) {
        throw new NoImplementadoException();
    }

    /** CU-31. */
    public PacienteResponseDTO altaPaciente(AltaPacienteDTO datos) {
        throw new NoImplementadoException();
    }

    /** CU-32. */
    public ResultadoImportacionDTO altaPacientesCSV(MultipartFile archivo) {
        throw new NoImplementadoException();
    }

    /** CU-33. */
    public List<PacienteResponseDTO> listarPacientes(CriteriosBusquedaDTO criterios) {
        throw new NoImplementadoException();
    }

    /** CU-33. */
    public PacienteResponseDTO obtenerPaciente(Long idPaciente) {
        throw new NoImplementadoException();
    }

    /** CU-44. */
    public PacienteResponseDTO modificarPaciente(Long idPaciente, ModificarPacienteDTO datos) {
        throw new NoImplementadoException();
    }

    /** CU-5. */
    public void actualizarPerfil(Long idPaciente, PerfilDTO datos) {
        throw new NoImplementadoException();
    }
}
