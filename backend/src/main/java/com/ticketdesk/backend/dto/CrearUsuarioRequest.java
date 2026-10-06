package com.ticketdesk.backend.dto;

import com.ticketdesk.backend.model.enums.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CrearUsuarioRequest {

    @NotBlank
    private String nombre;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, message = "debe tener al menos 8 caracteres")
    private String password;

    @NotNull
    private Rol rol;

    /** Roles extra opcionales (ej. un agente que además es supervisor). */
    private List<Rol> rolesAdicionales;
}
