package com.ticketdesk.backend.security;

import com.ticketdesk.backend.model.Usuario;
import com.ticketdesk.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Le dice a Spring Security cómo buscar un usuario y qué rol/permiso
 * tiene, para poder validar el login y las credenciales.
 *
 * El "username" que recibe no es un email suelto: es la credencial
 * compuesta "empresaId:email" (ver CredencialUsuario), porque el email
 * es único por empresa, no de forma global. Esto resuelve el caso de
 * una persona con cuentas en más de una empresa usando el mismo correo.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String credencialCompuesta) throws UsernameNotFoundException {
        CredencialUsuario credencial;
        try {
            credencial = CredencialUsuario.parsear(credencialCompuesta);
        } catch (IllegalArgumentException e) {
            throw new UsernameNotFoundException(e.getMessage());
        }

        Usuario usuario = usuarioRepository.findByEmpresaIdAndEmail(credencial.empresaId(), credencial.email())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado: " + credencial.email() + " en la empresa indicada"));

        return new User(
                credencialCompuesta, // se mantiene el mismo formato compuesto para poder re-resolverlo en cada request
                usuario.getPasswordHash(),
                List.of(new SimpleGrantedAuthority(usuario.getRol().name()))
        );
    }
}
