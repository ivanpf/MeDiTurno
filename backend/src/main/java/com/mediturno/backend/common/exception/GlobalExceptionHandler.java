package com.mediturno.backend.common.exception;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import tools.jackson.databind.exc.MismatchedInputException;

/**
 * Convierte las excepciones en respuestas con el formato {@link ApiError}.
 * Para devolver un error de negocio, lanzar {@code new ResponseStatusException(HttpStatus.XXX, "mensaje")}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validacion(MethodArgumentNotValidException ex) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .toList();
        return ResponseEntity.badRequest()
            .body(new ApiError(400, "Hay datos inválidos", detalles, Instant.now()));
    }

    /**
     * El cuerpo no se pudo leer: JSON mal formado o un valor que no corresponde al tipo del campo (un estado que no
     * existe, una fecha mal escrita, un texto donde va un sí/no). Es un error de quien llama, no del servidor.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> cuerpoIlegible(HttpMessageNotReadableException ex) {
        String detalle = "El cuerpo de la solicitud no es válido";
        if (ex.getCause() instanceof MismatchedInputException mismatch && !mismatch.getPath().isEmpty()
                && mismatch.getPath().get(0).getPropertyName() != null) {
            detalle = "El valor del campo «" + mismatch.getPath().get(0).getPropertyName() + "» no es válido";
        }
        return ResponseEntity.badRequest()
            .body(new ApiError(400, "Hay datos inválidos", List.of(detalle), Instant.now()));
    }

    /** Errores HTTP (ruta inexistente, método no permitido, ResponseStatusException) y errores inesperados. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> error(Exception ex) {
        if (ex instanceof ErrorResponse error) {
            HttpStatusCode estado = error.getStatusCode();
            String mensaje = ex instanceof ResponseStatusException rse && rse.getReason() != null
                ? rse.getReason()
                : mensajePorEstado(estado.value());
            return ResponseEntity.status(estado).body(ApiError.of(estado.value(), mensaje));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiError.of(500, "Ocurrió un error inesperado"));
    }

    private static String mensajePorEstado(int estado) {
        return switch (estado) {
            case 400 -> "Solicitud inválida";
            case 404 -> "Recurso no encontrado";
            case 405 -> "Método no permitido";
            case 415 -> "Tipo de contenido no soportado";
            default -> "No se pudo procesar la solicitud";
        };
    }
}
