package com.ticketdesk.backend.service;

import com.ticketdesk.backend.dto.AsignarAgenteRequest;
import com.ticketdesk.backend.dto.CambiarEstadoRequest;
import com.ticketdesk.backend.dto.CrearTicketRequest;
import com.ticketdesk.backend.model.*;
import com.ticketdesk.backend.model.enums.CampoModificado;
import com.ticketdesk.backend.model.enums.CanalOrigen;
import com.ticketdesk.backend.model.enums.EstadoTicket;
import com.ticketdesk.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository categoriaRepository;
    private final TicketHistoryRepository ticketHistoryRepository;

    /**
     * Alta de ticket por formulario (canal FORMULARIO).
     * El canal por voz se implementa en la Etapa 8, con su propio endpoint.
     */
    public Ticket crearTicket(CrearTicketRequest request, UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado"));

        Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new NoSuchElementException("Categoría no encontrada"));

        Ticket ticket = new Ticket();
        ticket.setEmpresa(usuario.getEmpresa());
        ticket.setUsuario(usuario);
        ticket.setCategoria(categoria);
        ticket.setTitulo(request.getTitulo());
        ticket.setDescripcion(request.getDescripcion());
        ticket.setPrioridad(request.getPrioridad());
        ticket.setCanalOrigen(CanalOrigen.FORMULARIO);
        ticket.setEstado(EstadoTicket.NUEVO);

        return ticketRepository.save(ticket);
    }

    /**
     * Lista tickets según el rol de quien consulta:
     * - ROLE_USER: solo sus propios tickets (se ignoran los filtros de empresa/agente).
     * - ROLE_AGENT / ROLE_SUPERVISOR: todos los tickets de su empresa, con filtros opcionales.
     * Esta distinción de "qué puede ver cada rol" también se refuerza a nivel de
     * autorización en el controller (ver AuthController y anotaciones @PreAuthorize).
     */
    public List<Ticket> listarTickets(UUID usuarioId, boolean esCliente, UUID empresaId,
                                       EstadoTicket estado, com.ticketdesk.backend.model.enums.Prioridad prioridad,
                                       UUID categoriaId, UUID agenteId) {
        if (esCliente) {
            return ticketRepository.findByUsuarioId(usuarioId);
        }
        return ticketRepository.buscarConFiltros(empresaId, estado, prioridad, categoriaId, agenteId);
    }

    public Ticket obtenerDetalle(UUID ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NoSuchElementException("Ticket no encontrado"));
    }

    /**
     * Cambia el estado de un ticket y registra la transición en ticket_history.
     * No valida acá las transiciones permitidas (ej. no saltar de NUEVO a CERRADO):
     * esa validación de máquina de estados se agrega como siguiente paso,
     * una vez confirmado el detalle exacto del flujo con el tutor.
     */
    public Ticket cambiarEstado(UUID ticketId, CambiarEstadoRequest request, UUID usuarioQueCambiaId) {
        Ticket ticket = obtenerDetalle(ticketId);
        Usuario usuarioQueCambia = usuarioRepository.findById(usuarioQueCambiaId)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado"));

        String estadoAnterior = ticket.getEstado().name();
        ticket.setEstado(request.getEstadoNuevo());

        // Necesario para que TicketAutoCierreScheduler calcule la ventana de
        // confirmación de 72hs (regla definida junto al tutor, Etapa 3).
        if (request.getEstadoNuevo() == EstadoTicket.RESUELTO) {
            ticket.setFechaResuelto(java.time.LocalDateTime.now());
        } else if (request.getEstadoNuevo() == EstadoTicket.REABIERTO) {
            ticket.setFechaResuelto(null); // ya no aplica el plazo de confirmación anterior
        }

        ticketRepository.save(ticket);

        registrarHistorial(ticket, CampoModificado.ESTADO, estadoAnterior,
                request.getEstadoNuevo().name(), usuarioQueCambia, request.getMotivo());

        return ticket;
    }

    /**
     * Asigna o reasigna un agente. Registra el cambio en el historial,
     * incluyendo el motivo (relevante para casos de escalado).
     */
    public Ticket asignarAgente(UUID ticketId, AsignarAgenteRequest request, UUID usuarioQueAsignaId) {
        Ticket ticket = obtenerDetalle(ticketId);
        Usuario nuevoAgente = usuarioRepository.findById(request.getAgenteId())
                .orElseThrow(() -> new NoSuchElementException("Agente no encontrado"));
        Usuario usuarioQueAsigna = usuarioRepository.findById(usuarioQueAsignaId)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado"));

        String agenteAnterior = ticket.getAgente() != null ? ticket.getAgente().getId().toString() : "sin_asignar";
        ticket.setAgente(nuevoAgente);
        ticketRepository.save(ticket);

        registrarHistorial(ticket, CampoModificado.AGENTE_ID, agenteAnterior,
                nuevoAgente.getId().toString(), usuarioQueAsigna, request.getMotivo());

        return ticket;
    }

    private void registrarHistorial(Ticket ticket, CampoModificado campo, String valorAnterior,
                                     String valorNuevo, Usuario usuario, String motivo) {
        TicketHistory historial = new TicketHistory();
        historial.setTicket(ticket);
        historial.setCampoModificado(campo);
        historial.setValorAnterior(valorAnterior);
        historial.setValorNuevo(valorNuevo);
        historial.setUsuario(usuario);
        historial.setMotivo(motivo);
        ticketHistoryRepository.save(historial);
    }
}
