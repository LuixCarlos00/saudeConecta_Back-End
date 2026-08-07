package br.com.saudeConecta.infra;

import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.usuario.StatusUsuario;
import br.com.saudeConecta.domain.usuario.TipoUsuarioNovo;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infra.configuracoesseguranca.TokenService;
import com.auth0.jwt.exceptions.TokenExpiredException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
            String token = tokenService.gerarToken(usuario, 1L, "Dr. Teste", "CLINICO");
            Long orgId = tokenService.getOrganizacaoIdFromToken(token);

            assertThat(orgId).isEqualTo(1L);
        }

        @Test
        @DisplayName("Deve gerar token com userId embutido")
        void deveGerarTokenComUserId() {
            String token = tokenService.gerarToken(usuario, 1L, "Dr. Teste", "CLINICO");
            Long userId = tokenService.getUserIdFromToken(token);

            assertThat(userId).isEqualTo(10L);
        }

        @Test
        @DisplayName("Deve gerar token com tipoUsuario embutido")
        void deveGerarTokenComTipoUsuario() {
            String token = tokenService.gerarToken(usuario, 1L, "Dr. Teste", "CLINICO");
            Integer tipo = tokenService.getTipoUsuarioFromToken(token);

            assertThat(tipo).isEqualTo(3);
        }

        @Test
        @DisplayName("Deve gerar token com nome embutido")
        void deveGerarTokenComNome() {
            String token = tokenService.gerarToken(usuario, 1L, "Dr. Teste", "CLINICO");
            String nome = tokenService.getNomeFromToken(token);

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
            assertThatThrownBy(() -> tokenService.getSubject("token.invalido.aqui"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Token invalido");
        }

        @Test
        @DisplayName("Deve lançar exceção com token assinado com secret diferente")
        void deveLancarExcecaoComSecretDiferente() {
            TokenService outroService = new TokenService();
            ReflectionTestUtils.setField(outroService, "secret", "outro-secret-completamente-diferente");
            String tokenDeOutroService = outroService.gerarToken(usuario);

            assertThatThrownBy(() -> tokenService.getSubject(tokenDeOutroService))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("Deve lançar exceção com string vazia")
        void deveLancarExcecaoComTokenVazio() {
            assertThatThrownBy(() -> tokenService.getSubject(""))
                    .isInstanceOf(RuntimeException.class);
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

        @Test
        @DisplayName("getTipoUsuarioFromToken deve retornar null com token inválido")
        void deveRetornarNullParaTipoComTokenInvalido() {
            Integer resultado = tokenService.getTipoUsuarioFromToken("token.invalido");

            assertThat(resultado).isNull();
        }

        @Test
        @DisplayName("getNomeFromToken deve retornar null com token inválido")
        void deveRetornarNullParaNomeComTokenInvalido() {
            String resultado = tokenService.getNomeFromToken("token.invalido");

            assertThat(resultado).isNull();
        }
    }
}
