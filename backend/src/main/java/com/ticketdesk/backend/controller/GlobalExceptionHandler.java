package com.ticketdesk.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Traduce las excepciones de negocio y de seguridad a respuestas HTTP con el
 * código correcto y un mensaje legible para el frontend, en lugar de un 500
 * generico.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> noEncontrado(NoSuchElementException e) {
        return respuesta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> accesoDenegado(AccessDeniedException e) {
        return respuesta(HttpStatus.FORBIDDEN, "No tenés permisos para realizar esta acción");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> credencialesInvalidas(BadCredentialsException e) {
        return respuesta(HttpStatus.UNAUTHORIZED, "Email, contraseña o empresa incorrectos");
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, String>> usuarioDesactivado(DisabledException e) {
        return respuesta(HttpStatus.UNAUTHORIZED, "El usuario está desactivado");
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> conflicto(IllegalStateException e) {
        return respuesta(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validacion(MethodArgumentNotValidException e) {
        String detalle = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return respuesta(HttpStatus.BAD_REQUEST, "Datos inválidos. " + detalle);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> archivoGrande(MaxUploadSizeExceededException e) {
        return respuesta(HttpStatus.PAYLOAD_TOO_LARGE, "El archivo supera el tamaño máximo permitido (10MB)");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> estadoExplicito(ResponseStatusException e) {
        HttpStatus estado = HttpStatus.resolve(e.getStatusCode().value());
        return respuesta(estado != null ? estado : HttpStatus.INTERNAL_SERVER_ERROR, e.getReason());
    }

    private ResponseEntity<Map<String, String>> respuesta(HttpStatus estado, String mensaje) {
        String texto = mensaje != null ? mensaje : estado.getReasonPhrase();
        return ResponseEntity.status(estado).body(Map.of("error", estado.getReasonPhrase(), "mensaje", texto));
    }
}
