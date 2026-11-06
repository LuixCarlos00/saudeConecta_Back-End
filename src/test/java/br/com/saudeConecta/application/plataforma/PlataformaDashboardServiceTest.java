package br.com.saudeConecta.application.plataforma;

import br.com.saudeConecta.application.exception.BusinessException;
import br.com.saudeConecta.application.plataforma.dto.AtencaoResponse;
import br.com.saudeConecta.application.plataforma.dto.DashboardPlataformaResponse;
import br.com.saudeConecta.application.plataforma.dto.PlanoResumoResponse;
import br.com.saudeConecta.application.plataforma.dto.SerieMensalResponse;
import br.com.saudeConecta.application.plataforma.dto.StatusQuantidadeResponse;
import br.com.saudeConecta.config.FusoHorarioConfig;
import br.com.saudeConecta.domain.planos.StatusAssinatura;
import br.com.saudeConecta.domain.planos.TipoPlano;
import br.com.saudeConecta.domain.usuario.TipoUsuarioNovo;
import br.com.saudeConecta.infrastructure.persistence.projection.PlataformaProjecoes;
import br.com.saudeConecta.infrastructure.persistence.repository.AssinaturaTenantRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.CobrancaTenantRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ConsultaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.MpWebhookEventoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlataformaDashboardService - montagem do painel")
class PlataformaDashboardServiceTest {

    @Mock private AssinaturaTenantRepository assinaturaRepository;
    @Mock private CobrancaTenantRepository cobrancaRepository;
    @Mock private ConsultaRepository consultaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ProfissionalRepository profissionalRepository;
    @Mock private MpWebhookEventoRepository webhookEventoRepository;

    private PlataformaDashboardService service;

    @BeforeEach
    void criarService() {
        // 2026-11-05 12:20 em Brasília (UTC-3)
        Clock relogio = Clock.fixed(Instant.parse("2026-11-05T15:20:00Z"), FusoHorarioConfig.ZONA);
        service = new PlataformaDashboardService(assinaturaRepository, cobrancaRepository, consultaRepository,
                usuarioRepository, profissionalRepository, webhookEventoRepository, relogio);
        // Padrão de "banco vazio"; cada teste sobrescreve só o que importa
        lenient().when(assinaturaRepository.contarVigentesPorStatus()).thenReturn(List.of());
        lenient().when(assinaturaRepository.contarAtivasPorPlano()).thenReturn(List.of());
        lenient().when(assinaturaRepository.somarMrrPorPlano()).thenReturn(List.of());
        lenient().when(assinaturaRepository.contarCriadasPorMes(any(), any())).thenReturn(List.of());
        lenient().when(assinaturaRepository.contarCanceladasPorMes(any(), any())).thenReturn(List.of());
        lenient().when(assinaturaRepository.buscarAtencaoPorStatus(any(), any())).thenReturn(List.of());
        lenient().when(assinaturaRepository.buscarTrialsVencendo(any(), any(), any())).thenReturn(List.of());
        lenient().when(cobrancaRepository.somarRecebidoPorMes(any(), any())).thenReturn(List.of());
        lenient().when(consultaRepository.contarNaoCanceladasPorMes(any(), any())).thenReturn(List.of());
        lenient().when(consultaRepository.buscarTopClinicasPorConsultas(any(), any(), any())).thenReturn(List.of());
        lenient().when(usuarioRepository.contarAtivosPorPerfil()).thenReturn(List.of());
        lenient().when(webhookEventoRepository.buscarPendentesManuais(any())).thenReturn(List.of());
    }

    @Test
    @DisplayName("Sem nenhum dado: todos os status e planos aparecem com 0 e a série tem os meses zerados")
    void bancoVazioMontaTudoZerado() {
        DashboardPlataformaResponse r = service.gerar(3);

        assertThat(r.geradoEm()).isEqualTo(LocalDateTime.of(2026, 11, 5, 12, 20));
        assertThat(r.porStatus()).extracting(StatusQuantidadeResponse::status)
                .containsExactly(StatusAssinatura.values());
        assertThat(r.porStatus()).allMatch(s -> s.quantidade() == 0);
        assertThat(r.porPlano()).extracting(PlanoResumoResponse::plano).containsExactly(TipoPlano.values());
        assertThat(r.porPlano()).allMatch(p -> p.quantidade() == 0 && p.mrr().compareTo(BigDecimal.ZERO) == 0);
        assertThat(r.resumo().mrr()).isEqualTo(new BigDecimal("0.00"));
        assertThat(r.resumo().ticketMedio()).isEqualTo(new BigDecimal("0.00"));
        assertThat(r.resumo().clinicasTotal()).isZero();
        assertThat(r.serieMensal()).extracting(SerieMensalResponse::mes)
                .containsExactly("2026-09", "2026-10", "2026-11");
        assertThat(r.serieMensal()).allMatch(m -> m.novasClinicas() == 0 && m.cancelamentos() == 0
                && m.consultas() == 0 && m.receitaRecebida().compareTo(BigDecimal.ZERO) == 0);
        assertThat(r.topClinicasUso()).isEmpty();
        assertThat(r.atencao()).isEmpty();
        verifyNoInteractions(profissionalRepository);
    }

    @Test
    @DisplayName("Resumo: ativas somam TRIAL+ATIVA+INADIMPLENTE; MRR e ticket médio vêm só dos pagantes")
    void resumoComMrrETicketMedio() {
        when(assinaturaRepository.contarVigentesPorStatus()).thenReturn(List.of(
                contagemStatus(StatusAssinatura.ATIVA, 28), contagemStatus(StatusAssinatura.TRIAL, 6),
                contagemStatus(StatusAssinatura.INADIMPLENTE, 3), contagemStatus(StatusAssinatura.SUSPENSA, 1),
                contagemStatus(StatusAssinatura.CANCELADA, 4)));
        // O repository já devolve o MRR sem cortesia (provado no teste H2); aqui conferimos a soma
        when(assinaturaRepository.somarMrrPorPlano()).thenReturn(List.of(
                mrr(TipoPlano.BUSINESS, 9, "6300.00"), mrr(TipoPlano.STARTER, 22, "3278.00")));
        when(assinaturaRepository.contarCortesiasAtivas()).thenReturn(2L);
        when(assinaturaRepository.contarTrialsVencendo(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 12)))
                .thenReturn(2L);
        when(webhookEventoRepository.countByRequerAtencaoTrue()).thenReturn(1L);
        when(usuarioRepository.contarAtivosPorPerfil()).thenReturn(List.of(
                perfil(TipoUsuarioNovo.GESTOR, 40), perfil(TipoUsuarioNovo.CLINICO, 180)));
        when(consultaRepository.contarNaoCanceladasNoPeriodo(any(), any())).thenReturn(5120L);

        DashboardPlataformaResponse r = service.gerar(12);

        assertThat(r.resumo().clinicasTotal()).isEqualTo(42);
        assertThat(r.resumo().clinicasAtivas()).isEqualTo(37);
        assertThat(r.resumo().emTrial()).isEqualTo(6);
        assertThat(r.resumo().trialsVencendo7Dias()).isEqualTo(2);
        assertThat(r.resumo().inadimplentes()).isEqualTo(3);
        assertThat(r.resumo().suspensas()).isEqualTo(1);
        assertThat(r.resumo().canceladas()).isEqualTo(4);
        assertThat(r.resumo().cortesias()).isEqualTo(2);
        assertThat(r.resumo().mrr()).isEqualTo(new BigDecimal("9578.00"));
        // 9578 / 31 pagantes
        assertThat(r.resumo().ticketMedio()).isEqualTo(new BigDecimal("308.97"));
        assertThat(r.resumo().pendenciasManuais()).isEqualTo(1);
        assertThat(r.resumo().usuariosAtivos()).isEqualTo(220);
        assertThat(r.resumo().consultasMesAtual()).isEqualTo(5120);
        assertThat(r.porPlano().get(2).mrr().scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("Receita do mês atual e do anterior usam intervalos de mês fechados no fuso de Brasília")
    void receitaDosMeses() {
        LocalDateTime outubro = LocalDateTime.of(2026, 10, 1, 0, 0);
        LocalDateTime novembro = LocalDateTime.of(2026, 11, 1, 0, 0);
        LocalDateTime dezembro = LocalDateTime.of(2026, 12, 1, 0, 0);
        when(cobrancaRepository.somarRecebidoNoPeriodo(novembro, dezembro)).thenReturn(new BigDecimal("9870"));
        when(cobrancaRepository.somarRecebidoNoPeriodo(outubro, novembro)).thenReturn(new BigDecimal("11020.5"));

        DashboardPlataformaResponse r = service.gerar(1);

        assertThat(r.resumo().receitaMesAtual()).isEqualTo(new BigDecimal("9870.00"));
        assertThat(r.resumo().receitaMesAnterior()).isEqualTo(new BigDecimal("11020.50"));
    }

    @Test
    @DisplayName("Série mensal: lista os meses no Java e preenche só os que têm dado, o resto fica 0")
    void serieComMesesZerados() {
        when(assinaturaRepository.contarCriadasPorMes(any(), any())).thenReturn(List.of(mensal(2026, 11, 3)));
        when(assinaturaRepository.contarCanceladasPorMes(any(), any())).thenReturn(List.of(mensal(2026, 9, 1)));
        when(consultaRepository.contarNaoCanceladasPorMes(any(), any())).thenReturn(List.of(mensal(2026, 10, 4300)));
        when(cobrancaRepository.somarRecebidoPorMes(any(), any())).thenReturn(List.of(valorMensal(2026, 9, "8800")));

        List<SerieMensalResponse> serie = service.gerar(4).serieMensal();

        assertThat(serie).containsExactly(
                new SerieMensalResponse("2026-08", 0, 0, new BigDecimal("0.00"), 0),
                new SerieMensalResponse("2026-09", 0, 1, new BigDecimal("8800.00"), 0),
                new SerieMensalResponse("2026-10", 0, 0, new BigDecimal("0.00"), 4300),
                new SerieMensalResponse("2026-11", 3, 0, new BigDecimal("0.00"), 0));
    }

    @Test
    @DisplayName("Janela da série cruza a virada de ano")
    void serieCruzaAnoNovo() {
        List<SerieMensalResponse> serie = service.gerar(12).serieMensal();

        assertThat(serie).hasSize(12);
        assertThat(serie.get(0).mes()).isEqualTo("2025-12");
        assertThat(serie.get(11).mes()).isEqualTo("2026-11");
    }

    @Test
    @DisplayName("Top clínicas traz profissionais ativos da clínica, 0 quando não há")
    void topClinicasComProfissionais() {
        when(consultaRepository.buscarTopClinicasPorConsultas(any(), any(), any())).thenReturn(List.of(
                clinicaUso(7L, "Clínica Vida Plena", 640), clinicaUso(9L, "Clínica Sem Médico", 3)));
        when(profissionalRepository.contarAtivosPorOrganizacao(anyCollection()))
                .thenReturn(List.of(profissionais(7L, 8)));

        var top = service.gerar(1).topClinicasUso();

        assertThat(top).hasSize(2);
        assertThat(top.get(0).organizacaoId()).isEqualTo(7L);
        assertThat(top.get(0).consultasMes()).isEqualTo(640);
        assertThat(top.get(0).profissionais()).isEqualTo(8);
        assertThat(top.get(1).profissionais()).isZero();
    }

    @Test
    @DisplayName("Atenção: PENDENCIA_MANUAL, SUSPENSA, INADIMPLENTE e TRIAL_VENCENDO, nessa ordem")
    void atencaoNaOrdemDoContrato() {
        when(webhookEventoRepository.buscarPendentesManuais(any())).thenReturn(List.of(
                evento(5L, LocalDateTime.of(2026, 11, 3, 8, 0))));
        when(assinaturaRepository.buscarAtencaoPorStatus(any(), any())).thenAnswer(inv ->
                switch ((StatusAssinatura) inv.getArgument(0)) {
                    case SUSPENSA -> List.of(atencao(2L, "Suspensa", StatusAssinatura.SUSPENSA, "149"));
                    case INADIMPLENTE -> List.of(atencao(3L, "Inadimplente", StatusAssinatura.INADIMPLENTE, "149"));
                    default -> List.of();
                });
        when(assinaturaRepository.buscarTrialsVencendo(any(), any(), any())).thenReturn(List.of(
                atencao(4L, "Trial", StatusAssinatura.TRIAL, "149")));

        List<AtencaoResponse> itens = service.gerar(1).atencao();

        assertThat(itens).extracting(AtencaoResponse::motivo)
                .containsExactly("PENDENCIA_MANUAL", "SUSPENSA", "INADIMPLENTE", "TRIAL_VENCENDO");
        assertThat(itens.get(0).organizacaoId()).isNull();
        assertThat(itens.get(0).dataVencimento()).isEqualTo(LocalDate.of(2026, 11, 3));
        assertThat(itens.get(2).nome()).isEqualTo("Inadimplente");
        assertThat(itens.get(2).valorMensal()).isEqualTo(new BigDecimal("149.00"));
    }

    @Test
    @DisplayName("Atenção para em 20 itens e não consulta as listas seguintes quando o limite já foi atingido")
    void atencaoRespeitaLimiteDeVinte() {
        List<PlataformaProjecoes.EventoPendente> eventos = java.util.stream.IntStream.rangeClosed(1, 20)
                .mapToObj(i -> evento((long) i, LocalDateTime.of(2026, 11, 3, 8, 0))).toList();
        when(webhookEventoRepository.buscarPendentesManuais(any())).thenReturn(eventos);

        List<AtencaoResponse> itens = service.gerar(1).atencao();

        assertThat(itens).hasSize(20);
        verify(assinaturaRepository, never()).buscarAtencaoPorStatus(any(), any());
        verify(assinaturaRepository, never()).buscarTrialsVencendo(any(), any(), any());
    }

    @Test
    @DisplayName("meses fora de 1..24 devolve 422 com código e não toca no banco")
    void mesesInvalidoDevolve422() {
        for (int invalido : new int[]{0, -1, 25}) {
            assertThatThrownBy(() -> service.gerar(invalido))
                    .isInstanceOfSatisfying(BusinessException.class, e -> {
                        assertThat(e.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                        assertThat(e.getErrorCode()).isEqualTo("MESES_INVALIDO");
                    });
        }
        verifyNoInteractions(assinaturaRepository, cobrancaRepository, consultaRepository);
    }

    @Test
    @DisplayName("Limites 1 e 24 são aceitos")
    void limitesAceitos() {
        assertThat(service.gerar(1).serieMensal()).hasSize(1);
        assertThat(service.gerar(24).serieMensal()).hasSize(24);
    }

    // ========== Helpers ==========

    private static final ProjectionFactory PROJECOES = new SpelAwareProxyProjectionFactory();

    /** Projeção real (sem mock) sobre um mapa chave/valor, como o Spring Data devolve da query. */
    private <T> T projecao(Class<T> tipo, Object... chaveValor) {
        Map<String, Object> dados = new HashMap<>();
        for (int i = 0; i < chaveValor.length; i += 2) {
            dados.put((String) chaveValor[i], chaveValor[i + 1]);
        }
        return PROJECOES.createProjection(tipo, dados);
    }

    private PlataformaProjecoes.ContagemPorStatus contagemStatus(StatusAssinatura status, long qtd) {
        return projecao(PlataformaProjecoes.ContagemPorStatus.class, "status", status, "quantidade", qtd);
    }

    private PlataformaProjecoes.MrrPorPlano mrr(TipoPlano plano, long qtd, String valor) {
        return projecao(PlataformaProjecoes.MrrPorPlano.class, "plano", plano, "quantidade", qtd,
                "mrr", new BigDecimal(valor));
    }

    private PlataformaProjecoes.ContagemPorPerfil perfil(TipoUsuarioNovo perfil, long qtd) {
        return projecao(PlataformaProjecoes.ContagemPorPerfil.class, "perfil", perfil, "quantidade", qtd);
    }

    private PlataformaProjecoes.ContagemMensal mensal(int ano, int mes, long qtd) {
        return projecao(PlataformaProjecoes.ContagemMensal.class, "ano", ano, "mes", mes, "quantidade", qtd);
    }

    private PlataformaProjecoes.ValorMensal valorMensal(int ano, int mes, String valor) {
        return projecao(PlataformaProjecoes.ValorMensal.class, "ano", ano, "mes", mes, "valor", new BigDecimal(valor));
    }

    private PlataformaProjecoes.ConsultasPorClinica clinicaUso(Long id, String nome, long consultas) {
        return projecao(PlataformaProjecoes.ConsultasPorClinica.class, "organizacaoId", id, "nome", nome,
                "plano", TipoPlano.BUSINESS, "status", StatusAssinatura.ATIVA, "consultasMes", consultas);
    }

    private PlataformaProjecoes.ProfissionaisPorClinica profissionais(Long orgId, long qtd) {
        return projecao(PlataformaProjecoes.ProfissionaisPorClinica.class, "organizacaoId", orgId, "quantidade", qtd);
    }

    private PlataformaProjecoes.EventoPendente evento(Long id, LocalDateTime recebidoEm) {
        return projecao(PlataformaProjecoes.EventoPendente.class, "id", id, "recebidoEm", recebidoEm);
    }

    private PlataformaProjecoes.ClinicaAtencao atencao(Long id, String nome, StatusAssinatura status, String valor) {
        return projecao(PlataformaProjecoes.ClinicaAtencao.class, "organizacaoId", id, "nome", nome,
                "plano", TipoPlano.STARTER, "status", status, "dataVencimento", LocalDate.of(2026, 10, 28),
                "valorMensal", new BigDecimal(valor));
    }
}
