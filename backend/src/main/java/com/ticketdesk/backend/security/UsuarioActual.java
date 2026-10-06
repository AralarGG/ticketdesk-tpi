package com.ticketdesk.backend.security;

import com.ticketdesk.backend.model.Usuario;
import com.ticketdesk.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.NoSuchElementException;

/**
 * Resuelve a qué Usuario corresponde la Authentication de la request.
 * El nombre de la autenticación es la credencial compuesta "empresaId:email"
 * (ver CredencialUsuario).
 */
@Component
@RequiredArgsConstructor
public class UsuarioActual {

    private final UsuarioRepository usuarioRepository;

    public Usuario obtener(Authentication authentication) {
        CredencialUsuario credencial = CredencialUsuario.parsear(authentication.getName());
        return usuarioRepository.findByEmpresaIdAndEmail(credencial.empresaId(), credencial.email())
                .orElseThrow(() -> new NoSuchElementException("Usuario autenticado no encontrado"));
    }
}
