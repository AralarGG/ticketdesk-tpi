package com.ticketdesk.backend.service;

import com.ticketdesk.backend.dto.CrearCategoriaRequest;
import com.ticketdesk.backend.model.Categoria;
import com.ticketdesk.backend.model.Usuario;
import com.ticketdesk.backend.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Categorías de tickets (módulo 4, regla RN10): un catálogo genérico compartido
 * por todas las empresas más las categorías propias de cada una.
 */
@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ConfiguracionService configuracionService;

    /** Las genéricas más las propias de la empresa del usuario (nunca las de otra empresa). */
    public List<Categoria> listarVisibles(Usuario usuario) {
        return categoriaRepository.findVisiblesParaEmpresa(usuario.getEmpresa().getId());
    }

    /**
     * Crea una categoría propia de la empresa del administrador. Requiere que la
     * empresa tenga habilitado el módulo "categorias_propias" y que el nombre no
     * repita uno propio ni uno del catálogo genérico.
     */
    public Categoria crearPropia(CrearCategoriaRequest request, Usuario admin) {
        configuracionService.exigirModulo(admin.getEmpresa().getId(), "categorias_propias");

        String nombre = request.getNombre().trim();
        boolean repiteGenerica = categoriaRepository.existsByEmpresaIsNullAndNombreIgnoreCase(nombre);
        boolean repitePropia = categoriaRepository.existsByEmpresaIdAndNombreIgnoreCase(admin.getEmpresa().getId(), nombre);
        if (repiteGenerica || repitePropia) {
            throw new IllegalStateException("Ya existe una categoría llamada '" + nombre + "'");
        }

        Categoria categoria = new Categoria();
        categoria.setEmpresa(admin.getEmpresa());
        categoria.setNombre(nombre);
        categoria.setDescripcion(request.getDescripcion());
        return categoriaRepository.save(categoria);
    }
}
