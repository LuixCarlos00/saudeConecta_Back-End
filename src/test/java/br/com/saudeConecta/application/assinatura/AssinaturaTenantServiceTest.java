package br.com.saudeConecta.application.assinatura;

import br.com.saudeConecta.application.assinatura.dto.AssinaturaPlataformaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import br.com.saudeConecta.application.assinatura.dto.AssinaturaTenantResponse;
import br.com.saudeConecta.application.assinatura.dto.CustomizarPlanoTenantRequest;
import br.com.saudeConecta.application.exception.BusinessException;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.planos.AssinaturaTenant;
import br.com.saudeConecta.domain.planos.PlanoAssinatura;
import br.com.saudeConecta.domain.planos.StatusAssinatura;
import br.com.saudeConecta.domain.planos.TipoPlano;
import br.com.saudeConecta.infrastructure.persistence.repository.AssinaturaTenantRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.OrganizacaoRepository;
import org.springframework.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import br.com.saudeConecta.config.FusoHorarioConfig;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    @Mock private AssinaturaMercadoPagoService assinaturaMercadoPagoService;
    @Mock private org.springframework.context.ApplicationEventPublisher eventos;

    private AssinaturaTenantService service;

    @BeforeEach
    void criarService() {
        service = new AssinaturaTenantService(assinaturaTenantRepository, organizacaoRepository,
                planoAssinaturaService, assinaturaMercadoPagoService, eventos, Clock.system(FusoHorarioConfig.ZONA));
        // Sem transação no teste unitário: a ação "após o commit" roda na hora e, como no serviço
        // real, a falha dela é só registrada
        lenient().doAnswer(inv -> {
            try {
                inv.<Runnable>getArgument(2).run();
            } catch (RuntimeException ignorada) {
                // o executarAposCommit real só registra a falha
            }
            return null;
        }).when(assinaturaMercadoPagoService).executarAposCommit(any(), any(), any());
    }

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
            when(assinaturaTenantRepository.findByIdParaAtualizacao(1L)).thenReturn(Optional.of(existente));
            when(planoAssinaturaService.buscarEntidadePorId(20L))
                    .thenReturn(criarPlano(20L));
            when(assinaturaTenantRepository.saveAndFlush(any())).thenReturn(existente);

            service.assinar(1L, 20L);

            verify(planoAssinaturaService).buscarEntidadePorId(20L);
            verify(assinaturaTenantRepository, times(1)).saveAndFlush(any());
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
            assertThat(assinatura.getDataSuspensao()).isNotNull();
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

    // ========== buscarAssinaturaVigente ==========

    @Nested
    @DisplayName("buscarAssinaturaVigente")
    class BuscarAssinaturaVigente {

        @Test
        @DisplayName("Deve retornar assinatura ativa da organização")
        void deveRetornarAssinaturaAtiva() {
            AssinaturaTenant assinatura = criarAssinatura(1L, StatusAssinatura.ATIVA);
            when(assinaturaTenantRepository.findAssinaturaVigenteByOrganizacaoId(1L))
                    .thenReturn(Optional.of(assinatura));

            AssinaturaTenantResponse response = service.buscarAssinaturaVigente(1L);

            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("C1 - Deve retornar a assinatura SUSPENSA (a clínica precisa vê-la para pagar)")
        void deveRetornarAssinaturaSuspensa() {
            AssinaturaTenant assinatura = criarAssinatura(1L, StatusAssinatura.SUSPENSA);
            when(assinaturaTenantRepository.findAssinaturaVigenteByOrganizacaoId(1L))
                    .thenReturn(Optional.of(assinatura));

            AssinaturaTenantResponse response = service.buscarAssinaturaVigente(1L);

            assertThat(response.status()).isEqualTo("SUSPENSA");
        }

        @Test
        @DisplayName("Deve lançar exceção quando não há assinatura ativa")
        void deveLancarExcecaoQuandoSemAssinaturaAtiva() {
            when(assinaturaTenantRepository.findAssinaturaVigenteByOrganizacaoId(99L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.buscarAssinaturaVigente(99L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ========== listarTodas / listarPorOrganizacao ==========

    @Nested
    @DisplayName("listar")
    class Listar {

        @Test
        @DisplayName("Listagem da plataforma: filtros chegam à query, busca escapada e sem dado pessoal")
        void listarParaPlataforma() {
            AssinaturaTenant a = criarAssinatura(1L, StatusAssinatura.ATIVA);
            a.setMpPreapprovalId("pre-1");
            a.setMpPayerEmail("dono@clinica.com");
            when(assinaturaTenantRepository.listarVigentesParaPlataforma(eq(StatusAssinatura.ATIVA), eq(TipoPlano.STARTER),
                    eq(Boolean.TRUE), eq("%50!% off!_x%"), any())).thenReturn(new PageImpl<>(List.of(a)));

            var pagina = service.listarParaPlataforma(StatusAssinatura.ATIVA, TipoPlano.STARTER, true, " 50% OFF_x ",
                    PageRequest.of(2, 500, Sort.by(Sort.Direction.DESC, "dataVencimento")));

            assertThat(pagina.getContent()).singleElement().satisfies(l -> {
                assertThat(l.assinaturaId()).isEqualTo(1L);
                assertThat(l.temPreapproval()).isTrue();
            });
            assertThat(AssinaturaPlataformaResponse.class.getRecordComponents())
                    .extracting(java.lang.reflect.RecordComponent::getName)
                    .isNotEmpty()
                    .doesNotContain("mpPayerEmail", "mpInitPoint", "mpPreapprovalId");
            var pageable = org.mockito.ArgumentCaptor.forClass(Pageable.class);
            verify(assinaturaTenantRepository).listarVigentesParaPlataforma(any(), any(), any(), any(), pageable.capture());
            assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
            assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
            assertThat(pageable.getValue().getSort().getOrderFor("dataVencimento").isDescending()).isTrue();
            assertThat(pageable.getValue().getSort().getOrderFor("id")).isNotNull();
        }

        @Test
        @DisplayName("Listagem da plataforma: sem ordenação ordena pelo nome da clínica; busca vazia vira null")
        void listarParaPlataformaPadrao() {
            when(assinaturaTenantRepository.listarVigentesParaPlataforma(isNull(), isNull(), isNull(), isNull(), any()))
                    .thenReturn(Page.empty());

            service.listarParaPlataforma(null, null, null, "  ", PageRequest.of(0, 20));

            var pageable = org.mockito.ArgumentCaptor.forClass(Pageable.class);
            verify(assinaturaTenantRepository).listarVigentesParaPlataforma(any(), any(), any(), any(), pageable.capture());
            assertThat(pageable.getValue().getSort().getOrderFor("organizacao.nome")).isNotNull();
        }

        @Test
        @DisplayName("Listagem da plataforma: ordenação fora da lista branca responde 400")
        void listarParaPlataformaOrdenacaoInvalida() {
            PageRequest ordenacaoInvalida = PageRequest.of(0, 20, Sort.by("mpPayerEmail"));
            assertThatThrownBy(() -> service.listarParaPlataforma(null, null, null, null, ordenacaoInvalida))
                    .isInstanceOf(IllegalArgumentException.class);
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

        @Test
        @DisplayName("2.8 - Renovação parte do vencimento atual e fixa o dia âncora; limpa a data da suspensão")
        void deveRenovarAPartirDoCicloELimparSuspensao() {
            AssinaturaTenant assinatura = criarAssinatura(1L, StatusAssinatura.SUSPENSA);
            assinatura.setDataVencimento(LocalDate.now().minusDays(20));
            assinatura.setDataSuspensao(LocalDateTime.now().minusDays(3));
            when(assinaturaTenantRepository.findById(1L)).thenReturn(Optional.of(assinatura));

            service.renovarAposPagamento(1L);

            assertThat(assinatura.getDiaCobranca()).isEqualTo(LocalDate.now().getDayOfMonth());
            assertThat(assinatura.getDataVencimento()).isAfter(LocalDate.now());
            assertThat(assinatura.getDataSuspensao()).isNull();
        }

        @Test
        @DisplayName("5.5 (M4) - Pagamento às 22h30 de Brasília (já 01h30 UTC do dia seguinte) ancora no dia de Brasília")
        void ancoraNoDiaDeBrasiliaNaoNoUtc() {
            // 2026-03-01T01:30Z = 2026-02-28 22:30 em São Paulo
            Clock relogio = Clock.fixed(Instant.parse("2026-03-01T01:30:00Z"), FusoHorarioConfig.ZONA);
            AssinaturaTenantService comRelogio = new AssinaturaTenantService(assinaturaTenantRepository,
                    organizacaoRepository, planoAssinaturaService, assinaturaMercadoPagoService, eventos, relogio);
            AssinaturaTenant assinatura = criarAssinatura(1L, StatusAssinatura.INADIMPLENTE);
            assinatura.setDataVencimento(LocalDate.of(2026, 2, 20));
            assinatura.setDiaCobranca(null);
            when(assinaturaTenantRepository.findById(1L)).thenReturn(Optional.of(assinatura));

            comRelogio.renovarAposPagamento(1L);

            assertThat(assinatura.getDiaCobranca()).isEqualTo(28);
            assertThat(assinatura.getDataVencimento()).isEqualTo(LocalDate.of(2026, 3, 28));
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
            when(assinaturaTenantRepository.findByIdParaAtualizacao(1L)).thenReturn(Optional.of(assinatura));
            when(planoAssinaturaService.buscarEntidadePorId(20L)).thenReturn(novoPlano);
            when(assinaturaTenantRepository.saveAndFlush(any())).thenReturn(assinatura);

            AssinaturaTenantResponse response = service.trocarPlano(1L, 20L);

            assertThat(response).isNotNull();
            assertThat(assinatura.getPlanoAssinatura()).isEqualTo(novoPlano);
        }

        @Test
        @DisplayName("C2 - Troca de plano mantém os limites personalizados e cobra os extras contra o plano novo")
        void trocaDePlanoCobraExtrasContraPlanoNovo() {
            AssinaturaTenant assinatura = criarAssinatura(1L, StatusAssinatura.ATIVA);
            assinatura.setLimiteProfissionalCustom(6);
            PlanoAssinatura novoPlano = criarPlano(20L);
            novoPlano.setValorMensal(BigDecimal.valueOf(199.90));
            novoPlano.setLimiteProfissional(4);
            novoPlano.setValorAdicionalProfissional(BigDecimal.valueOf(30));
            when(assinaturaTenantRepository.findAssinaturaAtivaByOrganizacaoId(1L)).thenReturn(Optional.of(assinatura));
            when(assinaturaTenantRepository.findByIdParaAtualizacao(1L)).thenReturn(Optional.of(assinatura));
            when(planoAssinaturaService.buscarEntidadePorId(20L)).thenReturn(novoPlano);
            when(assinaturaTenantRepository.saveAndFlush(any())).thenReturn(assinatura);

            service.trocarPlano(1L, 20L);

            assertThat(assinatura.getLimiteProfissionalCustom()).isEqualTo(6);
            assertThat(assinatura.getQtdProfissionalExtra()).isEqualTo(2);
            assertThat(assinatura.getValorAdicionalPerfis()).isEqualByComparingTo("60");
            assertThat(assinatura.getValorMensal()).isEqualByComparingTo("259.90");
            verify(assinaturaMercadoPagoService).propagarNovoValor(assinatura, BigDecimal.valueOf(99.90));
        }

        @Test
        @DisplayName("C2 - Grava e só depois leva o valor ao MP; falha do MP sobe e desfaz a troca")
        void falhaDoMpAbortaTroca() {
            AssinaturaTenant assinatura = criarAssinatura(1L, StatusAssinatura.ATIVA);
            when(assinaturaTenantRepository.findAssinaturaAtivaByOrganizacaoId(1L)).thenReturn(Optional.of(assinatura));
            when(assinaturaTenantRepository.findByIdParaAtualizacao(1L)).thenReturn(Optional.of(assinatura));
            when(planoAssinaturaService.buscarEntidadePorId(20L)).thenReturn(criarPlano(20L));
            when(assinaturaTenantRepository.saveAndFlush(any())).thenReturn(assinatura);
            doThrow(new BusinessException("MP fora", HttpStatus.BAD_GATEWAY, "MP_VALOR_NAO_ATUALIZADO"))
                    .when(assinaturaMercadoPagoService).propagarNovoValor(any(), any());

            assertThatThrownBy(() -> service.trocarPlano(1L, 20L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo("MP_VALOR_NAO_ATUALIZADO");

            var ordem = inOrder(assinaturaTenantRepository, assinaturaMercadoPagoService);
            ordem.verify(assinaturaTenantRepository).findByIdParaAtualizacao(1L);
            ordem.verify(assinaturaTenantRepository).saveAndFlush(assinatura);
            ordem.verify(assinaturaMercadoPagoService).propagarNovoValor(any(), any());
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
            when(assinaturaTenantRepository.findByIdParaAtualizacao(1L)).thenReturn(Optional.of(assinatura));
            when(assinaturaTenantRepository.saveAndFlush(any())).thenReturn(assinatura);

            CustomizarPlanoTenantRequest request = new CustomizarPlanoTenantRequest(3, 5, 4);

            AssinaturaTenantResponse response = service.customizarPlano(1L, request);

            assertThat(response).isNotNull();
            assertThat(assinatura.getQtdAdminExtra()).isEqualTo(2); // 3 - 1 = 2
            assertThat(assinatura.getQtdProfissionalExtra()).isEqualTo(2); // 5 - 3 = 2
            assertThat(assinatura.getQtdSecretariaExtra()).isEqualTo(2); // 4 - 2 = 2
        }

        @Test
        @DisplayName("C2 - Plano personalizado leva o valor total novo ao MP")
        void customizarPropagaValorAoMp() {
            AssinaturaTenant assinatura = criarAssinatura(1L, StatusAssinatura.ATIVA);
            assinatura.getPlanoAssinatura().setValorAdicionalSecretaria(BigDecimal.valueOf(20));
            when(assinaturaTenantRepository.findAssinaturaAtivaByOrganizacaoId(1L)).thenReturn(Optional.of(assinatura));
            when(assinaturaTenantRepository.findByIdParaAtualizacao(1L)).thenReturn(Optional.of(assinatura));
            when(assinaturaTenantRepository.saveAndFlush(any())).thenReturn(assinatura);

            service.customizarPlano(1L, new CustomizarPlanoTenantRequest(null, null, 3));

            assertThat(assinatura.getValorMensal()).isEqualByComparingTo("119.90");
            verify(assinaturaMercadoPagoService).propagarNovoValor(assinatura, BigDecimal.valueOf(99.90));
        }

        @Test
        @DisplayName("Deve retornar zero extras quando limite custom menor que do plano")
        void deveRetornarZeroExtrasQuandoLimiteMenor() {
            AssinaturaTenant assinatura = criarAssinatura(1L, StatusAssinatura.ATIVA);

            when(assinaturaTenantRepository.findAssinaturaAtivaByOrganizacaoId(1L))
                    .thenReturn(Optional.of(assinatura));
            when(assinaturaTenantRepository.findByIdParaAtualizacao(1L)).thenReturn(Optional.of(assinatura));
            when(assinaturaTenantRepository.saveAndFlush(any())).thenReturn(assinatura);

            CustomizarPlanoTenantRequest request = new CustomizarPlanoTenantRequest(0, 1, 1);

            service.customizarPlano(1L, request);

            assertThat(assinatura.getQtdAdminExtra()).isZero();
        }
    }

    // ========== definirCortesia ==========

    @Nested
    @DisplayName("definirCortesia")
    class DefinirCortesia {

        @Test
        @DisplayName("D2 - Remover cortesia sem cartão: volta para TRIAL com vencimento hoje+7 e gera/envia o link")
        void removerCortesiaVoltaParaTrialEGeraLink() {
            AssinaturaTenant assinatura = criarAssinatura(1L, StatusAssinatura.ATIVA);
            assinatura.setCortesia(true);
            assinatura.setDataVencimento(LocalDate.now().minusDays(200));
            when(assinaturaTenantRepository.findByIdParaAtualizacao(1L)).thenReturn(Optional.of(assinatura));
            when(assinaturaTenantRepository.save(any())).thenReturn(assinatura);

            service.definirCortesia(1L, false);

            assertThat(assinatura.isCortesia()).isFalse();
            assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.TRIAL);
            assertThat(assinatura.getDataVencimento()).isEqualTo(LocalDate.now().plusDays(7));
            assertThat(assinatura.getDataProximaCobranca()).isEqualTo(LocalDate.now().plusDays(7));
            verify(eventos).publishEvent(new LinkPagamentoSolicitadoEvent(1L));
        }

        @Test
        @DisplayName("2.6 - Falha ao gerar o link não desfaz a remoção da cortesia (a rotina diária de trial cobre)")
        void falhaNoLinkNaoDesfazRemocao() {
            AssinaturaTenant assinatura = criarAssinatura(1L, StatusAssinatura.ATIVA);
            assinatura.setCortesia(true);
            when(assinaturaTenantRepository.findByIdParaAtualizacao(1L)).thenReturn(Optional.of(assinatura));
            when(assinaturaTenantRepository.save(any())).thenReturn(assinatura);
            doThrow(new BusinessException("MP fora", HttpStatus.BAD_GATEWAY))
                    .when(eventos).publishEvent(new LinkPagamentoSolicitadoEvent(1L));

            assertThatCode(() -> service.definirCortesia(1L, false)).doesNotThrowAnyException();
            assertThat(assinatura.isCortesia()).isFalse();
            assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.TRIAL);
        }

        @Test
        @DisplayName("D2 - Remover cortesia com cartão autorizado: vencimento = next_payment_date do MP, status mantido, sem link")
        void removerCortesiaComCartaoAutorizadoUsaProximaCobrancaDoMp() {
            AssinaturaTenant assinatura = criarAssinatura(1L, StatusAssinatura.ATIVA);
            assinatura.setCortesia(true);
            assinatura.setMpPreapprovalId("pa-1");
            assinatura.setMpStatus("authorized");
            LocalDate proximaCobranca = LocalDate.now().plusDays(12);
            when(assinaturaTenantRepository.findByIdParaAtualizacao(1L)).thenReturn(Optional.of(assinatura));
            when(assinaturaTenantRepository.save(any())).thenReturn(assinatura);
            when(assinaturaMercadoPagoService.buscarProximaCobrancaAutorizada(assinatura)).thenReturn(proximaCobranca);

            service.definirCortesia(1L, false);

            assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.ATIVA);
            assertThat(assinatura.getDataVencimento()).isEqualTo(proximaCobranca);
            assertThat(assinatura.getDataProximaCobranca()).isEqualTo(proximaCobranca);
            verify(eventos, never()).publishEvent(any(Object.class));
        }

        @Test
        @DisplayName("D2 - Cartão autorizado mas MP não responde: mantém status, vencimento hoje+7, sem link")
        void removerCortesiaComCartaoAutorizadoEMpFora() {
            AssinaturaTenant assinatura = criarAssinatura(1L, StatusAssinatura.ATIVA);
            assinatura.setCortesia(true);
            assinatura.setMpPreapprovalId("pa-1");
            assinatura.setMpStatus("authorized");
            when(assinaturaTenantRepository.findByIdParaAtualizacao(1L)).thenReturn(Optional.of(assinatura));
            when(assinaturaTenantRepository.save(any())).thenReturn(assinatura);
            when(assinaturaMercadoPagoService.buscarProximaCobrancaAutorizada(assinatura))
                    .thenThrow(new BusinessException("MP fora", HttpStatus.BAD_GATEWAY));

            service.definirCortesia(1L, false);

            assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.ATIVA);
            assertThat(assinatura.getDataVencimento()).isEqualTo(LocalDate.now().plusDays(7));
            verify(eventos, never()).publishEvent(any(Object.class));
        }

        @Test
        @DisplayName("D2 - Preapproval que não está authorized no MP: volta para TRIAL e gera o link")
        void removerCortesiaComPreapprovalNaoAutorizadoVoltaParaTrial() {
            AssinaturaTenant assinatura = criarAssinatura(1L, StatusAssinatura.ATIVA);
            assinatura.setCortesia(true);
            assinatura.setMpPreapprovalId("pa-1");
            assinatura.setMpStatus("authorized");
            when(assinaturaTenantRepository.findByIdParaAtualizacao(1L)).thenReturn(Optional.of(assinatura));
            when(assinaturaTenantRepository.save(any())).thenReturn(assinatura);
            when(assinaturaMercadoPagoService.buscarProximaCobrancaAutorizada(assinatura)).thenReturn(null);

            service.definirCortesia(1L, false);

            assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.TRIAL);
            assertThat(assinatura.getDataVencimento()).isEqualTo(LocalDate.now().plusDays(7));
            verify(eventos).publishEvent(new LinkPagamentoSolicitadoEvent(1L));
        }

        @Test
        @DisplayName("D2 - Remover cortesia de assinatura CANCELADA não reabre nem gera link")
        void removerCortesiaDeCanceladaNaoReabre() {
            AssinaturaTenant assinatura = criarAssinatura(1L, StatusAssinatura.CANCELADA);
            assinatura.setCortesia(true);
            LocalDate vencimento = assinatura.getDataVencimento();
            when(assinaturaTenantRepository.findByIdParaAtualizacao(1L)).thenReturn(Optional.of(assinatura));
            when(assinaturaTenantRepository.save(any())).thenReturn(assinatura);

            service.definirCortesia(1L, false);

            assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.CANCELADA);
            assertThat(assinatura.getDataVencimento()).isEqualTo(vencimento);
            verifyNoInteractions(assinaturaMercadoPagoService);
        }

        @Test
        @DisplayName("A2 - Dar cortesia a SUSPENSA ou INADIMPLENTE: vira ATIVA e o preapproval é desvinculado para cancelar após o commit")
        void darCortesiaLiberaEParaCobranca() {
            for (StatusAssinatura status : List.of(StatusAssinatura.SUSPENSA, StatusAssinatura.INADIMPLENTE)) {
                AssinaturaTenant assinatura = criarAssinatura(1L, status);
                assinatura.setDataSuspensao(LocalDateTime.now().minusDays(3));
                assinatura.setMpPreapprovalId("pa-1");
                assinatura.setMpStatus("paused");
                when(assinaturaTenantRepository.findByIdParaAtualizacao(1L)).thenReturn(Optional.of(assinatura));
                when(assinaturaTenantRepository.save(any())).thenReturn(assinatura);

                service.definirCortesia(1L, true);

                assertThat(assinatura.isCortesia()).isTrue();
                assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.ATIVA);
                assertThat(assinatura.getDataSuspensao()).isNull();
                verify(assinaturaMercadoPagoService).desvincularECancelarAposCommit(assinatura, "CORTESIA");
                clearInvocations(assinaturaMercadoPagoService);
            }
            verify(eventos, never()).publishEvent(any(Object.class));
        }

        @Test
        @DisplayName("A2 - Cortesia em CANCELADA não reabre a assinatura nem mexe no MP")
        void darCortesiaACanceladaNaoReabre() {
            AssinaturaTenant assinatura = criarAssinatura(1L, StatusAssinatura.CANCELADA);
            when(assinaturaTenantRepository.findByIdParaAtualizacao(1L)).thenReturn(Optional.of(assinatura));
            when(assinaturaTenantRepository.save(any())).thenReturn(assinatura);

            service.definirCortesia(1L, true);

            assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.CANCELADA);
            verify(assinaturaMercadoPagoService, never()).desvincularECancelarAposCommit(any(), any());
        }

        @Test
        @DisplayName("Conceder cortesia não gera link nem mexe no vencimento")
        void concederCortesiaNaoGeraLink() {
            AssinaturaTenant assinatura = criarAssinatura(1L, StatusAssinatura.TRIAL);
            LocalDate vencimento = assinatura.getDataVencimento();
            when(assinaturaTenantRepository.findByIdParaAtualizacao(1L)).thenReturn(Optional.of(assinatura));
            when(assinaturaTenantRepository.save(any())).thenReturn(assinatura);

            service.definirCortesia(1L, true);

            assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.ATIVA);
            assertThat(assinatura.getDataVencimento()).isEqualTo(vencimento);
            verifyNoInteractions(assinaturaMercadoPagoService);
        }
    }

    // ========== operações do SUPER_ADMIN ==========

    @Nested
    @DisplayName("SUPER_ADMIN: associar, suspender e cancelar")
    class OperacoesSuperAdmin {

        @Test
        @DisplayName("Associar plano a clínica com assinatura SUSPENSA vigente: 409 ASSINATURA_SUSPENSA, nada criado")
        void assinarComSuspensaRecusa() {
            when(assinaturaTenantRepository.findAssinaturaAtivaByOrganizacaoId(1L)).thenReturn(Optional.empty());
            when(assinaturaTenantRepository.findAssinaturaVigenteByOrganizacaoId(1L))
                    .thenReturn(Optional.of(criarAssinatura(5L, StatusAssinatura.SUSPENSA)));

            assertThatThrownBy(() -> service.assinar(1L, 10L))
                    .isInstanceOfSatisfying(BusinessException.class, e -> {
                        assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(e.getErrorCode()).isEqualTo("ASSINATURA_SUSPENSA");
                    });
            verify(assinaturaTenantRepository, never()).save(any());
        }

        @Test
        @DisplayName("Suspender: trava a linha, grava SUSPENSA com data e pausa no MP na mesma transação")
        void suspenderPausaNoMp() {
            AssinaturaTenant a = criarAssinatura(1L, StatusAssinatura.INADIMPLENTE);
            a.setMpPreapprovalId("pa-1");
            a.setMpStatus("authorized");
            when(assinaturaTenantRepository.findByIdParaAtualizacao(1L)).thenReturn(Optional.of(a));

            service.suspenderPeloSuperAdmin(1L);

            assertThat(a.getStatus()).isEqualTo(StatusAssinatura.SUSPENSA);
            assertThat(a.getDataSuspensao()).isNotNull();
            var ordem = inOrder(assinaturaTenantRepository, assinaturaMercadoPagoService);
            ordem.verify(assinaturaTenantRepository).findByIdParaAtualizacao(1L);
            ordem.verify(assinaturaTenantRepository).save(a);
            ordem.verify(assinaturaMercadoPagoService).pausar(a);
            verify(assinaturaTenantRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Suspender já SUSPENSA: mantém a data da suspensão e só tenta pausar de novo")
        void suspenderJaSuspensa() {
            AssinaturaTenant a = criarAssinatura(1L, StatusAssinatura.SUSPENSA);
            LocalDateTime suspensaEm = LocalDateTime.of(2026, 10, 1, 9, 0);
            a.setDataSuspensao(suspensaEm);
            when(assinaturaTenantRepository.findByIdParaAtualizacao(1L)).thenReturn(Optional.of(a));

            service.suspenderPeloSuperAdmin(1L);

            assertThat(a.getDataSuspensao()).isEqualTo(suspensaEm);
            verify(assinaturaTenantRepository, never()).save(any());
            verify(assinaturaMercadoPagoService).pausar(a);
        }

        @Test
        @DisplayName("Suspender CANCELADA: 409 ASSINATURA_CANCELADA, não reabre nem chama o MP")
        void suspenderCanceladaRecusa() {
            AssinaturaTenant a = criarAssinatura(1L, StatusAssinatura.CANCELADA);
            when(assinaturaTenantRepository.findByIdParaAtualizacao(1L)).thenReturn(Optional.of(a));

            assertThatThrownBy(() -> service.suspenderPeloSuperAdmin(1L))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo("ASSINATURA_CANCELADA"));
            assertThat(a.getStatus()).isEqualTo(StatusAssinatura.CANCELADA);
            verifyNoInteractions(assinaturaMercadoPagoService);
        }

        @Test
        @DisplayName("Suspender inexistente: 404")
        void suspenderInexistente() {
            when(assinaturaTenantRepository.findByIdParaAtualizacao(9L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.suspenderPeloSuperAdmin(9L))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        }

        @Test
        @DisplayName("Cancelar: relê com lock e cancela no MP com origem SUPER_ADMIN")
        void cancelarComLock() {
            AssinaturaTenant a = criarAssinatura(1L, StatusAssinatura.ATIVA);
            when(assinaturaTenantRepository.findByIdParaAtualizacao(1L)).thenReturn(Optional.of(a));

            service.cancelarPeloSuperAdmin(1L);

            verify(assinaturaMercadoPagoService).cancelar(a,
                    br.com.saudeConecta.domain.planos.OrigemCancelamento.SUPER_ADMIN);
        }

        @Test
        @DisplayName("Cancelar: falha do MP sobe (nada muda localmente)")
        void cancelarFalhaNoMp() {
            AssinaturaTenant a = criarAssinatura(1L, StatusAssinatura.ATIVA);
            when(assinaturaTenantRepository.findByIdParaAtualizacao(1L)).thenReturn(Optional.of(a));
            doThrow(new BusinessException("MP fora", HttpStatus.BAD_GATEWAY))
                    .when(assinaturaMercadoPagoService).cancelar(any(), any());

            assertThatThrownBy(() -> service.cancelarPeloSuperAdmin(1L))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY));
            assertThat(a.getStatus()).isEqualTo(StatusAssinatura.ATIVA);
        }
    }
}
