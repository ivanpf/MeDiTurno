package com.mediturno.backend.notificaciones;

import java.util.Map;

import com.mediturno.backend.common.exception.NoImplementadoException;
import org.springframework.stereotype.Service;

/**
 * Lógica de negocio de: CU-4, CU-8, CU-13, CU-16, CU-23, CU-34.
 * Los métodos todavía no están implementados: lanzan NoImplementadoException (respuesta 501).
 */
@Service
public class EmailService {

    /** CU-8. */
    public String enviarRecordatorioConsulta(String destino, Map<String, Object> datos) {
        throw new NoImplementadoException();
    }

    /** CU-23. */
    public String enviarAvisoHistoriaClinica(String destino) {
        throw new NoImplementadoException();
    }

    /** CU-13. */
    public String enviarAvisoSuspensionTurno(String destino, Map<String, Object> datos) {
        throw new NoImplementadoException();
    }

    /** CU-34. */
    public String enviarCredencialesTemporales(String destino, String contrasenaTemporal) {
        throw new NoImplementadoException();
    }

    /** CU-4. */
    public String enviarRecuperacionAcceso(String destino, String token) {
        throw new NoImplementadoException();
    }

    /** CU-16. */
    public String enviarOrdenPDF(String pdfBase64, String destino) {
        throw new NoImplementadoException();
    }
}
