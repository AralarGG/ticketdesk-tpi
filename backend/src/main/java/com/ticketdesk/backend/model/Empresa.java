package com.ticketdesk.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "empresas")
@Getter
@Setter
public class Empresa {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 100)
    private String rubro;

    @Column(name = "fecha_alta")
    private LocalDateTime fechaAlta;

    private boolean activa = true;

    @PrePersist
    protected void onCreate() {
        fechaAlta = LocalDateTime.now();
    }
}
