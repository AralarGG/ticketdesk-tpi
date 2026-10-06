package com.ticketdesk.backend.controller;

import com.ticketdesk.backend.dto.AsignarAgenteRequest;
import com.ticketdesk.backend.dto.CambiarEstadoRequest;
import com.ticketdesk.backend.dto.CambiarNivelRequest;
import com.ticketdesk.backend.dto.CrearTicketRequest;
import com.ticketdesk.backend.dto.HistorialResponse;
import com.ticketdesk.backend.dto.TicketDetalleResponse;
import com.ticketdesk.backend.dto.TicketResumenResponse;
import com.ticketdesk.backend.model.Adjunto;
import com.ticketdesk.backend.model.Comentario;
import com.ticketdesk.backend.model.Ticket;
import com.ticketdesk.backend.model.Usuario;
import com.ticketdesk.backend.model.enums.EstadoTicket;
import com.ticketdesk.backend.model.enums.NivelAtencion;
import com.ticketdesk.backend.model.enums.Prioridad;
import com.ticketdesk.backend.security.UsuarioActual;
import com.ticketdesk.backend.service.ComunicacionService;
import com.ticketdesk.backend.service.TicketService;
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
 * POST   /api/v1/tickets                  (cliente)
 * GET    /api/v1/tickets                  (cliente: los propios; personal: los de la empresa)
 * GET    /api/v1/tickets/{id}
 * GET    /api/v1/tickets/{id}/historial   (personal de soporte)
 * PATCH  /api/v1/tickets/{id}/estado
 * PATCH  /api/v1/tickets/{id}/asignar     (agente o supervisor)
 * PATCH  /api/v1/tickets/{id}/nivel       (agente o supervisor)
 *
 * Las respuestas son siempre DTOs, nunca entidades. La validación fina de quién
 * puede hacer qué sobre cada ticket vive en TicketService.
 */
@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final ComunicacionService comunicacionService;
    private final UsuarioActual usuarioActual;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TicketDetalleResponse> crearTicket(
            @Valid @RequestBody CrearTicketRequest request, Authentication authentication) {
        Usuario cliente = usuarioActual.obtener(authentication);
        Ticket ticket = ticketService.crearTicket(request, cliente);
        return ResponseEntity.status(201).body(new TicketDetalleResponse(ticket, List.of(), List.of()));
    }

    @GetMapping
    public ResponseEntity<List<TicketResumenResponse>> listarTickets(
            @RequestParam(required = false) EstadoTicket estado,
            @RequestParam(required = false) Prioridad prioridad,
            @RequestParam(required = false) UUID categoriaId,
            @RequestParam(required = false) UUID agenteId,
            @RequestParam(required = false) NivelAtencion nivel,
            Authentication authentication
    ) {
        Usuario actor = usuarioActual.obtener(authentication);
        List<TicketResumenResponse> respuesta = ticketService
                .listar(actor, estado, prioridad, categoriaId, agenteId, nivel).stream()
                .map(TicketResumenResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketDetalleResponse> obtenerDetalle(@PathVariable UUID id, Authentication authentication) {
        Usuario actor = usuarioActual.obtener(authentication);
        Ticket ticket = ticketService.obtenerParaUsuario(id, actor);
        List<Comentario> comentarios = comunicacionService.listarComentarios(id, actor);
        List<Adjunto> adjuntos = comunicacionService.listarAdjuntos(id, actor);
        return ResponseEntity.ok(new TicketDetalleResponse(ticket, comentarios, adjuntos));
    }

    @GetMapping("/{id}/historial")
    @PreAuthorize("hasAnyRole('AGENT', 'SUPERVISOR', 'ADMIN')")
    public ResponseEntity<List<HistorialResponse>> historial(@PathVariable UUID id, Authentication authentication) {
        Usuario actor = usuarioActual.obtener(authentication);
        return ResponseEntity.ok(ticketService.obtenerHistorial(id, actor).stream()
                .map(HistorialResponse::new)
                .collect(Collectors.toList()));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<TicketDetalleResponse> cambiarEstado(
            @PathVariable UUID id, @Valid @RequestBody CambiarEstadoRequest request, Authentication authentication) {
        Usuario actor = usuarioActual.obtener(authentication);
        return ResponseEntity.ok(detalle(ticketService.cambiarEstado(id, request, actor), actor));
    }

    @PatchMapping("/{id}/asignar")
    @PreAuthorize("hasAnyRole('AGENT', 'SUPERVISOR')")
    public ResponseEntity<TicketDetalleResponse> asignarAgente(
            @PathVariable UUID id, @Valid @RequestBody AsignarAgenteRequest request, Authentication authentication) {
        Usuario actor = usuarioActual.obtener(authentication);
        return ResponseEntity.ok(detalle(ticketService.asignarAgente(id, request, actor), actor));
    }

    @PatchMapping("/{id}/nivel")
    @PreAuthorize("hasAnyRole('AGENT', 'SUPERVISOR')")
    public ResponseEntity<TicketDetalleResponse> cambiarNivel(
            @PathVariable UUID id, @Valid @RequestBody CambiarNivelRequest request, Authentication authentication) {
        Usuario actor = usuarioActual.obtener(authentication);
        return ResponseEntity.ok(detalle(ticketService.cambiarNivel(id, request, actor), actor));
    }

    private TicketDetalleResponse detalle(Ticket ticket, Usuario actor) {
        return new TicketDetalleResponse(
                ticket,
                comunicacionService.listarComentarios(ticket.getId(), actor),
                comunicacionService.listarAdjuntos(ticket.getId(), actor));
    }
}
