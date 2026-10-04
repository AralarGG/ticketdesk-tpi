package com.ticketdesk.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Corrección pedida por el tutor: reemplaza a la antigua "TicketHistory",
 * que solo podía trackear cambios de un Ticket. Esta tabla es genérica:
 * puede registrar cambios de CUALQUIER entidad del sistema (Ticket,
 * Usuario, Empresa, ConfiguracionEmpresa, etc.), con la estructura
 * pedida: entidad / campo / valor anterior / valor nuevo.
 *
 * Trade-off aceptado: como "entidadId" puede apuntar a distintas tablas
 * según el valor de "entidad", no puede ser una relación JPA tipada
 * (@ManyToOne) como antes con Ticket. Se guarda como UUID simple, sin
 * integridad referencial a nivel de base de datos para ese campo
 * puntual. La relación con Usuario (quién hizo el cambio) sí se
 * mantiene tipada, porque esa tabla es siempre la misma sin importar
 * qué entidad cambió.
 */
@Entity
@Table(name = "historial_cambios")
@Getter
@Setter
public class HistorialCambios {

    @Id
    @GeneratedValue
    private UUID id;

    /**
     * Nombre de la entidad afectada por el cambio, ej: "Ticket",
     * "Usuario", "Empresa", "ConfiguracionEmpresa". Texto libre en vez
     * de enum fijo, para no tener que tocar código cada vez que se
     * agrega auditoría a una entidad nueva.
     */
    @Column(nullable = false, length = 100)
    private String entidad;

    /**
     * Id del registro específico que cambió, dentro de la tabla que
     * indica "entidad" (ej. el id de ese Ticket puntual, o de ese
     * Usuario puntual).
     */
    @Column(name = "entidad_id", nullable = false)
    private UUID entidadId;

    /**
     * Nombre del campo que cambió (ej: "estado", "rol", "activo").
     * Texto libre en vez de enum fijo, por el mismo motivo que "entidad":
     * cada tipo de entidad tiene sus propios campos.
     */
    @Column(name = "campo_modificado", nullable = false, length = 100)
    private String campoModificado;

    @Column(name = "valor_anterior", length = 255)
    private String valorAnterior;

    @Column(name = "valor_nuevo", length = 255)
    private String valorNuevo;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario; // quién hizo el cambio; nulo si fue automático (ver TicketAutoCierreScheduler)

    @Column(columnDefinition = "TEXT")
    private String motivo;

    private LocalDateTime fecha;

    @PrePersist
    protected void onCreate() {
        fecha = LocalDateTime.now();
    }
}
