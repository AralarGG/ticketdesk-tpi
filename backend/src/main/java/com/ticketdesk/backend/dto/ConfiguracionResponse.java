package com.ticketdesk.backend.dto;

import com.ticketdesk.backend.model.ConfiguracionEmpresa;
import lombok.Getter;

import java.time.LocalTime;
import java.util.List;

@Getter
public class ConfiguracionResponse {

    private final List<String> modulosHabilitados;
    private final LocalTime ventanaMantenimientoInicio;
    private final LocalTime ventanaMantenimientoFin;

    public ConfiguracionResponse(ConfiguracionEmpresa configuracion) {
        this.modulosHabilitados = configuracion.getModulosHabilitados();
        this.ventanaMantenimientoInicio = configuracion.getVentanaMantenimientoInicio();
        this.ventanaMantenimientoFin = configuracion.getVentanaMantenimientoFin();
    }
}
