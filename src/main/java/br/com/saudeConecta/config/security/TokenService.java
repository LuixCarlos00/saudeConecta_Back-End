package br.com.saudeConecta.config.security;

import br.com.saudeConecta.domain.usuario.Usuario;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@Slf4j
public class TokenService {

    private static final int TOKEN_EXPIRATION_HOURS = 24;
    private static final String ISSUER = "saudeConecta";
    private static final String CLAIM_USER_ID = "id";
    private static final String CLAIM_ORGANIZACAO_ID = "organizacaoId";
    private static final String CLAIM_TIPO_USUARIO = "tipoUsuario";
    private static final String CLAIM_NOME = "nome";
    private static final String CLAIM_PERFIL = "perfil";
    private static final String CLAIM_TIPO_PROFISSIONAL = "tipoProfissional";

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

    /**
     * Gera o token JWT do usuario autenticado.
     *
     * @param usuario          usuario autenticado
     * @param organizacaoId    ID da organizacao (tenant)
     * @param nome             nome exibido do usuario
     * @param perfil           perfil geral (GESTOR, CLINICO, ASSISTENTE)
     * @param tipoProfissional especializacao do clinico (MEDICO, DENTISTA) ou null
     * @return token JWT assinado
     */
    public String gerarToken(@NotNull Usuario usuario, Long organizacaoId, String nome, String perfil,
                             String tipoProfissional) {
        String autorizacao = usuario.getAuthorities().toString();
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(usuario.getUsername())
                .withClaim(CLAIM_USER_ID, usuario.getId())
                .withClaim(CLAIM_ORGANIZACAO_ID, organizacaoId)
                .withClaim(CLAIM_TIPO_USUARIO, usuario.getTipoUsuarioNovo().getCodigo())
                .withClaim(CLAIM_NOME, nome)
                .withClaim(CLAIM_PERFIL, perfil)
                .withClaim(CLAIM_TIPO_PROFISSIONAL, tipoProfissional)
                .withAudience(autorizacao)
                .withExpiresAt(calcularDataExpiracao())
                .sign(Algorithm.HMAC256(secret));
    }

    private Instant calcularDataExpiracao() {
        // Instant é absoluto: não depende do fuso do servidor (antes, em UTC, o token valia 27h)
        return Instant.now().plus(TOKEN_EXPIRATION_HOURS, ChronoUnit.HOURS);
    }

    /**
     * Valida o token e devolve o subject (login).
     *
     * @param tokenJwt token recebido no header Authorization
     * @return login do usuário
     * @throws JWTVerificationException token expirado, adulterado, malformado ou vazio. Toda falha de
     *         validação sai com esse tipo para o FiltroAcesso responder 401 (antes virava RuntimeException → 500)
     */
    public String getSubject(String tokenJwt) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .build()
                    .verify(tokenJwt)
                    .getSubject();
        } catch (JWTVerificationException verificacao) {
            throw verificacao;
        } catch (RuntimeException exception) {
            throw new JWTVerificationException("Token invalido", exception);
        }
    }

    public Long getOrganizacaoIdFromToken(String token) {
        try {
            DecodedJWT jwt = decodeToken(token);
            return jwt.getClaim(CLAIM_ORGANIZACAO_ID).asLong();
        } catch (Exception e) {
            log.warn("auth.token negado motivo=CLAIM_INVALIDA claim=organizacaoId causa={}", e.getClass().getSimpleName());
            return null;
        }
    }

    public Long getUserIdFromToken(String token) {
        try {
            DecodedJWT jwt = decodeToken(token);
            return jwt.getClaim(CLAIM_USER_ID).asLong();
        } catch (Exception e) {
            log.warn("auth.token negado motivo=CLAIM_INVALIDA claim=userId causa={}", e.getClass().getSimpleName());
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
