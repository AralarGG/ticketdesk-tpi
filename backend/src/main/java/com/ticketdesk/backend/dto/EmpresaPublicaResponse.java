package com.ticketdesk.backend.dto;

import com.ticketdesk.backend.model.Empresa;
import lombok.Getter;

import java.util.UUID;

/** Datos mínimos de una empresa que se muestran en la pantalla de login (sin información sensible). */
@Getter
public class EmpresaPublicaResponse {

    private final UUID id;
    private final String nombre;

    public EmpresaPublicaResponse(Empresa empresa) {
        this.id = empresa.getId();
        this.nombre = empresa.getNombre();
    }
}
