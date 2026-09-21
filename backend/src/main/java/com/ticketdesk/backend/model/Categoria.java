package com.ticketdesk.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Categorías de tickets. Pueden ser genéricas (compartidas por todas las
 * empresas, `empresa` en null) o propias de una empresa en particular
 * (`empresa` apunta a esa empresa).
 *
 * Corrección pedida por el tutor: originalmente se había definido que
 * las empresas podían tener categorías propias, pero en una vuelta
 * anterior se simplificó a un catálogo único global. Se revierte esa
 * simplificación: el catálogo global sigue existiendo como base común
 * (útil para reportes comparables entre empresas), pero cada empresa
 * puede sumar las suyas propias además de las genéricas.
 *
 * La combinación (empresa, nombre) es única: una empresa no puede tener
 * dos categorías propias con el mismo nombre. Entre categorías globales
 * (empresa null) la unicidad de nombre no se refuerza a nivel de base de
 * datos por cómo PostgreSQL trata los valores NULL en restricciones
 * UNIQUE; se controla a nivel de aplicación al dar de alta una categoría
 * global (tarea administrativa, de baja frecuencia).
 */
@Entity
@Table(
    name = "categorias",
    uniqueConstraints = @UniqueConstraint(columnNames = {"empresa_id", "nombre"})
)
@Getter
@Setter
public class Categoria {

    @Id
    @GeneratedValue
    private UUID id;

    /**
     * Nulo = categoría genérica, disponible para todas las empresas.
     * Con valor = categoría propia, visible solo para esa empresa.
     */
    @ManyToOne
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;
}
