package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.planos.AssinaturaTenant;
import br.com.saudeConecta.domain.planos.PlanoAssinatura;
import br.com.saudeConecta.domain.planos.StatusAssinatura;
import br.com.saudeConecta.domain.planos.TipoPlano;
import br.com.saudeConecta.infra.exceptions.BusinessException;
import br.com.saudeConecta.infrastructure.persistence.repository.AssinaturaTenantRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.OrganizacaoRepository;
import br.com.saudeConecta.presentation.dto.planos.AssinaturaTenantResponse;
import br.com.saudeConecta.presentation.dto.planos.CustomizarPlanoTenantRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssinaturaTenantService - Testes unitários")
class AssinaturaTenantServiceTest {

    @Mock private AssinaturaTenantRepository assinaturaTenantRepository;
    @Mock private OrganizacaoRepository organizacaoRepository;
    @Mock private PlanoAssinaturaService planoAssinaturaService;

    @InjectMocks
    private AssinaturaTenantService service;

    // ========== Helpers ==========

    private Organizacao criarOrganizacao(Long id) {
        Organizacao org = new Organizacao();
        org.setId(id);
        org.setNome("Clínica Teste");
        return org;
    }

    private PlanoAssinatura criarPlano(Long id) {
        PlanoAssinatura plano = new PlanoAssinatura();
        plano.setId(id);
        plano.setNome("Plano Basic");
        plano.setTipo(TipoPlano.STARTER);
        plano.setValorMensal(BigDecimal.valueOf(99.90));
        plano.setLimiteAdminOrg(1);
        plano.setLimiteProfissional(3);
        plano.setLimiteSecretaria(2);
        return plano;
    }

    private AssinaturaTenant criarAssinatura(Long id, StatusAssinatura status) {
        AssinaturaTenant assinatura = new AssinaturaTenant();
        assinatura.setId(id);
        assinatura.setStatus(status);
        assinatura.setOrganizacao(criarOrganizacao(1L));
        assinatura.setPlanoAssinatura(criarPlano(10L));
        assinatura.setDataInicio(LocalDate.now());
        assinatura.setDataVencimento(LocalDate.now().plusDays(30));
        assinatura.setDataProximaCobranca(LocalDate.now().plusDays(30));
        assinatura.setValorMensal(BigDecimal.valueOf(99.90));
        assinatura.setQtdAdminExtra(0);
        assinatura.setQtdProfissionalExtra(0);
        assinatura.setQtdSecretariaExtra(0);
        assinatura.setValorAdicionalPerfis(BigDecimal.ZERO);
        return assinatura;
    }

    // ========== assinar ==========

    @Nested
    @DisplayName("assinar")
    class Assinar {

        @Test
        @DisplayName("Deve criar assinatura TRIAL quando organização não tem assinatura ativa")
        void deveCriarTrialQuandoSemAssinaturaAtiva() {
            when(assinaturaTenantRepository.findAssinaturaAtivaByOrganizacaoId(1L))
                    .thenReturn(Optional.empty());
            when(organizacaoRepository.findById(1L))
                    .thenReturn(Optional.of(criarOrganizacao(1L)));
            when(planoAssinaturaService.buscarEntidadePorId(10L))
                    .thenReturn(criarPlano(10L));

            AssinaturaTenant salva = criarAssinatura(1L, StatusAssinatura.TRIAL);
            when(assinaturaTenantRepository.save(any())).thenReturn(salva);

            AssinaturaTenantResponse response = service.assinar(1L, 10L);

            assertThat(response).isNotNull();
            verify(assinaturaTenantRepository).save(any(AssinaturaTenant.class));
        }

        @Test
        @DisplayName("Deve trocar plano quando organização já tem assinatura ativa")
        void deveTrocarPlanoQuandoAssinaturaExistente() {
            AssinaturaTenant existente = criarAssinatura(1L, StatusAssinatura.ATIVA);
            when(assinaturaTenantRepository.findAssinaturaAtivaByOrganizacaoId(1L))
                    .thenReturn(Optional.of(existente));
            when(planoAssinaturaService.buscarEntidadePorId(20L))
                    .thenReturn(criarPlano(20L));
            when(assinaturaTenantRepository.save(any())).thenReturn(existente);

            service.assinar(1L, 20L);

            verify(planoAssinaturaService).buscarEntidadePorId(20L);
            verify(assinaturaTenantRepository, times(1)).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando organização não encontrada")
        void deveLancarExcecaoQuandoOrganizacaoNaoEncontrada() {
            when(assinaturaTenantRepository.findAssinaturaAtivaByOrganizacaoId(99L))
                    .thenReturn(Optional.empty());
            when(organizacaoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.assinar(99L, 10L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ========== ativar ==========

    @Nested
    @DisplayName("ativar")
    class Ativar {

        @Test
        @DisplayName("Deve ativar assinatura TRIAL para ATIVA")
        void deveAtivarAssinatura() {
            AssinaturaTenant assinatura = criarAssinatura(1L, StatusAssinatura.TRIAL);
            when(assinaturaTenantRepository.findAssinaturaAtivaByOrganizacaoId(1L))
                    .thenReturn(Optional.of(assinatura));
            when(assinaturaTenantRepository.save(any())).thenReturn(assinatura);

            AssinaturaTenantResponse response = service.ativar(1L);

            assertThat(response).isNotNull();
            assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.ATIVA);
        }

        @Test
        @DisplayName("Deve lançar exceção quando não encontrar assinatura para ativar")
        void deveLancarExcecaoQuandoSemAssinatura() {
            when(assinaturaTenantRepository.findAssinaturaAtivaByOrganizacaoId(99L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.ativar(99L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ========== marcarInadimplente ==========

    @Nested
    @DisplayName("marcarInadimplente / suspender / cancelar")
    class AlterarStatus {

        @Test
        @DisplayName("Deve marcar assinatura como INADIMPLENTE")
        void deveMarcarInadimplente() {
            AssinaturaTenant assinatura = criarAssinatura(1L, StatusAssinatura.ATIVA);
            when(assinaturaTenantRepository.findById(1L)).thenReturn(Optional.of(assinatura));
            when(assinaturaTenantRepository.save(any())).thenReturn(assinatura);

            service.marcarInadimplente(1L);

            assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.INADIMPLENTE);
        }

        @Test
        @DisplayName("Deve suspender assinatura")
        void deveSuspender() {
            AssinaturaTenant assinatura = criarAssinatura(1L, StatusAssinatura.INADIMPLENTE);
            when(assinaturaTenantRepository.findById(1L)).thenReturn(Optional.of(assinatura));
            when(assinaturaTenantRepository.save(any())).thenReturn(assinatura);

            service.suspender(1L);

            assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.SUSPENSA);
        }

        @Test
        @DisplayName("Deve cancelar assinatura")
        void deveCancelar() {
            AssinaturaTenant assinatura = criarAssinatura(1L, StatusAssinatura.ATIVA);
            when(assinaturaTenantRepository.findById(1L)).thenReturn(Optional.of(assinatura));
            when(assinaturaTenantRepository.save(any())).thenReturn(assinatura);

            service.cancelar(1L);

            assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.CANCELADA);
        }

        @Test
        @DisplayName("Deve lançar exceção ao marcar inadimplente quando não encontrada")
        void deveLancarExcecaoInadimplente() {
            when(assinaturaTenantRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.marcarInadimplente(99L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ========== buscarAssinaturaAtiva ==========

    @Nested
    @DisplayName("buscarAssinaturaAtiva")
    class BuscarAssinaturaAtiva {

        @Test
        @DisplayName("Deve retornar assinatura ativa da organização")
        void deveRetornarAssinaturaAtiva() {
            AssinaturaTenant assinatura = criarAssinatura(1L, StatusAssinatura.ATIVA);
            when(assinaturaTenantRepository.findAssinaturaAtivaByOrganizacaoId(1L))
                    .thenReturn(Optional.of(assinatura));

            AssinaturaTenantResponse response = service.buscarAssinaturaAtiva(1L);

            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("Deve lançar exceção quando não há assinatura ativa")
        void deveLancarExcecaoQuandoSemAssinaturaAtiva() {
            when(assinaturaTenantRepository.findAssinaturaAtivaByOrganizacaoId(99L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.buscarAssinaturaAtiva(99L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ========== listarTodas / listarPorOrganizacao ==========

    @Nested
    @DisplayName("listar")
    class Listar {

        @Test
        @DisplayName("Deve listar todas as assinaturas")
        void deveListarTodas() {
            AssinaturaTenant a1 = criarAssinatura(1L, StatusAssinatura.ATIVA);
            AssinaturaTenant a2 = criarAssinatura(2L, StatusAssinatura.TRIAL);
            when(assinaturaTenantRepository.findAllWithRelations()).thenReturn(List.of(a1, a2));

            List<AssinaturaTenantResponse> resultado = service.listarTodas();

            assertThat(resultado).hasSize(2);
        }

        @Test
        @DisplayName("Deve listar assinaturas por organização")
        void deveListarPorOrganizacao() {
            AssinaturaTenant a = criarAssinatura(1L, StatusAssinatura.ATIVA);
            when(assinaturaTenantRepository.findByOrganizacaoId(1L)).thenReturn(List.of(a));

            List<AssinaturaTenantResponse> resultado = service.listarPorOrganizacao(1L);

            assertThat(resultado).hasSize(1);
        }
    }

    // ========== renovarAposPagamento ==========

    @Nested
    @DisplayName("renovarAposPagamento")
    class RenovarAposPagamento {

        @Test
        @DisplayName("Deve renovar assinatura e manter status ATIVA")
        void deveRenovar() {
            AssinaturaTenant assinatura = criarAssinatura(1L, StatusAssinatura.INADIMPLENTE);
            when(assinaturaTenantRepository.findById(1L)).thenReturn(Optional.of(assinatura));

            service.renovarAposPagamento(1L);

            assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.ATIVA);
            verify(assinaturaTenantRepository).save(assinatura);
        }
    }

    // ========== trocarPlano ==========

    @Nested
    @DisplayName("trocarPlano")
    class TrocarPlano {

        @Test
        @DisplayName("Deve trocar plano da assinatura ativa")
        void deveTrocarPlano() {
            AssinaturaTenant assinatura = criarAssinatura(1L, StatusAssinatura.ATIVA);
            PlanoAssinatura novoPlano = criarPlano(20L);
            novoPlano.setValorMensal(BigDecimal.valueOf(199.90));

            when(assinaturaTenantRepository.findAssinaturaAtivaByOrganizacaoId(1L))
                    .thenReturn(Optional.of(assinatura));
            when(planoAssinaturaService.buscarEntidadePorId(20L)).thenReturn(novoPlano);
            when(assinaturaTenantRepository.save(any())).thenReturn(assinatura);

            AssinaturaTenantResponse response = service.trocarPlano(1L, 20L);

            assertThat(response).isNotNull();
            assertThat(assinatura.getPlanoAssinatura()).isEqualTo(novoPlano);
        }

        @Test
        @DisplayName("Deve lançar exceção quando não há assinatura ativa para trocar plano")
        void deveLancarExcecaoQuandoSemAssinatura() {
            when(assinaturaTenantRepository.findAssinaturaAtivaByOrganizacaoId(99L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.trocarPlano(99L, 10L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ========== customizarPlano ==========

    @Nested
    @DisplayName("customizarPlano")
    class CustomizarPlano {

        @Test
        @DisplayName("Deve customizar plano calculando extras corretamente")
        void deveCustomizarPlano() {
            AssinaturaTenant assinatura = criarAssinatura(1L, StatusAssinatura.ATIVA);
            PlanoAssinatura plano = criarPlano(10L);
            plano.setValorAdicionalAdmin(BigDecimal.valueOf(50));
            plano.setValorAdicionalProfissional(BigDecimal.valueOf(30));
            plano.setValorAdicionalSecretaria(BigDecimal.valueOf(20));
            assinatura.setPlanoAssinatura(plano);

            when(assinaturaTenantRepository.findAssinaturaAtivaByOrganizacaoId(1L))
                    .thenReturn(Optional.of(assinatura));
            when(assinaturaTenantRepository.save(any())).thenReturn(assinatura);

            CustomizarPlanoTenantRequest request = new CustomizarPlanoTenantRequest(3, 5, 4);

            AssinaturaTenantResponse response = service.customizarPlano(1L, request);

            assertThat(response).isNotNull();
            assertThat(assinatura.getQtdAdminExtra()).isEqualTo(2); // 3 - 1 = 2
            assertThat(assinatura.getQtdProfissionalExtra()).isEqualTo(2); // 5 - 3 = 2
            assertThat(assinatura.getQtdSecretariaExtra()).isEqualTo(2); // 4 - 2 = 2
        }

        @Test
        @DisplayName("Deve retornar zero extras quando limite custom menor que do plano")
        void deveRetornarZeroExtrasQuandoLimiteMenor() {
            AssinaturaTenant assinatura = criarAssinatura(1L, StatusAssinatura.ATIVA);

            when(assinaturaTenantRepository.findAssinaturaAtivaByOrganizacaoId(1L))
                    .thenReturn(Optional.of(assinatura));
            when(assinaturaTenantRepository.save(any())).thenReturn(assinatura);

            CustomizarPlanoTenantRequest request = new CustomizarPlanoTenantRequest(0, 1, 1);

            service.customizarPlano(1L, request);

            assertThat(assinatura.getQtdAdminExtra()).isEqualTo(0);
        }
    }
}
