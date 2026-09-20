package com.ticketdesk.backend.controller;

import com.ticketdesk.backend.dto.AuthResponse;
import com.ticketdesk.backend.dto.LoginRequest;
import com.ticketdesk.backend.dto.RegisterRequest;
import com.ticketdesk.backend.model.Empresa;
import com.ticketdesk.backend.model.Usuario;
import com.ticketdesk.backend.model.enums.Rol;
import com.ticketdesk.backend.repository.EmpresaRepository;
import com.ticketdesk.backend.repository.UsuarioRepository;
import com.ticketdesk.backend.security.CredencialUsuario;
import com.ticketdesk.backend.security.CustomUserDetailsService;
import com.ticketdesk.backend.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

/**
 * Corresponde a la especificación de endpoints:
 * POST /api/v1/auth/login
 * POST /api/v1/auth/register
 * (ver /docs/endpoints-api-ticketdesk.md)
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Requiere email+password+empresaId (no solo email+password). Esto es
     * una corrección estructural: el email es único por empresa, no global,
     * así que el login necesita saber a qué empresa se está entrando desde
     * el principio, sin ambigüedad, para el caso de una persona con cuentas
     * en más de una empresa usando el mismo correo.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        String credencialCompuesta = CredencialUsuario.componer(request.getEmpresaId(), request.getEmail());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(credencialCompuesta, request.getPassword())
        );

        Usuario usuario = usuarioRepository.findByEmpresaIdAndEmail(request.getEmpresaId(), request.getEmail())
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(credencialCompuesta);

        String token = jwtService.generateToken(
                userDetails,
                usuario.getRol().name(),
                usuario.getId().toString(),
                usuario.getEmpresa().getId().toString()
        );

        return ResponseEntity.ok(new AuthResponse(
                token,
                usuario.getRol().name(),
                usuario.getId().toString(),
                usuario.getEmpresa().getId().toString()
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        if (usuarioRepository.existsByEmpresaIdAndEmail(request.getEmpresaId(), request.getEmail())) {
            return ResponseEntity.badRequest().build();
        }

        Empresa empresa = empresaRepository.findById(request.getEmpresaId())
                .orElseThrow(() -> new NoSuchElementException("Empresa no encontrada"));

        Usuario usuario = new Usuario();
        usuario.setNombre(request.getNombre());
        usuario.setEmail(request.getEmail());
        usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        usuario.setRol(Rol.ROLE_USER); // el alta pública siempre crea clientes, nunca agentes
        usuario.setEmpresa(empresa);

        usuarioRepository.save(usuario);

        String credencialCompuesta = CredencialUsuario.componer(empresa.getId(), usuario.getEmail());
        UserDetails userDetails = userDetailsService.loadUserByUsername(credencialCompuesta);
        String token = jwtService.generateToken(
                userDetails,
                usuario.getRol().name(),
                usuario.getId().toString(),
                empresa.getId().toString()
        );

        return ResponseEntity.ok(new AuthResponse(
                token,
                usuario.getRol().name(),
                usuario.getId().toString(),
                empresa.getId().toString()
        ));
    }
}
