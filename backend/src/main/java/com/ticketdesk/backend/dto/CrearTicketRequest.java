package com.ticketdesk.backend.dto;

import com.ticketdesk.backend.model.enums.Prioridad;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CrearTicketRequest {

    @NotBlank
    private String titulo;

    @NotBlank
    private String descripcion;

    @NotNull
    private UUID categoriaId;

    @NotNull
    private Prioridad prioridad;
}
