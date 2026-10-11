package com.mediturno.backend.especialidades;

import java.util.List;

import com.mediturno.backend.common.dto.CriteriosBusquedaDTO;
import com.mediturno.backend.common.dto.ResultadoImportacionDTO;
import com.mediturno.backend.common.exception.NoImplementadoException;
import com.mediturno.backend.especialidades.dto.AltaEspecialidadDTO;
import com.mediturno.backend.especialidades.dto.EspecialidadResponseDTO;
import com.mediturno.backend.especialidades.dto.ModificarEspecialidadDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Lógica de negocio de: CU-28, CU-29, CU-30, CU-43.
 * Los métodos todavía no están implementados: lanzan NoImplementadoException (respuesta 501).
 */
@Service
public class EspecialidadService {

    /** CU-28. */
    public EspecialidadResponseDTO altaEspecialidad(AltaEspecialidadDTO datos) {
        throw new NoImplementadoException();
    }

    /** CU-29. */
    public ResultadoImportacionDTO altaEspecialidadesCSV(MultipartFile archivo) {
        throw new NoImplementadoException();
    }

    /** CU-30. */
    public List<EspecialidadResponseDTO> listarEspecialidades(CriteriosBusquedaDTO criterios) {
        throw new NoImplementadoException();
    }

    /** CU-30. */
    public EspecialidadResponseDTO obtenerEspecialidad(String codigoEspecialidad) {
        throw new NoImplementadoException();
    }

    /** CU-43. */
    public EspecialidadResponseDTO modificarEspecialidad(String codigoEspecialidad, ModificarEspecialidadDTO datos) {
        throw new NoImplementadoException();
    }
}
