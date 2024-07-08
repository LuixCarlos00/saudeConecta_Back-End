package br.com.saudeConecta.infra.configuracoesseguranca;



import br.com.saudeConecta.endpoinst.usuario.Entity.Usuario;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.TokenExpiredException;
import jdk.jfr.Name;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class TokenService {

    @Value("${api.securitysconfigurations.tokensService}") //caminho da chave secreta
    private String secret;




    public String gerarToken(@NotNull Usuario usuario) {
        System.out.printf("usuario %s",usuario.getAuthorities().toString());
        String autorizacao = usuario.getAuthorities().toString();
        return JWT.create()
                .withIssuer("saudeConecta")
                .withSubject(usuario.getUsername())
                .withClaim("id",usuario.getId())
                .withAudience(autorizacao)

                .withExpiresAt(TempoValidoToken()).sign(Algorithm.HMAC256(secret));

    }





    @Name("Método é responsave por determina o tempo do  token  ")
    private Instant TempoValidoToken() {
        return LocalDateTime.now().plusYears(1).toInstant(ZoneOffset.of("-03:00"));
    }







    @Name("Método é responsável por fazer a validação do token JWT recebido. Se a validação for bem-sucedida, o método retorna um (subject) do token. " +
            "Caso contrário, uma exceção é lançada indicando que o token é inválido ou expirado.")
    public String getSubjet(String tokenJwt) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("saudeConecta")
                    .build()
                    .verify(tokenJwt)
                    .getSubject();
        } catch (TokenExpiredException expiredException) {
            throw expiredException; // Lança a exceção TokenExpiredException
        } catch (Exception exception) {
            throw new RuntimeException("Token inválido", exception);
        }


    }


}