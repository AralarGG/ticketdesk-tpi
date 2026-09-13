package com.ticketdesk.backend.model;

import com.ticketdesk.backend.model.enums.CampoModificado;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Historial de cambios relevantes de cada ticket: no solo el estado,
 * sino también reasignación de agente, cambio de categoría o de prioridad.
 */
@Entity
@Table(name = "ticket_history")
@Getter
@Setter
public class TicketHistory {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Enumerated(EnumType.STRING)
    @Column(name = "campo_modificado", nullable = false)
    private CampoModificado campoModificado;

    @Column(name = "valor_anterior", length = 100)
    private String valorAnterior;

    @Column(name = "valor_nuevo", length = 100)
    private String valorNuevo;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario; // quién hizo el cambio

    @Column(columnDefinition = "TEXT")
    private String motivo;

    private LocalDateTime fecha;

    @PrePersist
    protected void onCreate() {
        fecha = LocalDateTime.now();
    }
}
