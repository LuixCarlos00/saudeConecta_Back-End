package br.com.saudeConecta.infra.configuracoesseguranca;

import br.com.saudeConecta.domain.usuario.Usuario;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@Slf4j
public class TokenService {

    private static final int TOKEN_EXPIRATION_HOURS = 24;
    private static final String ISSUER = "saudeConecta";
    private static final String CLAIM_USER_ID = "id";
    private static final String CLAIM_ORGANIZACAO_ID = "organizacaoId";
    private static final String CLAIM_TIPO_USUARIO = "tipoUsuario";

    @Value("${api.security.token.secret}")
    private String secret;

    public String gerarToken(@NotNull Usuario usuario) {
        String autorizacao = usuario.getAuthorities().toString();
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(usuario.getUsername())
                .withClaim(CLAIM_USER_ID, usuario.getId())
                .withAudience(autorizacao)
                .withExpiresAt(calcularDataExpiracao())
                .sign(Algorithm.HMAC256(secret));
    }

    public String gerarToken(@NotNull Usuario usuario, Long organizacaoId) {
        String autorizacao = usuario.getAuthorities().toString();
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(usuario.getUsername())
                .withClaim(CLAIM_USER_ID, usuario.getId())
                .withClaim(CLAIM_ORGANIZACAO_ID, organizacaoId)
                .withClaim(CLAIM_TIPO_USUARIO, usuario.getTipoUsuario().intValue())
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
                    .withIssuer(ISSUER)
                    .build()
                    .verify(tokenJwt)
                    .getSubject();
        } catch (TokenExpiredException expiredException) {
            throw expiredException;
        } catch (Exception exception) {
            throw new RuntimeException("Token invalido", exception);
        }
    }

    public Long getOrganizacaoIdFromToken(String token) {
        try {
            DecodedJWT jwt = decodeToken(token);
            return jwt.getClaim(CLAIM_ORGANIZACAO_ID).asLong();
        } catch (Exception e) {
            log.warn("Erro ao extrair organizacaoId do token: {}", e.getMessage());
            return null;
        }
    }

    public Long getUserIdFromToken(String token) {
        try {
            DecodedJWT jwt = decodeToken(token);
            return jwt.getClaim(CLAIM_USER_ID).asLong();
        } catch (Exception e) {
            log.warn("Erro ao extrair userId do token: {}", e.getMessage());
            return null;
        }
    }

    public Integer getTipoUsuarioFromToken(String token) {
        try {
            DecodedJWT jwt = decodeToken(token);
            return jwt.getClaim(CLAIM_TIPO_USUARIO).asInt();
        } catch (Exception e) {
            log.warn("Erro ao extrair tipoUsuario do token: {}", e.getMessage());
            return null;
        }
    }

    private DecodedJWT decodeToken(String token) {
        Algorithm algorithm = Algorithm.HMAC256(secret);
        return JWT.require(algorithm)
                .withIssuer(ISSUER)
                .build()
                .verify(token);
    }
}