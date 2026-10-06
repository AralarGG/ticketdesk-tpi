package com.ticketdesk.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CrearCategoriaRequest {

    @NotBlank
    @Size(max = 100)
    private String nombre;

    private String descripcion;
}
