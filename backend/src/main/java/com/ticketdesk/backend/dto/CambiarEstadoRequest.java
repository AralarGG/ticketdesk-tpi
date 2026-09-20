package com.ticketdesk.backend.dto;

import com.ticketdesk.backend.model.enums.EstadoTicket;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CambiarEstadoRequest {

    @NotNull
    private EstadoTicket estadoNuevo;

    private String motivo; // opcional, pero recomendado en escalados y reaperturas
}
