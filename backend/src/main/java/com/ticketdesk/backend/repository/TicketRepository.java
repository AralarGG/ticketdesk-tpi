package com.ticketdesk.backend.repository;

import com.ticketdesk.backend.model.Ticket;
import com.ticketdesk.backend.model.enums.EstadoTicket;
import com.ticketdesk.backend.model.enums.NivelAtencion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * El listado con filtros opcionales (estado, prioridad, categoría, agente,
 * nivel) se arma con Specifications desde TicketService, porque un filtro
 * "opcional" con parámetros nulos en una consulta fija da problemas de tipos
 * en PostgreSQL.
 */
public interface TicketRepository extends JpaRepository<Ticket, UUID>, JpaSpecificationExecutor<Ticket> {

    // Tickets propios de un cliente
    List<Ticket> findByUsuarioIdOrderByFechaCreacionDesc(UUID usuarioId);

    // Para la validación de "no desactivar agente con tickets activos"
    List<Ticket> findByAgenteIdAndEstadoNot(UUID agenteId, EstadoTicket estadoExcluido);

    // Cierre automático: tickets resueltos cuya ventana de confirmación venció
    List<Ticket> findByEstadoAndFechaResueltoBefore(EstadoTicket estado, LocalDateTime limite);

    // Rebote por estancamiento: tickets de un nivel, fuera de ciertos estados, sin movimiento desde un límite
    List<Ticket> findByNivelAtencionAndEstadoNotInAndFechaActualizacionBefore(
            NivelAtencion nivel, Collection<EstadoTicket> estadosExcluidos, LocalDateTime limite);
}
