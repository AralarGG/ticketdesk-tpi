package com.ticketdesk.backend.security;

import com.ticketdesk.backend.model.ConfiguracionEmpresa;
import com.ticketdesk.backend.repository.ConfiguracionEmpresaRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;

/**
 * Regla RN17: durante la ventana de mantenimiento de la empresa del usuario,
 * el sistema no procesa altas ni cambios sobre tickets y responde 503.
 * Las consultas (GET) siguen disponibles.
 *
 * Se ubica en la cadena de seguridad después de JwtAuthFilter, porque necesita
 * saber a qué empresa pertenece el usuario autenticado.
 */
public class MantenimientoFilter extends OncePerRequestFilter {

    private static final Set<String> METODOS_DE_ESCRITURA = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final String PREFIJO_TICKETS = "/api/v1/tickets";

    private final ConfiguracionEmpresaRepository configuracionRepository;
    private final ZoneId zona;

    public MantenimientoFilter(ConfiguracionEmpresaRepository configuracionRepository, ZoneId zona) {
        this.configuracionRepository = configuracionRepository;
        this.zona = zona;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        if (esEscrituraSobreTickets(request) && empresaEnMantenimiento()) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setContentType("application/json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(
                    "{\"error\":\"Mantenimiento programado\","
                  + "\"mensaje\":\"El sistema está en mantenimiento y no procesa altas ni cambios en este horario. "
                  + "Intentá nuevamente al finalizar la ventana de mantenimiento.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean esEscrituraSobreTickets(HttpServletRequest request) {
        return METODOS_DE_ESCRITURA.contains(request.getMethod())
                && request.getRequestURI().startsWith(PREFIJO_TICKETS);
    }

    private boolean empresaEnMantenimiento() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        try {
            CredencialUsuario credencial = CredencialUsuario.parsear(authentication.getName());
            return configuracionRepository.findByEmpresaId(credencial.empresaId())
                    .map(this::dentroDeLaVentana)
                    .orElse(false);
        } catch (IllegalArgumentException e) {
            // usuario anónimo u otra autenticación que no sigue el formato empresaId:email
            return false;
        }
    }

    private boolean dentroDeLaVentana(ConfiguracionEmpresa configuracion) {
        LocalTime inicio = configuracion.getVentanaMantenimientoInicio();
        LocalTime fin = configuracion.getVentanaMantenimientoFin();
        if (inicio == null || fin == null) {
            return false;
        }

        LocalTime ahora = LocalTime.now(zona);
        if (inicio.isBefore(fin)) {
            return !ahora.isBefore(inicio) && ahora.isBefore(fin);
        }
        // la ventana cruza la medianoche (ej. 23:00 a 02:00)
        return !ahora.isBefore(inicio) || ahora.isBefore(fin);
    }
}
