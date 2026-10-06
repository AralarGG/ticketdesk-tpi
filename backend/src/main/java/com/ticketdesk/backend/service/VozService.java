package com.ticketdesk.backend.service;

import com.ticketdesk.backend.dto.SugerenciaVozResponse;
import com.ticketdesk.backend.model.Categoria;
import com.ticketdesk.backend.model.Ticket;
import com.ticketdesk.backend.model.Usuario;
import com.ticketdesk.backend.model.enums.CanalOrigen;
import com.ticketdesk.backend.model.enums.Prioridad;
import com.ticketdesk.backend.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Canal de voz (módulo 6, regla RN14).
 *
 * La transcripción de voz a texto la hace el navegador del cliente (Web Speech
 * API); el backend recibe el texto ya transcripto junto con el audio original.
 * Se guardan las dos cosas: el audio (en Cloudinary) y la transcripción exacta,
 * para poder auditar después si la categoría sugerida fue correcta.
 */
@Service
@RequiredArgsConstructor
public class VozService {

    /**
     * Palabras clave por categoría del catálogo genérico, sin tildes y en minúsculas.
     * El orden importa: ante un empate de coincidencias gana la categoría que aparece
     * primero (por ejemplo "error de pago" coincide con Bug y con Facturación, y
     * corresponde a Facturación).
     */
    private static final Map<String, List<String>> PALABRAS_CLAVE = new LinkedHashMap<>();

    static {
        PALABRAS_CLAVE.put("facturacion", List.of("pago", "pagar", "cobro", "cobraron", "factura", "tarjeta",
                "reembolso", "devolucion", "cargo", "comprobante", "suscripcion", "precio"));
        PALABRAS_CLAVE.put("acceso a la cuenta", List.of("sesion", "contrasena", "clave", "ingresar", "iniciar",
                "login", "acceso", "bloqueada", "bloqueado", "cuenta", "entrar"));
        PALABRAS_CLAVE.put("reclamo de servicio", List.of("reclamo", "queja", "demora", "mala atencion",
                "insatisfecho", "incumplimiento", "denuncia", "nadie me respondio"));
        PALABRAS_CLAVE.put("bug", List.of("error", "falla", "fallo", "no funciona", "no anda", "se cierra",
                "se cuelga", "se tranca", "bug", "no carga", "pantalla negra"));
        PALABRAS_CLAVE.put("consulta", List.of("consulta", "duda", "como hago", "informacion", "quisiera saber",
                "pregunta", "quiero saber"));
    }

    private static final String CATEGORIA_POR_DEFECTO = "consulta";
    private static final int LARGO_MAXIMO_TITULO = 80;

    private final CategoriaRepository categoriaRepository;
    private final TicketService ticketService;
    private final ConfiguracionService configuracionService;
    private final CloudinaryService cloudinaryService;

    /** Sugiere categoría y título a partir de la transcripción, para que el cliente los confirme. */
    public SugerenciaVozResponse sugerir(String transcripcion, Usuario cliente) {
        configuracionService.exigirModulo(cliente.getEmpresa().getId(), "voz");

        Optional<Categoria> categoria = sugerirCategoria(transcripcion, cliente);
        return new SugerenciaVozResponse(
                categoria.map(Categoria::getId).orElse(null),
                categoria.map(Categoria::getNombre).orElse(null),
                sugerirTitulo(transcripcion));
    }

    /**
     * Crea el ticket a partir de un mensaje de voz ya confirmado por el cliente:
     * sube el audio, y guarda el ticket con canal VOZ, la URL del audio y la
     * transcripción original.
     */
    public Ticket crearPorVoz(MultipartFile audio, String transcripcion, UUID categoriaId,
                              Prioridad prioridad, Usuario cliente) throws IOException {
        configuracionService.exigirModulo(cliente.getEmpresa().getId(), "voz");

        if (transcripcion == null || transcripcion.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La transcripción no puede estar vacía");
        }
        if (audio == null || audio.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta el audio original");
        }
        String tipo = audio.getContentType();
        if (tipo == null || !tipo.startsWith("audio/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo enviado no es un audio");
        }

        Categoria categoria = ticketService.obtenerCategoriaVisible(categoriaId, cliente);
        String audioUrl = cloudinaryService.subirAudio(audio);
        String texto = transcripcion.trim();

        return ticketService.guardarNuevoTicket(cliente, categoria, sugerirTitulo(texto), texto,
                prioridad, CanalOrigen.VOZ, audioUrl, texto);
    }

    private Optional<Categoria> sugerirCategoria(String transcripcion, Usuario cliente) {
        List<Categoria> visibles = categoriaRepository.findVisiblesParaEmpresa(cliente.getEmpresa().getId());
        String texto = normalizar(transcripcion);

        String mejor = null;
        int mejorPuntaje = 0;
        for (Map.Entry<String, List<String>> entrada : PALABRAS_CLAVE.entrySet()) {
            int puntaje = 0;
            for (String palabra : entrada.getValue()) {
                if (texto.contains(palabra)) {
                    puntaje++;
                }
            }
            if (puntaje > mejorPuntaje) { // estrictamente mayor: ante empate gana la primera
                mejorPuntaje = puntaje;
                mejor = entrada.getKey();
            }
        }

        String buscada = mejor != null ? mejor : CATEGORIA_POR_DEFECTO;
        return visibles.stream()
                .filter(c -> normalizar(c.getNombre()).equals(buscada))
                .findFirst();
    }

    /** Usa la primera frase de lo que dijo el cliente como título, acortada si es muy larga. */
    private String sugerirTitulo(String transcripcion) {
        String limpio = transcripcion.trim().replaceAll("\\s+", " ");

        int corte = limpio.length();
        for (char fin : new char[]{'.', '?', '!'}) {
            int posicion = limpio.indexOf(fin);
            if (posicion > 0 && posicion < corte) {
                corte = posicion;
            }
        }

        String titulo = limpio.substring(0, corte).trim();
        if (titulo.length() > LARGO_MAXIMO_TITULO) {
            titulo = titulo.substring(0, LARGO_MAXIMO_TITULO);
            int ultimoEspacio = titulo.lastIndexOf(' ');
            if (ultimoEspacio > LARGO_MAXIMO_TITULO / 2) {
                titulo = titulo.substring(0, ultimoEspacio);
            }
            titulo = titulo + "...";
        }
        return Character.toUpperCase(titulo.charAt(0)) + titulo.substring(1);
    }

    private String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}
