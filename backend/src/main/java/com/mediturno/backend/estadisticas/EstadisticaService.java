package com.mediturno.backend.estadisticas;

import java.text.Collator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mediturno.backend.common.dto.PeriodoDTO;
import com.mediturno.backend.estadisticas.dto.EstadisticaOcupacionDTO;
import com.mediturno.backend.estadisticas.dto.EstadisticaReservaDTO;
import com.mediturno.backend.estadisticas.dto.EstadisticaTurnoDTO;
import com.mediturno.backend.reservas.model.EstadoReserva;
import com.mediturno.backend.reservas.repository.ReservaRepository;
import com.mediturno.backend.turnos.model.TurnoSemanal;
import com.mediturno.backend.turnos.repository.OcurrenciaTurnoRepository;
import com.mediturno.backend.turnos.repository.TurnoSemanalRepository;

/**
 * Lógica de negocio de: CU-40, CU-41, CU-42. Son consultas de solo lectura; el período se mide por la fecha de atención del
 * turno (ver {@link PeriodoDTO}).
 */
@Service
@Transactional(readOnly = true)
public class EstadisticaService {

    // Sin límite de un lado del período: las fechas extremas que acepta PostgreSQL sirven de tope.
    private static final LocalDate SIN_LIMITE_DESDE = LocalDate.of(1900, 1, 1);
    private static final LocalDate SIN_LIMITE_HASTA = LocalDate.of(9999, 12, 31);

    private final ReservaRepository reservaRepository;
    private final OcurrenciaTurnoRepository ocurrenciaTurnoRepository;
    private final TurnoSemanalRepository turnoSemanalRepository;

    public EstadisticaService(ReservaRepository reservaRepository, OcurrenciaTurnoRepository ocurrenciaTurnoRepository,
            TurnoSemanalRepository turnoSemanalRepository) {
        this.reservaRepository = reservaRepository;
        this.ocurrenciaTurnoRepository = ocurrenciaTurnoRepository;
        this.turnoSemanalRepository = turnoSemanalRepository;
    }

    /** CU-40. */
    public List<EstadisticaReservaDTO> estadisticaReservasPorEspecialidad(PeriodoDTO periodo) {
        LocalDate desde = desde(periodo);
        LocalDate hasta = hasta(periodo);
        validarRango(desde, hasta);
        Collection<EstadoReserva> estados = interpretarEstadoReserva(periodo.estado());
        boolean porNombre = ordenPorNombre(periodo.ordenarPor());
        boolean descendente = periodo.descendente() != null ? periodo.descendente() : !porNombre;

        List<Object[]> filas = reservaRepository.contarPorEspecialidad(desde, hasta, estados);
        long total = filas.stream().mapToLong(f -> (Long) f[2]).sum();
        Collator alfabetico = Collator.getInstance(Locale.forLanguageTag("es"));
        Comparator<EstadisticaReservaDTO> orden = porNombre
            ? Comparator.comparing(EstadisticaReservaDTO::nombreEspecialidad, alfabetico)
            : Comparator.comparingLong(EstadisticaReservaDTO::cantidadReservas)
                .thenComparing(EstadisticaReservaDTO::nombreEspecialidad, alfabetico.reversed());
        return filas.stream()
            .map(f -> convertirAEstadisticaReservaDTO((String) f[0], (String) f[1], (Long) f[2], total))
            .sorted(descendente ? orden.reversed() : orden)
            .toList();
    }

    /** CU-41. */
    public List<EstadisticaTurnoDTO> estadisticaTurnosMasDemandados(PeriodoDTO periodo) {
        LocalDate desde = desde(periodo);
        LocalDate hasta = hasta(periodo);
        validarRango(desde, hasta);
        boolean descendente = periodo.descendente() == null || periodo.descendente();
        validarOrden(periodo.ordenarPor(), "reservas");

        Map<Long, long[]> reservas = reservasPorTurno(desde, hasta, periodo.codigoEspecialidad(), periodo.idMedico(), null);
        Map<Long, Long> cupos = cuposPorTurno(desde, hasta, periodo.codigoEspecialidad(), periodo.idMedico(), null);
        // Un turno con fechas en el período entra al ranking aunque no haya recibido reservas (con 0), para ver los menos demandados.
        Set<Long> ids = new HashSet<>(reservas.keySet());
        ids.addAll(cupos.keySet());

        Comparator<EstadisticaTurnoDTO> orden = Comparator.comparingLong(EstadisticaTurnoDTO::cantidadReservas);
        return turnos(ids).stream()
            .map(t -> convertirAEstadisticaTurnoDTO(t, reservas.getOrDefault(t.getIdTurnoSemanal(), CEROS)[0]))
            .sorted((descendente ? orden.reversed() : orden).thenComparing(this::desempate))
            .toList();
    }

    /** CU-42. */
    public List<EstadisticaOcupacionDTO> estadisticaOcupacionTurnos(PeriodoDTO periodo) {
        LocalDate desde = desde(periodo);
        LocalDate hasta = hasta(periodo);
        validarRango(desde, hasta);
        boolean descendente = periodo.descendente() == null || periodo.descendente();
        validarOrden(periodo.ordenarPor(), "porcentaje");

        Map<Long, Long> cupos = cuposPorTurno(desde, hasta, periodo.codigoEspecialidad(), periodo.idMedico(), periodo.codigoConsultorio());
        Map<Long, long[]> reservas = reservasPorTurno(desde, hasta, periodo.codigoEspecialidad(), periodo.idMedico(), periodo.codigoConsultorio());

        List<EstadisticaOcupacionDTO> resultado = new ArrayList<>();
        for (TurnoSemanal t : turnos(cupos.keySet())) {
            long[] r = reservas.getOrDefault(t.getIdTurnoSemanal(), CEROS);
            resultado.add(convertirAEstadisticaOcupacionDTO(t, cupos.get(t.getIdTurnoSemanal()), r[1], r[2]));
        }
        Comparator<EstadisticaOcupacionDTO> orden = Comparator.comparingDouble(EstadisticaOcupacionDTO::porcentajeOcupacion);
        Comparator<EstadisticaOcupacionDTO> ordenFinal = (descendente ? orden.reversed() : orden)
            .thenComparing(o -> o.nombreMedico(), Collator.getInstance(Locale.forLanguageTag("es")))
            .thenComparing(EstadisticaOcupacionDTO::dia)
            .thenComparing(EstadisticaOcupacionDTO::horaInicio);
        resultado.sort(ordenFinal);
        return resultado;
    }

    // Reservas totales, vigentes y que conservan su número, en ese orden.
    private static final long[] CEROS = {0, 0, 0};

    private Map<Long, long[]> reservasPorTurno(LocalDate desde, LocalDate hasta, String especialidad, Long medico, String consultorio) {
        Map<Long, long[]> porTurno = new HashMap<>();
        for (Object[] f : reservaRepository.contarPorTurno(desde, hasta, vacioANulo(especialidad), medico, vacioANulo(consultorio))) {
            porTurno.put((Long) f[0], new long[] {(Long) f[1], (Long) f[2], (Long) f[3]});
        }
        return porTurno;
    }

    private Map<Long, Long> cuposPorTurno(LocalDate desde, LocalDate hasta, String especialidad, Long medico, String consultorio) {
        Map<Long, Long> porTurno = new HashMap<>();
        for (Object[] f : ocurrenciaTurnoRepository.sumarCuposPorTurno(desde, hasta, vacioANulo(especialidad), medico, vacioANulo(consultorio))) {
            porTurno.put((Long) f[0], (Long) f[1]);
        }
        return porTurno;
    }

    private List<TurnoSemanal> turnos(Set<Long> ids) {
        return ids.isEmpty() ? List.of() : turnoSemanalRepository.buscarConRelaciones(ids);
    }

    /** Ante igual demanda: médico, día y hora de inicio. */
    private int desempate(EstadisticaTurnoDTO a, EstadisticaTurnoDTO b) {
        int porMedico = Collator.getInstance(Locale.forLanguageTag("es")).compare(a.nombreMedico(), b.nombreMedico());
        if (porMedico != 0) {
            return porMedico;
        }
        int porDia = a.dia().compareTo(b.dia());
        return porDia != 0 ? porDia : a.horaInicio().compareTo(b.horaInicio());
    }

    private EstadisticaReservaDTO convertirAEstadisticaReservaDTO(String codigo, String nombre, long cantidad, long total) {
        return new EstadisticaReservaDTO(codigo, nombre, cantidad, porcentaje(cantidad, total));
    }

    private EstadisticaTurnoDTO convertirAEstadisticaTurnoDTO(TurnoSemanal t, long cantidadReservas) {
        return new EstadisticaTurnoDTO(t.getIdTurnoSemanal(), t.getEspecialidad().getCodigo(), t.getEspecialidad().getNombre(),
            t.getMedico().getIdUsuario(), t.getMedico().nombreCompleto(), t.getDia(), t.getHoraInicio(), t.getHoraFin(),
            t.getConsultorio().getCodigo(), t.getConsultorio().getNombre(), t.getCupoMaximo(), cantidadReservas);
    }

    private EstadisticaOcupacionDTO convertirAEstadisticaOcupacionDTO(TurnoSemanal t, long ofrecidos, long vigentes, long reservados) {
        return new EstadisticaOcupacionDTO(t.getIdTurnoSemanal(), t.getEspecialidad().getCodigo(), t.getEspecialidad().getNombre(),
            t.getMedico().getIdUsuario(), t.getMedico().nombreCompleto(), t.getDia(), t.getHoraInicio(), t.getHoraFin(),
            t.getConsultorio().getCodigo(), t.getConsultorio().getNombre(), ofrecidos, reservados, vigentes,
            ofrecidos - reservados, porcentaje(reservados, ofrecidos));
    }

    /** Porcentaje con dos decimales; 0 si no hay con qué compararlo. */
    private static double porcentaje(long parte, long total) {
        return total == 0 ? 0 : Math.round(parte * 10000.0 / total) / 100.0;
    }

    private static LocalDate desde(PeriodoDTO periodo) {
        return periodo.fechaDesde() != null ? periodo.fechaDesde() : SIN_LIMITE_DESDE;
    }

    private static LocalDate hasta(PeriodoDTO periodo) {
        return periodo.fechaHasta() != null ? periodo.fechaHasta() : SIN_LIMITE_HASTA;
    }

    private static void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde.isAfter(hasta)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha desde no puede ser posterior a la fecha hasta.");
        }
    }

    /** Estas estadísticas se ordenan por un solo criterio: si piden otro, 400. */
    private static void validarOrden(String ordenarPor, String valido) {
        if (ordenarPor != null && !ordenarPor.isBlank() && !valido.equalsIgnoreCase(ordenarPor.trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El orden indicado no es válido (use " + valido + ").");
        }
    }

    private static boolean ordenPorNombre(String ordenarPor) {
        if (ordenarPor == null || ordenarPor.isBlank() || "cantidad".equalsIgnoreCase(ordenarPor.trim())) {
            return false;
        }
        if ("nombre".equalsIgnoreCase(ordenarPor.trim())) {
            return true;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El orden indicado no es válido (use cantidad o nombre).");
    }

    /** El estado pedido, o todos si no se indicó; 400 si no existe. */
    private static Collection<EstadoReserva> interpretarEstadoReserva(String texto) {
        if (texto == null || texto.isBlank()) {
            return Arrays.asList(EstadoReserva.values());
        }
        try {
            return List.of(EstadoReserva.valueOf(texto.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "El estado de la reserva indicado no es válido (use " + String.join(", ", Arrays.stream(EstadoReserva.values()).map(Enum::name).toList()) + ").");
        }
    }

    private static String vacioANulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
