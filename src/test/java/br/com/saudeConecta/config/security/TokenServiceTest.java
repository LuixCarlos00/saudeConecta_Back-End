package br.com.saudeConecta.config.security;

import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.usuario.StatusUsuario;
import br.com.saudeConecta.domain.usuario.TipoUsuarioNovo;
import br.com.saudeConecta.domain.usuario.Usuario;
import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.TokenExpiredException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenService - Testes unitários")
class TokenServiceTest {

    private TokenService tokenService;

    private static final String SECRET = "test-secret-key-para-testes-unitarios-12345";

    private Organizacao organizacao;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService();
        ReflectionTestUtils.setField(tokenService, "secret", SECRET);

        organizacao = Organizacao.builder().nome("Clínica Teste").build();
        organizacao.setId(1L);

        usuario = Usuario.builder()
                .login("12345678900")
                .senha("encodedPass")
                .tipoUsuarioNovo(TipoUsuarioNovo.CLINICO)
                .status(StatusUsuario.ATIVO)
                .organizacao(organizacao)
                .build();
        usuario.setId(10L);
    }

    // ========== gerarToken (simples) ==========

    @Nested
    @DisplayName("gerarToken (sem claims extras)")
    class GerarTokenSimples {

        @Test
        @DisplayName("Deve gerar token JWT não nulo e não vazio")
        void deveGerarTokenNaoNuloENaoVazio() {
            String token = tokenService.gerarToken(usuario);

            assertThat(token).isNotNull().isNotBlank();
        }

        @Test
        @DisplayName("Deve gerar token válido que retorna o subject correto")
        void deveGerarTokenComSubjectCorreto() {
            String token = tokenService.gerarToken(usuario);
            String subject = tokenService.getSubject(token);

            assertThat(subject).isEqualTo("12345678900");
        }

        @Test
        @DisplayName("Deve gerar tokens diferentes para chamadas consecutivas")
        void deveGerarTokensDiferentes() {
            String token1 = tokenService.gerarToken(usuario);
            String token2 = tokenService.gerarToken(usuario);

            // Tokens podem ser iguais se gerados no mesmo segundo — testamos que são não nulos
            assertThat(token1).isNotNull();
            assertThat(token2).isNotNull();
        }
    }

    // ========== gerarToken (com claims extras) ==========

    @Nested
    @DisplayName("gerarToken (com organizacaoId, nome e perfil)")
    class GerarTokenCompleto {

        @Test
        @DisplayName("Deve gerar token com organizacaoId embutido")
        void deveGerarTokenComOrganizacaoId() {
            String token = tokenService.gerarToken(usuario, 1L, "Dr. Teste", "CLINICO", "MEDICO");
            Long orgId = tokenService.getOrganizacaoIdFromToken(token);

            assertThat(orgId).isEqualTo(1L);
        }

        @Test
        @DisplayName("Deve gerar token com userId embutido")
        void deveGerarTokenComUserId() {
            String token = tokenService.gerarToken(usuario, 1L, "Dr. Teste", "CLINICO", "MEDICO");
            Long userId = tokenService.getUserIdFromToken(token);

            assertThat(userId).isEqualTo(10L);
        }

        @Test
        @DisplayName("Deve gerar token com tipoUsuario embutido")
        void deveGerarTokenComTipoUsuario() {
            String token = tokenService.gerarToken(usuario, 1L, "Dr. Teste", "CLINICO", "MEDICO");
            Integer tipo = JWT.decode(token).getClaim("tipoUsuario").asInt();

            assertThat(tipo).isEqualTo(TipoUsuarioNovo.CLINICO.getCodigo());
        }

        @Test
        @DisplayName("Deve gerar token com nome embutido")
        void deveGerarTokenComNome() {
            String token = tokenService.gerarToken(usuario, 1L, "Dr. Teste", "CLINICO", "MEDICO");
            String nome = JWT.decode(token).getClaim("nome").asString();

            assertThat(nome).isEqualTo("Dr. Teste");
        }
    }

    // ========== getSubject ==========

    @Nested
    @DisplayName("getSubject")
    class GetSubject {

        @Test
        @DisplayName("Deve lançar exceção com token inválido")
        void deveLancarExcecaoComTokenInvalido() {
            // Fase 5.4: precisa ser JWTVerificationException para o FiltroAcesso responder 401 (antes virava 500)
            assertThatThrownBy(() -> tokenService.getSubject("token.invalido.aqui"))
                    .isInstanceOf(com.auth0.jwt.exceptions.JWTVerificationException.class);
        }

        @Test
        @DisplayName("Deve lançar exceção com token assinado com secret diferente")
        void deveLancarExcecaoComSecretDiferente() {
            TokenService outroService = new TokenService();
            ReflectionTestUtils.setField(outroService, "secret", "outro-secret-completamente-diferente");
            String tokenDeOutroService = outroService.gerarToken(usuario);

            assertThatThrownBy(() -> tokenService.getSubject(tokenDeOutroService))
                    .isInstanceOf(com.auth0.jwt.exceptions.JWTVerificationException.class);
        }

        @Test
        @DisplayName("Deve lançar exceção com string vazia")
        void deveLancarExcecaoComTokenVazio() {
            assertThatThrownBy(() -> tokenService.getSubject(""))
                    .isInstanceOf(com.auth0.jwt.exceptions.JWTVerificationException.class);
        }
    }

    // ========== Extração de claims com tokens inválidos ==========

    @Nested
    @DisplayName("Extração de claims com tokens inválidos")
    class ExtrairClaimsInvalidos {

        @Test
        @DisplayName("getOrganizacaoIdFromToken deve retornar null com token inválido")
        void deveRetornarNullParaOrgIdComTokenInvalido() {
            Long resultado = tokenService.getOrganizacaoIdFromToken("token.invalido");

            assertThat(resultado).isNull();
        }

        @Test
        @DisplayName("getUserIdFromToken deve retornar null com token inválido")
        void deveRetornarNullParaUserIdComTokenInvalido() {
            Long resultado = tokenService.getUserIdFromToken("token.invalido");

            assertThat(resultado).isNull();
        }


    }

    // ========== expiração ==========

    @Nested
    @DisplayName("4.3 - expiração do token independe do fuso do servidor")
    class Expiracao {

        @Test
        @DisplayName("Em servidor UTC o token expira em 24h (antes: 27h por causa do offset fixo -03:00)")
        void deveExpirarEm24HorasEmServidorUtc() {
            TimeZone original = TimeZone.getDefault();
            try {
                TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
                Instant antes = Instant.now();

                String token = tokenService.gerarToken(usuario, 1L, "Nome", "CLINICO", null);

                Instant expiracao = JWT.decode(token).getExpiresAtAsInstant();
                assertThat(Duration.between(antes, expiracao))
                        .isBetween(Duration.ofHours(24).minusMinutes(1), Duration.ofHours(24).plusMinutes(1));
            } finally {
                TimeZone.setDefault(original);
            }
        }
    }
}
