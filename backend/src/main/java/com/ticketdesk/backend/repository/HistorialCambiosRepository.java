package com.ticketdesk.backend.repository;

import com.ticketdesk.backend.model.HistorialCambios;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HistorialCambiosRepository extends JpaRepository<HistorialCambios, UUID> {

    /**
     * Historial de cambios de un registro específico de cualquier
     * entidad (ej. entidad="Ticket", entidadId=<uuid del ticket>).
     */
    List<HistorialCambios> findByEntidadAndEntidadIdOrderByFechaDesc(String entidad, UUID entidadId);
}
