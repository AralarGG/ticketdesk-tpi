package com.ticketdesk.backend.security;

import com.ticketdesk.backend.model.Usuario;
import com.ticketdesk.backend.model.UsuarioRol;
import com.ticketdesk.backend.model.enums.Rol;
import com.ticketdesk.backend.repository.UsuarioRolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/**
 * Responde qué roles tiene un usuario. Un usuario puede tener más de un rol
 * (regla RN4): el principal (usuarios.rol) más los adicionales listados en
 * usuario_roles.
 */
@Component
@RequiredArgsConstructor
public class PermisosUsuario {

    private final UsuarioRolRepository usuarioRolRepository;

    public Set<Rol> rolesDe(Usuario usuario) {
        Set<Rol> roles = EnumSet.of(usuario.getRol());
        for (UsuarioRol usuarioRol : usuarioRolRepository.findByUsuarioId(usuario.getId())) {
            roles.add(usuarioRol.getRol());
        }
        return roles;
    }

    public boolean tieneAlgunRol(Usuario usuario, Rol... buscados) {
        Set<Rol> roles = rolesDe(usuario);
        for (Rol buscado : buscados) {
            if (roles.contains(buscado)) {
                return true;
            }
        }
        return false;
    }

    /** Personal de soporte con visibilidad sobre todos los tickets de su empresa. */
    public boolean esPersonal(Usuario usuario) {
        return tieneAlgunRol(usuario, Rol.ROLE_AGENT, Rol.ROLE_SUPERVISOR, Rol.ROLE_ADMIN);
    }

    /** Quienes pueden trabajar un ticket: tomarlo, cambiar su estado, escalarlo. */
    public boolean puedeGestionarTickets(Usuario usuario) {
        return tieneAlgunRol(usuario, Rol.ROLE_AGENT, Rol.ROLE_SUPERVISOR);
    }

    public boolean esSupervisor(Usuario usuario) {
        return tieneAlgunRol(usuario, Rol.ROLE_SUPERVISOR);
    }
}
