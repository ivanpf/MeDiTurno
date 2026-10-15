package com.mediturno.backend.usuarios;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.especialidades.dto.EspecialidadResponseDTO;
import com.mediturno.backend.especialidades.model.Especialidad;
import com.mediturno.backend.usuarios.dto.MedicoResponseDTO;
import com.mediturno.backend.usuarios.model.Medico;
import com.mediturno.backend.usuarios.repository.MedicoRepository;

/**
 * Consultas de médicos para las pantallas. No corresponde a un caso de uso del Documento de diseño: es un apoyo para elegir
 * al médico en otros formularios.
 */
@Service
public class MedicoService {

    private final MedicoRepository medicoRepository;

    public MedicoService(MedicoRepository medicoRepository) {
        this.medicoRepository = medicoRepository;
    }

    /** Médicos activos, ordenados por apellido y nombre, con sus especialidades activas ordenadas por nombre. */
    @Transactional(readOnly = true)
    public List<MedicoResponseDTO> listarMedicosActivos() {
        return medicoRepository.listarConEspecialidades(EstadoActivacion.ACTIVO).stream().map(MedicoService::convertirAMedicoResponseDTO).toList();
    }

    private static MedicoResponseDTO convertirAMedicoResponseDTO(Medico medico) {
        List<EspecialidadResponseDTO> especialidades = medico.getEspecialidades().stream()
            .filter(e -> e.getEstado() == EstadoActivacion.ACTIVO)
            .sorted(Comparator.comparing(Especialidad::getNombre, String.CASE_INSENSITIVE_ORDER))
            .map(e -> new EspecialidadResponseDTO(e.getCodigo(), e.getNombre(), e.getDescripcion(), e.isRequierePase(), e.getEstado()))
            .toList();
        return new MedicoResponseDTO(medico.getIdUsuario(), medico.getNombre(), medico.getApellido(), especialidades);
    }
}
