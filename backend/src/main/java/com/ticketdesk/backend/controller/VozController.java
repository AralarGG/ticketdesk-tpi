package com.ticketdesk.backend.controller;

import com.ticketdesk.backend.dto.SugerenciaVozRequest;
import com.ticketdesk.backend.dto.SugerenciaVozResponse;
import com.ticketdesk.backend.dto.TicketDetalleResponse;
import com.ticketdesk.backend.model.Usuario;
import com.ticketdesk.backend.model.enums.Prioridad;
import com.ticketdesk.backend.security.UsuarioActual;
import com.ticketdesk.backend.service.VozService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Apertura de tickets por voz, en dos pasos (ver flujo 2 y wireframe 4):
 *
 * 1) POST /api/v1/tickets/voz/sugerencia   recibe la transcripción y devuelve la
 *    categoría y el título sugeridos, para que el cliente los revise.
 * 2) POST /api/v1/tickets/voz              recibe el audio, la transcripción ya
 *    confirmada y la categoría elegida, y crea el ticket.
 */
@RestController
@RequestMapping("/api/v1/tickets/voz")
@RequiredArgsConstructor
public class VozController {

    private final VozService vozService;
    private final UsuarioActual usuarioActual;

    @PostMapping("/sugerencia")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SugerenciaVozResponse> sugerir(
            @Valid @RequestBody SugerenciaVozRequest request, Authentication authentication) {
        Usuario cliente = usuarioActual.obtener(authentication);
        return ResponseEntity.ok(vozService.sugerir(request.getTranscripcion(), cliente));
    }

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TicketDetalleResponse> crear(
            @RequestParam("audio") MultipartFile audio,
            @RequestParam("transcripcion") String transcripcion,
            @RequestParam("categoriaId") UUID categoriaId,
            @RequestParam(value = "prioridad", defaultValue = "MEDIA") Prioridad prioridad,
            Authentication authentication
    ) throws IOException {
        Usuario cliente = usuarioActual.obtener(authentication);
        return ResponseEntity.status(201).body(new TicketDetalleResponse(
                vozService.crearPorVoz(audio, transcripcion, categoriaId, prioridad, cliente),
                List.of(), List.of()));
    }
}
