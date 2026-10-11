package com.mediturno.backend.reservas;

import java.util.List;

import com.mediturno.backend.common.dto.CriteriosBusquedaDTO;
import com.mediturno.backend.common.exception.NoImplementadoException;
import com.mediturno.backend.reservas.dto.ReservaRequestDTO;
import com.mediturno.backend.reservas.dto.ReservaResponseDTO;
import org.springframework.stereotype.Service;

/**
 * Lógica de negocio de: CU-8, CU-9, CU-10, CU-15, CU-19.
 * Los métodos todavía no están implementados: lanzan NoImplementadoException (respuesta 501).
 */
@Service
public class ReservaService {

    /** CU-8. */
    public ReservaResponseDTO reservarOrden(Long idPaciente, ReservaRequestDTO datos) {
        throw new NoImplementadoException();
    }

    /** CU-9. */
    public void cancelarReserva(Long idReserva, Long idPaciente) {
        throw new NoImplementadoException();
    }

    /** CU-15. */
    public ReservaResponseDTO buscarReservaPorId(Long idReserva) {
        throw new NoImplementadoException();
    }

    /** CU-19. */
    public List<ReservaResponseDTO> listarReservasParaAtencion(CriteriosBusquedaDTO criterios) {
        throw new NoImplementadoException();
    }

    /** CU-10. */
    public List<ReservaResponseDTO> consultarHistorialReservas(Long idPaciente, CriteriosBusquedaDTO criterios) {
        throw new NoImplementadoException();
    }
}
