
package com.mediturno.backend.consultorios.dto;

import com.mediturno.backend.common.model.EstadoActivacion;

public record AltaConsultorioDTO(
    String codigo,
    String ubicacion,
    String nombre,
    String descripcion,
    EstadoActivacion estado
) {}
