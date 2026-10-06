package com.ticketdesk.backend.dto;

import com.ticketdesk.backend.model.enums.Rol;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgregarRolRequest {

    @NotNull
    private Rol rol;
}
