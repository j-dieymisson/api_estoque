package com.api.estoque.service;

import com.api.estoque.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

@Service
public class TokenService {

    @Value("${api.security.token.secret}")
    private String secret;

    public String gerarToken(Usuario usuario) {
        return Jwts.builder()
                .setIssuer("API Estoque")
                .setSubject(usuario.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(Date.from(dataExpiracao()))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String getSubject(String tokenJWT) {
        try {

            Claims claims = Jwts.parser()             // 1. Inicia o construtor do parser
                    .setSigningKey(getSignInKey())    // 2. Configura a chave de assinatura
                    .build()                          // 3. Constrói o parser final
                    .parseClaimsJws(tokenJWT)         // 4. Usa o parser para validar e ler o token
                    .getBody();

            return claims.getSubject();
        } catch (Exception e) {
            throw new RuntimeException("Token JWT inválido ou expirado!");
        }
    }

    private Instant dataExpiracao() {
        return LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.of("-03:00"));
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(this.secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}