package com.ticketdesk.backend.dto;

import com.ticketdesk.backend.model.Categoria;
import lombok.Getter;

import java.util.UUID;

@Getter
public class CategoriaResponse {

    private final UUID id;
    private final String nombre;
    private final String descripcion;
    private final boolean esGenerica; // true: catálogo base compartido; false: categoría propia de la empresa

    public CategoriaResponse(Categoria categoria) {
        this.id = categoria.getId();
        this.nombre = categoria.getNombre();
        this.descripcion = categoria.getDescripcion();
        this.esGenerica = categoria.getEmpresa() == null;
    }
}
