package com.ticketdesk.backend.model.enums;

/**
 * Indica qué campo del ticket cambió, para registrar en ticket_history.
 * No se trackea solo el estado: también agente reasignado, categoría y prioridad.
 */
public enum CampoModificado {
    ESTADO,
    AGENTE_ID,
    CATEGORIA_ID,
    PRIORIDAD
}
