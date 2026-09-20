package com.ticketdesk.backend.repository;

import com.ticketdesk.backend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
