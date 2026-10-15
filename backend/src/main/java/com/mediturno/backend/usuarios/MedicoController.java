package com.mediturno.backend.usuarios;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mediturno.backend.common.seguridad.RolesPermitidos;
import com.mediturno.backend.usuarios.dto.MedicoResponseDTO;
import com.mediturno.backend.usuarios.model.Administrador;
import com.mediturno.backend.usuarios.model.Funcionario;

/**
 * Consulta de médicos para las pantallas (no es un caso de uso): los usan, por ejemplo, el funcionario al dar de alta un
 * turno (CU-12) y el administrador.
 */
@RolesPermitidos({Administrador.class, Funcionario.class})
@RestController
@RequestMapping("/api/medicos")
public class MedicoController {

    private final MedicoService medicoService;

    public MedicoController(MedicoService medicoService) {
        this.medicoService = medicoService;
    }

    /** Médicos activos con sus especialidades activas. */
    @GetMapping
    public List<MedicoResponseDTO> listarMedicosActivos() {
        return medicoService.listarMedicosActivos();
    }
}
