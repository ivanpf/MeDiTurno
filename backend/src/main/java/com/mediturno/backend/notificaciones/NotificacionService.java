package com.mediturno.backend.notificaciones;

import java.util.List;

import com.mediturno.backend.common.exception.NoImplementadoException;
import com.mediturno.backend.notificaciones.dto.NotificacionResponseDTO;
import org.springframework.stereotype.Service;

/**
 * Lógica de negocio de: CU-6, CU-8, CU-13, CU-23.
 * Los métodos todavía no están implementados: lanzan NoImplementadoException (respuesta 501).
 */
@Service
public class NotificacionService {

    /** CU-8. Uso interno. */
    public void enviarRecordatorioConsulta(Long idReserva) {
        throw new NoImplementadoException();
    }

    /** CU-13. Uso interno. */
    public void notificarSuspensionTurno(Long idSuspensionTurno) {
        throw new NoImplementadoException();
    }

    /** CU-23. Uso interno. */
    public void notificarConsultaRegistrada(Long idConsulta) {
        throw new NoImplementadoException();
    }

    /** CU-6. */
    public List<NotificacionResponseDTO> listarNotificaciones(Long idUsuario) {
        throw new NoImplementadoException();
    }

    /** CU-6. */
    public void marcarLeida(Long idNotificacion) {
        throw new NoImplementadoException();
    }
}
