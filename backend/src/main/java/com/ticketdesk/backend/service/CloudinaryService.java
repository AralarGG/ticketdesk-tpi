package com.ticketdesk.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Sube archivos a Cloudinary y devuelve la URL pública.
 * El archivo nunca se guarda en la base de datos, solo la URL
 * (ver regla de negocio RN13 en reglas-negocio-ticketdesk.md).
 */
@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret
    ) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }

    public String subirImagen(MultipartFile archivo) throws IOException {
        Map<?, ?> resultado = cloudinary.uploader().upload(
                archivo.getBytes(),
                ObjectUtils.asMap("resource_type", "image")
        );
        return (String) resultado.get("secure_url");
    }

    public String subirAudio(MultipartFile archivo) throws IOException {
        Map<?, ?> resultado = cloudinary.uploader().upload(
                archivo.getBytes(),
                ObjectUtils.asMap("resource_type", "video") // Cloudinary trata el audio como "video" sin pista visual
        );
        return (String) resultado.get("secure_url");
    }
}
