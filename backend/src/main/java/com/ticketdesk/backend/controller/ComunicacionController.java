package com.ticketdesk.backend.controller;

import com.ticketdesk.backend.dto.AdjuntoResponse;
import com.ticketdesk.backend.dto.ComentarioResponse;
import com.ticketdesk.backend.dto.CrearComentarioRequest;
import com.ticketdesk.backend.model.Usuario;
import com.ticketdesk.backend.security.UsuarioActual;
import com.ticketdesk.backend.service.ComunicacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * POST   /api/v1/tickets/{ticketId}/comentarios
 * GET    /api/v1/tickets/{ticketId}/comentarios
 * POST   /api/v1/tickets/{ticketId}/adjuntos   (multipart, solo clientes)
 * GET    /api/v1/tickets/{ticketId}/adjuntos
 * (ver endpoints-api-ticketdesk.md)
 */
@RestController
@RequestMapping("/api/v1/tickets/{ticketId}")
@RequiredArgsConstructor
public class ComunicacionController {

    private final ComunicacionService comunicacionService;
    private final UsuarioActual usuarioActual;

    @PostMapping("/comentarios")
    public ResponseEntity<ComentarioResponse> crearComentario(
            @PathVariable UUID ticketId,
            @Valid @RequestBody CrearComentarioRequest request,
            Authentication authentication
    ) {
        Usuario autor = usuarioActual.obtener(authentication);
        return ResponseEntity.status(201)
                .body(new ComentarioResponse(comunicacionService.crearComentario(ticketId, request, autor)));
    }

    @GetMapping("/comentarios")
    public ResponseEntity<List<ComentarioResponse>> listarComentarios(
            @PathVariable UUID ticketId, Authentication authentication) {
        Usuario actor = usuarioActual.obtener(authentication);
        return ResponseEntity.ok(comunicacionService.listarComentarios(ticketId, actor).stream()
                .map(ComentarioResponse::new)
                .collect(Collectors.toList()));
    }

    @PostMapping(value = "/adjuntos", consumes = "multipart/form-data")
    public ResponseEntity<AdjuntoResponse> subirAdjunto(
            @PathVariable UUID ticketId,
            @RequestParam(required = false) UUID comentarioId,
            @RequestParam("archivo") MultipartFile archivo,
            Authentication authentication
    ) throws IOException {
        Usuario autor = usuarioActual.obtener(authentication);
        return ResponseEntity.status(201)
                .body(new AdjuntoResponse(comunicacionService.subirAdjunto(ticketId, comentarioId, archivo, autor)));
    }

    @GetMapping("/adjuntos")
    public ResponseEntity<List<AdjuntoResponse>> listarAdjuntos(
            @PathVariable UUID ticketId, Authentication authentication) {
        Usuario actor = usuarioActual.obtener(authentication);
        return ResponseEntity.ok(comunicacionService.listarAdjuntos(ticketId, actor).stream()
                .map(AdjuntoResponse::new)
                .collect(Collectors.toList()));
    }
}
