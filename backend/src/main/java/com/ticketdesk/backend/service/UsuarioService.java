package com.ticketdesk.backend.service;

import com.ticketdesk.backend.dto.CrearUsuarioRequest;
import com.ticketdesk.backend.model.HistorialCambios;
import com.ticketdesk.backend.model.Ticket;
import com.ticketdesk.backend.model.Usuario;
import com.ticketdesk.backend.model.UsuarioRol;
import com.ticketdesk.backend.model.enums.EstadoTicket;
import com.ticketdesk.backend.model.enums.Rol;
import com.ticketdesk.backend.repository.HistorialCambiosRepository;
import com.ticketdesk.backend.repository.TicketRepository;
import com.ticketdesk.backend.repository.UsuarioRepository;
import com.ticketdesk.backend.repository.UsuarioRolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

/**
 * Gestión de usuarios de una empresa (módulo 1). Reglas que se aplican acá:
 * - RN2: los usuarios nunca se borran, se desactivan.
 * - RN3: un agente con tickets abiertos no se puede desactivar sin reasignarlos antes.
 * - RN4: un usuario puede tener varios roles; antes de sumar uno se verifica que no lo tenga ya.
 * Todas las operaciones de administración quedan limitadas a la empresa del administrador.
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private static final String ENTIDAD = "Usuario";

    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final TicketRepository ticketRepository;
    private final HistorialCambiosRepository historialCambiosRepository;
    private final PasswordEncoder passwordEncoder;

    /** Agentes y supervisores activos de la empresa, para armar las listas de asignación. */
    public List<Usuario> listarPersonalDeSoporte(Usuario actor) {
        UUID empresaId = actor.getEmpresa().getId();
        List<Rol> rolesDeSoporte = List.of(Rol.ROLE_AGENT, Rol.ROLE_SUPERVISOR);

        List<Usuario> resultado = new ArrayList<>(
                usuarioRepository.findByEmpresaIdAndActivoTrueAndRolIn(empresaId, rolesDeSoporte));
        for (UsuarioRol usuarioRol : usuarioRolRepository
                .findByRolInAndUsuario_Empresa_IdAndUsuario_ActivoTrue(rolesDeSoporte, empresaId)) {
            Usuario candidato = usuarioRol.getUsuario();
            boolean yaEsta = resultado.stream().anyMatch(u -> u.getId().equals(candidato.getId()));
            if (!yaEsta) {
                resultado.add(candidato);
            }
        }
        return resultado;
    }

    public List<Usuario> listarTodos(Usuario admin) {
        return usuarioRepository.findByEmpresaIdOrderByNombreAsc(admin.getEmpresa().getId());
    }

    public Set<Rol> rolesDe(Usuario usuario) {
        Set<Rol> roles = new LinkedHashSet<>();
        roles.add(usuario.getRol());
        for (UsuarioRol usuarioRol : usuarioRolRepository.findByUsuarioId(usuario.getId())) {
            roles.add(usuarioRol.getRol());
        }
        return roles;
    }

    public Usuario crear(CrearUsuarioRequest request, Usuario admin) {
        UUID empresaId = admin.getEmpresa().getId();
        if (usuarioRepository.existsByEmpresaIdAndEmail(empresaId, request.getEmail())) {
            throw new IllegalStateException("Ya existe un usuario con ese email en la empresa");
        }

        Usuario usuario = new Usuario();
        usuario.setEmpresa(admin.getEmpresa());
        usuario.setNombre(request.getNombre());
        usuario.setEmail(request.getEmail());
        usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        usuario.setRol(request.getRol());
        usuarioRepository.save(usuario);

        asignarRol(usuario, request.getRol());
        if (request.getRolesAdicionales() != null) {
            for (Rol adicional : request.getRolesAdicionales()) {
                if (!usuarioRolRepository.existsByUsuarioIdAndRol(usuario.getId(), adicional)) {
                    asignarRol(usuario, adicional);
                }
            }
        }
        return usuario;
    }

    /** Suma un rol a un usuario existente, con el chequeo de rol previo (RN4). */
    public Usuario agregarRol(UUID usuarioId, Rol rol, Usuario admin) {
        Usuario usuario = obtenerDeLaEmpresa(usuarioId, admin);

        boolean yaLoTiene = usuario.getRol() == rol || usuarioRolRepository.existsByUsuarioIdAndRol(usuario.getId(), rol);
        if (yaLoTiene) {
            throw new IllegalStateException("El usuario ya tiene el rol " + rol.name());
        }

        asignarRol(usuario, rol);
        registrar(usuario.getId(), "roles", "", rol.name(), admin, "Rol agregado");
        return usuario;
    }

    /**
     * Desactiva a un usuario (nunca se elimina, RN2). Si es personal de soporte con
     * tickets abiertos a su cargo, se rechaza hasta que se reasignen (RN3).
     */
    public Usuario desactivar(UUID usuarioId, Usuario admin) {
        Usuario usuario = obtenerDeLaEmpresa(usuarioId, admin);

        if (usuario.getId().equals(admin.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No podés desactivar tu propia cuenta");
        }
        if (!usuario.isActivo()) {
            throw new IllegalStateException("El usuario ya está desactivado");
        }

        List<Ticket> abiertos = ticketRepository.findByAgenteIdAndEstadoNot(usuario.getId(), EstadoTicket.CERRADO);
        if (!abiertos.isEmpty()) {
            throw new IllegalStateException("El agente tiene " + abiertos.size()
                    + " ticket(s) abierto(s) a su cargo. Reasignalos antes de desactivarlo.");
        }

        usuario.setActivo(false);
        usuarioRepository.save(usuario);
        registrar(usuario.getId(), "activo", "true", "false", admin, "Usuario desactivado");
        return usuario;
    }

    public Usuario activar(UUID usuarioId, Usuario admin) {
        Usuario usuario = obtenerDeLaEmpresa(usuarioId, admin);
        if (usuario.isActivo()) {
            throw new IllegalStateException("El usuario ya está activo");
        }
        usuario.setActivo(true);
        usuarioRepository.save(usuario);
        registrar(usuario.getId(), "activo", "false", "true", admin, "Usuario reactivado");
        return usuario;
    }

    /** Un administrador solo puede operar sobre usuarios de su propia empresa. */
    private Usuario obtenerDeLaEmpresa(UUID usuarioId, Usuario admin) {
        return usuarioRepository.findById(usuarioId)
                .filter(u -> u.getEmpresa().getId().equals(admin.getEmpresa().getId()))
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado"));
    }

    private void asignarRol(Usuario usuario, Rol rol) {
        UsuarioRol usuarioRol = new UsuarioRol();
        usuarioRol.setUsuario(usuario);
        usuarioRol.setRol(rol);
        usuarioRolRepository.save(usuarioRol);
    }

    private void registrar(UUID usuarioId, String campo, String anterior, String nuevo, Usuario quien, String motivo) {
        HistorialCambios historial = new HistorialCambios();
        historial.setEntidad(ENTIDAD);
        historial.setEntidadId(usuarioId);
        historial.setCampoModificado(campo);
        historial.setValorAnterior(anterior);
        historial.setValorNuevo(nuevo);
        historial.setUsuario(quien);
        historial.setMotivo(motivo);
        historialCambiosRepository.save(historial);
    }
}
