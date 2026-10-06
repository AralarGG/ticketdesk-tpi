package com.ticketdesk.backend.dto;

import com.ticketdesk.backend.model.HistorialCambios;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class HistorialResponse {

    private final UUID id;
    private final String campoModificado;
    private final String valorAnterior;
    private final String valorNuevo;
    private final String realizadoPor; // nombre de la persona, o "Sistema" si fue un cambio automático
    private final String motivo;
    private final LocalDateTime fecha;

    public HistorialResponse(HistorialCambios historial) {
        this.id = historial.getId();
        this.campoModificado = historial.getCampoModificado();
        this.valorAnterior = historial.getValorAnterior();
        this.valorNuevo = historial.getValorNuevo();
        this.realizadoPor = historial.getUsuario() != null ? historial.getUsuario().getNombre() : "Sistema";
        this.motivo = historial.getMotivo();
        this.fecha = historial.getFecha();
    }
}
