package com.ticketdesk.backend.dto;

import com.ticketdesk.backend.model.Usuario;
import com.ticketdesk.backend.model.enums.Rol;
import lombok.Getter;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Datos públicos de un usuario. Nunca incluye la contraseña ni su hash. */
@Getter
public class UsuarioResponse {

    private final UUID id;
    private final String nombre;
    private final String email;
    private final String rol;          // rol principal
    private final List<String> roles;  // todos los roles habilitados, incluido el principal
    private final boolean activo;
    private final UUID empresaId;
    private final String empresaNombre;

    public UsuarioResponse(Usuario usuario, Set<Rol> roles) {
        this.id = usuario.getId();
        this.nombre = usuario.getNombre();
        this.email = usuario.getEmail();
        this.rol = usuario.getRol().name();
        this.roles = roles.stream().map(Rol::name).sorted().collect(Collectors.toList());
        this.activo = usuario.isActivo();
        this.empresaId = usuario.getEmpresa().getId();
        this.empresaNombre = usuario.getEmpresa().getNombre();
    }
}
