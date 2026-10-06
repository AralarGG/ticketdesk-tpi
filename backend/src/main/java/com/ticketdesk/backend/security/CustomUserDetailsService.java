package com.ticketdesk.backend.security;

import com.ticketdesk.backend.model.Usuario;
import com.ticketdesk.backend.model.UsuarioRol;
import com.ticketdesk.backend.repository.UsuarioRepository;
import com.ticketdesk.backend.repository.UsuarioRolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Le dice a Spring Security cómo buscar un usuario y qué roles tiene, para
 * poder validar el login y las credenciales.
 *
 * El "username" que recibe no es un email suelto: es la credencial
 * compuesta "empresaId:email" (ver CredencialUsuario), porque el email
 * es único por empresa, no de forma global.
 *
 * Los permisos del usuario son la unión de su rol principal (usuarios.rol)
 * y los roles adicionales de la tabla usuario_roles (regla RN4). Un usuario
 * desactivado (activo = false) no puede iniciar sesión (regla RN2).
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;

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

        Set<GrantedAuthority> autoridades = new LinkedHashSet<>();
        autoridades.add(new SimpleGrantedAuthority(usuario.getRol().name()));
        for (UsuarioRol usuarioRol : usuarioRolRepository.findByUsuarioId(usuario.getId())) {
            autoridades.add(new SimpleGrantedAuthority(usuarioRol.getRol().name()));
        }

        return new User(
                credencialCompuesta, // se mantiene el formato compuesto para poder re-resolverlo en cada request
                usuario.getPasswordHash(),
                usuario.isActivo(),
                true,
                true,
                true,
                autoridades
        );
    }
}
