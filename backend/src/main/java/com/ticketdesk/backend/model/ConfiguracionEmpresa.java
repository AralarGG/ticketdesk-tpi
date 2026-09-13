package com.ticketdesk.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Guarda qué módulos del menú base tiene habilitados cada empresa,
 * y su ventana de mantenimiento.
 *
 * "modulosHabilitados" se guarda como JSONB en PostgreSQL (no como una
 * base NoSQL separada), lo que permite modificar los módulos habilitados
 * sin necesidad de ALTER TABLE (ver justificación en /docs/DER-ticketdesk.md,
 * sección "Decisión de Almacenamiento").
 */
@Entity
@Table(name = "configuracion_empresa")
@Getter
@Setter
public class ConfiguracionEmpresa {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne
    @JoinColumn(name = "empresa_id", nullable = false, unique = true)
    private Empresa empresa;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "modulos_habilitados", columnDefinition = "jsonb")
    private String modulosHabilitados; // ej: ["facturacion", "categorias_avanzadas"]

    @Column(name = "ventana_mantenimiento_inicio")
    private LocalTime ventanaMantenimientoInicio;

    @Column(name = "ventana_mantenimiento_fin")
    private LocalTime ventanaMantenimientoFin;
}
