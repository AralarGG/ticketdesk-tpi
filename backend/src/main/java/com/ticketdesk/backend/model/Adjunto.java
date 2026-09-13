package com.ticketdesk.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Fotos u otros archivos asociados a un ticket o comentario.
 *
 * Regla de negocio (ver DER): solo pueden ser subidos por usuarios con
 * rol ROLE_USER (clientes). Esta validación se aplica en el service,
 * antes de aceptar la carga de un archivo, no en esta entidad.
 */
@Entity
@Table(name = "adjuntos")
@Getter
@Setter
public class Adjunto {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne
    @JoinColumn(name = "comentario_id")
    private Comentario comentario; // opcional

    @Column(nullable = false, length = 500)
    private String url; // URL en Cloudinary, nunca el archivo en sí

    @Column(length = 50)
    private String tipo;

    @Column(name = "fecha_subida")
    private LocalDateTime fechaSubida;

    @PrePersist
    protected void onCreate() {
        fechaSubida = LocalDateTime.now();
    }
}
