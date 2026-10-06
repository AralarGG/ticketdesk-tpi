package com.ticketdesk.backend.controller;

import com.ticketdesk.backend.dto.AgregarRolRequest;
import com.ticketdesk.backend.dto.CrearUsuarioRequest;
import com.ticketdesk.backend.dto.UsuarioResponse;
import com.ticketdesk.backend.model.Usuario;
import com.ticketdesk.backend.security.UsuarioActual;
import com.ticketdesk.backend.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * GET    /api/v1/usuarios/me                    datos del usuario logueado
 * GET    /api/v1/usuarios/agentes               agentes y supervisores activos (para asignar)
 * GET    /api/v1/usuarios                       todos los usuarios de la empresa (administrador)
 * POST   /api/v1/usuarios                       alta de usuario (administrador)
 * POST   /api/v1/usuarios/{id}/roles            sumar un rol (administrador)
 * PATCH  /api/v1/usuarios/{id}/desactivar       desactivar, nunca borrar (administrador)
 * PATCH  /api/v1/usuarios/{id}/activar          reactivar (administrador)
 */
@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioActual usuarioActual;

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> yo(Authentication authentication) {
        return ResponseEntity.ok(respuesta(usuarioActual.obtener(authentication)));
    }

    @GetMapping("/agentes")
    @PreAuthorize("hasAnyRole('AGENT', 'SUPERVISOR', 'ADMIN')")
    public ResponseEntity<List<UsuarioResponse>> agentes(Authentication authentication) {
        Usuario actor = usuarioActual.obtener(authentication);
        return ResponseEntity.ok(usuarioService.listarPersonalDeSoporte(actor).stream()
                .map(this::respuesta)
                .collect(Collectors.toList()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UsuarioResponse>> todos(Authentication authentication) {
        Usuario admin = usuarioActual.obtener(authentication);
        return ResponseEntity.ok(usuarioService.listarTodos(admin).stream()
                .map(this::respuesta)
                .collect(Collectors.toList()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> crear(
            @Valid @RequestBody CrearUsuarioRequest request, Authentication authentication) {
        Usuario admin = usuarioActual.obtener(authentication);
        return ResponseEntity.status(201).body(respuesta(usuarioService.crear(request, admin)));
    }

    @PostMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> agregarRol(
            @PathVariable UUID id, @Valid @RequestBody AgregarRolRequest request, Authentication authentication) {
        Usuario admin = usuarioActual.obtener(authentication);
        return ResponseEntity.ok(respuesta(usuarioService.agregarRol(id, request.getRol(), admin)));
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> desactivar(@PathVariable UUID id, Authentication authentication) {
        Usuario admin = usuarioActual.obtener(authentication);
        return ResponseEntity.ok(respuesta(usuarioService.desactivar(id, admin)));
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> activar(@PathVariable UUID id, Authentication authentication) {
        Usuario admin = usuarioActual.obtener(authentication);
        return ResponseEntity.ok(respuesta(usuarioService.activar(id, admin)));
    }

    private UsuarioResponse respuesta(Usuario usuario) {
        return new UsuarioResponse(usuario, usuarioService.rolesDe(usuario));
    }
}
