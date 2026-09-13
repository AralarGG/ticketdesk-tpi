package com.ticketdesk.backend.model.enums;

/**
 * Estados posibles de un ticket.
 * Workflow acordado con el tutor: incluye "RESUELTO" como paso intermedio
 * antes de "CERRADO" (el cliente puede confirmar la resolución).
 * NOTA: existe una propuesta de simplificar este flujo a futuro (cerrar
 * directo con la respuesta del agente, sin paso intermedio), pero el
 * tutor pidió mantener esta versión para la primera entrega.
 */
public enum EstadoTicket {
    NUEVO,
    ASIGNADO,
    EN_PROGRESO,
    ESPERANDO_CLIENTE,
    ESCALADO,
    RESUELTO,
    REABIERTO,
    CERRADO
}
