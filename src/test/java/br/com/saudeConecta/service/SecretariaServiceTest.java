package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.secretaria.Secretaria;
import br.com.saudeConecta.domain.secretaria.StatusSecretaria;
import br.com.saudeConecta.domain.usuario.StatusUsuario;
import br.com.saudeConecta.domain.usuario.TipoUsuarioNovo;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.email.EmailNotificacaoService;
import br.com.saudeConecta.infra.tenant.TenantHelper;
import br.com.saudeConecta.infrastructure.persistence.repository.OrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.SecretariaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import br.com.saudeConecta.presentation.dto.secretaria.CadastrarSecretariaRequest;
import br.com.saudeConecta.util.EmailUnicoService;
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
                .tipoUsuario((byte) 2).tipoUsuarioNovo(TipoUsuarioNovo.RECEPCIONISTA)
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
        @DisplayName("Deve atualizar nome, email e telefone com sucesso")
        void deveAtualizarDadosComSucesso() {
            Secretaria dadosAtualizados = Secretaria.builder()
                    .nome("Novo Nome").email("novo@email.com").telefone("11888888888").build();

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
        @DisplayName("Não deve alterar campos quando forem nulos")
        void naoDeveAlterarCamposNulos() {
            Secretaria dadosVazios = Secretaria.builder().build();

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

            assertThatThrownBy(() -> secretariaService.atualizarSecretariaIdByOrg(999L, new Secretaria()))
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

            assertThatThrownBy(() -> secretariaService.deletarSecretariaIdByOrg(SEC_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("relacionamentos existentes");
        }
    }
}
