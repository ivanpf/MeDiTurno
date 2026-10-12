package com.mediturno.backend.reservas;

import java.util.List;

import com.mediturno.backend.common.dto.CriteriosBusquedaDTO;
import com.mediturno.backend.common.seguridad.UsuarioActual;
import com.mediturno.backend.reservas.dto.ReservaRequestDTO;
import com.mediturno.backend.reservas.dto.ReservaResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.mediturno.backend.common.seguridad.RolesPermitidos;
import com.mediturno.backend.usuarios.model.Funcionario;
import com.mediturno.backend.pacientes.model.Paciente;

/**
 * Endpoints de: CU-8, CU-9, CU-10, CU-15, CU-19.
 * Las rutas son las de los diagramas de secuencia; donde el diagrama no la indica, es una propuesta.
 */
@RolesPermitidos(Paciente.class)
@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService reservaService;
    private final UsuarioActual usuarioActual;

    public ReservaController(ReservaService reservaService, UsuarioActual usuarioActual) {
        this.reservaService = reservaService;
        this.usuarioActual = usuarioActual;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaResponseDTO reservarOrden(@RequestBody ReservaRequestDTO datos) {
        return reservaService.reservarOrden(usuarioActual.obtenerIdPacienteAutenticado(), datos);
    }

    @PostMapping("/{idReserva}/cancelacion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelarReserva(@PathVariable Long idReserva) {
        reservaService.cancelarReserva(idReserva, usuarioActual.obtenerIdPacienteAutenticado());
    }

    @RolesPermitidos(Funcionario.class)
    @GetMapping("/{idReserva}")
    public ReservaResponseDTO buscarReservaPorId(@PathVariable Long idReserva) {
        return reservaService.buscarReservaPorId(idReserva);
    }

    @RolesPermitidos(Funcionario.class)
    @GetMapping("/para-atencion")
    public List<ReservaResponseDTO> listarReservasParaAtencion(CriteriosBusquedaDTO criterios) {
        return reservaService.listarReservasParaAtencion(criterios);
    }

    @GetMapping("/mias")
    public List<ReservaResponseDTO> consultarHistorialReservas(CriteriosBusquedaDTO criterios) {
        return reservaService.consultarHistorialReservas(usuarioActual.obtenerIdPacienteAutenticado(), criterios);
    }
}
