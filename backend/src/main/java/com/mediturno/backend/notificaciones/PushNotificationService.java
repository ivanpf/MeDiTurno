package com.mediturno.backend.notificaciones;

import com.mediturno.backend.common.exception.NoImplementadoException;
import org.springframework.stereotype.Service;

/**
 * Lógica de negocio de: CU-6, CU-23.
 * Los métodos todavía no están implementados: lanzan NoImplementadoException (respuesta 501).
 */
@Service
public class PushNotificationService {

    /** CU-6. El endpoint no aparece en los diagramas; es necesario para que la app móvil registre su token. */
    public void registrar(Long idUsuario, String token) {
        throw new NoImplementadoException();
    }

    /** CU-6. El endpoint no aparece en los diagramas; es necesario para que la app móvil elimine su token. */
    public void eliminar(Long idUsuario) {
        throw new NoImplementadoException();
    }

    /** CU-23. Uso interno. */
    public void enviarPush(Long idUsuario, String titulo, String mensaje) {
        throw new NoImplementadoException();
    }
}
