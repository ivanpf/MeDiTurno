package com.mediturno.backend.turnos;

import com.mediturno.backend.common.dto.CriteriosBusquedaDTO;
import com.mediturno.backend.common.exception.NoImplementadoException;
import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.especialidades.EspecialidadService;
import com.mediturno.backend.pases.PaseService;
import com.mediturno.backend.reservas.repository.ReservaRepository;
import com.mediturno.backend.turnos.dto.AltaTurnoSemanalDTO;
import com.mediturno.backend.turnos.dto.OcurrenciaTurnoDTO;
import com.mediturno.backend.turnos.dto.RehabilitacionTurnoDTO;
import com.mediturno.backend.turnos.dto.SuspensionTurnoDTO;
import com.mediturno.backend.turnos.dto.SuspensionTurnoResponseDTO;
import com.mediturno.backend.turnos.dto.TurnoSemanalResponseDTO;
import com.mediturno.backend.turnos.repository.OcurrenciaTurnoRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.TreeSet;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Lógica de negocio de: CU-8, CU-12, CU-13, CU-14, CU-20.
 * CU-8 implementado. Los demás casos de uso conservan su respuesta 501.
 */
@Service
public class TurnoSemanalService {
    private final OcurrenciaTurnoRepository ocurrencias;
    private final ReservaRepository reservas;
    private final EspecialidadService especialidades;
    private final PaseService pases;
    private final DisponibilidadService disponibilidad;
    private final Clock reloj;

    public TurnoSemanalService(OcurrenciaTurnoRepository ocurrencias,
            ReservaRepository reservas,
            EspecialidadService especialidades,
            PaseService pases, DisponibilidadService disponibilidad, Clock reloj) {
        this.ocurrencias = ocurrencias;
        this.reservas = reservas;
        this.especialidades = especialidades;
        this.pases = pases;
        this.disponibilidad = disponibilidad;
        this.reloj = reloj;
    }

    /** CU-12. */
    public TurnoSemanalResponseDTO altaTurnoSemanal(AltaTurnoSemanalDTO datos) {
        throw new NoImplementadoException();
    }

    /** CU-13. */
    public SuspensionTurnoResponseDTO suspenderTurno(Long idTurnoSemanal, SuspensionTurnoDTO datos) {
        throw new NoImplementadoException();
    }

    /** CU-14. */
    public TurnoSemanalResponseDTO rehabilitarTurno(Long idTurnoSemanal, RehabilitacionTurnoDTO datos) {
        throw new NoImplementadoException();
    }

    /** CU-20. */
    public List<TurnoSemanalResponseDTO> buscarTurnosSemanales(CriteriosBusquedaDTO criterios) {
        throw new NoImplementadoException();
    }

    /** CU-8. */
    @Transactional(readOnly = true)
    public List<OcurrenciaTurnoDTO> listarOcurrenciasDisponibles(String codigoEspecialidad, Long idPaciente) {
        var especialidad = especialidades.obtenerEspecialidad(codigoEspecialidad);
        if (especialidad.estado() != EstadoActivacion.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La especialidad no está activa");
        }
        if (especialidad.requierePase() && !pases.tienePaseVigente(idPaciente, codigoEspecialidad)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No posee un pase o habilitación de reconsulta vigente para esta especialidad");
        }
        var hoy = LocalDate.now(reloj);
        return ocurrencias.findByTurnoSemanalEspecialidadCodigoAndFechaBetweenOrderByFechaAscHoraInicioAsc(codigoEspecialidad, hoy, hoy.plusWeeks(8))
                .stream().filter(disponibilidad::habilitada).map(o -> {
                    var ocupados = new TreeSet<>(reservas.numerosOcupados(o.getIdOcurrenciaTurno()));
                    // Los números cuya hora ya pasó tampoco se ofrecen para reservar.
                    for (int n = 1; n <= o.getCupos(); n++) if (!disponibilidad.numeroFuturo(o, n)) ocupados.add(n);
                    var medico = o.getTurnoSemanal().getMedico();
                    return new OcurrenciaTurnoDTO(o.getIdOcurrenciaTurno(), medico.getIdUsuario(),
                            medico.getNombre() + " " + medico.getApellido(), o.getFecha(), o.getHoraInicio(), o.getHoraFin(),
                            o.getTurnoSemanal().getConsultorio().getCodigo(), o.getCupos(), List.copyOf(ocupados));
                }).filter(o -> o.numerosOcupados().size() < o.cupos()).toList();
    }
}
