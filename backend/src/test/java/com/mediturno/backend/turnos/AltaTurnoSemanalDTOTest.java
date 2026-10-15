package com.mediturno.backend.turnos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import com.mediturno.backend.turnos.dto.AltaTurnoSemanalDTO;
import com.mediturno.backend.turnos.model.DiaSemana;

/** Las reglas de datos obligatorios y capacidad del paso 2 de CU-12. */
class AltaTurnoSemanalDTOTest {

    private final Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

    private Set<String> camposInvalidos(AltaTurnoSemanalDTO dto) {
        return validador.validate(dto).stream().map(ConstraintViolation::getPropertyPath).map(Object::toString).collect(Collectors.toSet());
    }

    @Test
    void informaTodosLosCamposObligatoriosQueFaltan() {
        Set<String> campos = camposInvalidos(new AltaTurnoSemanalDTO(null, " ", "", List.of(), null, null, null, null));

        assertEquals(Set.of("idMedico", "codigoEspecialidad", "codigoConsultorio", "dias", "horaInicio", "horaFin", "cupoMaximo", "vigenteDesde"), campos);
    }

    @Test
    void laCapacidadTieneQueSerAlMenosUno() {
        AltaTurnoSemanalDTO dto = new AltaTurnoSemanalDTO(1L, "CARD", "C1", List.of(DiaSemana.LUNES),
            LocalTime.of(9, 0), LocalTime.of(12, 0), 0, LocalDate.now());

        assertEquals(Set.of("cupoMaximo"), camposInvalidos(dto));
    }
}
