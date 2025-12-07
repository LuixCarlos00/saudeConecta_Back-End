package br.com.saudeConecta.infra.configuracoesseguranca;

import br.com.saudeConecta.endpoinst.usuario.Entity.Usuario;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.TokenExpiredException;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class TokenService {

    private static final int TOKEN_EXPIRATION_HOURS = 24;

    @Value("${api.security.token.secret}")
    private String secret;

    public String gerarToken(@NotNull Usuario usuario) {
        String autorizacao = usuario.getAuthorities().toString();
        return JWT.create()
                .withIssuer("saudeConecta")
                .withSubject(usuario.getUsername())
                .withClaim("id", usuario.getId())
                .withAudience(autorizacao)
                .withExpiresAt(calcularDataExpiracao())
                .sign(Algorithm.HMAC256(secret));
    }

    private Instant calcularDataExpiracao() {
        return LocalDateTime.now()
                .plusHours(TOKEN_EXPIRATION_HOURS)
                .toInstant(ZoneOffset.of("-03:00"));
    }

    public String getSubject(String tokenJwt) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("saudeConecta")
                    .build()
                    .verify(tokenJwt)
                    .getSubject();
        } catch (TokenExpiredException expiredException) {
            throw expiredException;
        } catch (Exception exception) {
            throw new RuntimeException("Token inválido", exception);
        }
    }
}