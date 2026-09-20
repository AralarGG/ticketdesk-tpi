package com.ticketdesk.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Corrección estructural (pedida por el tutor): el login ahora requiere
 * también la empresa, no solo email+password. Esto resuelve la
 * ambigüedad de una persona con cuentas en más de una empresa usando
 * el mismo email: el sistema no necesita "adivinar" para cuál empresa
 * está intentando entrar, porque el frontend ya se lo indica (por
 * ejemplo, mediante un selector de empresa o un portal por subdominio).
 */
@Getter
@Setter
public class LoginRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

    @NotNull
    private UUID empresaId;
}
