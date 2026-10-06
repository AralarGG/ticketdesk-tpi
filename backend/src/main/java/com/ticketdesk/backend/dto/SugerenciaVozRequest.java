package com.ticketdesk.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SugerenciaVozRequest {

    @NotBlank
    private String transcripcion;
}
