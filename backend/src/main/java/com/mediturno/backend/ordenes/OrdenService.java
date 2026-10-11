package com.mediturno.backend.ordenes;

import com.mediturno.backend.common.exception.NoImplementadoException;
import com.mediturno.backend.ordenes.dto.OrdenResponseDTO;
import org.springframework.stereotype.Service;

/**
 * Lógica de negocio de: CU-16.
 * Los métodos todavía no están implementados: lanzan NoImplementadoException (respuesta 501).
 */
@Service
public class OrdenService {

    /** CU-16. */
    public OrdenResponseDTO emitirOrden(Long idReserva, Long idFuncionario) {
        throw new NoImplementadoException();
    }

    /** CU-16. */
    public byte[] generarOrdenPDF(Long numeroOrden) {
        throw new NoImplementadoException();
    }
}
