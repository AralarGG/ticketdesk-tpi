package com.ticketdesk.backend.model;

import com.ticketdesk.backend.model.enums.Rol;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representa tanto a clientes como a agentes, diferenciados por el campo "rol".
 *
 * Regla de negocio (ver DER): los usuarios nunca se eliminan físicamente.
 * Se desactivan mediante el campo "activo". Un agente con tickets asignados
 * debe reasignarlos antes de poder desactivarse (esa validación va en el
 * service, no en la entidad).
 *
 * Regla de negocio (corrección pedida por el tutor): el email es único por
 * empresa, no de forma global. Esto contempla el caso de una persona que
 * trabaja como agente para más de una empresa cliente con el mismo correo:
 * puede tener una cuenta en cada una, sin conflicto. La clave única real
 * es la combinación (empresa_id, email).
 */
@Entity
@Table(
    name = "usuarios",
    uniqueConstraints = @UniqueConstraint(columnNames = {"empresa_id", "email"})
)
@Getter
@Setter
public class Usuario {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    private boolean activo = true;

    @Column(name = "fecha_alta")
    private LocalDateTime fechaAlta;

    @PrePersist
    protected void onCreate() {
        fechaAlta = LocalDateTime.now();
    }
}
