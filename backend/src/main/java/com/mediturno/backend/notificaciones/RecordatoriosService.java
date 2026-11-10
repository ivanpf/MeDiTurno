package com.mediturno.backend.notificaciones;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mediturno.backend.reservas.model.Reserva;
import com.mediturno.backend.reservas.repository.ReservaRepository;
import com.mediturno.backend.turnos.DisponibilidadService;

/**
 * CU-8: recordatorio 24 horas antes de la consulta. Lo llama la tarea programada cada pocos minutos (ver
 * {@code TareasProgramadas}); la tarea solo decide cuándo, la lógica está acá.
 * <p>
 * Recibe recordatorio la reserva {@code VIGENTE} cuya hora estimada de atención (la del número) es futura y está a 24 horas o
 * menos, y que todavía no lo recibió (se comprueba con las notificaciones ya registradas, así que repetir la tarea o
 * reiniciar el servidor no duplica avisos). Si la reserva se hizo con menos de 24 horas de anticipación no se manda: ya
 * no hay «24 horas antes». Las canceladas, anuladas, con orden emitida, atendidas o vencidas no reciben recordatorio.
 */
@Service
public class RecordatoriosService {

    private static final Logger log = LoggerFactory.getLogger(RecordatoriosService.class);
    private static final Duration ANTICIPACION = Duration.ofHours(24);

    private final ReservaRepository reservaRepository;
    private final DisponibilidadService disponibilidadService;
    private final NotificacionService notificacionService;
    private final Clock reloj;

    public RecordatoriosService(ReservaRepository reservaRepository, DisponibilidadService disponibilidadService,
            NotificacionService notificacionService, Clock reloj) {
        this.reservaRepository = reservaRepository;
        this.disponibilidadService = disponibilidadService;
        this.notificacionService = notificacionService;
        this.reloj = reloj;
    }

    /** Devuelve cuántos recordatorios se enviaron. Un fallo en una reserva no frena a las demás. */
    public int enviarRecordatoriosPendientes() {
        int enviados = 0;
        for (Long idReserva : reservasParaRecordar()) {
            try {
                notificacionService.enviarRecordatorioConsulta(idReserva);
                enviados++;
            } catch (RuntimeException e) {
                log.warn("No se pudo enviar el recordatorio de la reserva {}: {}", idReserva, e.getMessage());
            }
        }
        if (enviados > 0) {
            log.info("Recordatorios de consulta enviados: {}", enviados);
        }
        return enviados;
    }

    @Transactional(readOnly = true)
    public List<Long> reservasParaRecordar() {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return reservaRepository.listarSinRecordatorio(ahora.toLocalDate(), ahora.plus(ANTICIPACION).toLocalDate()).stream()
            .filter(reserva -> correspondeRecordar(reserva, ahora))
            .map(Reserva::getIdReserva)
            .toList();
    }

    private boolean correspondeRecordar(Reserva reserva, LocalDateTime ahora) {
        var ocurrencia = reserva.getOcurrenciaTurno();
        LocalDateTime consulta = ocurrencia.getFecha().atTime(disponibilidadService.horaEstimada(ocurrencia, reserva.getNumero()));
        return consulta.isAfter(ahora)
            && !consulta.isAfter(ahora.plus(ANTICIPACION))
            && !reserva.getFechaReserva().isAfter(consulta.minus(ANTICIPACION));
    }
}
