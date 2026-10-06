package com.ticketdesk.backend.repository;

import com.ticketdesk.backend.model.ConfiguracionEmpresa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ConfiguracionEmpresaRepository extends JpaRepository<ConfiguracionEmpresa, UUID> {

    Optional<ConfiguracionEmpresa> findByEmpresaId(UUID empresaId);
}
