package com.ticketdesk.backend.service;

import com.ticketdesk.backend.dto.CrearComentarioRequest;
import com.ticketdesk.backend.model.Adjunto;
import com.ticketdesk.backend.model.Comentario;
import com.ticketdesk.backend.model.Ticket;
import com.ticketdesk.backend.model.Usuario;
import com.ticketdesk.backend.model.enums.EstadoTicket;
import com.ticketdesk.backend.model.enums.Rol;
import com.ticketdesk.backend.repository.AdjuntoRepository;
import com.ticketdesk.backend.repository.ComentarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Comentarios y adjuntos de un ticket (módulo 3). Todas las operaciones pasan
 * por TicketService.obtenerParaUsuario, así que respetan el aislamiento por
 * empresa y que un cliente solo ve sus propios tickets.
 */
@Service
@RequiredArgsConstructor
public class ComunicacionService {

    private final ComentarioRepository comentarioRepository;
    private final AdjuntoRepository adjuntoRepository;
    private final TicketService ticketService;
    private final ConfiguracionService configuracionService;
    private final CloudinaryService cloudinaryService;

    public Comentario crearComentario(UUID ticketId, CrearComentarioRequest request, Usuario autor) {
        Ticket ticket = ticketService.obtenerParaUsuario(ticketId, autor);
        configuracionService.exigirModulo(autor.getEmpresa().getId(), "comentarios");

        if (ticket.getEstado() == EstadoTicket.CERRADO) {
            throw new IllegalStateException("El ticket está cerrado y no admite nuevos comentarios");
        }

        Comentario comentario = new Comentario();
        comentario.setTicket(ticket);
        comentario.setUsuario(autor);
        comentario.setContenido(request.getContenido());
        Comentario guardado = comentarioRepository.save(comentario);

        ticketService.alResponderElCliente(ticket, autor);
        return guardado;
    }

    public List<Comentario> listarComentarios(UUID ticketId, Usuario actor) {
        Ticket ticket = ticketService.obtenerParaUsuario(ticketId, actor);
        return comentarioRepository.findByTicketIdOrderByFechaAsc(ticket.getId());
    }

    /**
     * Sube un adjunto. Regla RN13: solo el cliente (ROLE_USER) puede subir
     * adjuntos. El agente responde únicamente con texto, para que la resolución
     * técnica quede registrada como texto auditable.
     */
    public Adjunto subirAdjunto(UUID ticketId, UUID comentarioId, MultipartFile archivo, Usuario autor) throws IOException {
        if (autor.getRol() != Rol.ROLE_USER) {
            throw new AccessDeniedException(
                    "Solo los clientes pueden adjuntar archivos. Los agentes responden únicamente con texto.");
        }

        Ticket ticket = ticketService.obtenerParaUsuario(ticketId, autor);
        configuracionService.exigirModulo(autor.getEmpresa().getId(), "adjuntos");

        if (ticket.getEstado() == EstadoTicket.CERRADO) {
            throw new IllegalStateException("El ticket está cerrado y no admite nuevos adjuntos");
        }
        if (archivo == null || archivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo está vacío");
        }
        String tipo = archivo.getContentType();
        if (tipo == null || !tipo.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se aceptan imágenes (jpg, png, etc.)");
        }

        Comentario comentario = null;
        if (comentarioId != null) {
            comentario = comentarioRepository.findById(comentarioId)
                    .filter(c -> c.getTicket().getId().equals(ticket.getId()))
                    .orElseThrow(() -> new NoSuchElementException("Comentario no encontrado en este ticket"));
        }

        String url = cloudinaryService.subirImagen(archivo);

        Adjunto adjunto = new Adjunto();
        adjunto.setTicket(ticket);
        adjunto.setComentario(comentario);
        adjunto.setUrl(url);
        adjunto.setTipo(tipo);
        return adjuntoRepository.save(adjunto);
    }

    public List<Adjunto> listarAdjuntos(UUID ticketId, Usuario actor) {
        Ticket ticket = ticketService.obtenerParaUsuario(ticketId, actor);
        return adjuntoRepository.findByTicketId(ticket.getId());
    }
}
