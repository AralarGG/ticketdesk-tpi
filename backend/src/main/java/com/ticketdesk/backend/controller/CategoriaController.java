package com.ticketdesk.backend.controller;

import com.ticketdesk.backend.dto.CategoriaResponse;
import com.ticketdesk.backend.dto.CrearCategoriaRequest;
import com.ticketdesk.backend.security.UsuarioActual;
import com.ticketdesk.backend.service.CategoriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * GET  /api/v1/categorias   categorías visibles para la empresa del usuario
 * POST /api/v1/categorias   alta de una categoría propia (administrador)
 */
@RestController
@RequestMapping("/api/v1/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService categoriaService;
    private final UsuarioActual usuarioActual;

    @GetMapping
    public ResponseEntity<List<CategoriaResponse>> listar(Authentication authentication) {
        return ResponseEntity.ok(categoriaService.listarVisibles(usuarioActual.obtener(authentication)).stream()
                .map(CategoriaResponse::new)
                .collect(Collectors.toList()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoriaResponse> crear(
            @Valid @RequestBody CrearCategoriaRequest request, Authentication authentication) {
        return ResponseEntity.status(201).body(new CategoriaResponse(
                categoriaService.crearPropia(request, usuarioActual.obtener(authentication))));
    }
}
