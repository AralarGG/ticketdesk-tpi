package com.ticketdesk.backend.dto;

import lombok.Getter;

import java.util.UUID;

/**
 * Lo que el cliente ve para confirmar antes de crear el ticket por voz:
 * el título y la categoría sugeridos a partir de lo que dijo. La categoría
 * puede venir vacía si no se reconoció ninguna palabra clave.
 */
@Getter
public class SugerenciaVozResponse {

    private final UUID categoriaId;
    private final String categoriaNombre;
    private final String tituloSugerido;

    public SugerenciaVozResponse(UUID categoriaId, String categoriaNombre, String tituloSugerido) {
        this.categoriaId = categoriaId;
        this.categoriaNombre = categoriaNombre;
        this.tituloSugerido = tituloSugerido;
    }
}
