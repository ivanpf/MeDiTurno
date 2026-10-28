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
import com.mediturno.backend.consultas.model.Consulta;
import com.mediturno.backend.especialidades.model.Especialidad;
import com.mediturno.backend.consultas.repository.ConsultaRepository;
import com.mediturno.backend.especialidades.repository.EspecialidadRepository;
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
    
    // Agregamos los repositorios necesarios para el CU-24
    private final ConsultaRepository consultaRepository;
    private final EspecialidadRepository especialidadRepository;

    // Actualizamos el constructor para inyectarlos (Spring lo hará automáticamente)
    public PaseService(PaseRepository paseRepository, Clock reloj, 
                       ConsultaRepository consultaRepository, 
                       EspecialidadRepository especialidadRepository) {
        this.paseRepository = paseRepository;
        this.reloj = reloj;
        this.consultaRepository = consultaRepository;
        this.especialidadRepository = especialidadRepository;
    }

    /** CU-24. */
    @Transactional
    public PaseResponseDTO emitirPase(Long idMedico, Long idConsulta, EmitirPaseDTO datos) {
        // 1. Buscar la consulta de origen
        Consulta consulta = consultaRepository.findById(idConsulta)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La consulta no existe"));

        // 2. Validar que el médico que intenta emitir el pase sea el mismo que realizó la consulta
        if (!consulta.getMedico().getIdUsuario().equals(idMedico)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene autorización para operar sobre esta consulta");
        }

        // 3. Validar que la especialidad de destino exista y esté activa
        Especialidad destino = especialidadRepository.findById(datos.especialidadDestino())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Especialidad de destino no encontrada"));
        
        // Asumo que el estado viene como "ACTIVO" en String o Enum. Uso toString() por seguridad para evitar conflictos.
        if (!"ACTIVO".equals(destino.getEstado().toString())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La especialidad de destino no está disponible");
        }

        // 4. Determinar especialidad de origen navegando el modelo
        Especialidad especialidadOrigen = consulta.getReserva().getOcurrenciaTurno().getTurnoSemanal().getEspecialidad();

        // 5. Crear el Pase utilizando el 'reloj' del equipo para la fecha
        LocalDateTime ahora = LocalDateTime.now(reloj);
        Pase pase = new Pase();
        pase.setPaciente(consulta.getPaciente());
        pase.setConsulta(consulta);
        pase.setEspecialidadOrigen(especialidadOrigen);
        pase.setEspecialidadDestino(destino);
        pase.setFechaEmision(ahora);
        pase.setPaseEstado(EstadoPase.VIGENTE);

        Pase paseGuardado = paseRepository.save(pase);

        // 6. Calcular la vigencia con la utilidad que ya crearon tus compañeros
        LocalDateTime vigenciaCalculada = VigenciaPase.MAXIMA.vencimiento(paseGuardado.getFechaEmision());
        // 7. Retornar DTO
        return new PaseResponseDTO(
                paseGuardado.getIdPase(),
                paseGuardado.getConsulta().getIdConsulta(),
                paseGuardado.getEspecialidadOrigen().getCodigo(),   // <-- Agrega .getCodigo()
                paseGuardado.getEspecialidadDestino().getCodigo(),  // <-- Agrega .getCodigo()
                paseGuardado.getFechaEmision(),
                vigenciaCalculada,
                paseGuardado.getPaseEstado()
        );
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
