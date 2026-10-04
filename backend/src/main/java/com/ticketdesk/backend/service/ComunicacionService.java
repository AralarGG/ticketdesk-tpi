package com.ticketdesk.backend.service;

import com.ticketdesk.backend.dto.CrearComentarioRequest;
import com.ticketdesk.backend.model.Adjunto;
import com.ticketdesk.backend.model.Comentario;
import com.ticketdesk.backend.model.Ticket;
import com.ticketdesk.backend.model.Usuario;
import com.ticketdesk.backend.model.enums.Rol;
import com.ticketdesk.backend.repository.AdjuntoRepository;
import com.ticketdesk.backend.repository.ComentarioRepository;
import com.ticketdesk.backend.repository.TicketRepository;
import com.ticketdesk.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ComunicacionService {

    private final ComentarioRepository comentarioRepository;
    private final AdjuntoRepository adjuntoRepository;
    private final TicketRepository ticketRepository;
    private final UsuarioRepository usuarioRepository;
    private final CloudinaryService cloudinaryService;

    public Comentario crearComentario(UUID ticketId, CrearComentarioRequest request, UUID usuarioId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NoSuchElementException("Ticket no encontrado"));
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado"));

        Comentario comentario = new Comentario();
        comentario.setTicket(ticket);
        comentario.setUsuario(usuario);
        comentario.setContenido(request.getContenido());

        return comentarioRepository.save(comentario);
    }

    public List<Comentario> listarComentarios(UUID ticketId) {
        return comentarioRepository.findByTicketIdOrderByFechaAsc(ticketId);
    }

    /**
     * Sube un adjunto. Regla de negocio RN13: solo ROLE_USER puede subir
     * adjuntos. El agente responde únicamente con texto, para mantener
     * la resolución técnica estrictamente registrada como texto auditable
     * (ver reglas-negocio-ticketdesk.md).
     */
    public Adjunto subirAdjunto(UUID ticketId, UUID comentarioId, MultipartFile archivo, UUID usuarioId) throws IOException {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado"));

        if (usuario.getRol() != Rol.ROLE_USER) {
            throw new AccessDeniedException(
                    "Solo los clientes pueden adjuntar archivos. Los agentes responden únicamente con texto.");
        }

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NoSuchElementException("Ticket no encontrado"));

        Comentario comentario = null;
        if (comentarioId != null) {
            comentario = comentarioRepository.findById(comentarioId)
                    .orElseThrow(() -> new NoSuchElementException("Comentario no encontrado"));
        }

        String url = cloudinaryService.subirImagen(archivo);

        Adjunto adjunto = new Adjunto();
        adjunto.setTicket(ticket);
        adjunto.setComentario(comentario);
        adjunto.setUrl(url);
        adjunto.setTipo(archivo.getContentType());

        return adjuntoRepository.save(adjunto);
    }

    public List<Adjunto> listarAdjuntos(UUID ticketId) {
        return adjuntoRepository.findByTicketId(ticketId);
    }
}
