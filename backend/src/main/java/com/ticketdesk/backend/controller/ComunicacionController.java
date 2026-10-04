package com.ticketdesk.backend.controller;

import com.ticketdesk.backend.dto.AdjuntoResponse;
import com.ticketdesk.backend.dto.ComentarioResponse;
import com.ticketdesk.backend.dto.CrearComentarioRequest;
import com.ticketdesk.backend.model.Adjunto;
import com.ticketdesk.backend.model.Comentario;
import com.ticketdesk.backend.model.Usuario;
import com.ticketdesk.backend.repository.UsuarioRepository;
import com.ticketdesk.backend.security.CredencialUsuario;
import com.ticketdesk.backend.service.ComunicacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Corresponde a la especificación de endpoints:
 * POST   /api/v1/tickets/{id}/comentarios
 * GET    /api/v1/tickets/{id}/comentarios
 * POST   /api/v1/tickets/{id}/adjuntos
 * (ver endpoints-api-ticketdesk.md)
 */
@RestController
@RequestMapping("/api/v1/tickets/{ticketId}")
@RequiredArgsConstructor
public class ComunicacionController {

    private final ComunicacionService comunicacionService;
    private final UsuarioRepository usuarioRepository;

    @PostMapping("/comentarios")
    public ResponseEntity<ComentarioResponse> crearComentario(
            @PathVariable UUID ticketId,
            @Valid @RequestBody CrearComentarioRequest request,
            Authentication authentication
    ) {
        Usuario usuario = obtenerUsuarioAutenticado(authentication);
        Comentario comentario = comunicacionService.crearComentario(ticketId, request, usuario.getId());
        return ResponseEntity.status(201).body(new ComentarioResponse(comentario));
    }

    @GetMapping("/comentarios")
    public ResponseEntity<List<ComentarioResponse>> listarComentarios(@PathVariable UUID ticketId) {
        List<ComentarioResponse> respuesta = comunicacionService.listarComentarios(ticketId).stream()
                .map(ComentarioResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping(value = "/adjuntos", consumes = "multipart/form-data")
    public ResponseEntity<AdjuntoResponse> subirAdjunto(
            @PathVariable UUID ticketId,
            @RequestParam(required = false) UUID comentarioId,
            @RequestParam("archivo") MultipartFile archivo,
            Authentication authentication
    ) throws IOException {
        Usuario usuario = obtenerUsuarioAutenticado(authentication);
        Adjunto adjunto = comunicacionService.subirAdjunto(ticketId, comentarioId, archivo, usuario.getId());
        return ResponseEntity.status(201).body(new AdjuntoResponse(adjunto));
    }

    @GetMapping("/adjuntos")
    public ResponseEntity<List<AdjuntoResponse>> listarAdjuntos(@PathVariable UUID ticketId) {
        List<AdjuntoResponse> respuesta = comunicacionService.listarAdjuntos(ticketId).stream()
                .map(AdjuntoResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(respuesta);
    }

    private Usuario obtenerUsuarioAutenticado(Authentication authentication) {
        CredencialUsuario credencial = CredencialUsuario.parsear(authentication.getName());
        return usuarioRepository.findByEmpresaIdAndEmail(credencial.empresaId(), credencial.email())
                .orElseThrow(() -> new NoSuchElementException("Usuario autenticado no encontrado"));
    }
}
