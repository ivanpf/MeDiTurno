package com.mediturno.backend.pases;

import com.mediturno.backend.common.dto.CriteriosBusquedaDTO;
import com.mediturno.backend.common.exception.NoImplementadoException;
import com.mediturno.backend.pases.dto.EmitirPaseDTO;
import com.mediturno.backend.pases.dto.PaseResponseDTO;
import com.mediturno.backend.pases.dto.ReconsultaDTO;
import com.mediturno.backend.pases.model.EstadoPase;
import com.mediturno.backend.pases.model.Pase;
import com.mediturno.backend.pases.model.VigenciaPase;
import com.mediturno.backend.pases.repository.PaseRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Lógica de negocio de: CU-8, CU-24, CU-25, CU-27.
 * La verificación de pases para CU-8 está implementada; los demás casos siguen pendientes.
 */
@Service
public class PaseService {
    private final PaseRepository paseRepository;
    private final Clock reloj;

    public PaseService(PaseRepository paseRepository, Clock reloj) {
        this.paseRepository = paseRepository;
        this.reloj = reloj;
    }

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
        return !pasesVigentes(idPaciente, codigoEspecialidad).isEmpty();
    }

    private List<Pase> pasesVigentes(Long idPaciente, String codigoEspecialidad) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return paseRepository.buscarPorPacienteEspecialidadYEstado(
                idPaciente, codigoEspecialidad, EstadoPase.VIGENTE, ahora.minusDays(VigenciaPase.MAXIMA.getDias()), ahora);
    }

    /** Bloquea y vincula el pase dentro de la transacción de reserva. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Pase obtenerPaseParaReserva(Long idPaciente, String codigoEspecialidad) {
        for (Pase candidato : pasesVigentes(idPaciente, codigoEspecialidad)) {
            Pase pase = paseRepository.buscarConLock(candidato.getIdPase()).orElseThrow();
            LocalDateTime ahora = LocalDateTime.now(reloj);
            if (pase.getPaseEstado() == EstadoPase.VIGENTE
                    && !pase.getFechaEmision().isAfter(ahora)
                    && !VigenciaPase.MAXIMA.vencimiento(pase.getFechaEmision()).isBefore(ahora)) {
                return pase;
            }
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "No posee un pase o habilitación de reconsulta vigente para esta especialidad");
    }
}
