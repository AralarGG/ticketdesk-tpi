package com.ticketdesk.backend.service;

import com.ticketdesk.backend.model.Ticket;
import com.ticketdesk.backend.model.TicketHistory;
import com.ticketdesk.backend.model.enums.CampoModificado;
import com.ticketdesk.backend.model.enums.EstadoTicket;
import com.ticketdesk.backend.model.enums.NivelAtencion;
import com.ticketdesk.backend.repository.TicketHistoryRepository;
import com.ticketdesk.backend.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Aplica automáticamente dos reglas de negocio definidas junto al tutor (Etapa 3):
 *
 * 1) Cierre por silencio del cliente: un ticket en estado RESUELTO que no
 *    recibe respuesta ni reapertura del cliente dentro de la ventana de
 *    confirmación (72hs por defecto) se cierra automáticamente. El silencio
 *    se interpreta como conformidad tácita, sin que el agente lo cierre
 *    directamente (se mantiene el criterio de esperar confirmación).
 *
 * 2) Rebote por estancamiento en nivel máximo: un ticket que llega a
 *    CRITICO (nivel de atención más alto) y queda sin avances por un
 *    tiempo prolongado (5 días por defecto) sin resolverse, vuelve a
 *    NIVEL_1 marcado como "reincidente", para que se retome desde cero
 *    con la alerta de que ya falló una vez en el nivel más alto.
 *
 * Los plazos son configurables y, por ahora, están como valores fijos:
 * quedan pendientes de ajustar según lo que decida el equipo/tutor.
 */
@Service
@RequiredArgsConstructor
public class TicketAutoCierreScheduler {

    private static final int HORAS_VENTANA_CONFIRMACION = 72;
    private static final int DIAS_ESTANCAMIENTO_NIVEL_MAXIMO = 5;

    private final TicketRepository ticketRepository;
    private final TicketHistoryRepository ticketHistoryRepository;

    /**
     * Corre una vez por hora. Frecuencia elegida para no sobrecargar la
     * base de datos, dado que los plazos que evalúa son de días/horas,
     * no de minutos.
     */
    @Scheduled(fixedRate = 60 * 60 * 1000) // cada 1 hora
    public void ejecutarReglasAutomaticas() {
        cerrarPorSilencioDelCliente();
        rebotarPorEstancamientoEnNivelMaximo();
    }

    private void cerrarPorSilencioDelCliente() {
        LocalDateTime limite = LocalDateTime.now().minusHours(HORAS_VENTANA_CONFIRMACION);

        List<Ticket> candidatos = ticketRepository.findAll().stream()
                .filter(t -> t.getEstado() == EstadoTicket.RESUELTO)
                .filter(t -> t.getFechaResuelto() != null && t.getFechaResuelto().isBefore(limite))
                .collect(Collectors.toList());

        for (Ticket ticket : candidatos) {
            String estadoAnterior = ticket.getEstado().name();
            ticket.setEstado(EstadoTicket.CERRADO);
            ticketRepository.save(ticket);

            registrarHistorialAutomatico(ticket, CampoModificado.ESTADO, estadoAnterior,
                    EstadoTicket.CERRADO.name(),
                    "Cierre automático: sin respuesta del cliente dentro de las "
                            + HORAS_VENTANA_CONFIRMACION + " horas de confirmación (silencio = conformidad tácita)");
        }
    }

    private void rebotarPorEstancamientoEnNivelMaximo() {
        LocalDateTime limite = LocalDateTime.now().minusDays(DIAS_ESTANCAMIENTO_NIVEL_MAXIMO);

        List<Ticket> candidatos = ticketRepository.findAll().stream()
                .filter(t -> t.getNivelAtencion() == NivelAtencion.CRITICO)
                .filter(t -> t.getEstado() != EstadoTicket.CERRADO && t.getEstado() != EstadoTicket.RESUELTO)
                .filter(t -> t.getFechaActualizacion() != null && t.getFechaActualizacion().isBefore(limite))
                .collect(Collectors.toList());

        for (Ticket ticket : candidatos) {
            String nivelAnterior = ticket.getNivelAtencion().name();
            ticket.setNivelAtencion(NivelAtencion.NIVEL_1);
            ticket.setReincidente(true);
            ticketRepository.save(ticket);

            registrarHistorialAutomatico(ticket, CampoModificado.NIVEL_ATENCION, nivelAnterior,
                    NivelAtencion.NIVEL_1.name(),
                    "Rebote automático: sin avances por más de " + DIAS_ESTANCAMIENTO_NIVEL_MAXIMO
                            + " días en nivel máximo de atención. Marcado como reincidente.");
        }
    }

    private void registrarHistorialAutomatico(Ticket ticket, CampoModificado campo,
                                               String valorAnterior, String valorNuevo, String motivo) {
        TicketHistory historial = new TicketHistory();
        historial.setTicket(ticket);
        historial.setCampoModificado(campo);
        historial.setValorAnterior(valorAnterior);
        historial.setValorNuevo(valorNuevo);
        historial.setUsuario(null); // cambio hecho por el sistema, no por una persona
        historial.setMotivo(motivo);
        ticketHistoryRepository.save(historial);
    }
}
