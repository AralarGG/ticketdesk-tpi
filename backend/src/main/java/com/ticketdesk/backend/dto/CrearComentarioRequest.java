package com.ticketdesk.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CrearComentarioRequest {

    @NotBlank
    private String contenido;
}
