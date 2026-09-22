package com.ticketdesk.backend.repository;

import com.ticketdesk.backend.model.UsuarioRol;
import com.ticketdesk.backend.model.enums.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UsuarioRolRepository extends JpaRepository<UsuarioRol, UUID> {

    /**
     * Todos los roles habilitados de un usuario (incluye el principal).
     */
    List<UsuarioRol> findByUsuarioId(UUID usuarioId);

    /**
     * "Checking de rol previo" pedido por el tutor: antes de asignarle
     * un rol nuevo a un usuario, hay que verificar que no lo tenga ya
     * (la restricción unique(usuario_id, rol) también lo impediría a
     * nivel de base de datos, pero chequear antes permite dar un
     * mensaje de error claro en vez de que falle la inserción).
     */
    boolean existsByUsuarioIdAndRol(UUID usuarioId, Rol rol);
}
