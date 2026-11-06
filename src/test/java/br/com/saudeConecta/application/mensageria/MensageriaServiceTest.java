package br.com.saudeConecta.application.mensageria;

import br.com.saudeConecta.application.mensageria.dto.MensageriaResponse;
import br.com.saudeConecta.application.notificacao.EmailNotificacaoService;
import br.com.saudeConecta.config.tenant.TenantHelper;
import br.com.saudeConecta.config.tenant.TenantNotDefinedException;
import br.com.saudeConecta.domain.mensageria.Mensageria;
import br.com.saudeConecta.domain.mensageria.StatusMensagem;
import br.com.saudeConecta.domain.mensageria.TipoMensagem;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.infrastructure.persistence.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MensageriaService - Testes unitários")
class MensageriaServiceTest {

    @Mock private MensageriaRepository mensageriaRepository;
    @Mock private OrganizacaoRepository organizacaoRepository;
    @Mock private ProfissionalRepository profissionalRepository;
    @Mock private SecretariaRepository secretariaRepository;
    @Mock private AdminOrganizacaoRepository adminOrganizacaoRepository;
    @Mock private PacienteRepository pacienteRepository;
    @Mock private TenantHelper tenantHelper;
    @Mock private EmailNotificacaoService emailNotificacaoService;

    @InjectMocks
    private MensageriaService mensageriaService;

    private static final Long ORG_ID = 1L;
    private static final Long MSG_ID = 100L;
    private static final Long ENTIDADE_ID = 50L;

    private Mensageria mensageria;

    @BeforeEach
    void setUp() {
        Organizacao org = Organizacao.builder().nome("Clínica Teste").build();
        org.setId(ORG_ID);

        mensageria = Mensageria.builder()
                .id(MSG_ID)
                .organizacao(org)
                .destinatarioEmail("dest@teste.com")
                .assunto("Credenciais de acesso")
                .corpoMensagem("<html>corpo</html>")
                .status(StatusMensagem.FALHOU)
                .adminNotificado(false)
                .destinatarioEntidadeId(ENTIDADE_ID)
                .tipoMensagem(TipoMensagem.EMAIL_CREDENCIAIS_CLINICO)
                .build();
    }

    // ========== listarMensagens ==========

    @Nested
    @DisplayName("listarMensagens")
    class ListarMensagens {

        @Test
        @DisplayName("Admin Org deve listar mensagens apenas da sua organização")
        void adminOrgDeveListarMensagensDaSuaOrg() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Mensageria> page = new PageImpl<>(List.of(mensageria));

            when(tenantHelper.getCurrentTenantIdOrNullIfSuperAdmin()).thenReturn(ORG_ID);
            when(mensageriaRepository.findByOrganizacaoIdWithFilters(ORG_ID, null, null, pageable))
                    .thenReturn(page);

            Page<MensageriaResponse> resultado = mensageriaService.listarMensagens(null, null, pageable);

            assertThat(resultado.getContent()).hasSize(1);
            verify(mensageriaRepository).findByOrganizacaoIdWithFilters(eq(ORG_ID), isNull(), isNull(), eq(pageable));
            verify(mensageriaRepository, never()).findAllWithFilters(any(), any(), any());
        }

        @Test
        @DisplayName("Super Admin deve listar mensagens globais")
        void superAdminDeveListarMensagensGlobais() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Mensageria> page = new PageImpl<>(List.of(mensageria));

            when(tenantHelper.getCurrentTenantIdOrNullIfSuperAdmin()).thenReturn(null);
            when(mensageriaRepository.findAllWithFilters(null, null, pageable)).thenReturn(page);

            Page<MensageriaResponse> resultado = mensageriaService.listarMensagens(null, null, pageable);

            assertThat(resultado.getContent()).hasSize(1);
            verify(mensageriaRepository).findAllWithFilters(isNull(), isNull(), eq(pageable));
        }

        @Test
        @DisplayName("Deve filtrar por status quando informado")
        void deveFiltrarPorStatus() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Mensageria> page = new PageImpl<>(List.of(mensageria));

            when(tenantHelper.getCurrentTenantIdOrNullIfSuperAdmin()).thenReturn(ORG_ID);
            when(mensageriaRepository.findByOrganizacaoIdWithFilters(ORG_ID, StatusMensagem.FALHOU, null, pageable))
                    .thenReturn(page);

            Page<MensageriaResponse> resultado = mensageriaService.listarMensagens(StatusMensagem.FALHOU, null, pageable);

            assertThat(resultado.getContent()).hasSize(1);
        }
    }

    // ========== marcarComoNotificado ==========

    @Nested
    @DisplayName("marcarComoNotificado")
    class MarcarComoNotificado {

        @Test
        @DisplayName("Deve marcar mensagem como notificada com sucesso")
        void deveMarcarComoNotificadoComSucesso() {
            when(tenantHelper.getCurrentTenantIdOrNullIfSuperAdmin()).thenReturn(ORG_ID);
            when(mensageriaRepository.findById(MSG_ID)).thenReturn(Optional.of(mensageria));

            mensageriaService.marcarComoNotificado(MSG_ID);

            assertThat(mensageria.getAdminNotificado()).isTrue();
            assertThat(mensageria.getStatus()).isEqualTo(StatusMensagem.RENOTIFICADO);
            verify(mensageriaRepository).save(mensageria);
        }

        @Test
        @DisplayName("Deve lançar exceção quando mensagem não existir")
        void deveLancarExcecaoQuandoMensagemNaoExistir() {
            when(tenantHelper.getCurrentTenantIdOrNullIfSuperAdmin()).thenReturn(ORG_ID);
            when(mensageriaRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> mensageriaService.marcarComoNotificado(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Mensagem não encontrada");
        }

        @Test
        @DisplayName("Deve lançar exceção de acesso negado quando mensagem pertencer a outra organização")
        void deveLancarExcecaoDeAcessoNegado() {
            Organizacao outraOrg = Organizacao.builder().nome("Outra Org").build();
            outraOrg.setId(99L);
            mensageria.setOrganizacao(outraOrg);

            when(tenantHelper.getCurrentTenantIdOrNullIfSuperAdmin()).thenReturn(ORG_ID);
            when(mensageriaRepository.findById(MSG_ID)).thenReturn(Optional.of(mensageria));

            assertThatThrownBy(() -> mensageriaService.marcarComoNotificado(MSG_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Acesso negado");
        }

        @Test
        @DisplayName("Deve negar acesso (sem NPE) quando mensagem não tem organização e usuário tem tenant")
        void deveNegarMensagemSemOrganizacao() {
            mensageria.setOrganizacao(null);
            when(tenantHelper.getCurrentTenantIdOrNullIfSuperAdmin()).thenReturn(ORG_ID);
            when(mensageriaRepository.findById(MSG_ID)).thenReturn(Optional.of(mensageria));

            assertThatThrownBy(() -> mensageriaService.marcarComoNotificado(MSG_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Acesso negado");
            verify(mensageriaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Super Admin deve marcar mensagem de qualquer organização")
        void superAdminDeveMarcarQualquerMensagem() {
            when(tenantHelper.getCurrentTenantIdOrNullIfSuperAdmin()).thenReturn(null);
            when(mensageriaRepository.findById(MSG_ID)).thenReturn(Optional.of(mensageria));

            mensageriaService.marcarComoNotificado(MSG_ID);

            assertThat(mensageria.getAdminNotificado()).isTrue();
        }
    }

    // ========== contarFalhasPendentes ==========

    @Nested
    @DisplayName("contarFalhasPendentes")
    class ContarFalhasPendentes {

        @Test
        @DisplayName("Deve retornar contagem de falhas da organização")
        void deveRetornarContagemDaOrg() {
            when(tenantHelper.getCurrentTenantIdOrNullIfSuperAdmin()).thenReturn(ORG_ID);
            when(mensageriaRepository.countByOrganizacao_IdAndAdminNotificadoFalseAndStatus(ORG_ID, StatusMensagem.FALHOU))
                    .thenReturn(3L);

            long resultado = mensageriaService.contarFalhasPendentes();

            assertThat(resultado).isEqualTo(3L);
        }

        @Test
        @DisplayName("Super Admin deve retornar contagem global")
        void superAdminDeveRetornarContagemGlobal() {
            when(tenantHelper.getCurrentTenantIdOrNullIfSuperAdmin()).thenReturn(null);
            when(mensageriaRepository.countByAdminNotificadoFalseAndStatus(StatusMensagem.FALHOU)).thenReturn(10L);

            long resultado = mensageriaService.contarFalhasPendentes();

            assertThat(resultado).isEqualTo(10L);
        }
    }

    // ========== reenviarMensagem ==========

    @Nested
    @DisplayName("reenviarMensagem")
    class ReenviarMensagem {

        @Test
        @DisplayName("Deve reenviar mensagem de credenciais de clínico com sucesso")
        void deveReenviarMensagemClinicoComSucesso() {
            Profissional profissional = Profissional.builder().email("prof@teste.com").build();

            when(tenantHelper.getCurrentTenantIdOrNullIfSuperAdmin()).thenReturn(ORG_ID);
            when(mensageriaRepository.findById(MSG_ID)).thenReturn(Optional.of(mensageria));
            when(profissionalRepository.findById(ENTIDADE_ID)).thenReturn(Optional.of(profissional));
            doNothing().when(emailNotificacaoService).reenviarEmail(any(), any(), any(), any());

            mensageriaService.reenviarMensagem(MSG_ID);

            assertThat(mensageria.getStatus()).isEqualTo(StatusMensagem.PENDENTE);
            assertThat(mensageria.getErroDetalhe()).isNull();
            verify(mensageriaRepository).save(mensageria);
            verify(emailNotificacaoService).reenviarEmail(
                    MSG_ID, "prof@teste.com",
                    mensageria.getAssunto(), mensageria.getCorpoMensagem());
        }

        @Test
        @DisplayName("Deve lançar exceção quando profissional não encontrado ao reenviar")
        void deveLancarExcecaoQuandoProfissionalNaoEncontrado() {
            when(tenantHelper.getCurrentTenantIdOrNullIfSuperAdmin()).thenReturn(ORG_ID);
            when(mensageriaRepository.findById(MSG_ID)).thenReturn(Optional.of(mensageria));
            when(profissionalRepository.findById(ENTIDADE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> mensageriaService.reenviarMensagem(MSG_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Profissional não encontrado");
        }

        @Test
        @DisplayName("Deve negar reenvio (sem NPE) quando mensagem não tem organização e usuário tem tenant")
        void deveNegarReenvioMensagemSemOrganizacao() {
            mensageria.setOrganizacao(null);
            when(tenantHelper.getCurrentTenantIdOrNullIfSuperAdmin()).thenReturn(ORG_ID);
            when(mensageriaRepository.findById(MSG_ID)).thenReturn(Optional.of(mensageria));

            assertThatThrownBy(() -> mensageriaService.reenviarMensagem(MSG_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Acesso negado");
            verify(emailNotificacaoService, never()).reenviarEmail(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando mensagem não existir")
        void deveLancarExcecaoQuandoMensagemNaoExistir() {
            when(tenantHelper.getCurrentTenantIdOrNullIfSuperAdmin()).thenReturn(ORG_ID);
            when(mensageriaRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> mensageriaService.reenviarMensagem(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Mensagem não encontrada");
        }
    }

    // ========== Falha fechada sem tenant ==========

    @Nested
    @DisplayName("Sem tenant e sem papel SUPER_ADMIN")
    class SemTenantNaoSuperAdmin {

        @BeforeEach
        void semTenant() {
            when(tenantHelper.getCurrentTenantIdOrNullIfSuperAdmin())
                    .thenThrow(new TenantNotDefinedException("Tenant não definido"));
        }

        @Test
        @DisplayName("listarMensagens nega e não consulta a visão global")
        void listarNega() {
            var pagina = PageRequest.of(0, 10);
            assertThatThrownBy(() -> mensageriaService.listarMensagens(null, null, pagina))
                    .isInstanceOf(TenantNotDefinedException.class);
            verify(mensageriaRepository, never()).findAllWithFilters(any(), any(), any());
        }

        @Test
        @DisplayName("contarFalhasPendentes nega e não conta globalmente")
        void contarNega() {
            assertThatThrownBy(() -> mensageriaService.contarFalhasPendentes())
                    .isInstanceOf(TenantNotDefinedException.class);
            verify(mensageriaRepository, never()).countByAdminNotificadoFalseAndStatus(any());
        }

    }
}
