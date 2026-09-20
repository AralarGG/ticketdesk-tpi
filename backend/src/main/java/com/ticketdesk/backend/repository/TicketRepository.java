package com.ticketdesk.backend.repository;

import com.ticketdesk.backend.model.Ticket;
import com.ticketdesk.backend.model.enums.EstadoTicket;
import com.ticketdesk.backend.model.enums.Prioridad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    // Tickets propios de un cliente (ROLE_USER)
    List<Ticket> findByUsuarioId(UUID usuarioId);

    // Tickets de una empresa (para agentes/supervisores), con filtros opcionales
    @Query("SELECT t FROM Ticket t WHERE t.empresa.id = :empresaId " +
           "AND (:estado IS NULL OR t.estado = :estado) " +
           "AND (:prioridad IS NULL OR t.prioridad = :prioridad) " +
           "AND (:categoriaId IS NULL OR t.categoria.id = :categoriaId) " +
           "AND (:agenteId IS NULL OR t.agente.id = :agenteId)")
    List<Ticket> buscarConFiltros(
            @Param("empresaId") UUID empresaId,
            @Param("estado") EstadoTicket estado,
            @Param("prioridad") Prioridad prioridad,
            @Param("categoriaId") UUID categoriaId,
            @Param("agenteId") UUID agenteId
    );

    // Para la validación de "no desactivar agente con tickets activos" (Etapa 6)
    List<Ticket> findByAgenteIdAndEstadoNot(UUID agenteId, EstadoTicket estadoExcluido);
}
