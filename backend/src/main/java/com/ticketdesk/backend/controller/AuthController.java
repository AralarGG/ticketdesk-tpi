package com.ticketdesk.backend.controller;

import com.ticketdesk.backend.dto.AuthResponse;
import com.ticketdesk.backend.dto.LoginRequest;
import com.ticketdesk.backend.dto.RegisterRequest;
import com.ticketdesk.backend.model.Empresa;
import com.ticketdesk.backend.model.Usuario;
import com.ticketdesk.backend.model.enums.Rol;
import com.ticketdesk.backend.repository.EmpresaRepository;
import com.ticketdesk.backend.repository.UsuarioRepository;
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

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());

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
        if (usuarioRepository.existsByEmail(request.getEmail())) {
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

        UserDetails userDetails = userDetailsService.loadUserByUsername(usuario.getEmail());
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
