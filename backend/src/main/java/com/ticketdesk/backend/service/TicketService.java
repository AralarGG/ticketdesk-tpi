package com.ticketdesk.backend.service;

import com.ticketdesk.backend.dto.AsignarAgenteRequest;
import com.ticketdesk.backend.dto.CambiarEstadoRequest;
import com.ticketdesk.backend.dto.CambiarNivelRequest;
import com.ticketdesk.backend.dto.CrearTicketRequest;
import com.ticketdesk.backend.model.Categoria;
import com.ticketdesk.backend.model.HistorialCambios;
import com.ticketdesk.backend.model.Ticket;
import com.ticketdesk.backend.model.Usuario;
import com.ticketdesk.backend.model.UsuarioRol;
import com.ticketdesk.backend.model.enums.CanalOrigen;
import com.ticketdesk.backend.model.enums.EstadoTicket;
import com.ticketdesk.backend.model.enums.NivelAtencion;
import com.ticketdesk.backend.model.enums.Prioridad;
import com.ticketdesk.backend.model.enums.Rol;
import com.ticketdesk.backend.repository.CategoriaRepository;
import com.ticketdesk.backend.repository.HistorialCambiosRepository;
import com.ticketdesk.backend.repository.TicketRepository;
import com.ticketdesk.backend.repository.UsuarioRepository;
import com.ticketdesk.backend.repository.UsuarioRolRepository;
import com.ticketdesk.backend.security.PermisosUsuario;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Lógica de negocio de los tickets (módulo 2).
 *
 * Reglas que se aplican acá:
 * - RN5: el agente marca RESUELTO; el cierre lo confirma el cliente o se produce
 *   solo a las 72hs (ver TicketAutoCierreScheduler). Un agente no cierra.
 * - RN9: escalar exige motivo y reasigna a un supervisor.
 * - RN11: todo cambio relevante queda en historial_cambios.
 * - RN15: un usuario solo accede a tickets de su propia empresa.
 */
@Service
@RequiredArgsConstructor
public class TicketService {

    private static final String ENTIDAD_TICKET = "Ticket";

    /** Transiciones que puede hacer el personal de soporte (agente o supervisor). */
    private static final Map<EstadoTicket, Set<EstadoTicket>> TRANSICIONES_PERSONAL = new EnumMap<>(EstadoTicket.class);

    /** Transiciones que puede hacer el cliente dueño del ticket. */
    private static final Map<EstadoTicket, Set<EstadoTicket>> TRANSICIONES_CLIENTE = new EnumMap<>(EstadoTicket.class);

    static {
        TRANSICIONES_PERSONAL.put(EstadoTicket.NUEVO, Set.of(EstadoTicket.ASIGNADO));
        TRANSICIONES_PERSONAL.put(EstadoTicket.ASIGNADO, Set.of(EstadoTicket.EN_PROGRESO));
        TRANSICIONES_PERSONAL.put(EstadoTicket.EN_PROGRESO,
                Set.of(EstadoTicket.ESPERANDO_CLIENTE, EstadoTicket.ESCALADO, EstadoTicket.RESUELTO));
        TRANSICIONES_PERSONAL.put(EstadoTicket.ESPERANDO_CLIENTE, Set.of(EstadoTicket.EN_PROGRESO));
        TRANSICIONES_PERSONAL.put(EstadoTicket.ESCALADO, Set.of(EstadoTicket.EN_PROGRESO, EstadoTicket.RESUELTO));
        TRANSICIONES_PERSONAL.put(EstadoTicket.REABIERTO, Set.of(EstadoTicket.EN_PROGRESO));

        TRANSICIONES_CLIENTE.put(EstadoTicket.RESUELTO, Set.of(EstadoTicket.CERRADO, EstadoTicket.REABIERTO));
    }

    private final TicketRepository ticketRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final CategoriaRepository categoriaRepository;
    private final HistorialCambiosRepository historialCambiosRepository;
    private final PermisosUsuario permisos;

    /**
     * Alta de ticket por formulario (canal FORMULARIO). El canal por voz tiene su
     * propio flujo en VozService.
     */
    public Ticket crearTicket(CrearTicketRequest request, Usuario cliente) {
        Categoria categoria = obtenerCategoriaVisible(request.getCategoriaId(), cliente);
        return guardarNuevoTicket(cliente, categoria, request.getTitulo(), request.getDescripcion(),
                request.getPrioridad(), CanalOrigen.FORMULARIO, null, null);
    }

    /**
     * Crea y guarda un ticket nuevo. Lo comparten el alta por formulario y el alta
     * por voz, que solo se diferencian en el canal y en los datos del audio.
     */
    public Ticket guardarNuevoTicket(Usuario cliente, Categoria categoria, String titulo, String descripcion,
                                     Prioridad prioridad, CanalOrigen canal, String audioUrl, String transcripcion) {
        Ticket ticket = new Ticket();
        ticket.setEmpresa(cliente.getEmpresa());
        ticket.setUsuario(cliente);
        ticket.setCategoria(categoria);
        ticket.setTitulo(titulo);
        ticket.setDescripcion(descripcion);
        ticket.setPrioridad(prioridad);
        ticket.setCanalOrigen(canal);
        ticket.setAudioUrl(audioUrl);
        ticket.setTranscripcionOriginal(transcripcion);
        ticket.setEstado(EstadoTicket.NUEVO);
        return ticketRepository.save(ticket);
    }

    /** La categoría debe ser genérica o propia de la empresa del usuario (nunca de otra empresa). */
    public Categoria obtenerCategoriaVisible(UUID categoriaId, Usuario usuario) {
        Categoria categoria = categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new NoSuchElementException("Categoría no encontrada"));
        boolean esGenerica = categoria.getEmpresa() == null;
        boolean esDeLaEmpresa = !esGenerica && categoria.getEmpresa().getId().equals(usuario.getEmpresa().getId());
        if (!esGenerica && !esDeLaEmpresa) {
            throw new NoSuchElementException("Categoría no encontrada");
        }
        return categoria;
    }

    /**
     * Lista tickets según quién consulta:
     * - cliente: solo sus propios tickets.
     * - personal de soporte: todos los de su empresa, con filtros opcionales.
     * Los filtros se resuelven en la base de datos, no en memoria.
     */
    public List<Ticket> listar(Usuario actor, EstadoTicket estado, Prioridad prioridad, UUID categoriaId,
                               UUID agenteId, NivelAtencion nivel) {
        if (!permisos.esPersonal(actor)) {
            return ticketRepository.findByUsuarioIdOrderByFechaCreacionDesc(actor.getId());
        }

        UUID empresaId = actor.getEmpresa().getId();
        Specification<Ticket> filtro = (root, query, cb) -> cb.equal(root.get("empresa").get("id"), empresaId);
        if (estado != null) {
            filtro = filtro.and((root, query, cb) -> cb.equal(root.get("estado"), estado));
        }
        if (prioridad != null) {
            filtro = filtro.and((root, query, cb) -> cb.equal(root.get("prioridad"), prioridad));
        }
        if (categoriaId != null) {
            filtro = filtro.and((root, query, cb) -> cb.equal(root.get("categoria").get("id"), categoriaId));
        }
        if (agenteId != null) {
            filtro = filtro.and((root, query, cb) -> cb.equal(root.get("agente").get("id"), agenteId));
        }
        if (nivel != null) {
            filtro = filtro.and((root, query, cb) -> cb.equal(root.get("nivelAtencion"), nivel));
        }
        return ticketRepository.findAll(filtro, Sort.by(Sort.Direction.DESC, "fechaCreacion"));
    }

    /**
     * Devuelve el ticket solo si el usuario tiene acceso: debe ser de su empresa y,
     * si es un cliente, además tiene que ser un ticket suyo. En cualquier otro caso
     * responde "no encontrado" para no revelar que el ticket existe.
     */
    public Ticket obtenerParaUsuario(UUID ticketId, Usuario actor) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NoSuchElementException("Ticket no encontrado"));

        boolean mismaEmpresa = ticket.getEmpresa().getId().equals(actor.getEmpresa().getId());
        boolean tieneAcceso = mismaEmpresa
                && (permisos.esPersonal(actor) || ticket.getUsuario().getId().equals(actor.getId()));
        if (!tieneAcceso) {
            throw new NoSuchElementException("Ticket no encontrado");
        }
        return ticket;
    }

    /** El historial de un ticket lo ve solo el personal de soporte. */
    public List<HistorialCambios> obtenerHistorial(UUID ticketId, Usuario actor) {
        if (!permisos.esPersonal(actor)) {
            throw new AccessDeniedException("El historial de cambios es solo para el personal de soporte");
        }
        Ticket ticket = obtenerParaUsuario(ticketId, actor);
        return historialCambiosRepository.findByEntidadAndEntidadIdOrderByFechaDesc(ENTIDAD_TICKET, ticket.getId());
    }

    /**
     * Cambia el estado de un ticket validando quién lo pide y que la transición sea
     * válida. El personal de soporte trabaja el ticket hasta dejarlo en RESUELTO; el
     * cliente dueño confirma el cierre o lo reabre.
     */
    public Ticket cambiarEstado(UUID ticketId, CambiarEstadoRequest request, Usuario actor) {
        Ticket ticket = obtenerParaUsuario(ticketId, actor);
        EstadoTicket actual = ticket.getEstado();
        EstadoTicket nuevo = request.getEstadoNuevo();

        boolean esPersonal = permisos.puedeGestionarTickets(actor);
        boolean esDueno = ticket.getUsuario().getId().equals(actor.getId());
        if (!esPersonal && !esDueno) {
            throw new AccessDeniedException("No tenés permisos para cambiar el estado de este ticket");
        }

        Map<EstadoTicket, Set<EstadoTicket>> permitidas = esPersonal ? TRANSICIONES_PERSONAL : TRANSICIONES_CLIENTE;
        if (!permitidas.getOrDefault(actual, Set.of()).contains(nuevo)) {
            if (esPersonal && actual == EstadoTicket.RESUELTO) {
                throw new IllegalStateException(
                        "El ticket está resuelto: lo cierra el cliente al confirmar, o el sistema a las 72hs sin respuesta");
            }
            throw new IllegalStateException("No se puede pasar un ticket de " + actual + " a " + nuevo);
        }

        if (nuevo == EstadoTicket.ESCALADO && (request.getMotivo() == null || request.getMotivo().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Para escalar un ticket el motivo es obligatorio");
        }

        ticket.setEstado(nuevo);
        if (nuevo == EstadoTicket.RESUELTO) {
            ticket.setFechaResuelto(LocalDateTime.now()); // arranca la ventana de confirmación de 72hs
        } else if (nuevo == EstadoTicket.REABIERTO) {
            ticket.setFechaResuelto(null); // ya no aplica el plazo de confirmación anterior
        }
        ticketRepository.save(ticket);
        registrarHistorial(ticket.getId(), "estado", actual.name(), nuevo.name(), actor, request.getMotivo());

        if (nuevo == EstadoTicket.ESCALADO) {
            aplicarEscalado(ticket, actor, request.getMotivo());
        }
        return ticket;
    }

    /**
     * Un agente puede tomar un ticket para sí mismo; reasignarlo a otra persona
     * requiere rol de supervisor. Si el ticket estaba NUEVO pasa a ASIGNADO.
     */
    public Ticket asignarAgente(UUID ticketId, AsignarAgenteRequest request, Usuario actor) {
        Ticket ticket = obtenerParaUsuario(ticketId, actor);
        if (!permisos.puedeGestionarTickets(actor)) {
            throw new AccessDeniedException("Solo agentes y supervisores pueden asignar tickets");
        }
        if (ticket.getEstado() == EstadoTicket.CERRADO) {
            throw new IllegalStateException("El ticket está cerrado y no admite cambios");
        }

        Usuario nuevoAgente = usuarioRepository.findById(request.getAgenteId())
                .filter(u -> u.isActivo() && u.getEmpresa().getId().equals(actor.getEmpresa().getId()))
                .orElseThrow(() -> new NoSuchElementException("Agente no encontrado"));
        if (!permisos.puedeGestionarTickets(nuevoAgente)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La persona elegida no es agente ni supervisor");
        }
        if (!permisos.esSupervisor(actor) && !nuevoAgente.getId().equals(actor.getId())) {
            throw new AccessDeniedException(
                    "Un agente solo puede tomar tickets para sí mismo. Reasignar a otra persona requiere rol de supervisor");
        }

        String agenteAnterior = ticket.getAgente() != null ? ticket.getAgente().getId().toString() : "sin_asignar";
        ticket.setAgente(nuevoAgente);
        ticketRepository.save(ticket);
        registrarHistorial(ticket.getId(), "agente_id", agenteAnterior, nuevoAgente.getId().toString(),
                actor, request.getMotivo());

        if (ticket.getEstado() == EstadoTicket.NUEVO) {
            ticket.setEstado(EstadoTicket.ASIGNADO);
            ticketRepository.save(ticket);
            registrarHistorial(ticket.getId(), "estado", EstadoTicket.NUEVO.name(), EstadoTicket.ASIGNADO.name(),
                    actor, "Asignación del ticket");
        }
        return ticket;
    }

    /** Cambia el nivel de atención técnico (independiente de la prioridad que puso el cliente). */
    public Ticket cambiarNivel(UUID ticketId, CambiarNivelRequest request, Usuario actor) {
        Ticket ticket = obtenerParaUsuario(ticketId, actor);
        if (!permisos.puedeGestionarTickets(actor)) {
            throw new AccessDeniedException("Solo agentes y supervisores pueden cambiar el nivel de atención");
        }
        if (ticket.getEstado() == EstadoTicket.CERRADO) {
            throw new IllegalStateException("El ticket está cerrado y no admite cambios");
        }

        NivelAtencion anterior = ticket.getNivelAtencion();
        if (anterior != request.getNivel()) {
            ticket.setNivelAtencion(request.getNivel());
            ticketRepository.save(ticket);
            registrarHistorial(ticket.getId(), "nivel_atencion", anterior.name(), request.getNivel().name(),
                    actor, request.getMotivo());
        }
        return ticket;
    }

    /**
     * Cuando el cliente responde a un pedido de información (ticket en
     * ESPERANDO_CLIENTE) el ticket vuelve a EN_PROGRESO automáticamente.
     */
    public void alResponderElCliente(Ticket ticket, Usuario autor) {
        if (ticket.getEstado() == EstadoTicket.ESPERANDO_CLIENTE && ticket.getUsuario().getId().equals(autor.getId())) {
            ticket.setEstado(EstadoTicket.EN_PROGRESO);
            ticketRepository.save(ticket);
            registrarHistorial(ticket.getId(), "estado", EstadoTicket.ESPERANDO_CLIENTE.name(),
                    EstadoTicket.EN_PROGRESO.name(), autor, "El cliente respondió");
        }
    }

    /**
     * Efectos del escalado (RN9): sube un nivel de atención (hasta NIVEL_3; CRITICO
     * se asigna explícitamente) y reasigna el ticket al supervisor con menos carga.
     */
    private void aplicarEscalado(Ticket ticket, Usuario actor, String motivo) {
        NivelAtencion nivelActual = ticket.getNivelAtencion();
        NivelAtencion siguiente = siguienteNivel(nivelActual);
        if (siguiente != nivelActual) {
            ticket.setNivelAtencion(siguiente);
            registrarHistorial(ticket.getId(), "nivel_atencion", nivelActual.name(), siguiente.name(), actor, motivo);
        }

        Optional<Usuario> supervisor = elegirSupervisor(ticket, actor.getEmpresa().getId());
        if (supervisor.isPresent()) {
            String anterior = ticket.getAgente() != null ? ticket.getAgente().getId().toString() : "sin_asignar";
            ticket.setAgente(supervisor.get());
            registrarHistorial(ticket.getId(), "agente_id", anterior, supervisor.get().getId().toString(),
                    actor, "Reasignación por escalado: " + motivo);
        }
        ticketRepository.save(ticket);
    }

    private NivelAtencion siguienteNivel(NivelAtencion actual) {
        return switch (actual) {
            case NIVEL_1 -> NivelAtencion.NIVEL_2;
            case NIVEL_2 -> NivelAtencion.NIVEL_3;
            default -> actual;
        };
    }

    /** Supervisor activo de la empresa con menos tickets abiertos, distinto del agente actual. */
    private Optional<Usuario> elegirSupervisor(Ticket ticket, UUID empresaId) {
        List<Usuario> candidatos = new ArrayList<>(
                usuarioRepository.findByEmpresaIdAndActivoTrueAndRolIn(empresaId, List.of(Rol.ROLE_SUPERVISOR)));
        for (UsuarioRol usuarioRol : usuarioRolRepository
                .findByRolInAndUsuario_Empresa_IdAndUsuario_ActivoTrue(List.of(Rol.ROLE_SUPERVISOR), empresaId)) {
            Usuario candidato = usuarioRol.getUsuario();
            boolean yaEsta = candidatos.stream().anyMatch(c -> c.getId().equals(candidato.getId()));
            if (!yaEsta) {
                candidatos.add(candidato);
            }
        }

        UUID agenteActual = ticket.getAgente() != null ? ticket.getAgente().getId() : null;
        return candidatos.stream()
                .filter(c -> !c.getId().equals(agenteActual))
                .min(Comparator.comparingInt(c ->
                        ticketRepository.findByAgenteIdAndEstadoNot(c.getId(), EstadoTicket.CERRADO).size()));
    }

    private void registrarHistorial(UUID ticketId, String campo, String valorAnterior, String valorNuevo,
                                    Usuario usuario, String motivo) {
        HistorialCambios historial = new HistorialCambios();
        historial.setEntidad(ENTIDAD_TICKET);
        historial.setEntidadId(ticketId);
        historial.setCampoModificado(campo);
        historial.setValorAnterior(valorAnterior);
        historial.setValorNuevo(valorNuevo);
        historial.setUsuario(usuario);
        historial.setMotivo(motivo);
        historialCambiosRepository.save(historial);
    }
}
