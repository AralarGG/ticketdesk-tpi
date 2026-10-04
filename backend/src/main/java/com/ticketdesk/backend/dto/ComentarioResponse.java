package com.ticketdesk.backend.dto;

import com.ticketdesk.backend.model.Comentario;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class ComentarioResponse {

    private final UUID id;
    private final String contenido;
    private final UUID usuarioId;
    private final String usuarioNombre;
    private final String usuarioRol;
    private final LocalDateTime fecha;

    public ComentarioResponse(Comentario comentario) {
        this.id = comentario.getId();
        this.contenido = comentario.getContenido();
        this.usuarioId = comentario.getUsuario().getId();
        this.usuarioNombre = comentario.getUsuario().getNombre();
        this.usuarioRol = comentario.getUsuario().getRol().name();
        this.fecha = comentario.getFecha();
    }
}
