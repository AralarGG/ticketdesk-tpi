package com.ticketdesk.backend.model;

import com.ticketdesk.backend.model.enums.CanalOrigen;
import com.ticketdesk.backend.model.enums.EstadoTicket;
import com.ticketdesk.backend.model.enums.NivelAtencion;
import com.ticketdesk.backend.model.enums.Prioridad;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tickets")
@Getter
@Setter
public class Ticket {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario; // quién creó el ticket (el cliente)

    @ManyToOne
    @JoinColumn(name = "agente_id")
    private Usuario agente; // agente asignado, nulo hasta que se asigna

    @ManyToOne
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal_origen", nullable = false)
    private CanalOrigen canalOrigen;

    @Column(name = "audio_url", length = 500)
    private String audioUrl; // solo si canalOrigen = VOZ

    @Column(name = "transcripcion_original", columnDefinition = "TEXT")
    private String transcripcionOriginal; // solo si canalOrigen = VOZ

    /**
     * Prioridad que el CLIENTE le asigna a su propio reclamo (subjetiva).
     * No se confunde con el nivel de atención técnico interno (ver "nivelAtencion").
     * Mantener ambos campos separados evita que el sistema le "baje" la
     * urgencia percibida al cliente, lo que generaría enojo y cierre
     * prematuro del ticket sin resolución real (indicación del tutor).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Prioridad prioridad;

    /**
     * Nivel de atención técnico interno, asignado por el sistema o el
     * agente. Determina a qué nivel de soporte corresponde el ticket,
     * independientemente de la prioridad percibida por el cliente.
     * Un ticket puede "rebotar" a NIVEL_1 luego de ser resuelto en un
     * nivel superior, para la comunicación final con el cliente.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_atencion", nullable = false)
    private NivelAtencion nivelAtencion = NivelAtencion.NIVEL_1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoTicket estado = EstadoTicket.NUEVO;

    /**
     * Marca si el ticket corresponde a un reclamo formal sujeto a
     * normativa de protección al consumidor (o similar), lo que puede
     * implicar plazos y manejo especial fuera de la lógica estándar
     * del sistema. Indicación del tutor: contemplar este caso, a
     * profundizar en una futura iteración.
     */
    @Column(name = "es_reclamo_formal")
    private boolean esReclamoFormal = false;

    /**
     * Datos de contacto de una persona responsable FUERA del sistema,
     * para casos que requieren intervención humana directa que el
     * software no puede resolver (ej. un reclamo formal escalado a un
     * responsable legal o gerencial de la empresa cliente).
     * Se guarda como texto libre (nombre/contacto), no como Usuario,
     * porque esta persona no necesariamente opera dentro del sistema.
     */
    @Column(name = "responsable_externo", length = 200)
    private String responsableExterno;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}
