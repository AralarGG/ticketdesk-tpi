package com.ticketdesk.backend.repository;

import com.ticketdesk.backend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ticketdesk.backend.model.enums.Rol;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    /**
     * Única forma de buscar un usuario por email: siempre junto con la
     * empresa, porque el email es único por empresa, no global (ver
     * Usuario.java y CredencialUsuario). El login ahora exige empresaId,
     * así que esta búsqueda nunca es ambigua.
     */
    Optional<Usuario> findByEmpresaIdAndEmail(UUID empresaId, String email);

    boolean existsByEmpresaIdAndEmail(UUID empresaId, String email);

    List<Usuario> findByEmpresaIdOrderByNombreAsc(UUID empresaId);

    // Personal activo de una empresa según su rol principal
    List<Usuario> findByEmpresaIdAndActivoTrueAndRolIn(UUID empresaId, Collection<Rol> roles);
}
