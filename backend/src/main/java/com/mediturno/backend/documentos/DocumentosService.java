package com.mediturno.backend.documentos;

import org.springframework.stereotype.Service;

/**
 * Generación de documentos (por ahora, el PDF de la orden, CU-16).
 * Aparece en el diagrama de secuencia de CU-16 como "DocumentosService" (paquete documentos, Apache PDFBox);
 * no figura en el diagrama de clases. TODO(CU-16): agregar {@code generarPDF(orden)} cuando se defina la entidad Orden.
 */
@Service
public class DocumentosService {
}
