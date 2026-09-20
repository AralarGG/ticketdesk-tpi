package com.ticketdesk.backend.dto;

import com.ticketdesk.backend.model.Ticket;
import com.ticketdesk.backend.model.enums.EstadoTicket;
import com.ticketdesk.backend.model.enums.NivelAtencion;
import com.ticketdesk.backend.model.enums.Prioridad;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Versión resumida de un ticket, usada en listados (GET /tickets).
 * El detalle completo (con comentarios y adjuntos) va en TicketDetalleResponse.
 */
@Getter
public class TicketResumenResponse {

    private final UUID id;
    private final String titulo;
    private final EstadoTicket estado;
    private final Prioridad prioridad;
    private final NivelAtencion nivelAtencion;
    private final String categoria;
    private final String agenteAsignado; // nombre, o null si no está asignado
    private final LocalDateTime fechaCreacion;

    public TicketResumenResponse(Ticket ticket) {
        this.id = ticket.getId();
        this.titulo = ticket.getTitulo();
        this.estado = ticket.getEstado();
        this.prioridad = ticket.getPrioridad();
        this.nivelAtencion = ticket.getNivelAtencion();
        this.categoria = ticket.getCategoria().getNombre();
        this.agenteAsignado = ticket.getAgente() != null ? ticket.getAgente().getNombre() : null;
        this.fechaCreacion = ticket.getFechaCreacion();
    }
}
