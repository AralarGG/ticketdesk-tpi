package com.ticketdesk.backend.dto;

import com.ticketdesk.backend.model.Adjunto;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class AdjuntoResponse {

    private final UUID id;
    private final String url;
    private final String tipo;
    private final UUID comentarioId;
    private final LocalDateTime fechaSubida;

    public AdjuntoResponse(Adjunto adjunto) {
        this.id = adjunto.getId();
        this.url = adjunto.getUrl();
        this.tipo = adjunto.getTipo();
        this.comentarioId = adjunto.getComentario() != null ? adjunto.getComentario().getId() : null;
        this.fechaSubida = adjunto.getFechaSubida();
    }
}
