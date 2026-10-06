package com.ticketdesk.backend.dto;

import com.ticketdesk.backend.model.Adjunto;
import com.ticketdesk.backend.model.Comentario;
import com.ticketdesk.backend.model.Ticket;
import com.ticketdesk.backend.model.enums.CanalOrigen;
import com.ticketdesk.backend.model.enums.EstadoTicket;
import com.ticketdesk.backend.model.enums.NivelAtencion;
import com.ticketdesk.backend.model.enums.Prioridad;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Detalle completo de un ticket. Existe para no exponer nunca las entidades
 * JPA directamente: una entidad Ticket arrastra al Usuario y, con él, datos
 * que no deben salir de la API (como el hash de la contraseña).
 */
@Getter
public class TicketDetalleResponse {

    private final UUID id;
    private final String titulo;
    private final String descripcion;
    private final EstadoTicket estado;
    private final Prioridad prioridad;
    private final NivelAtencion nivelAtencion;
    private final int slaHoras;
    private final LocalDateTime vencimientoSla;
    private final boolean reincidente;
    private final boolean esReclamoFormal;
    private final String responsableExterno;
    private final CanalOrigen canalOrigen;
    private final String audioUrl;
    private final String transcripcionOriginal;
    private final UUID categoriaId;
    private final String categoria;
    private final UUID clienteId;
    private final String clienteNombre;
    private final UUID agenteId;
    private final String agenteNombre;
    private final LocalDateTime fechaCreacion;
    private final LocalDateTime fechaActualizacion;
    private final LocalDateTime fechaResuelto;
    private final List<ComentarioResponse> comentarios;
    private final List<AdjuntoResponse> adjuntos;

    public TicketDetalleResponse(Ticket ticket, List<Comentario> comentarios, List<Adjunto> adjuntos) {
        this.id = ticket.getId();
        this.titulo = ticket.getTitulo();
        this.descripcion = ticket.getDescripcion();
        this.estado = ticket.getEstado();
        this.prioridad = ticket.getPrioridad();
        this.nivelAtencion = ticket.getNivelAtencion();
        this.slaHoras = ticket.getNivelAtencion().getHorasObjetivoResolucion();
        this.vencimientoSla = ticket.getFechaCreacion() != null
                ? ticket.getFechaCreacion().plusHours(this.slaHoras) : null;
        this.reincidente = ticket.isReincidente();
        this.esReclamoFormal = ticket.isEsReclamoFormal();
        this.responsableExterno = ticket.getResponsableExterno();
        this.canalOrigen = ticket.getCanalOrigen();
        this.audioUrl = ticket.getAudioUrl();
        this.transcripcionOriginal = ticket.getTranscripcionOriginal();
        this.categoriaId = ticket.getCategoria().getId();
        this.categoria = ticket.getCategoria().getNombre();
        this.clienteId = ticket.getUsuario().getId();
        this.clienteNombre = ticket.getUsuario().getNombre();
        this.agenteId = ticket.getAgente() != null ? ticket.getAgente().getId() : null;
        this.agenteNombre = ticket.getAgente() != null ? ticket.getAgente().getNombre() : null;
        this.fechaCreacion = ticket.getFechaCreacion();
        this.fechaActualizacion = ticket.getFechaActualizacion();
        this.fechaResuelto = ticket.getFechaResuelto();
        this.comentarios = comentarios.stream().map(ComentarioResponse::new).collect(Collectors.toList());
        this.adjuntos = adjuntos.stream().map(AdjuntoResponse::new).collect(Collectors.toList());
    }
}
