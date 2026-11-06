package br.com.saudeConecta.application.secretaria;

import br.com.saudeConecta.application.assinatura.LimitePlanoService;
import br.com.saudeConecta.application.cache.CacheEvictionService;
import br.com.saudeConecta.application.historico.HistoricoDadosPessoaisService;
import br.com.saudeConecta.application.notificacao.EmailNotificacaoService;
import br.com.saudeConecta.application.secretaria.dto.AtualizarSecretariaRequest;
import br.com.saudeConecta.application.secretaria.dto.CadastrarSecretariaRequest;
import br.com.saudeConecta.application.usuario.EmailUnicoService;
import br.com.saudeConecta.config.tenant.TenantHelper;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.secretaria.Secretaria;
import br.com.saudeConecta.domain.secretaria.StatusSecretaria;
import br.com.saudeConecta.domain.usuario.StatusUsuario;
import br.com.saudeConecta.domain.usuario.TipoUsuarioNovo;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infrastructure.persistence.repository.OrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.SecretariaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecretariaService - Testes unitários")
class SecretariaServiceTest {

    @Mock private SecretariaRepository secretariaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private OrganizacaoRepository organizacaoRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TenantHelper tenantHelper;
    @Mock private EmailNotificacaoService emailNotificacaoService;
    @Mock private EmailUnicoService emailUnicoService;
    @Mock private HistoricoDadosPessoaisService historicoDadosPessoaisService;
    @Mock private LimitePlanoService limitePlanoService;
    @Mock private CacheEvictionService cacheEvictionService;

    @InjectMocks
    private SecretariaService secretariaService;

    private static final Long ORG_ID = 1L;
    private static final Long SEC_ID = 10L;
    private static final Long USUARIO_ID = 20L;

    private Organizacao organizacao;
    private Usuario usuario;
    private Secretaria secretaria;

    @BeforeEach
    void setUp() {
        organizacao = Organizacao.builder()
                .nome("Clínica Teste").cnpj("12345678000100")
                .build();
        organizacao.setId(ORG_ID);

        usuario = Usuario.builder()
                .login("12345678900").senha("encodedPass")
                .tipoUsuarioNovo(TipoUsuarioNovo.ASSISTENTE)
                .status(StatusUsuario.ATIVO).organizacao(organizacao)
                .build();
        usuario.setId(USUARIO_ID);

        secretaria = Secretaria.builder()
                .organizacao(organizacao).usuario(usuario)
                .nome("Secretaria Teste").cpf("12345678900")
                .email("sec@teste.com").telefone("11999999999")
                .status(StatusSecretaria.ATIVO)
                .build();
        secretaria.setId(SEC_ID);
    }

    // ========== buscarSecretariaIdByOrg ==========

    @Nested
    @DisplayName("buscarSecretariaIdByOrg")
    class BuscarSecretariaIdByOrg {

        @Test
        @DisplayName("Deve retornar secretaria quando ID existir na organização")
        void deveRetornarSecretariaQuandoIdExistir() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(secretariaRepository.findByIdAndOrganizacao_Id(SEC_ID, ORG_ID))
                    .thenReturn(Optional.of(secretaria));

            Optional<Secretaria> resultado = secretariaService.buscarSecretariaIdByOrg(SEC_ID);

            assertThat(resultado).isPresent();
            assertThat(resultado.get().getId()).isEqualTo(SEC_ID);
        }

        @Test
        @DisplayName("Deve retornar vazio quando ID não existir na organização")
        void deveRetornarVazioQuandoNaoExistir() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(secretariaRepository.findByIdAndOrganizacao_Id(999L, ORG_ID))
                    .thenReturn(Optional.empty());

            Optional<Secretaria> resultado = secretariaService.buscarSecretariaIdByOrg(999L);

            assertThat(resultado).isEmpty();
        }
    }

    // ========== atualizarSecretariaIdByOrg ==========

    @Nested
    @DisplayName("atualizarSecretariaIdByOrg")
    class AtualizarSecretariaIdByOrg {

        @Test
        @DisplayName("Sem ser gestor nem a dona, a atualização é negada antes de qualquer alteração")
        void naoGestorNemDonaEhNegado() {
            AtualizarSecretariaRequest dadosAtualizados = new AtualizarSecretariaRequest(null, "invasor@email.com", null);
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(secretariaRepository.findByIdAndOrganizacao_Id(SEC_ID, ORG_ID))
                    .thenReturn(Optional.of(secretaria));
            doThrow(new br.com.saudeConecta.config.tenant.TenantAccessDeniedException("negado"))
                    .when(tenantHelper).validarGestorOuDono(USUARIO_ID);

            assertThatThrownBy(() -> secretariaService.atualizarSecretariaIdByOrg(SEC_ID, dadosAtualizados))
                    .isInstanceOf(br.com.saudeConecta.config.tenant.TenantAccessDeniedException.class);
            assertThat(secretaria.getEmail()).isNotEqualTo("invasor@email.com");
            verify(secretariaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve atualizar nome, email e telefone com sucesso")
        void deveAtualizarDadosComSucesso() {
            AtualizarSecretariaRequest dadosAtualizados =
                    new AtualizarSecretariaRequest("Novo Nome", "novo@email.com", "11888888888");

            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(tenantHelper.getCurrentUserId()).thenReturn(USUARIO_ID);
            when(secretariaRepository.findByIdAndOrganizacao_Id(SEC_ID, ORG_ID))
                    .thenReturn(Optional.of(secretaria));
            when(secretariaRepository.save(any(Secretaria.class))).thenReturn(secretaria);
            doNothing().when(historicoDadosPessoaisService).registrarAlteracoesDeObjeto(any(), any(), any(), any(), any());

            Secretaria resultado = secretariaService.atualizarSecretariaIdByOrg(SEC_ID, dadosAtualizados);

            assertThat(resultado).isNotNull();
            assertThat(secretaria.getNome()).isEqualTo("Novo Nome");
            assertThat(secretaria.getEmail()).isEqualTo("novo@email.com");
            assertThat(secretaria.getTelefone()).isEqualTo("11888888888");
            verify(secretariaRepository).save(secretaria);
        }

        @Test
        @DisplayName("4.5d - Trocar para e-mail de outro usuário é rejeitado")
        void deveRejeitarEmailDeOutroUsuario() {
            AtualizarSecretariaRequest dadosAtualizados = new AtualizarSecretariaRequest(null, "gestor@outra.com", null);

            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(secretariaRepository.findByIdAndOrganizacao_Id(SEC_ID, ORG_ID))
                    .thenReturn(Optional.of(secretaria));
            doThrow(new IllegalStateException("Email ja cadastrado no sistema para outro usuario"))
                    .when(emailUnicoService)
                    .validarEmailLivreParaAtualizacao("gestor@outra.com", "sec@teste.com", USUARIO_ID);

            assertThatThrownBy(() -> secretariaService.atualizarSecretariaIdByOrg(SEC_ID, dadosAtualizados))
                    .isInstanceOf(IllegalStateException.class);
            assertThat(secretaria.getEmail()).isEqualTo("sec@teste.com");
            verify(secretariaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Não deve alterar campos quando forem nulos")
        void naoDeveAlterarCamposNulos() {
            AtualizarSecretariaRequest dadosVazios = new AtualizarSecretariaRequest(null, null, null);

            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(tenantHelper.getCurrentUserId()).thenReturn(USUARIO_ID);
            when(secretariaRepository.findByIdAndOrganizacao_Id(SEC_ID, ORG_ID))
                    .thenReturn(Optional.of(secretaria));
            when(secretariaRepository.save(any())).thenReturn(secretaria);
            doNothing().when(historicoDadosPessoaisService).registrarAlteracoesDeObjeto(any(), any(), any(), any(), any());

            secretariaService.atualizarSecretariaIdByOrg(SEC_ID, dadosVazios);

            assertThat(secretaria.getNome()).isEqualTo("Secretaria Teste");
            assertThat(secretaria.getEmail()).isEqualTo("sec@teste.com");
        }

        @Test
        @DisplayName("Deve lançar exceção quando secretaria não for encontrada")
        void deveLancarExcecaoQuandoNaoEncontrada() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(secretariaRepository.findByIdAndOrganizacao_Id(999L, ORG_ID))
                    .thenReturn(Optional.empty());

            AtualizarSecretariaRequest semAlteracoes = new AtualizarSecretariaRequest(null, null, null);
            assertThatThrownBy(() -> secretariaService.atualizarSecretariaIdByOrg(999L, semAlteracoes))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Secretaria nao encontrada");
        }
    }

    // ========== cadastrarSecretariaByOrg ==========

    @Nested
    @DisplayName("cadastrarSecretariaByOrg")
    class CadastrarSecretariaByOrg {

        private CadastrarSecretariaRequest request;

        @BeforeEach
        void setUp() {
            request = new CadastrarSecretariaRequest("Nova Sec", "123.456.789-00", "nova@sec.com", "11777777777");
        }

        @Test
        @DisplayName("Deve cadastrar secretaria com sucesso")
        void deveCadastrarSecretariaComSucesso() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            doNothing().when(limitePlanoService).validarLimiteSecretaria(ORG_ID);
            when(usuarioRepository.existsByLogin("12345678900")).thenReturn(false);
            when(emailUnicoService.emailJaExiste("nova@sec.com")).thenReturn(false);
            when(organizacaoRepository.findById(ORG_ID)).thenReturn(Optional.of(organizacao));
            when(passwordEncoder.encode(any())).thenReturn("encodedPass");
            when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
            when(secretariaRepository.save(any(Secretaria.class))).thenReturn(secretaria);

            Secretaria resultado = secretariaService.cadastrarSecretariaByOrg(request);

            assertThat(resultado).isNotNull();
            assertThat(resultado.getId()).isEqualTo(SEC_ID);
            verify(usuarioRepository).save(any(Usuario.class));
            verify(secretariaRepository).save(any(Secretaria.class));
            verify(emailNotificacaoService).enviarCredenciaisSecretaria(
                    eq("nova@sec.com"), eq("Nova Sec"), eq("12345678900"),
                    any(), eq(ORG_ID), eq(SEC_ID));
        }

        @Test
        @DisplayName("Deve lançar exceção quando CPF já estiver cadastrado")
        void deveLancarExcecaoQuandoCpfJaCadastrado() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            doNothing().when(limitePlanoService).validarLimiteSecretaria(ORG_ID);
            when(usuarioRepository.existsByLogin("12345678900")).thenReturn(true);

            assertThatThrownBy(() -> secretariaService.cadastrarSecretariaByOrg(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("CPF já cadastrado");
        }

        @Test
        @DisplayName("Deve lançar exceção quando email já existir")
        void deveLancarExcecaoQuandoEmailJaExiste() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            doNothing().when(limitePlanoService).validarLimiteSecretaria(ORG_ID);
            when(usuarioRepository.existsByLogin("12345678900")).thenReturn(false);
            when(emailUnicoService.emailJaExiste("nova@sec.com")).thenReturn(true);
            when(emailUnicoService.ondeEmailFoiEncontrado("nova@sec.com")).thenReturn("SECRETARIA");

            assertThatThrownBy(() -> secretariaService.cadastrarSecretariaByOrg(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Email já cadastrado");
        }

        @Test
        @DisplayName("Deve lançar exceção quando organização não for encontrada")
        void deveLancarExcecaoQuandoOrganizacaoNaoEncontrada() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            doNothing().when(limitePlanoService).validarLimiteSecretaria(ORG_ID);
            when(usuarioRepository.existsByLogin(any())).thenReturn(false);
            when(emailUnicoService.emailJaExiste(any())).thenReturn(false);
            when(organizacaoRepository.findById(ORG_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> secretariaService.cadastrarSecretariaByOrg(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Organizacao nao encontrada");
        }
    }

    // ========== deletarSecretariaIdByOrg ==========

    @Nested
    @DisplayName("deletarSecretariaIdByOrg")
    class DeletarSecretariaIdByOrg {

        @Test
        @DisplayName("Deve deletar secretaria e usuário com sucesso")
        void deveDeletarSecretariaComSucesso() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(secretariaRepository.findByIdAndOrganizacao_Id(SEC_ID, ORG_ID))
                    .thenReturn(Optional.of(secretaria));

            secretariaService.deletarSecretariaIdByOrg(SEC_ID);

            verify(secretariaRepository).delete(secretaria);
            verify(usuarioRepository).delete(usuario);
        }

        @Test
        @DisplayName("Deve lançar exceção quando secretaria não for encontrada")
        void deveLancarExcecaoQuandoNaoEncontrada() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(secretariaRepository.findByIdAndOrganizacao_Id(999L, ORG_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> secretariaService.deletarSecretariaIdByOrg(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Secretaria nao encontrada");
        }

        @Test
        @DisplayName("Deve lançar exceção quando secretaria não tiver usuário associado")
        void deveLancarExcecaoQuandoSemUsuarioAssociado() {
            Secretaria secSemUsuario = Secretaria.builder()
                    .nome("Sem Usuário").usuario(null).status(StatusSecretaria.ATIVO).build();
            secSemUsuario.setId(SEC_ID);

            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(secretariaRepository.findByIdAndOrganizacao_Id(SEC_ID, ORG_ID))
                    .thenReturn(Optional.of(secSemUsuario));

            assertThatThrownBy(() -> secretariaService.deletarSecretariaIdByOrg(SEC_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Usuario associado");
        }

        @Test
        @DisplayName("Deve propagar exceção de relacionamento ao deletar")
        void devePropararExcecaoDeRelacionamento() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(secretariaRepository.findByIdAndOrganizacao_Id(SEC_ID, ORG_ID))
                    .thenReturn(Optional.of(secretaria));
            doThrow(new RuntimeException("FK constraint violation"))
                    .when(secretariaRepository).delete(secretaria);

            // Fase 5.3: a causa original fica encadeada e a mensagem do banco não vaza para o cliente
            assertThatThrownBy(() -> secretariaService.deletarSecretariaIdByOrg(SEC_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("relacionamentos existentes")
                    .hasMessageNotContaining("FK constraint")
                    .hasCauseInstanceOf(RuntimeException.class);
        }
    }
}
