package com.ticketdesk.backend.controller;

import com.ticketdesk.backend.dto.*;
import com.ticketdesk.backend.model.Ticket;
import com.ticketdesk.backend.model.Usuario;
import com.ticketdesk.backend.model.enums.EstadoTicket;
import com.ticketdesk.backend.model.enums.Prioridad;
import com.ticketdesk.backend.model.enums.Rol;
import com.ticketdesk.backend.repository.UsuarioRepository;
import com.ticketdesk.backend.security.CredencialUsuario;
import com.ticketdesk.backend.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Corresponde a la especificación de endpoints (ver /docs/endpoints-api-ticketdesk.md):
 * POST   /api/v1/tickets
 * GET    /api/v1/tickets
 * GET    /api/v1/tickets/{id}
 * PATCH  /api/v1/tickets/{id}/estado
 * PATCH  /api/v1/tickets/{id}/asignar
 *
 * Nota: la restricción de acceso por rol (ej. que un ROLE_USER no pueda
 * asignar agentes) todavía no está reforzada con @PreAuthorize en esta
 * versión inicial. Queda como siguiente paso antes de la entrega final.
 */
@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final UsuarioRepository usuarioRepository;

    @PostMapping
    public ResponseEntity<Ticket> crearTicket(@Valid @RequestBody CrearTicketRequest request,
                                               Authentication authentication) {
        Usuario usuario = obtenerUsuarioAutenticado(authentication);
        Ticket ticket = ticketService.crearTicket(request, usuario.getId());
        return ResponseEntity.status(201).body(ticket);
    }

    @GetMapping
    public ResponseEntity<List<TicketResumenResponse>> listarTickets(
            @RequestParam(required = false) EstadoTicket estado,
            @RequestParam(required = false) Prioridad prioridad,
            @RequestParam(required = false) UUID categoriaId,
            @RequestParam(required = false) UUID agenteId,
            Authentication authentication
    ) {
        Usuario usuario = obtenerUsuarioAutenticado(authentication);
        boolean esCliente = usuario.getRol() == Rol.ROLE_USER;

        List<Ticket> tickets = ticketService.listarTickets(
                usuario.getId(), esCliente, usuario.getEmpresa().getId(),
                estado, prioridad, categoriaId, agenteId
        );

        List<TicketResumenResponse> respuesta = tickets.stream()
                .map(TicketResumenResponse::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ticket> obtenerDetalle(@PathVariable UUID id) {
        Ticket ticket = ticketService.obtenerDetalle(id);
        return ResponseEntity.ok(ticket);
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Ticket> cambiarEstado(@PathVariable UUID id,
                                                 @Valid @RequestBody CambiarEstadoRequest request,
                                                 Authentication authentication) {
        Usuario usuario = obtenerUsuarioAutenticado(authentication);
        Ticket ticket = ticketService.cambiarEstado(id, request, usuario.getId());
        return ResponseEntity.ok(ticket);
    }

    @PatchMapping("/{id}/asignar")
    public ResponseEntity<Ticket> asignarAgente(@PathVariable UUID id,
                                                 @Valid @RequestBody AsignarAgenteRequest request,
                                                 Authentication authentication) {
        Usuario usuario = obtenerUsuarioAutenticado(authentication);
        Ticket ticket = ticketService.asignarAgente(id, request, usuario.getId());
        return ResponseEntity.ok(ticket);
    }

    /**
     * "authentication.getName()" devuelve el username tal como lo dejó
     * CustomUserDetailsService: la credencial compuesta "empresaId:email"
     * (ver CredencialUsuario), no un email suelto.
     */
    private Usuario obtenerUsuarioAutenticado(Authentication authentication) {
        CredencialUsuario credencial = CredencialUsuario.parsear(authentication.getName());
        return usuarioRepository.findByEmpresaIdAndEmail(credencial.empresaId(), credencial.email())
                .orElseThrow(() -> new NoSuchElementException("Usuario autenticado no encontrado"));
    }
}
