package com.ticketdesk.backend.repository;

import com.ticketdesk.backend.model.TicketHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TicketHistoryRepository extends JpaRepository<TicketHistory, UUID> {
    List<TicketHistory> findByTicketIdOrderByFechaDesc(UUID ticketId);
}
