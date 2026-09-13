package com.ticketdesk.backend.repository;

import com.ticketdesk.backend.model.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EmpresaRepository extends JpaRepository<Empresa, UUID> {
}
