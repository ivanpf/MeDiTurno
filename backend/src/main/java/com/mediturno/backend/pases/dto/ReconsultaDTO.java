package com.mediturno.backend.pases.dto;

/**
 * CU-25: la reconsulta no lleva datos. Es un pase a la misma especialidad de la consulta, con la vigencia fija de 60 días
 * de todos los pases; por eso el cuerpo puede ir vacío ({@code {}}).
 */
public record ReconsultaDTO() {
}
