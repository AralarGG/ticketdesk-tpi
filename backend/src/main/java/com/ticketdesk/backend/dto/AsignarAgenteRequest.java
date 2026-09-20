package com.ticketdesk.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class AsignarAgenteRequest {

    @NotNull
    private UUID agenteId;

    private String motivo;
}
