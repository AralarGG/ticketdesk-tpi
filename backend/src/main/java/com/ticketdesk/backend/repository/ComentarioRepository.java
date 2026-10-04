package com.ticketdesk.backend.repository;

import com.ticketdesk.backend.model.Comentario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComentarioRepository extends JpaRepository<Comentario, UUID> {

    List<Comentario> findByTicketIdOrderByFechaAsc(UUID ticketId);
}
