package br.com.saudeConecta.application.plataforma;

import br.com.saudeConecta.application.exception.BusinessException;
import br.com.saudeConecta.application.plataforma.dto.AtencaoResponse;
import br.com.saudeConecta.application.plataforma.dto.ClinicaUsoResponse;
import br.com.saudeConecta.application.plataforma.dto.DashboardPlataformaResponse;
import br.com.saudeConecta.application.plataforma.dto.PerfilQuantidadeResponse;
import br.com.saudeConecta.application.plataforma.dto.PlanoResumoResponse;
import br.com.saudeConecta.application.plataforma.dto.ResumoPlataformaResponse;
import br.com.saudeConecta.application.plataforma.dto.SerieMensalResponse;
import br.com.saudeConecta.application.plataforma.dto.StatusQuantidadeResponse;
import br.com.saudeConecta.domain.planos.StatusAssinatura;
import br.com.saudeConecta.domain.planos.TipoPlano;
import br.com.saudeConecta.infrastructure.persistence.projection.PlataformaProjecoes;
import br.com.saudeConecta.infrastructure.persistence.repository.AssinaturaTenantRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.CobrancaTenantRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ConsultaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.MpWebhookEventoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Monta o painel agregado da plataforma para o SUPER_ADMIN. É visão global: nenhuma query filtra
 * por tenant e nada pessoal sai na resposta. Toda soma e contagem é feita no banco; o Java só
 * monta a lista de meses e distribui o resultado agrupado.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlataformaDashboardService {

    public static final int MESES_MINIMO = 1;
    public static final int MESES_MAXIMO = 24;
    static final String CODIGO_MESES_INVALIDO = "MESES_INVALIDO";

    private static final int LIMITE_TOP_CLINICAS = 10;
    private static final int LIMITE_ATENCAO = 20;
    private static final int DIAS_TRIAL_VENCENDO = 7;
    private static final List<StatusAssinatura> ORDEM_ATENCAO = List.of(StatusAssinatura.SUSPENSA,
            StatusAssinatura.INADIMPLENTE);
    private static final String MOTIVO_PENDENCIA_MANUAL = "PENDENCIA_MANUAL";
    private static final String MOTIVO_TRIAL_VENCENDO = "TRIAL_VENCENDO";

    private final AssinaturaTenantRepository assinaturaRepository;
    private final CobrancaTenantRepository cobrancaRepository;
    private final ConsultaRepository consultaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProfissionalRepository profissionalRepository;
    private final MpWebhookEventoRepository webhookEventoRepository;
    private final Clock clock;

    /**
     * Gera o dashboard da plataforma (cache de 5 min por quantidade de meses).
     *
     * @param meses janela das séries mensais, de 1 a 24, terminando no mês atual
     * @return painel agregado
     * @throws BusinessException 422 quando {@code meses} está fora de 1..24
     */
    @Cacheable(value = "plataforma-dashboard", key = "#meses")
    @Transactional(readOnly = true)
    public DashboardPlataformaResponse gerar(int meses) {
        if (meses < MESES_MINIMO || meses > MESES_MAXIMO) {
            log.warn("plataforma.dashboard negado motivo={} meses={}", CODIGO_MESES_INVALIDO, meses);
            throw new BusinessException("O parametro meses deve estar entre " + MESES_MINIMO + " e " + MESES_MAXIMO,
                    HttpStatus.UNPROCESSABLE_ENTITY, CODIGO_MESES_INVALIDO);
        }
        long inicioNs = System.nanoTime();

        LocalDateTime agora = LocalDateTime.now(clock);
        LocalDate hoje = agora.toLocalDate();
        YearMonth mesAtual = YearMonth.from(hoje);
        YearMonth mesAnterior = mesAtual.minusMonths(1);
        LocalDateTime inicioMesAtual = mesAtual.atDay(1).atStartOfDay();
        LocalDateTime inicioProximoMes = mesAtual.plusMonths(1).atDay(1).atStartOfDay();
        LocalDateTime inicioMesAnterior = mesAnterior.atDay(1).atStartOfDay();

        List<StatusQuantidadeResponse> porStatus = montarPorStatus();
        Map<StatusAssinatura, Long> qtdPorStatus = porStatus.stream()
                .collect(Collectors.toMap(StatusQuantidadeResponse::status, StatusQuantidadeResponse::quantidade));
        List<PlataformaProjecoes.MrrPorPlano> mrrPorPlano = assinaturaRepository.somarMrrPorPlano();
        List<PlanoResumoResponse> porPlano = montarPorPlano(mrrPorPlano);
        BigDecimal mrr = porPlano.stream().map(PlanoResumoResponse::mrr).reduce(BigDecimal.ZERO, BigDecimal::add);
        long pagantes = mrrPorPlano.stream().mapToLong(PlataformaProjecoes.MrrPorPlano::getQuantidade).sum();
        List<PerfilQuantidadeResponse> usuariosPorPerfil = usuarioRepository.contarAtivosPorPerfil().stream()
                .map(p -> new PerfilQuantidadeResponse(p.getPerfil(), p.getQuantidade()))
                .toList();

        long clinicasTotal = porStatus.stream().mapToLong(StatusQuantidadeResponse::quantidade).sum();
        long emTrial = qtdPorStatus.get(StatusAssinatura.TRIAL);
        long inadimplentes = qtdPorStatus.get(StatusAssinatura.INADIMPLENTE);
        ResumoPlataformaResponse resumo = new ResumoPlataformaResponse(
                clinicasTotal,
                emTrial + qtdPorStatus.get(StatusAssinatura.ATIVA) + inadimplentes,
                emTrial,
                assinaturaRepository.contarTrialsVencendo(hoje, hoje.plusDays(DIAS_TRIAL_VENCENDO)),
                inadimplentes,
                qtdPorStatus.get(StatusAssinatura.SUSPENSA),
                qtdPorStatus.get(StatusAssinatura.CANCELADA),
                assinaturaRepository.contarCortesiasAtivas(),
                dinheiro(mrr),
                dinheiro(cobrancaRepository.somarRecebidoNoPeriodo(inicioMesAtual, inicioProximoMes)),
                dinheiro(cobrancaRepository.somarRecebidoNoPeriodo(inicioMesAnterior, inicioMesAtual)),
                pagantes == 0 ? dinheiro(null) : mrr.divide(BigDecimal.valueOf(pagantes), 2, RoundingMode.HALF_UP),
                webhookEventoRepository.countByRequerAtencaoTrue(),
                usuariosPorPerfil.stream().mapToLong(PerfilQuantidadeResponse::quantidade).sum(),
                consultaRepository.contarNaoCanceladasNoPeriodo(inicioMesAtual, inicioProximoMes));

        DashboardPlataformaResponse resposta = new DashboardPlataformaResponse(
                agora, resumo, porStatus, porPlano,
                montarSerieMensal(mesAtual, meses, inicioProximoMes),
                usuariosPorPerfil,
                montarTopClinicas(inicioMesAtual, inicioProximoMes),
                montarAtencao(hoje));

        log.debug("plataforma.dashboard ok meses={} clinicas={} duracaoMs={}", meses, clinicasTotal,
                (System.nanoTime() - inicioNs) / 1_000_000);
        return resposta;
    }

    /** Todos os status, inclusive os sem nenhuma clínica, na ordem do enum. */
    private List<StatusQuantidadeResponse> montarPorStatus() {
        Map<StatusAssinatura, Long> contagem = new EnumMap<>(StatusAssinatura.class);
        assinaturaRepository.contarVigentesPorStatus().forEach(c -> contagem.put(c.getStatus(), c.getQuantidade()));
        return Arrays.stream(StatusAssinatura.values())
                .map(s -> new StatusQuantidadeResponse(s, contagem.getOrDefault(s, 0L)))
                .toList();
    }

    /** Todos os planos base, com clínicas ativas e MRR (sem cortesia). */
    private List<PlanoResumoResponse> montarPorPlano(List<PlataformaProjecoes.MrrPorPlano> mrrPorPlano) {
        Map<TipoPlano, Long> ativas = new EnumMap<>(TipoPlano.class);
        assinaturaRepository.contarAtivasPorPlano().forEach(c -> ativas.put(c.getPlano(), c.getQuantidade()));
        Map<TipoPlano, BigDecimal> mrr = new EnumMap<>(TipoPlano.class);
        mrrPorPlano.forEach(m -> mrr.put(m.getPlano(), m.getMrr()));
        return Arrays.stream(TipoPlano.values())
                .map(p -> new PlanoResumoResponse(p, ativas.getOrDefault(p, 0L), dinheiro(mrr.get(p))))
                .toList();
    }

    /** Lista os meses no Java e preenche com o agrupado do banco; mês sem dado fica com 0. */
    private List<SerieMensalResponse> montarSerieMensal(YearMonth mesAtual, int meses,
                                                         LocalDateTime inicioProximoMes) {
        YearMonth primeiro = mesAtual.minusMonths(meses - 1L);
        LocalDateTime inicio = primeiro.atDay(1).atStartOfDay();

        Map<YearMonth, Long> novas = contagemPorMes(assinaturaRepository.contarCriadasPorMes(inicio, inicioProximoMes));
        Map<YearMonth, Long> canceladas = contagemPorMes(
                assinaturaRepository.contarCanceladasPorMes(inicio, inicioProximoMes));
        Map<YearMonth, Long> consultas = contagemPorMes(
                consultaRepository.contarNaoCanceladasPorMes(inicio, inicioProximoMes));
        Map<YearMonth, BigDecimal> receita = new LinkedHashMap<>();
        cobrancaRepository.somarRecebidoPorMes(inicio, inicioProximoMes)
                .forEach(v -> receita.put(YearMonth.of(v.getAno(), v.getMes()), v.getValor()));

        List<SerieMensalResponse> serie = new ArrayList<>(meses);
        for (YearMonth mes = primeiro; !mes.isAfter(mesAtual); mes = mes.plusMonths(1)) {
            serie.add(new SerieMensalResponse(mes.toString(), novas.getOrDefault(mes, 0L),
                    canceladas.getOrDefault(mes, 0L), dinheiro(receita.get(mes)), consultas.getOrDefault(mes, 0L)));
        }
        return serie;
    }

    private Map<YearMonth, Long> contagemPorMes(List<PlataformaProjecoes.ContagemMensal> linhas) {
        return linhas.stream().collect(Collectors.toMap(
                l -> YearMonth.of(l.getAno(), l.getMes()), PlataformaProjecoes.ContagemMensal::getQuantidade));
    }

    private List<ClinicaUsoResponse> montarTopClinicas(LocalDateTime inicioMes, LocalDateTime inicioProximoMes) {
        List<PlataformaProjecoes.ConsultasPorClinica> top = consultaRepository.buscarTopClinicasPorConsultas(
                inicioMes, inicioProximoMes, PageRequest.ofSize(LIMITE_TOP_CLINICAS));
        if (top.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> profissionais = profissionalRepository.contarAtivosPorOrganizacao(
                        top.stream().map(PlataformaProjecoes.ConsultasPorClinica::getOrganizacaoId).toList())
                .stream().collect(Collectors.toMap(PlataformaProjecoes.ProfissionaisPorClinica::getOrganizacaoId,
                        PlataformaProjecoes.ProfissionaisPorClinica::getQuantidade));
        return top.stream()
                .map(c -> new ClinicaUsoResponse(c.getOrganizacaoId(), c.getNome(), c.getPlano(), c.getStatus(),
                        c.getConsultasMes(), profissionais.getOrDefault(c.getOrganizacaoId(), 0L)))
                .toList();
    }

    /** Até 20 itens: PENDENCIA_MANUAL, SUSPENSA, INADIMPLENTE e TRIAL_VENCENDO, nessa ordem. */
    private List<AtencaoResponse> montarAtencao(LocalDate hoje) {
        List<AtencaoResponse> itens = new ArrayList<>();
        webhookEventoRepository.buscarPendentesManuais(PageRequest.ofSize(LIMITE_ATENCAO)).forEach(e ->
                itens.add(new AtencaoResponse(null, "Evento Mercado Pago #" + e.getId(), null, null,
                        MOTIVO_PENDENCIA_MANUAL, e.getRecebidoEm().toLocalDate(), null)));
        for (StatusAssinatura status : ORDEM_ATENCAO) {
            int restante = LIMITE_ATENCAO - itens.size();
            if (restante > 0) {
                assinaturaRepository.buscarAtencaoPorStatus(status, PageRequest.ofSize(restante))
                        .forEach(c -> itens.add(paraAtencao(c, status.name())));
            }
        }
        int restante = LIMITE_ATENCAO - itens.size();
        if (restante > 0) {
            assinaturaRepository.buscarTrialsVencendo(hoje, hoje.plusDays(DIAS_TRIAL_VENCENDO),
                            PageRequest.ofSize(restante))
                    .forEach(c -> itens.add(paraAtencao(c, MOTIVO_TRIAL_VENCENDO)));
        }
        return itens;
    }

    private AtencaoResponse paraAtencao(PlataformaProjecoes.ClinicaAtencao c, String motivo) {
        return new AtencaoResponse(c.getOrganizacaoId(), c.getNome(), c.getPlano(), c.getStatus(), motivo,
                c.getDataVencimento(), dinheiro(c.getValorMensal()));
    }

    /** Valor em reais com 2 casas; ausência vira 0,00. */
    private BigDecimal dinheiro(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).setScale(2, RoundingMode.HALF_UP);
    }
}
