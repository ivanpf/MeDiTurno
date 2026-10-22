package com.mediturno.backend.pases.dto;

import com.mediturno.backend.pases.model.EstadoPase;
import java.time.LocalDateTime;

public record PaseResponseDTO(
    Long idPase,
    Long idConsulta,
    String especialidadOrigen,
    String especialidadDestino,
    LocalDateTime fechaEmision,
    LocalDateTime fechaVigenciaCalculada, 
    EstadoPase paseEstado
) {}