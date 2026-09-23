package com.giovanni.assistenterag.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey chave;
    private final long expiracaoHoras;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiracao-horas}") long expiracaoHoras) {

        this.chave = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiracaoHoras = expiracaoHoras;

            System.out.println(">>> JwtService iniciado. Secret com "
                + secret.length() + " caracteres.");
    }

    public String gerarToken(Long usuarioId, String email) {
        Instant agora = Instant.now();
        Instant expiracao = agora.plus(expiracaoHoras, ChronoUnit.HOURS);

        return Jwts.builder()
                .subject(String.valueOf(usuarioId))
                .claim("email", email)
                .issuedAt(Date.from(agora))
                .expiration(Date.from(expiracao))
                .signWith(chave)
                .compact();
    }

    public Long extrairUsuarioId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(chave)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return Long.valueOf(claims.getSubject());
    }

    public boolean tokenValido(String token) {
        try {
            Jwts.parser().verifyWith(chave).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
             System.out.println(">>> Falha ao validar token: "
                    + e.getClass().getSimpleName() + " - " + e.getMessage());
            return false;
        }
    }
}