package com.ticketdesk.backend.dto;

import com.ticketdesk.backend.model.enums.NivelAtencion;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CambiarNivelRequest {

    @NotNull
    private NivelAtencion nivel;

    private String motivo;
}
