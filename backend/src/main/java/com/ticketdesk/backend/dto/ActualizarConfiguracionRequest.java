package com.ticketdesk.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.util.List;

/**
 * Todos los campos son opcionales: solo se actualizan los que vienen
 * informados (semántica de PATCH).
 */
@Getter
@Setter
public class ActualizarConfiguracionRequest {

    private List<String> modulosHabilitados;
    private LocalTime ventanaMantenimientoInicio;
    private LocalTime ventanaMantenimientoFin;
}
