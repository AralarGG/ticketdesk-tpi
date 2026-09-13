package com.ticketdesk.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Genera y valida tokens JWT. El token incluye el email (subject) y el
 * rol del usuario, para que el backend pueda verificar permisos sin
 * consultar la base de datos en cada request (autenticación stateless).
 */
@Service
public class JwtService {

    // En producción, esta clave debe salir de una variable de entorno,
    // nunca hardcodeada en el código fuente.
    @Value("${jwt.secret:ticketdesk-clave-secreta-cambiar-en-produccion-por-una-mas-larga}")
    private String secretKey;

    @Value("${jwt.expiration-ms:86400000}") // 24hs por defecto
    private long expirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    public String generateToken(UserDetails userDetails, String rol, String usuarioId, String empresaId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("rol", rol);
        claims.put("usuario_id", usuarioId);
        claims.put("empresa_id", empresaId);

        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername()) // acá usamos el email como "username"
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRol(String token) {
        return extractAllClaims(token).get("rol", String.class);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String email = extractEmail(token);
        return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
