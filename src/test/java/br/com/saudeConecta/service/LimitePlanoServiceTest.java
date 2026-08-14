package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.planos.AssinaturaTenant;
import br.com.saudeConecta.domain.planos.PlanoAssinatura;
import br.com.saudeConecta.domain.planos.StatusAssinatura;
import br.com.saudeConecta.domain.planos.TipoPlano;
import br.com.saudeConecta.infra.exceptions.LimitePlanoException;
import br.com.saudeConecta.infrastructure.persistence.repository.AdminOrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.AssinaturaTenantRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.SecretariaRepository;
import br.com.saudeConecta.presentation.dto.planos.LimitesPlanoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LimitePlanoService - Testes de validação de limites do plano")
class LimitePlanoServiceTest {

    @Mock
    private AssinaturaTenantRepository assinaturaTenantRepository;

    @Mock
    private AdminOrganizacaoRepository adminOrganizacaoRepository;

    @Mock
    private ProfissionalRepository profissionalRepository;

    @Mock
    private SecretariaRepository secretariaRepository;

    @InjectMocks
    private LimitePlanoService limitePlanoService;

    private PlanoAssinatura planoStarter;
    private AssinaturaTenant assinaturaAtiva;

    @BeforeEach
    void setUp() {
        planoStarter = PlanoAssinatura.builder()
                .id(1L)
                .nome("Starter")
                .tipo(TipoPlano.STARTER)
                .valorMensal(new BigDecimal("199.00"))
                .limiteAdminOrg(1)
                .limiteProfissional(3)
                .limiteSecretaria(2)
                .ativo(true)
                .build();

        assinaturaAtiva = AssinaturaTenant.builder()
                .id(1L)
                .planoAssinatura(planoStarter)
                .status(StatusAssinatura.ATIVA)
                .dataInicio(LocalDate.now().minusMonths(1))
                .dataVencimento(LocalDate.now().plusMonths(1))
                .valorMensal(new BigDecimal("199.00"))
                .build();
    }

    // ========== validarLimiteAdminOrg ==========

    @Nested
    @DisplayName("validarLimiteAdminOrg")
    class ValidarLimiteAdminOrg {

        @Test
        @DisplayName("Deve permitir quando há slots disponíveis")
        void devePermitirQuandoHaSlotsDisponiveis() {
            when(assinaturaTenantRepository.findAssinaturaAtivaByOrganizacaoId(1L))
                    .thenReturn(Optional.of(assinaturaAtiva));
            when(adminOrganizacaoRepository.countAtivosByOrganizacaoId(1L)).thenReturn(0L);

            assertThatCode(() -> limitePlanoService.validarLimiteAdminOrg(1L))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Deve lançar LimitePlanoException quando limite de AdminOrg for atingido")
        void deveLancarExcecaoQuandoLimiteAdminOrgAtingido() {
            when(assinaturaTenantRepository.findAssinaturaAtivaByOrganizacaoId(1L))
                    .thenReturn(Optional.of(assinaturaAtiva));
            when(adminOrganizacaoRepository.countAtivosByOrganizacaoId(1L)).thenReturn(1L);

            assertThatThrownBy(() -> limitePlanoService.validarLimiteAdminOrg(1L))
                    .isInstanceOf(LimitePlanoException.class)
                    .hasMessageContaining("GESTOR");
        }

        @Test
        @DisplayName("Deve permitir quando não há assinatura ativa (sem plano)")
        void devePermitirQuandoSemAssinatura() {
            when(assinaturaTenantRepository.findAssinaturaAtivaByOrganizacaoId(1L))
                    .thenReturn(Optional.empty());

            assertThatCode(() -> limitePlanoService.validarLimiteAdminOrg(1L))
                    .doesNotThrowAnyException();

            verify(adminOrganizacaoRepository, never()).countAtivosByOrganizacaoId(any());
        }

        @Test
        @DisplayName("Deve permitir quando limite personalizado (custom) sobreescreve limite do plano")
        void deveUsarLimiteCustomQuandoDefinido() {
            assinaturaAtiva.setLimiteAdminOrgCustom(5);
            when(assinaturaTenantRepository.findAssinaturaAtivaByOrganizacaoId(1L))
                    .thenReturn(Optional.of(assinaturaAtiva));
            when(adminOrganizacaoRepository.countAtivosByOrganizacaoId(1L)).thenReturn(4L);

            assertThatCode(() -> limitePlanoService.validarLimiteAdminOrg(1L))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Deve permitir quando limite do plano é nulo (ilimitado)")
        void devePermitirQuandoLimiteIlimitado() {
            planoStarter.setLimiteAdminOrg(null);
            when(assinaturaTenantRepository.findAssinaturaAtivaByOrganizacaoId(1L))
                    .thenReturn(Optional.of(assinaturaAtiva));

            assertThatCode(() -> limitePlanoService.validarLimiteAdminOrg(1L))
                    .doesNotThrowAnyException();

            verify(adminOrganizacaoRepository, never()).countAtivosByOrganizacaoId(any());
        }
    }

    // ========== validarLimiteProfissional ==========

    @Nested
    @DisplayName("validarLimiteProfissional")
    class ValidarLimiteProfissional {

        @Test
        @DisplayName("Deve permitir quando há slots disponíveis para profissional")
        void devePermitirQuandoHaSlotsParaProfissional() {
            when(assinaturaTenantRepository.findAssinaturaAtivaByOrganizacaoId(1L))
                    .thenReturn(Optional.of(assinaturaAtiva));
            when(profissionalRepository.countAtivosByOrganizacaoId(1L)).thenReturn(2L);

            assertThatCode(() -> limitePlanoService.validarLimiteProfissional(1L))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Deve lançar LimitePlanoException quando limite de Profissional for atingido")
        void deveLancarExcecaoQuandoLimiteProfissionalAtingido() {
            when(assinaturaTenantRepository.findAssinaturaAtivaByOrganizacaoId(1L))
                    .thenReturn(Optional.of(assinaturaAtiva));
            when(profissionalRepository.countAtivosByOrganizacaoId(1L)).thenReturn(3L);

            assertThatThrownBy(() -> limitePlanoService.validarLimiteProfissional(1L))
                    .isInstanceOf(LimitePlanoException.class)
                    .hasMessageContaining("PROFISSIONAL");
        }

        @Test
        @DisplayName("LimitePlanoException deve conter role, limite e usado")
        void excecaoDeveConterDetalhes() {
            when(assinaturaTenantRepository.findAssinaturaAtivaByOrganizacaoId(1L))
                    .thenReturn(Optional.of(assinaturaAtiva));
            when(profissionalRepository.countAtivosByOrganizacaoId(1L)).thenReturn(3L);

            try {
                limitePlanoService.validarLimiteProfissional(1L);
            } catch (LimitePlanoException e) {
                assertThat(e.getRole()).isEqualTo("PROFISSIONAL");
                assertThat(e.getLimite()).isEqualTo(3);
                assertThat(e.getUsado()).isEqualTo(3L);
            }
        }

        @Test
        @DisplayName("Deve permitir quando não há assinatura ativa (sem plano)")
        void devePermitirSemAssinaturaParaProfissional() {
            when(assinaturaTenantRepository.findAssinaturaAtivaByOrganizacaoId(1L))
                    .thenReturn(Optional.empty());

            assertThatCode(() -> limitePlanoService.validarLimiteProfissional(1L))
                    .doesNotThrowAnyException();

            verify(profissionalRepository, never()).countAtivosByOrganizacaoId(any());
        }
    }

    // ========== validarLimiteSecretaria ==========

    @Nested
    @DisplayName("validarLimiteSecretaria")
    class ValidarLimiteSecretaria {

        @Test
        @DisplayName("Deve permitir quando há slots disponíveis para secretária")
        void devePermitirQuandoHaSlotsParaSecretaria() {
            when(assinaturaTenantRepository.findAssinaturaAtivaByOrganizacaoId(1L))
                    .thenReturn(Optional.of(assinaturaAtiva));
            when(secretariaRepository.countAtivasByOrganizacaoId(1L)).thenReturn(1L);

            assertThatCode(() -> limitePlanoService.validarLimiteSecretaria(1L))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Deve lançar LimitePlanoException quando limite de Secretária for atingido")
        void deveLancarExcecaoQuandoLimiteSecretariaAtingido() {
            when(assinaturaTenantRepository.findAssinaturaAtivaByOrganizacaoId(1L))
                    .thenReturn(Optional.of(assinaturaAtiva));
            when(secretariaRepository.countAtivasByOrganizacaoId(1L)).thenReturn(2L);

            assertThatThrownBy(() -> limitePlanoService.validarLimiteSecretaria(1L))
                    .isInstanceOf(LimitePlanoException.class)
                    .hasMessageContaining("SECRETARIA");
        }

        @Test
        @DisplayName("Deve usar limite custom de secretária quando definido")
        void deveUsarLimiteCustomParaSecretaria() {
            assinaturaAtiva.setLimiteSecretariaCustom(10);
            when(assinaturaTenantRepository.findAssinaturaAtivaByOrganizacaoId(1L))
                    .thenReturn(Optional.of(assinaturaAtiva));
            when(secretariaRepository.countAtivasByOrganizacaoId(1L)).thenReturn(9L);

            assertThatCode(() -> limitePlanoService.validarLimiteSecretaria(1L))
                    .doesNotThrowAnyException();
        }
    }

    // ========== obterLimitesPlano ==========

    @Nested
    @DisplayName("obterLimitesPlano")
    class ObterLimitesPlano {

        @Test
        @DisplayName("Deve retornar limites do plano ativo com uso atual")
        void deveRetornarLimitesDoPlanoAtivo() {
            when(assinaturaTenantRepository.findAssinaturaAtivaByOrganizacaoId(1L))
                    .thenReturn(Optional.of(assinaturaAtiva));
            when(adminOrganizacaoRepository.countAtivosByOrganizacaoId(1L)).thenReturn(0L);
            when(profissionalRepository.countAtivosByOrganizacaoId(1L)).thenReturn(2L);
            when(secretariaRepository.countAtivasByOrganizacaoId(1L)).thenReturn(1L);

            LimitesPlanoResponse response = limitePlanoService.obterLimitesPlano(1L);

            assertThat(response.planoNome()).isEqualTo("Starter");
            assertThat(response.planoTipo()).isEqualTo("STARTER");
            assertThat(response.limiteAdminOrg()).isEqualTo(1);
            assertThat(response.limiteProfissional()).isEqualTo(3);
            assertThat(response.limiteSecretaria()).isEqualTo(2);
            assertThat(response.usadoAdminOrg()).isEqualTo(0L);
            assertThat(response.usadoProfissional()).isEqualTo(2L);
            assertThat(response.usadoSecretaria()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Deve indicar que pode adicionar quando ainda há slots")
        void deveIndicarPodeAdicionarQuandoHaSlots() {
            when(assinaturaTenantRepository.findAssinaturaAtivaByOrganizacaoId(1L))
                    .thenReturn(Optional.of(assinaturaAtiva));
            when(adminOrganizacaoRepository.countAtivosByOrganizacaoId(1L)).thenReturn(0L);
            when(profissionalRepository.countAtivosByOrganizacaoId(1L)).thenReturn(1L);
            when(secretariaRepository.countAtivasByOrganizacaoId(1L)).thenReturn(0L);

            LimitesPlanoResponse response = limitePlanoService.obterLimitesPlano(1L);

            assertThat(response.podeAdicionarAdminOrg()).isTrue();
            assertThat(response.podeAdicionarProfissional()).isTrue();
            assertThat(response.podeAdicionarSecretaria()).isTrue();
        }

        @Test
        @DisplayName("Deve indicar que não pode adicionar quando limite atingido")
        void deveIndicarNaoPodeAdicionarQuandoLimiteAtingido() {
            when(assinaturaTenantRepository.findAssinaturaAtivaByOrganizacaoId(1L))
                    .thenReturn(Optional.of(assinaturaAtiva));
            when(adminOrganizacaoRepository.countAtivosByOrganizacaoId(1L)).thenReturn(1L);
            when(profissionalRepository.countAtivosByOrganizacaoId(1L)).thenReturn(3L);
            when(secretariaRepository.countAtivasByOrganizacaoId(1L)).thenReturn(2L);

            LimitesPlanoResponse response = limitePlanoService.obterLimitesPlano(1L);

            assertThat(response.podeAdicionarAdminOrg()).isFalse();
            assertThat(response.podeAdicionarProfissional()).isFalse();
            assertThat(response.podeAdicionarSecretaria()).isFalse();
        }

        @Test
        @DisplayName("Deve retornar 'Sem plano' quando não há assinatura ativa")
        void deveRetornarSemPlanoQuandoSemAssinatura() {
            when(assinaturaTenantRepository.findAssinaturaAtivaByOrganizacaoId(1L))
                    .thenReturn(Optional.empty());
            when(adminOrganizacaoRepository.countAtivosByOrganizacaoId(1L)).thenReturn(0L);
            when(profissionalRepository.countAtivosByOrganizacaoId(1L)).thenReturn(0L);
            when(secretariaRepository.countAtivasByOrganizacaoId(1L)).thenReturn(0L);

            LimitesPlanoResponse response = limitePlanoService.obterLimitesPlano(1L);

            assertThat(response.planoNome()).isEqualTo("Sem plano");
            assertThat(response.planoTipo()).isNull();
            assertThat(response.limiteAdminOrg()).isNull();
            assertThat(response.limiteProfissional()).isNull();
            assertThat(response.limiteSecretaria()).isNull();
        }

        @Test
        @DisplayName("Deve retornar podeAdicionar=true para todos quando sem assinatura (ilimitado)")
        void devePermitirTudoQuandoSemPlano() {
            when(assinaturaTenantRepository.findAssinaturaAtivaByOrganizacaoId(1L))
                    .thenReturn(Optional.empty());
            when(adminOrganizacaoRepository.countAtivosByOrganizacaoId(1L)).thenReturn(0L);
            when(profissionalRepository.countAtivosByOrganizacaoId(1L)).thenReturn(0L);
            when(secretariaRepository.countAtivasByOrganizacaoId(1L)).thenReturn(0L);

            LimitesPlanoResponse response = limitePlanoService.obterLimitesPlano(1L);

            assertThat(response.podeAdicionarAdminOrg()).isTrue();
            assertThat(response.podeAdicionarProfissional()).isTrue();
            assertThat(response.podeAdicionarSecretaria()).isTrue();
        }

        @Test
        @DisplayName("Deve usar limite custom de profissional quando definido na assinatura")
        void deveUsarLimiteCustomProfissionalNaResposta() {
            assinaturaAtiva.setLimiteProfissionalCustom(10);
            when(assinaturaTenantRepository.findAssinaturaAtivaByOrganizacaoId(1L))
                    .thenReturn(Optional.of(assinaturaAtiva));
            when(adminOrganizacaoRepository.countAtivosByOrganizacaoId(1L)).thenReturn(0L);
            when(profissionalRepository.countAtivosByOrganizacaoId(1L)).thenReturn(5L);
            when(secretariaRepository.countAtivasByOrganizacaoId(1L)).thenReturn(0L);

            LimitesPlanoResponse response = limitePlanoService.obterLimitesPlano(1L);

            assertThat(response.limiteProfissional()).isEqualTo(10);
            assertThat(response.podeAdicionarProfissional()).isTrue();
        }
    }
}
