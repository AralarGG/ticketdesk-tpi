package com.ticketdesk.backend.controller;

import com.ticketdesk.backend.dto.ActualizarConfiguracionRequest;
import com.ticketdesk.backend.dto.ConfiguracionResponse;
import com.ticketdesk.backend.model.Usuario;
import com.ticketdesk.backend.security.UsuarioActual;
import com.ticketdesk.backend.service.ConfiguracionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * GET   /api/v1/empresas/{id}/configuracion
 * PATCH /api/v1/empresas/{id}/configuracion
 *
 * La lectura está abierta a cualquier usuario de la misma empresa, porque el
 * frontend necesita saber qué módulos están habilitados para armar el menú.
 * La modificación es exclusiva del administrador. En ambos casos un usuario
 * solo puede operar sobre la configuración de su propia empresa.
 */
@RestController
@RequestMapping("/api/v1/empresas/{empresaId}/configuracion")
@RequiredArgsConstructor
public class EmpresaController {

    private final ConfiguracionService configuracionService;
    private final UsuarioActual usuarioActual;

    @GetMapping
    public ResponseEntity<ConfiguracionResponse> obtener(@PathVariable UUID empresaId, Authentication authentication) {
        verificarMismaEmpresa(empresaId, usuarioActual.obtener(authentication));
        return ResponseEntity.ok(new ConfiguracionResponse(configuracionService.obtener(empresaId)));
    }

    @PatchMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ConfiguracionResponse> actualizar(
            @PathVariable UUID empresaId,
            @RequestBody ActualizarConfiguracionRequest request,
            Authentication authentication
    ) {
        Usuario usuario = usuarioActual.obtener(authentication);
        verificarMismaEmpresa(empresaId, usuario);
        return ResponseEntity.ok(new ConfiguracionResponse(
                configuracionService.actualizar(empresaId, request, usuario)));
    }

    private void verificarMismaEmpresa(UUID empresaId, Usuario usuario) {
        if (!usuario.getEmpresa().getId().equals(empresaId)) {
            throw new AccessDeniedException("No podés acceder a la configuración de otra empresa");
        }
    }
}
