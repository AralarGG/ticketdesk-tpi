package com.ticketdesk.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String rol;
    private String usuarioId;
    private String empresaId;
}
