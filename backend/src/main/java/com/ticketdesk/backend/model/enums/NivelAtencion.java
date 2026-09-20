package com.ticketdesk.backend.model.enums;

/**
 * Nivel de atención técnico interno, separado de la prioridad que
 * percibe el cliente. Un ticket puede tener prioridad ALTA para el
 * cliente pero ser NIVEL_1 técnicamente (simple de resolver), o
 * viceversa. Ver justificación en README/DER: mezclar ambos conceptos
 * genera riesgo de que el cliente sienta que se subestima su reclamo.
 *
 * Cada nivel lleva asociado un tiempo objetivo de resolución (SLA
 * básico), pedido por el tutor. Estos valores son un punto de partida;
 * quedan sujetos a ajuste una vez que el equipo tenga datos reales
 * de tiempos de atención (ver Fase 3 del roadmap: Métricas y SLAs).
 */
public enum NivelAtencion {
    NIVEL_1(24),   // soporte general, cualquier agente puede resolverlo
    NIVEL_2(48),   // requiere mayor expertise técnico
    NIVEL_3(72),   // requiere especialista o supervisor
    CRITICO(4);    // máxima urgencia: impacto amplio o bloqueante (ej. caída del servicio, fallo de pagos)

    private final int horasObjetivoResolucion;

    NivelAtencion(int horasObjetivoResolucion) {
        this.horasObjetivoResolucion = horasObjetivoResolucion;
    }

    public int getHorasObjetivoResolucion() {
        return horasObjetivoResolucion;
    }
}
