package com.ticketdesk.backend.repository;

import com.ticketdesk.backend.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CategoriaRepository extends JpaRepository<Categoria, UUID> {

    /**
     * Categorías visibles para una empresa: las genéricas (empresa_id
     * null) más las propias de esa empresa específica.
     */
    @Query("SELECT c FROM Categoria c WHERE c.empresa IS NULL OR c.empresa.id = :empresaId")
    List<Categoria> findVisiblesParaEmpresa(@Param("empresaId") UUID empresaId);
}
