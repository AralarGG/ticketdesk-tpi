package com.ticketdesk.backend.repository;

import com.ticketdesk.backend.model.UsuarioRol;
import com.ticketdesk.backend.model.enums.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
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

    /**
     * Roles asignados a usuarios activos de una empresa, filtrando por rol.
     * Sirve para encontrar personal que tiene un rol de soporte como rol
     * adicional (ej. un usuario cuyo rol principal es agente y que además
     * es supervisor).
     */
    List<UsuarioRol> findByRolInAndUsuario_Empresa_IdAndUsuario_ActivoTrue(Collection<Rol> roles, UUID empresaId);
}
