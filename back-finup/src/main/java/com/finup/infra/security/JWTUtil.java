package com.finup.infra.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.finup.credencial.Credencial;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
public class JWTUtil {
    @Value("${jwt_secret}")
    private String jwtSecret;

    public String gerarToken(Credencial credencial) {
        try {
            var algoritmo = Algorithm.HMAC256(jwtSecret);
            return JWT.create()
                    .withIssuer("API FinUp")
                    .withSubject(credencial.getUsername())
                    .withClaim("username", credencial.getUsername())
                    .withExpiresAt(dataExpiracao())
                    .sign(algoritmo);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("erro ao gerar token jwt", exception);
        }
    }

    public String validateTokenAndRetrieveSubject(String tokenJWT) {
        try {
            var algoritmo = Algorithm.HMAC256(jwtSecret);
            JWTVerifier verifier = JWT.require(algoritmo)
                    .withIssuer("API FinUp")
                    .build();

            DecodedJWT jwt = verifier.verify(tokenJWT);

            return jwt.getClaim("username").asString();
        } catch (JWTVerificationException exception) {
            throw new RuntimeException("Token inválido ou expirado!");
        }
    }

    private Instant dataExpiracao() {
        return LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.of("-03:00"));
    }
}