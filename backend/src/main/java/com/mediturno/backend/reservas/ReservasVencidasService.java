package com.mediturno.backend.reservas;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mediturno.backend.reservas.repository.ReservaRepository;

/**
 * Marca como VENCIDA las reservas que pasaron sin atención. Lo llama la tarea programada (ver {@code TareasProgramadas}).
 * <ul>
 *   <li>{@code VIGENTE}: vence cuando termina el turno (a su hora de fin, ya no se puede emitir la orden).</li>
 *   <li>{@code ORDEN_EMITIDA}: vence recién cuando pasa el día del turno, para que el médico pueda registrar la consulta
 *       aunque la cargue un rato después de terminar la atención.</li>
 * </ul>
 * No toca la orden ni libera el número. Es seguro repetirla.
 */
@Service
public class ReservasVencidasService {

    private static final Logger log = LoggerFactory.getLogger(ReservasVencidasService.class);

    private final ReservaRepository reservaRepository;
    private final Clock reloj;

    public ReservasVencidasService(ReservaRepository reservaRepository, Clock reloj) {
        this.reservaRepository = reservaRepository;
        this.reloj = reloj;
    }

    /** Devuelve cuántas reservas pasaron a VENCIDA. */
    @Transactional
    public int marcarVencidas() {
        LocalDate hoy = LocalDate.now(reloj);
        int vigentes = reservaRepository.vencerVigentes(hoy, LocalTime.now(reloj));
        int conOrden = reservaRepository.vencerConOrdenEmitida(hoy);
        if (vigentes + conOrden > 0) {
            log.info("Reservas vencidas: {} vigentes y {} con orden emitida", vigentes, conOrden);
        }
        return vigentes + conOrden;
    }
}
