package com.ticketdesk.backend.config;

import com.ticketdesk.backend.model.Categoria;
import com.ticketdesk.backend.model.ConfiguracionEmpresa;
import com.ticketdesk.backend.model.Empresa;
import com.ticketdesk.backend.model.Usuario;
import com.ticketdesk.backend.model.UsuarioRol;
import com.ticketdesk.backend.model.enums.Rol;
import com.ticketdesk.backend.repository.CategoriaRepository;
import com.ticketdesk.backend.repository.ConfiguracionEmpresaRepository;
import com.ticketdesk.backend.repository.EmpresaRepository;
import com.ticketdesk.backend.repository.UsuarioRepository;
import com.ticketdesk.backend.repository.UsuarioRolRepository;
import com.ticketdesk.backend.service.ConfiguracionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Carga datos de prueba la primera vez que arranca el sistema con la base vacía,
 * para poder probar el flujo completo sin insertar filas a mano: una empresa con
 * su configuración, el catálogo de categorías genéricas y un usuario de cada rol.
 *
 * Si ya existe alguna empresa no hace nada. Se desactiva con app.seed.enabled=false,
 * que es lo que corresponde en un entorno real (los usuarios de prueba tienen una
 * contraseña conocida).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataInitializer implements CommandLineRunner {

    private static final List<String> CATEGORIAS_GENERICAS = List.of(
            "Bug", "Consulta", "Facturación", "Reclamo de servicio", "Acceso a la cuenta");

    private final EmpresaRepository empresaRepository;
    private final ConfiguracionEmpresaRepository configuracionRepository;
    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.password:Demo1234!}")
    private String passwordDePrueba;

    @Override
    public void run(String... args) {
        if (empresaRepository.count() > 0) {
            return;
        }

        Empresa empresa = new Empresa();
        empresa.setNombre("Empresa Demo");
        empresa.setRubro("Servicios");
        empresa = empresaRepository.save(empresa);

        ConfiguracionEmpresa configuracion = new ConfiguracionEmpresa();
        configuracion.setEmpresa(empresa);
        configuracion.setModulosHabilitados(new ArrayList<>(ConfiguracionService.MODULOS_BASE));
        configuracion.setVentanaMantenimientoInicio(LocalTime.of(3, 0));
        configuracion.setVentanaMantenimientoFin(LocalTime.of(4, 0));
        configuracionRepository.save(configuracion);

        for (String nombre : CATEGORIAS_GENERICAS) {
            if (!categoriaRepository.existsByEmpresaIsNullAndNombreIgnoreCase(nombre)) {
                Categoria categoria = new Categoria();
                categoria.setNombre(nombre);
                categoriaRepository.save(categoria);
            }
        }

        crearUsuario(empresa, "Admin Demo", "admin@demo.com", Rol.ROLE_ADMIN);
        Usuario supervisor = crearUsuario(empresa, "Sofía Supervisora", "supervisor@demo.com", Rol.ROLE_SUPERVISOR);
        agregarRol(supervisor, Rol.ROLE_AGENT); // ejemplo de usuario con más de un rol
        crearUsuario(empresa, "Martín Agente", "agente@demo.com", Rol.ROLE_AGENT);
        crearUsuario(empresa, "Clara Cliente", "cliente@demo.com", Rol.ROLE_USER);

        log.info("Datos de prueba creados. Empresa '{}' con id {}", empresa.getNombre(), empresa.getId());
        log.info("Usuarios de prueba: admin@demo.com, supervisor@demo.com, agente@demo.com, cliente@demo.com");
    }

    private Usuario crearUsuario(Empresa empresa, String nombre, String email, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setEmpresa(empresa);
        usuario.setNombre(nombre);
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordEncoder.encode(passwordDePrueba));
        usuario.setRol(rol);
        usuario = usuarioRepository.save(usuario);
        agregarRol(usuario, rol); // usuario_roles siempre incluye el rol principal
        return usuario;
    }

    private void agregarRol(Usuario usuario, Rol rol) {
        UsuarioRol usuarioRol = new UsuarioRol();
        usuarioRol.setUsuario(usuario);
        usuarioRol.setRol(rol);
        usuarioRolRepository.save(usuarioRol);
    }
}
