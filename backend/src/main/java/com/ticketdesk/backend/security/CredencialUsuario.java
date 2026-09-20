package com.ticketdesk.backend.security;

import java.util.UUID;

/**
 * Spring Security identifica al usuario autenticado con un único String
 * ("username"). Como nuestra clave real de login es la combinación
 * (empresa_id, email), no el email solo, usamos un identificador
 * compuesto con el formato "empresaId:email" como ese username.
 *
 * Esta clase centraliza cómo se arma y se interpreta ese formato, para
 * no duplicar el parseo en cada lugar que lo necesita (login, filtro
 * JWT, controllers).
 */
public record CredencialUsuario(UUID empresaId, String email) {

    public static String componer(UUID empresaId, String email) {
        return empresaId + ":" + email;
    }

    public static CredencialUsuario parsear(String credencialCompuesta) {
        String[] partes = credencialCompuesta.split(":", 2);
        if (partes.length != 2) {
            throw new IllegalArgumentException(
                    "Formato de credencial inválido, se espera 'empresaId:email'");
        }
        try {
            return new CredencialUsuario(UUID.fromString(partes[0]), partes[1]);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("empresaId inválido en la credencial", e);
        }
    }
}
