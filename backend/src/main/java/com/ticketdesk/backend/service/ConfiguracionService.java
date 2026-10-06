package com.ticketdesk.backend.service;

import com.ticketdesk.backend.dto.ActualizarConfiguracionRequest;
import com.ticketdesk.backend.model.ConfiguracionEmpresa;
import com.ticketdesk.backend.model.HistorialCambios;
import com.ticketdesk.backend.model.Usuario;
import com.ticketdesk.backend.repository.ConfiguracionEmpresaRepository;
import com.ticketdesk.backend.repository.HistorialCambiosRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

/**
 * Configuración por empresa (módulo 5, reglas RN16 y RN17).
 *
 * Cada empresa parte de un menú base de módulos y puede habilitar o
 * deshabilitar los que no necesita, y definir su propia ventana de
 * mantenimiento. Todo cambio queda registrado en historial_cambios.
 */
@Service
@RequiredArgsConstructor
public class ConfiguracionService {

    private static final String ENTIDAD = "ConfiguracionEmpresa";

    /** Menú base completo de módulos que una empresa puede habilitar. */
    public static final List<String> MODULOS_BASE = List.of(
            "tickets", "comentarios", "adjuntos", "voz", "facturacion", "categorias_propias");

    /** Módulos que no se pueden deshabilitar porque son el núcleo del sistema. */
    private static final Set<String> MODULOS_OBLIGATORIOS = Set.of("tickets", "comentarios");

    private final ConfiguracionEmpresaRepository configuracionRepository;
    private final HistorialCambiosRepository historialCambiosRepository;

    public ConfiguracionEmpresa obtener(UUID empresaId) {
        return configuracionRepository.findByEmpresaId(empresaId)
                .orElseThrow(() -> new NoSuchElementException("La empresa no tiene configuración"));
    }

    /**
     * Verifica que la empresa tenga habilitado un módulo del menú base (RN16).
     * Si la empresa todavía no tiene configuración cargada, se asume todo habilitado.
     */
    public void exigirModulo(UUID empresaId, String modulo) {
        boolean habilitado = configuracionRepository.findByEmpresaId(empresaId)
                .map(c -> c.getModulosHabilitados().contains(modulo))
                .orElse(true);
        if (!habilitado) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "El módulo '" + modulo + "' no está habilitado para tu empresa");
        }
    }

    public ConfiguracionEmpresa actualizar(UUID empresaId, ActualizarConfiguracionRequest request, Usuario quienCambia) {
        ConfiguracionEmpresa configuracion = obtener(empresaId);

        if (request.getModulosHabilitados() != null) {
            List<String> nuevos = validarModulos(request.getModulosHabilitados());
            registrar(configuracion, "modulos_habilitados",
                    configuracion.getModulosHabilitados().toString(), nuevos.toString(), quienCambia);
            configuracion.setModulosHabilitados(nuevos);
        }

        if (request.getVentanaMantenimientoInicio() != null || request.getVentanaMantenimientoFin() != null) {
            LocalTime inicio = request.getVentanaMantenimientoInicio() != null
                    ? request.getVentanaMantenimientoInicio() : configuracion.getVentanaMantenimientoInicio();
            LocalTime fin = request.getVentanaMantenimientoFin() != null
                    ? request.getVentanaMantenimientoFin() : configuracion.getVentanaMantenimientoFin();

            if (inicio == null || fin == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La ventana de mantenimiento necesita hora de inicio y hora de fin");
            }
            if (inicio.equals(fin)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La hora de inicio y la de fin de la ventana de mantenimiento no pueden ser iguales");
            }

            registrar(configuracion, "ventana_mantenimiento",
                    descripcionVentana(configuracion.getVentanaMantenimientoInicio(), configuracion.getVentanaMantenimientoFin()),
                    descripcionVentana(inicio, fin), quienCambia);
            configuracion.setVentanaMantenimientoInicio(inicio);
            configuracion.setVentanaMantenimientoFin(fin);
        }

        return configuracionRepository.save(configuracion);
    }

    /**
     * Valida que los módulos pedidos existan en el menú base, elimina
     * repetidos y agrega siempre los obligatorios.
     */
    private List<String> validarModulos(List<String> pedidos) {
        Set<String> resultado = new LinkedHashSet<>();
        for (String modulo : pedidos) {
            if (!MODULOS_BASE.contains(modulo)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Módulo desconocido: " + modulo + ". Los módulos válidos son: " + MODULOS_BASE);
            }
            resultado.add(modulo);
        }
        resultado.addAll(MODULOS_OBLIGATORIOS);
        return new ArrayList<>(resultado);
    }

    private String descripcionVentana(LocalTime inicio, LocalTime fin) {
        if (inicio == null || fin == null) {
            return "sin definir";
        }
        return inicio + " a " + fin;
    }

    private void registrar(ConfiguracionEmpresa configuracion, String campo, String anterior, String nuevo, Usuario usuario) {
        HistorialCambios historial = new HistorialCambios();
        historial.setEntidad(ENTIDAD);
        historial.setEntidadId(configuracion.getId());
        historial.setCampoModificado(campo);
        historial.setValorAnterior(recortar(anterior));
        historial.setValorNuevo(recortar(nuevo));
        historial.setUsuario(usuario);
        historialCambiosRepository.save(historial);
    }

    private String recortar(String texto) {
        if (texto == null) {
            return null;
        }
        return texto.length() > 255 ? texto.substring(0, 255) : texto;
    }
}
