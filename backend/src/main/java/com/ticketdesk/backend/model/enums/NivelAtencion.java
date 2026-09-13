package com.ticketdesk.backend.model.enums;

/**
 * Nivel de atención técnico interno, separado de la prioridad que
 * percibe el cliente. Un ticket puede tener prioridad ALTA para el
 * cliente pero ser NIVEL_1 técnicamente (simple de resolver), o
 * viceversa. Ver justificación en README/DER: mezclar ambos conceptos
 * genera riesgo de que el cliente sienta que se subestima su reclamo.
 */
public enum NivelAtencion {
    NIVEL_1, // soporte general, cualquier agente puede resolverlo
    NIVEL_2, // requiere mayor expertise técnico
    NIVEL_3  // requiere especialista o supervisor
}
