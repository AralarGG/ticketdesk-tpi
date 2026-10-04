package com.ticketdesk.backend.repository;

import com.ticketdesk.backend.model.Adjunto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdjuntoRepository extends JpaRepository<Adjunto, UUID> {

    List<Adjunto> findByTicketId(UUID ticketId);
}
