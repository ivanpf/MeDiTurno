package com.mediturno.backend.turnos;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.turnos.model.EstadoOcurrenciaTurno;
import com.mediturno.backend.turnos.model.EstadoTurnoSemanal;
import com.mediturno.backend.turnos.model.OcurrenciaTurno;
import com.mediturno.backend.turnos.repository.SuspensionTurnoRepository;

/** Reglas compartidas por la consulta y la confirmación de CU-8. */
@Service
public class DisponibilidadService {
    private final Clock reloj;
    private final SuspensionTurnoRepository suspensiones;

    public DisponibilidadService(Clock reloj, SuspensionTurnoRepository suspensiones) {
        this.reloj = reloj;
        this.suspensiones = suspensiones;
    }

    public boolean habilitada(OcurrenciaTurno o) {
        var t = o.getTurnoSemanal();
        LocalDate hoy = LocalDate.now(reloj);
        return !o.getFecha().isBefore(hoy) && !o.getFecha().isAfter(hoy.plusWeeks(8))
                && o.getOcurrenciaTurnoEstado() == EstadoOcurrenciaTurno.HABILITADA
                && t.getEstado() == EstadoTurnoSemanal.ACTIVO
                && t.getMedico().getEstado() == EstadoActivacion.ACTIVO
                && t.getEspecialidad().getEstado() == EstadoActivacion.ACTIVO
                && t.getConsultorio().getEstado() == EstadoActivacion.ACTIVO
                && t.getMedico().getEspecialidades().stream().anyMatch(e -> e.getCodigo().equals(t.getEspecialidad().getCodigo()))
                && !o.getFecha().isBefore(t.getVigenteDesde())
                && (t.getVigenteHasta() == null || !o.getFecha().isAfter(t.getVigenteHasta()))
                && suspensiones.findByTurnoSemanalIdTurnoSemanalAndFechaRehabilitacionIsNull(t.getIdTurnoSemanal()).stream()
                    .noneMatch(s -> !o.getFecha().isBefore(s.getFechaInicio())
                            && (s.getFechaFin() == null || !o.getFecha().isAfter(s.getFechaFin())))
                && (!o.getFecha().equals(hoy) || o.getHoraFin().isAfter(LocalTime.now(reloj)));
    }

    public LocalTime horaEstimada(OcurrenciaTurno o, int numero) {
        long duracion = ChronoUnit.MINUTES.between(o.getHoraInicio(), o.getHoraFin());
        return o.getHoraInicio().plusMinutes((numero - 1L) * duracion / o.getCupos()).truncatedTo(ChronoUnit.MINUTES);
    }

    public boolean numeroFuturo(OcurrenciaTurno o, int numero) {
        return !o.getFecha().equals(LocalDate.now(reloj)) || horaEstimada(o, numero).isAfter(LocalTime.now(reloj));
    }
}
