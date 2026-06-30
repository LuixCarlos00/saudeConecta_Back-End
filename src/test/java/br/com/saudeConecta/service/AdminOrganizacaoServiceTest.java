package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.admin.AdminOrganizacao;
import br.com.saudeConecta.domain.endereco.Endereco;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.organizacao.StatusOrganizacao;
import br.com.saudeConecta.domain.organizacao.TipoOrganizacao;
import br.com.saudeConecta.domain.usuario.StatusUsuario;
import br.com.saudeConecta.domain.usuario.TipoUsuarioNovo;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.email.EmailNotificacaoService;
import br.com.saudeConecta.infra.tenant.TenantHelper;
import br.com.saudeConecta.infrastructure.persistence.repository.AdminOrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.EnderecoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.OrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import br.com.saudeConecta.presentation.dto.admin.AdminOrgCompletoResponse;
import br.com.saudeConecta.presentation.dto.admin.AtualizarAdminOrgCompletoRequest;
import br.com.saudeConecta.presentation.dto.admin.CadastrarAdminOrgCompletoRequest;
import br.com.saudeConecta.presentation.dto.admin.CadastrarAdminRequest;
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
@DisplayName("AdminOrganizacaoService - Testes unitários")
class AdminOrganizacaoServiceTest {

    @Mock private AdminOrganizacaoRepository adminOrganizacaoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private OrganizacaoRepository organizacaoRepository;
    @Mock private EnderecoRepository enderecoRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailNotificacaoService emailNotificacaoService;
    @Mock private TenantHelper tenantHelper;
    @Mock private EmailUnicoService emailUnicoService;
    @Mock private HistoricoDadosPessoaisService historicoDadosPessoaisService;
    @Mock private ConfiguracaoGraficoDashboardService configuracaoGraficoDashboardService;
    @Mock private LimitePlanoService limitePlanoService;

    @InjectMocks
    private AdminOrganizacaoService adminOrganizacaoService;

    private static final Long ORG_ID = 1L;
    private static final Long ADMIN_ID = 10L;
    private static final Long USUARIO_ID = 20L;

    private Organizacao organizacao;
    private Endereco endereco;
    private Usuario usuario;
    private AdminOrganizacao adminOrganizacao;

    @BeforeEach
    void setUp() {
        endereco = Endereco.builder()
                .endCep("12345-000").endUF("SP").endMunicipio("São Paulo")
                .endBairro("Centro").endRua("Rua A").endNumero(100L)
                .build();

        organizacao = Organizacao.builder()
                .nome("Clínica Teste").razaoSocial("Clínica Teste LTDA")
                .cnpj("12345678000100").tipo(TipoOrganizacao.CLINICA)
                .email("clinica@teste.com").telefone("11999999999")
                .endereco(endereco).status(StatusOrganizacao.ATIVO)
                .build();
        organizacao.setId(ORG_ID);

        usuario = Usuario.builder()
                .login("12345678900").senha("encodedPass")
                .tipoUsuario((byte) 1).tipoUsuarioNovo(TipoUsuarioNovo.ADMIN_ORG)
                .status(StatusUsuario.ATIVO).organizacao(organizacao)
                .build();
        usuario.setId(USUARIO_ID);

        adminOrganizacao = AdminOrganizacao.builder()
                .organizacao(organizacao).usuario(usuario)
                .nome("Admin Teste").email("admin@teste.com")
                .cargo("Administrador").isOwner(false)
                .status(AdminOrganizacao.StatusAdmin.ATIVO)
                .build();
        adminOrganizacao.setId(ADMIN_ID);
    }

    // ========== buscarrAdminByOrg ==========

    @Nested
    @DisplayName("buscarrAdminByOrg")
    class BuscarrAdminByOrg {

        @Test
        @DisplayName("Deve buscar admin pelo ID sem filtro de org quando SUPER_ADMIN (orgId null)")
        void deveBuscarSemFiltroOrgQuandoSuperAdmin() {
            when(tenantHelper.getCurrentTenantIdOrNull()).thenReturn(null);
            when(adminOrganizacaoRepository.findById(ADMIN_ID))
                    .thenReturn(Optional.of(adminOrganizacao));

            Optional<AdminOrganizacao> resultado = adminOrganizacaoService.buscarrAdminByOrg(ADMIN_ID);

            assertThat(resultado).isPresent();
            assertThat(resultado.get().getId()).isEqualTo(ADMIN_ID);
            verify(adminOrganizacaoRepository).findById(ADMIN_ID);
            verify(adminOrganizacaoRepository, never()).findByIdAndOrganizacao_Id(any(), any());
        }

        @Test
        @DisplayName("Deve buscar admin com filtro de org quando orgId presente")
        void deveBuscarComFiltroOrgQuandoTenantPresente() {
            when(tenantHelper.getCurrentTenantIdOrNull()).thenReturn(ORG_ID);
            when(adminOrganizacaoRepository.findByIdAndOrganizacao_Id(ADMIN_ID, ORG_ID))
                    .thenReturn(Optional.of(adminOrganizacao));

            Optional<AdminOrganizacao> resultado = adminOrganizacaoService.buscarrAdminByOrg(ADMIN_ID);

            assertThat(resultado).isPresent();
            verify(adminOrganizacaoRepository).findByIdAndOrganizacao_Id(ADMIN_ID, ORG_ID);
            verify(adminOrganizacaoRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Deve retornar vazio quando admin não pertence à organização")
        void deveRetornarVazioQuandoAdminNaoPertenceOrg() {
            when(tenantHelper.getCurrentTenantIdOrNull()).thenReturn(ORG_ID);
            when(adminOrganizacaoRepository.findByIdAndOrganizacao_Id(ADMIN_ID, ORG_ID))
                    .thenReturn(Optional.empty());

            Optional<AdminOrganizacao> resultado = adminOrganizacaoService.buscarrAdminByOrg(ADMIN_ID);

            assertThat(resultado).isEmpty();
        }
    }

    // ========== cadastrarAdminByOrg ==========

    @Nested
    @DisplayName("cadastrarAdminByOrg")
    class CadastrarAdminByOrg {

        private CadastrarAdminRequest request;

        @BeforeEach
        void setUp() {
            request = new CadastrarAdminRequest("Admin Novo", "123.456.789-00", "novo@teste.com");
        }

        @Test
        @DisplayName("Deve cadastrar admin com sucesso")
        void deveCadastrarAdminComSucesso() {
            doNothing().when(limitePlanoService).validarLimiteAdminOrg(ORG_ID);
            when(usuarioRepository.existsByLogin("123.456.789-00")).thenReturn(false);
            when(emailUnicoService.emailJaExiste("novo@teste.com")).thenReturn(false);
            when(organizacaoRepository.findById(ORG_ID)).thenReturn(Optional.of(organizacao));
            when(passwordEncoder.encode(any())).thenReturn("encodedPass");
            when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
            when(adminOrganizacaoRepository.save(any(AdminOrganizacao.class))).thenReturn(adminOrganizacao);

            AdminOrganizacao resultado = adminOrganizacaoService.cadastrarAdminByOrg(request, ORG_ID);

            assertThat(resultado).isNotNull();
            assertThat(resultado.getId()).isEqualTo(ADMIN_ID);
            verify(usuarioRepository).save(any(Usuario.class));
            verify(adminOrganizacaoRepository).save(any(AdminOrganizacao.class));
            verify(emailNotificacaoService).enviarCredenciaisAdministrador(
                    eq("novo@teste.com"), eq("Admin Novo"), any(),
                    any(), eq(ORG_ID), eq(ADMIN_ID));
        }

        @Test
        @DisplayName("Deve lançar exceção quando CPF já cadastrado como login")
        void deveLancarExcecaoQuandoCpfJaCadastrado() {
            doNothing().when(limitePlanoService).validarLimiteAdminOrg(ORG_ID);
            when(usuarioRepository.existsByLogin("123.456.789-00")).thenReturn(true);

            assertThatThrownBy(() -> adminOrganizacaoService.cadastrarAdminByOrg(request, ORG_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("CPF já cadastrado");
        }

        @Test
        @DisplayName("Deve lançar exceção quando email já existe")
        void deveLancarExcecaoQuandoEmailJaExiste() {
            doNothing().when(limitePlanoService).validarLimiteAdminOrg(ORG_ID);
            when(usuarioRepository.existsByLogin("123.456.789-00")).thenReturn(false);
            when(emailUnicoService.emailJaExiste("novo@teste.com")).thenReturn(true);
            when(emailUnicoService.ondeEmailFoiEncontrado("novo@teste.com")).thenReturn("ADMIN_ORG");

            assertThatThrownBy(() -> adminOrganizacaoService.cadastrarAdminByOrg(request, ORG_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Email já cadastrado");
        }

        @Test
        @DisplayName("Deve lançar exceção quando organização não encontrada")
        void deveLancarExcecaoQuandoOrganizacaoNaoEncontrada() {
            doNothing().when(limitePlanoService).validarLimiteAdminOrg(ORG_ID);
            when(usuarioRepository.existsByLogin(any())).thenReturn(false);
            when(emailUnicoService.emailJaExiste(any())).thenReturn(false);
            when(organizacaoRepository.findById(ORG_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminOrganizacaoService.cadastrarAdminByOrg(request, ORG_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Organização não encontrada");
        }
    }

    // ========== atualizarAdmByOrg ==========

    @Nested
    @DisplayName("atualizarAdmByOrg")
    class AtualizarAdmByOrg {

        @Test
        @DisplayName("Deve atualizar nome e email do admin com sucesso")
        void deveAtualizarAdminComSucesso() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(tenantHelper.getCurrentUserId()).thenReturn(USUARIO_ID);
            when(adminOrganizacaoRepository.findById(ADMIN_ID))
                    .thenReturn(Optional.of(adminOrganizacao));
            when(adminOrganizacaoRepository.save(any())).thenReturn(adminOrganizacao);

            AdminOrganizacao resultado = adminOrganizacaoService.atualizarAdmByOrg(ADMIN_ID, "Novo Nome", "novo@email.com");

            assertThat(resultado).isNotNull();
            assertThat(adminOrganizacao.getNome()).isEqualTo("Novo Nome");
            assertThat(adminOrganizacao.getEmail()).isEqualTo("novo@email.com");
            verify(adminOrganizacaoRepository).save(adminOrganizacao);
        }

        @Test
        @DisplayName("Deve lançar exceção quando admin não encontrado")
        void deveLancarExcecaoQuandoAdminNaoEncontrado() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(adminOrganizacaoRepository.findById(ADMIN_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminOrganizacaoService.atualizarAdmByOrg(ADMIN_ID, "Nome", "email@teste.com"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Administrador não encontrado");
        }

        @Test
        @DisplayName("Não deve alterar nome quando for nulo")
        void naoDeveAlterarNomeQuandoNulo() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(tenantHelper.getCurrentUserId()).thenReturn(USUARIO_ID);
            when(adminOrganizacaoRepository.findById(ADMIN_ID))
                    .thenReturn(Optional.of(adminOrganizacao));
            when(adminOrganizacaoRepository.save(any())).thenReturn(adminOrganizacao);

            adminOrganizacaoService.atualizarAdmByOrg(ADMIN_ID, null, null);

            assertThat(adminOrganizacao.getNome()).isEqualTo("Admin Teste");
            assertThat(adminOrganizacao.getEmail()).isEqualTo("admin@teste.com");
        }
    }

    // ========== deletarAdmByOrg ==========

    @Nested
    @DisplayName("deletarAdmByOrg")
    class DeletarAdmByOrg {

        @Test
        @DisplayName("Deve deletar admin com sucesso quando SUPER_ADMIN (orgId null)")
        void deveDeletarAdminSuperAdmin() {
            when(tenantHelper.getCurrentTenantIdOrNull()).thenReturn(null);
            when(adminOrganizacaoRepository.findById(ADMIN_ID))
                    .thenReturn(Optional.of(adminOrganizacao));

            adminOrganizacaoService.deletarAdmByOrg(ADMIN_ID);

            verify(adminOrganizacaoRepository).delete(adminOrganizacao);
            verify(usuarioRepository).delete(usuario);
        }

        @Test
        @DisplayName("Deve deletar admin com filtro de org quando orgId presente")
        void deveDeletarAdminComFiltroOrg() {
            when(tenantHelper.getCurrentTenantIdOrNull()).thenReturn(ORG_ID);
            when(adminOrganizacaoRepository.findByIdAndOrganizacao_Id(ADMIN_ID, ORG_ID))
                    .thenReturn(Optional.of(adminOrganizacao));

            adminOrganizacaoService.deletarAdmByOrg(ADMIN_ID);

            verify(adminOrganizacaoRepository).delete(adminOrganizacao);
            verify(usuarioRepository).delete(usuario);
        }

        @Test
        @DisplayName("Deve lançar exceção quando admin não encontrado")
        void deveLancarExcecaoQuandoAdminNaoEncontrado() {
            when(tenantHelper.getCurrentTenantIdOrNull()).thenReturn(null);
            when(adminOrganizacaoRepository.findById(ADMIN_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminOrganizacaoService.deletarAdmByOrg(ADMIN_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Administrador não encontrado");
        }

        @Test
        @DisplayName("Deve lançar exceção quando admin não possui usuario associado")
        void deveLancarExcecaoQuandoSemUsuarioAssociado() {
            AdminOrganizacao adminSemUsuario = AdminOrganizacao.builder()
                    .nome("Sem Usuário").usuario(null).status(AdminOrganizacao.StatusAdmin.ATIVO).build();
            adminSemUsuario.setId(ADMIN_ID);

            when(tenantHelper.getCurrentTenantIdOrNull()).thenReturn(null);
            when(adminOrganizacaoRepository.findById(ADMIN_ID)).thenReturn(Optional.of(adminSemUsuario));

            assertThatThrownBy(() -> adminOrganizacaoService.deletarAdmByOrg(ADMIN_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("usuario associado");
        }

        @Test
        @DisplayName("Deve propagar exceção de relacionamento ao deletar")
        void devePropararExcecaoDeRelacionamento() {
            when(tenantHelper.getCurrentTenantIdOrNull()).thenReturn(null);
            when(adminOrganizacaoRepository.findById(ADMIN_ID)).thenReturn(Optional.of(adminOrganizacao));
            doThrow(new RuntimeException("FK constraint violation"))
                    .when(adminOrganizacaoRepository).delete(adminOrganizacao);

            assertThatThrownBy(() -> adminOrganizacaoService.deletarAdmByOrg(ADMIN_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("relacionamentos existentes");
        }
    }

    // ========== buscarAdminOrgCompleto ==========

    @Nested
    @DisplayName("buscarAdminOrgCompleto")
    class BuscarAdminOrgCompleto {

        @Test
        @DisplayName("Deve retornar resposta completa do admin")
        void deveRetornarRespostaCompleta() {
            when(adminOrganizacaoRepository.findByIdWithOrgAndEndereco(ADMIN_ID))
                    .thenReturn(Optional.of(adminOrganizacao));

            AdminOrgCompletoResponse resultado = adminOrganizacaoService.buscarAdminOrgCompleto(ADMIN_ID);

            assertThat(resultado).isNotNull();
            assertThat(resultado.id()).isEqualTo(ADMIN_ID);
            assertThat(resultado.nome()).isEqualTo("Admin Teste");
            assertThat(resultado.nomeClinica()).isEqualTo("Clínica Teste");
        }

        @Test
        @DisplayName("Deve lançar exceção quando admin não encontrado")
        void deveLancarExcecaoQuandoAdminNaoEncontrado() {
            when(adminOrganizacaoRepository.findByIdWithOrgAndEndereco(ADMIN_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminOrganizacaoService.buscarAdminOrgCompleto(ADMIN_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Administrador não encontrado");
        }
    }

    // ========== atualizarAdminOrgCompleto ==========

    @Nested
    @DisplayName("atualizarAdminOrgCompleto")
    class AtualizarAdminOrgCompleto {

        private AtualizarAdminOrgCompletoRequest request;

        @BeforeEach
        void setUp() {
            request = new AtualizarAdminOrgCompletoRequest(
                    "Novo Nome", "Gerente", "novo@email.com",
                    "Nova Clínica", "Nova Razao Social", "12.345.678/0001-00",
                    "CLINICA", "nova@clinica.com", "11888888888",
                    "09876-000", "RJ", "Rio de Janeiro", "Copacabana",
                    "Rua B", 200L, "Apto 1"
            );
        }

        @Test
        @DisplayName("Deve atualizar admin completo com sucesso")
        void deveAtualizarAdminCompletoComSucesso() {
            when(adminOrganizacaoRepository.findByIdWithOrgAndEndereco(ADMIN_ID))
                    .thenReturn(Optional.of(adminOrganizacao));
            when(organizacaoRepository.save(any())).thenReturn(organizacao);
            when(enderecoRepository.save(any())).thenReturn(endereco);
            when(adminOrganizacaoRepository.save(any())).thenReturn(adminOrganizacao);

            AdminOrgCompletoResponse resultado = adminOrganizacaoService.atualizarAdminOrgCompleto(ADMIN_ID, request);

            assertThat(resultado).isNotNull();
            verify(organizacaoRepository).save(organizacao);
            verify(enderecoRepository).save(endereco);
            verify(adminOrganizacaoRepository).save(adminOrganizacao);
        }

        @Test
        @DisplayName("Deve lançar exceção quando admin não encontrado")
        void deveLancarExcecaoQuandoAdminNaoEncontrado() {
            when(adminOrganizacaoRepository.findByIdWithOrgAndEndereco(ADMIN_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminOrganizacaoService.atualizarAdminOrgCompleto(ADMIN_ID, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Administrador não encontrado");
        }

        @Test
        @DisplayName("Não deve atualizar campos quando request tiver campos nulos/em branco")
        void naoDeveAtualizarCamposNulos() {
            AtualizarAdminOrgCompletoRequest requestVazio = new AtualizarAdminOrgCompletoRequest(
                    null, null, null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null
            );
            when(adminOrganizacaoRepository.findByIdWithOrgAndEndereco(ADMIN_ID))
                    .thenReturn(Optional.of(adminOrganizacao));
            when(adminOrganizacaoRepository.save(any())).thenReturn(adminOrganizacao);

            adminOrganizacaoService.atualizarAdminOrgCompleto(ADMIN_ID, requestVazio);

            assertThat(adminOrganizacao.getNome()).isEqualTo("Admin Teste");
            assertThat(adminOrganizacao.getEmail()).isEqualTo("admin@teste.com");
        }
    }

    // ========== cadastrarAdminOrgCompleto ==========

    @Nested
    @DisplayName("cadastrarAdminOrgCompleto")
    class CadastrarAdminOrgCompleto {

        private CadastrarAdminOrgCompletoRequest request;

        @BeforeEach
        void setUp() {
            request = new CadastrarAdminOrgCompletoRequest(
                    "Admin Completo", "123.456.789-00", "Gerente", "admin@nova.com",
                    "Nova Clínica", "Nova Razao Social LTDA", "12.345.678/0001-00",
                    "CLINICA", "clinica@nova.com", "11999999999",
                    "01310-100", "SP", "São Paulo", "Bela Vista",
                    "Avenida Paulista", 1000L, null
            );
        }

        @Test
        @DisplayName("Deve cadastrar admin org completo com sucesso")
        void deveCadastrarAdminOrgCompletoComSucesso() {
            when(usuarioRepository.existsByLogin("12345678000100")).thenReturn(false);
            when(organizacaoRepository.existsByCnpj("12345678000100")).thenReturn(false);
            when(enderecoRepository.save(any(Endereco.class))).thenReturn(endereco);
            when(organizacaoRepository.save(any(Organizacao.class))).thenReturn(organizacao);
            when(passwordEncoder.encode(any())).thenReturn("encodedPass");
            when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
            when(adminOrganizacaoRepository.save(any(AdminOrganizacao.class))).thenReturn(adminOrganizacao);

            AdminOrganizacao resultado = adminOrganizacaoService.cadastrarAdminOrgCompleto(request);

            assertThat(resultado).isNotNull();
            verify(enderecoRepository).save(any(Endereco.class));
            verify(organizacaoRepository).save(any(Organizacao.class));
            verify(usuarioRepository).save(any(Usuario.class));
            verify(adminOrganizacaoRepository).save(any(AdminOrganizacao.class));
            verify(configuracaoGraficoDashboardService).inicializarParaNovoUsuario(any());
            verify(emailNotificacaoService).enviarCredenciaisAdministrador(
                    eq("clinica@nova.com"), eq("Admin Completo"), eq("12345678000100"),
                    any(), any(), any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando CNPJ já cadastrado como login")
        void deveLancarExcecaoQuandoCnpjJaCadastradoComoLogin() {
            when(usuarioRepository.existsByLogin("12345678000100")).thenReturn(true);

            assertThatThrownBy(() -> adminOrganizacaoService.cadastrarAdminOrgCompleto(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("CNPJ já cadastrado como login");
        }

        @Test
        @DisplayName("Deve lançar exceção quando CNPJ já cadastrado na organização")
        void deveLancarExcecaoQuandoCnpjJaCadastradoNaOrg() {
            when(usuarioRepository.existsByLogin("12345678000100")).thenReturn(false);
            when(organizacaoRepository.existsByCnpj("12345678000100")).thenReturn(true);

            assertThatThrownBy(() -> adminOrganizacaoService.cadastrarAdminOrgCompleto(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("CNPJ já cadastrado no sistema");
        }
    }

    // ========== atualizarMeusDadosAdminOrg ==========

    @Nested
    @DisplayName("atualizarMeusDadosAdminOrg")
    class AtualizarMeusDadosAdminOrg {

        private AtualizarAdminOrgCompletoRequest request;

        @BeforeEach
        void setUp() {
            request = new AtualizarAdminOrgCompletoRequest(
                    "Admin Atualizado", "Gerente", "atualizado@email.com",
                    "Clínica Atualizada", "Razao Atualizada", null,
                    "CLINICA", "clinica@atualizada.com", "11777777777",
                    "01310-100", "SP", "São Paulo", "Bela Vista",
                    "Avenida Paulista", 1000L, null
            );
        }

        @Test
        @DisplayName("Deve atualizar dados do admin logado com sucesso")
        void deveAtualizarMeusDadosComSucesso() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(tenantHelper.getCurrentUserId()).thenReturn(USUARIO_ID);
            when(adminOrganizacaoRepository.findByIdWithOrgAndEndereco(ADMIN_ID))
                    .thenReturn(Optional.of(adminOrganizacao));
            when(adminOrganizacaoRepository.save(any())).thenReturn(adminOrganizacao);
            when(organizacaoRepository.save(any())).thenReturn(organizacao);
            when(enderecoRepository.save(any())).thenReturn(endereco);

            adminOrganizacaoService.atualizarMeusDadosAdminOrg(ADMIN_ID, request);

            verify(adminOrganizacaoRepository).save(adminOrganizacao);
            verify(organizacaoRepository).save(organizacao);
            verify(enderecoRepository).save(endereco);
        }

        @Test
        @DisplayName("Deve lançar exceção quando admin não encontrado")
        void deveLancarExcecaoQuandoAdminNaoEncontrado() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(tenantHelper.getCurrentUserId()).thenReturn(USUARIO_ID);
            when(adminOrganizacaoRepository.findByIdWithOrgAndEndereco(ADMIN_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminOrganizacaoService.atualizarMeusDadosAdminOrg(ADMIN_ID, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Administrador não encontrado");
        }

        @Test
        @DisplayName("Deve lançar exceção quando admin pertence a outra organização")
        void deveLancarExcecaoQuandoAdminDeOutraOrg() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(99L);
            when(tenantHelper.getCurrentUserId()).thenReturn(USUARIO_ID);
            when(adminOrganizacaoRepository.findByIdWithOrgAndEndereco(ADMIN_ID))
                    .thenReturn(Optional.of(adminOrganizacao));

            assertThatThrownBy(() -> adminOrganizacaoService.atualizarMeusDadosAdminOrg(ADMIN_ID, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Acesso negado");
        }
    }
}
