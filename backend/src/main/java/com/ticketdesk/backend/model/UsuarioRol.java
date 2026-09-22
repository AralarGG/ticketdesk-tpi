package com.ticketdesk.backend.model;

import com.ticketdesk.backend.model.enums.Rol;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Corrección pedida por el tutor: el DER original solo permitía un rol
 * fijo por usuario (campo Usuario.rol). Se agrega esta tabla intermedia
 * para soportar el caso real de una empresa grande donde una misma
 * persona puede tener varios roles a la vez (ej. Agente y Supervisor).
 *
 * Usuario.rol se mantiene como el "rol principal" (el que se usa por
 * defecto al loguearse y el que va en el claim del JWT), mientras que
 * esta tabla lista TODOS los roles habilitados para ese usuario,
 * incluyendo el principal. Antes de agregar un rol nuevo, el service
 * debe chequear que el usuario no lo tenga ya asignado (ver
 * UsuarioRolRepository.existsByUsuarioIdAndRol) — "checking de rol
 * previo" pedido por el tutor.
 */
@Entity
@Table(
    name = "usuario_roles",
    uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "rol"})
)
@Getter
@Setter
public class UsuarioRol {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    @Column(name = "fecha_asignacion")
    private LocalDateTime fechaAsignacion;

    @PrePersist
    protected void onCreate() {
        fechaAsignacion = LocalDateTime.now();
    }
}
