package br.com.saudeConecta.integration;

import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.consulta.StatusConsulta;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.organizacao.TipoOrganizacao;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.domain.planos.AssinaturaTenant;
import br.com.saudeConecta.domain.planos.CobrancaTenant;
import br.com.saudeConecta.domain.planos.MpWebhookEvento;
import br.com.saudeConecta.domain.planos.PlanoAssinatura;
import br.com.saudeConecta.domain.planos.StatusAssinatura;
import br.com.saudeConecta.domain.planos.StatusCobranca;
import br.com.saudeConecta.domain.planos.TipoPlano;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.profissional.StatusProfissional;
import br.com.saudeConecta.domain.profissional.TipoProfissional;
import br.com.saudeConecta.domain.usuario.StatusUsuario;
import br.com.saudeConecta.domain.usuario.TipoUsuarioNovo;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infrastructure.persistence.projection.PlataformaProjecoes;
import br.com.saudeConecta.infrastructure.persistence.repository.AssinaturaTenantRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.CobrancaTenantRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ConsultaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.MpWebhookEventoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prova, contra H2, cada query do dashboard da plataforma com uma massa mínima. As datas são fixas
 * (novembro de 2026): o intervalo de outubro e novembro é passado explicitamente às queries.
 */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
@ActiveProfiles("test")
@DisplayName("Dashboard da plataforma - queries de agregação")
class PlataformaDashboardRepositoryTest {

    private static final LocalDateTime INICIO_OUTUBRO = LocalDateTime.of(2026, 10, 1, 0, 0);
    private static final LocalDateTime INICIO_NOVEMBRO = LocalDateTime.of(2026, 11, 1, 0, 0);
    private static final LocalDateTime INICIO_DEZEMBRO = LocalDateTime.of(2026, 12, 1, 0, 0);

    @Autowired private EntityManager em;
    @Autowired private AssinaturaTenantRepository assinaturaRepository;
    @Autowired private CobrancaTenantRepository cobrancaRepository;
    @Autowired private ConsultaRepository consultaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ProfissionalRepository profissionalRepository;
    @Autowired private MpWebhookEventoRepository webhookRepository;

    private Organizacao alfa;
    private Organizacao beta;
    private Organizacao gama;
    private Organizacao delta;
    private Organizacao zeta;
    private Organizacao theta;
    private PlanoAssinatura starter;
    private PlanoAssinatura profissional;
    private PlanoAssinatura business;

    @BeforeEach
    void montarMassa() {
        em.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
        starter = plano(TipoPlano.STARTER, 149);
        profissional = plano(TipoPlano.PROFISSIONAL, 279);
        business = plano(TipoPlano.BUSINESS, 700);

        alfa = organizacao("Alfa");
        beta = organizacao("Beta");
        gama = organizacao("Gama");
        delta = organizacao("Delta");
        Organizacao epsilon = organizacao("Epsilon");
        zeta = organizacao("Zeta");
        Organizacao eta = organizacao("Eta");
        theta = organizacao("Theta");

        AssinaturaTenant aAlfa = assinatura(alfa, business, StatusAssinatura.ATIVA, 700, LocalDate.of(2026, 11, 20));
        AssinaturaTenant aBeta = assinatura(beta, profissional, StatusAssinatura.INADIMPLENTE, 279,
                LocalDate.of(2026, 10, 28));
        AssinaturaTenant aGama = assinatura(gama, starter, StatusAssinatura.ATIVA, 149, LocalDate.of(2026, 11, 25));
        aGama.setCortesia(true);
        assinatura(delta, starter, StatusAssinatura.TRIAL, 149, LocalDate.of(2026, 11, 8));
        assinatura(epsilon, starter, StatusAssinatura.TRIAL, 149, LocalDate.of(2026, 12, 30));
        assinatura(zeta, starter, StatusAssinatura.SUSPENSA, 149, LocalDate.of(2026, 9, 1));
        // Eta cancelou e assinou de novo: só a mais recente (ATIVA) é a vigente
        AssinaturaTenant etaAntiga = assinatura(eta, profissional, StatusAssinatura.CANCELADA, 279,
                LocalDate.of(2026, 9, 1));
        etaAntiga.setDataCancelamento(LocalDateTime.of(2026, 10, 10, 9, 0));
        AssinaturaTenant etaNova = assinatura(eta, profissional, StatusAssinatura.ATIVA, 279,
                LocalDate.of(2026, 12, 4));
        AssinaturaTenant aTheta = assinatura(theta, starter, StatusAssinatura.CANCELADA, 149,
                LocalDate.of(2026, 8, 1));
        aTheta.setDataCancelamento(LocalDateTime.of(2026, 11, 2, 9, 0));
        em.flush();

        // criado_em é preenchido pelo @PrePersist (relógio real): fixa as datas do cenário
        atualizarCriadoEm(null, LocalDateTime.of(2026, 10, 15, 10, 0));
        atualizarCriadoEm(aAlfa.getId(), LocalDateTime.of(2026, 11, 3, 10, 0));
        atualizarCriadoEm(etaNova.getId(), LocalDateTime.of(2026, 11, 4, 10, 0));
        atualizarCriadoEm(aTheta.getId(), LocalDateTime.of(2026, 8, 20, 10, 0));

        cobranca(aAlfa, 700, StatusCobranca.PAGO, LocalDateTime.of(2026, 11, 2, 12, 0));
        cobranca(aAlfa, 700, StatusCobranca.PAGO, LocalDateTime.of(2026, 9, 30, 23, 59));
        cobranca(aBeta, 279, StatusCobranca.PAGO, LocalDateTime.of(2026, 10, 5, 12, 0));
        cobranca(aBeta, 100, StatusCobranca.PAGO, LocalDateTime.of(2026, 10, 31, 23, 59));
        cobranca(aBeta, 279, StatusCobranca.PENDENTE, null);
        cobranca(aGama, 149, StatusCobranca.RECUSADO, null);

        Profissional profAlfa1 = profissional(alfa, "P1", StatusProfissional.ATIVO);
        profissional(alfa, "P2", StatusProfissional.ATIVO);
        profissional(alfa, "P3", StatusProfissional.INATIVO);
        Profissional profBeta = profissional(beta, "P4", StatusProfissional.ATIVO);
        Profissional profTheta = profissional(theta, "P5", StatusProfissional.ATIVO);
        Profissional profGama = profissional(gama, "P6", StatusProfissional.ATIVO);

        consulta(alfa, profAlfa1, StatusConsulta.AGENDADA, LocalDateTime.of(2026, 11, 4, 9, 0));
        consulta(alfa, profAlfa1, StatusConsulta.CONFIRMADA, LocalDateTime.of(2026, 11, 5, 9, 0));
        consulta(alfa, profAlfa1, StatusConsulta.REALIZADA, LocalDateTime.of(2026, 11, 1, 0, 0));
        consulta(alfa, profAlfa1, StatusConsulta.CANCELADA, LocalDateTime.of(2026, 11, 6, 9, 0));
        consulta(alfa, profAlfa1, StatusConsulta.PAGO, LocalDateTime.of(2026, 10, 31, 23, 30));
        consulta(beta, profBeta, StatusConsulta.AGENDADA, LocalDateTime.of(2026, 11, 7, 9, 0));
        for (int i = 0; i < 5; i++) {
            consulta(theta, profTheta, StatusConsulta.REALIZADA, LocalDateTime.of(2026, 11, 2, 8 + i, 0));
        }
        consulta(gama, profGama, StatusConsulta.AGENDADA, LocalDateTime.of(2026, 10, 12, 9, 0));

        usuario("gestor1", TipoUsuarioNovo.GESTOR, StatusUsuario.ATIVO, alfa);
        usuario("clinico1", TipoUsuarioNovo.CLINICO, StatusUsuario.ATIVO, alfa);
        usuario("clinico2", TipoUsuarioNovo.CLINICO, StatusUsuario.ATIVO, beta);
        usuario("clinico3", TipoUsuarioNovo.CLINICO, StatusUsuario.INATIVO, alfa);
        usuario("assistente1", TipoUsuarioNovo.ASSISTENTE, StatusUsuario.ATIVO, alfa);
        usuario("root1", TipoUsuarioNovo.ROOT, StatusUsuario.ATIVO, null);

        webhook("100", true, LocalDateTime.of(2026, 11, 1, 8, 0));
        webhook("101", true, LocalDateTime.of(2026, 11, 3, 8, 0));
        webhook("102", false, LocalDateTime.of(2026, 11, 4, 8, 0));
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("Clínicas por status usam só a assinatura vigente (a mais recente da clínica)")
    void contaVigentesPorStatus() {
        Map<StatusAssinatura, Long> porStatus = assinaturaRepository.contarVigentesPorStatus().stream()
                .collect(Collectors.toMap(PlataformaProjecoes.ContagemPorStatus::getStatus,
                        PlataformaProjecoes.ContagemPorStatus::getQuantidade));

        // Eta tem uma CANCELADA antiga e uma ATIVA nova: conta só como ATIVA
        assertThat(porStatus).containsOnly(
                Map.entry(StatusAssinatura.ATIVA, 3L),
                Map.entry(StatusAssinatura.INADIMPLENTE, 1L),
                Map.entry(StatusAssinatura.TRIAL, 2L),
                Map.entry(StatusAssinatura.SUSPENSA, 1L),
                Map.entry(StatusAssinatura.CANCELADA, 1L));
    }

    @Test
    @DisplayName("MRR por plano: ATIVA e INADIMPLENTE entram, cortesia e trial ficam de fora")
    void mrrPorPlano() {
        Map<TipoPlano, PlataformaProjecoes.MrrPorPlano> mrr = assinaturaRepository.somarMrrPorPlano().stream()
                .collect(Collectors.toMap(PlataformaProjecoes.MrrPorPlano::getPlano, m -> m));

        assertThat(mrr).containsOnlyKeys(TipoPlano.BUSINESS, TipoPlano.PROFISSIONAL);
        assertThat(mrr.get(TipoPlano.BUSINESS).getMrr()).isEqualByComparingTo("700");
        assertThat(mrr.get(TipoPlano.BUSINESS).getQuantidade()).isEqualTo(1L);
        // Beta (INADIMPLENTE) + Eta (ATIVA nova); a CANCELADA antiga da Eta não conta
        assertThat(mrr.get(TipoPlano.PROFISSIONAL).getMrr()).isEqualByComparingTo("558");
        assertThat(mrr.get(TipoPlano.PROFISSIONAL).getQuantidade()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Clínicas ativas por plano contam TRIAL, ATIVA, INADIMPLENTE e cortesia; e cortesias ativas")
    void ativasPorPlanoECortesias() {
        Map<TipoPlano, Long> ativas = assinaturaRepository.contarAtivasPorPlano().stream()
                .collect(Collectors.toMap(PlataformaProjecoes.ContagemPorPlano::getPlano,
                        PlataformaProjecoes.ContagemPorPlano::getQuantidade));

        // STARTER: Gama (cortesia ATIVA) + Delta e Epsilon (TRIAL); Zeta suspensa e Theta cancelada ficam fora
        assertThat(ativas).containsOnly(Map.entry(TipoPlano.BUSINESS, 1L), Map.entry(TipoPlano.PROFISSIONAL, 2L),
                Map.entry(TipoPlano.STARTER, 3L));
        assertThat(assinaturaRepository.contarCortesiasAtivas()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Trials vencendo: só TRIAL com vencimento dentro da janela, bordas incluídas")
    void trialsVencendo() {
        assertThat(assinaturaRepository.contarTrialsVencendo(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 12)))
                .isEqualTo(1L);
        assertThat(assinaturaRepository.contarTrialsVencendo(LocalDate.of(2026, 11, 8), LocalDate.of(2026, 11, 8)))
                .isEqualTo(1L);
        assertThat(assinaturaRepository.contarTrialsVencendo(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 16)))
                .isZero();
    }

    @Test
    @DisplayName("Novas clínicas e cancelamentos por mês respeitam o intervalo [inicio, fim)")
    void novasECancelamentosPorMes() {
        Map<String, Long> criadas = mapaMensal(assinaturaRepository.contarCriadasPorMes(INICIO_OUTUBRO,
                INICIO_DEZEMBRO));
        Map<String, Long> canceladas = mapaMensal(assinaturaRepository.contarCanceladasPorMes(INICIO_OUTUBRO,
                INICIO_DEZEMBRO));

        // Theta (agosto) fica fora da janela; Alfa e a Eta nova entram em novembro
        assertThat(criadas).containsOnly(Map.entry("2026-10", 6L), Map.entry("2026-11", 2L));
        assertThat(canceladas).containsOnly(Map.entry("2026-10", 1L), Map.entry("2026-11", 1L));
    }

    @Test
    @DisplayName("Receita recebida: só PAGO, pelo mês da data de pagamento, com as bordas certas")
    void receitaRecebida() {
        assertThat(cobrancaRepository.somarRecebidoNoPeriodo(INICIO_NOVEMBRO, INICIO_DEZEMBRO))
                .isEqualByComparingTo("700");
        assertThat(cobrancaRepository.somarRecebidoNoPeriodo(INICIO_OUTUBRO, INICIO_NOVEMBRO))
                .isEqualByComparingTo("379");
        assertThat(cobrancaRepository.somarRecebidoNoPeriodo(LocalDateTime.of(2027, 1, 1, 0, 0),
                LocalDateTime.of(2027, 2, 1, 0, 0))).isNull();

        Map<String, BigDecimal> porMes = cobrancaRepository.somarRecebidoPorMes(
                        LocalDateTime.of(2026, 9, 1, 0, 0), INICIO_DEZEMBRO).stream()
                .collect(Collectors.toMap(v -> "%d-%02d".formatted(v.getAno(), v.getMes()),
                        PlataformaProjecoes.ValorMensal::getValor));
        assertThat(porMes).hasSize(3);
        assertThat(porMes.get("2026-09")).isEqualByComparingTo("700");
        assertThat(porMes.get("2026-10")).isEqualByComparingTo("379");
        assertThat(porMes.get("2026-11")).isEqualByComparingTo("700");
    }

    @Test
    @DisplayName("Consultas do mês e por mês ignoram canceladas e somam todas as clínicas")
    void consultasDoMes() {
        // Novembro: Alfa 3 (a CANCELADA fora) + Beta 1 + Theta 5
        assertThat(consultaRepository.contarNaoCanceladasNoPeriodo(INICIO_NOVEMBRO, INICIO_DEZEMBRO)).isEqualTo(9L);
        Map<String, Long> porMes = mapaMensal(consultaRepository.contarNaoCanceladasPorMes(INICIO_OUTUBRO,
                INICIO_DEZEMBRO));
        assertThat(porMes).containsOnly(Map.entry("2026-10", 2L), Map.entry("2026-11", 9L));
    }

    @Test
    @DisplayName("Top clínicas: só clínicas ativas, por consultas do mês, respeitando o limite")
    void topClinicas() {
        List<PlataformaProjecoes.ConsultasPorClinica> top = consultaRepository.buscarTopClinicasPorConsultas(
                INICIO_NOVEMBRO, INICIO_DEZEMBRO, PageRequest.ofSize(10));

        // Theta tem 5 consultas, mas a assinatura está CANCELADA: não é clínica ativa
        assertThat(top).extracting(PlataformaProjecoes.ConsultasPorClinica::getNome)
                .containsExactly("Alfa", "Beta");
        assertThat(top.get(0).getConsultasMes()).isEqualTo(3L);
        assertThat(top.get(0).getPlano()).isEqualTo(TipoPlano.BUSINESS);
        assertThat(top.get(0).getStatus()).isEqualTo(StatusAssinatura.ATIVA);
        assertThat(top.get(1).getStatus()).isEqualTo(StatusAssinatura.INADIMPLENTE);
        assertThat(consultaRepository.buscarTopClinicasPorConsultas(INICIO_NOVEMBRO, INICIO_DEZEMBRO,
                PageRequest.ofSize(1))).hasSize(1);
    }

    @Test
    @DisplayName("Profissionais ativos por clínica, só das clínicas pedidas")
    void profissionaisAtivos() {
        Map<Long, Long> porClinica = profissionalRepository
                .contarAtivosPorOrganizacao(List.of(alfa.getId(), beta.getId())).stream()
                .collect(Collectors.toMap(PlataformaProjecoes.ProfissionaisPorClinica::getOrganizacaoId,
                        PlataformaProjecoes.ProfissionaisPorClinica::getQuantidade));

        assertThat(porClinica).containsOnly(Map.entry(alfa.getId(), 2L), Map.entry(beta.getId(), 1L));
    }

    @Test
    @DisplayName("Usuários ativos por perfil: bloqueados e ROOT ficam de fora")
    void usuariosPorPerfil() {
        Map<TipoUsuarioNovo, Long> porPerfil = usuarioRepository.contarAtivosPorPerfil().stream()
                .collect(Collectors.toMap(PlataformaProjecoes.ContagemPorPerfil::getPerfil,
                        PlataformaProjecoes.ContagemPorPerfil::getQuantidade));

        assertThat(porPerfil).containsOnly(Map.entry(TipoUsuarioNovo.GESTOR, 1L),
                Map.entry(TipoUsuarioNovo.CLINICO, 2L), Map.entry(TipoUsuarioNovo.ASSISTENTE, 1L));
    }

    @Test
    @DisplayName("Atenção: suspensas, inadimplentes e trials vencendo vêm da assinatura vigente, com limite")
    void listasDeAtencao() {
        List<PlataformaProjecoes.ClinicaAtencao> suspensas = assinaturaRepository.buscarAtencaoPorStatus(
                StatusAssinatura.SUSPENSA, PageRequest.ofSize(20));
        List<PlataformaProjecoes.ClinicaAtencao> inadimplentes = assinaturaRepository.buscarAtencaoPorStatus(
                StatusAssinatura.INADIMPLENTE, PageRequest.ofSize(20));
        List<PlataformaProjecoes.ClinicaAtencao> trials = assinaturaRepository.buscarTrialsVencendo(
                LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 12), PageRequest.ofSize(20));

        assertThat(suspensas).extracting(PlataformaProjecoes.ClinicaAtencao::getNome).containsExactly("Zeta");
        assertThat(inadimplentes).hasSize(1);
        PlataformaProjecoes.ClinicaAtencao clinicaBeta = inadimplentes.get(0);
        assertThat(clinicaBeta.getOrganizacaoId()).isEqualTo(this.beta.getId());
        assertThat(clinicaBeta.getNome()).isEqualTo("Beta");
        assertThat(clinicaBeta.getPlano()).isEqualTo(TipoPlano.PROFISSIONAL);
        assertThat(clinicaBeta.getDataVencimento()).isEqualTo(LocalDate.of(2026, 10, 28));
        assertThat(clinicaBeta.getValorMensal()).isEqualByComparingTo("279");
        assertThat(trials).extracting(PlataformaProjecoes.ClinicaAtencao::getNome).containsExactly("Delta");
        assertThat(assinaturaRepository.buscarAtencaoPorStatus(StatusAssinatura.ATIVA, PageRequest.ofSize(2)))
                .hasSize(2);
    }

    @Test
    @DisplayName("Pendências manuais: conta e lista só requer_atencao, do mais recente, sem payload")
    void pendenciasManuais() {
        assertThat(webhookRepository.countByRequerAtencaoTrue()).isEqualTo(2L);
        List<PlataformaProjecoes.EventoPendente> pendentes = webhookRepository
                .buscarPendentesManuais(PageRequest.ofSize(20));

        assertThat(pendentes).extracting(PlataformaProjecoes.EventoPendente::getRecebidoEm)
                .containsExactly(LocalDateTime.of(2026, 11, 3, 8, 0), LocalDateTime.of(2026, 11, 1, 8, 0));
        assertThat(webhookRepository.buscarPendentesManuais(PageRequest.ofSize(1))).hasSize(1);
    }

    // ========== Helpers ==========

    private Map<String, Long> mapaMensal(List<PlataformaProjecoes.ContagemMensal> linhas) {
        return linhas.stream().collect(Collectors.toMap(l -> "%d-%02d".formatted(l.getAno(), l.getMes()),
                PlataformaProjecoes.ContagemMensal::getQuantidade));
    }

    private PlanoAssinatura plano(TipoPlano tipo, int valor) {
        PlanoAssinatura p = new PlanoAssinatura();
        p.setNome(tipo.name());
        p.setTipo(tipo);
        p.setValorMensal(BigDecimal.valueOf(valor));
        p.setAtivo(true);
        em.persist(p);
        return p;
    }

    private Organizacao organizacao(String nome) {
        Organizacao o = new Organizacao();
        o.setNome(nome);
        o.setTipo(TipoOrganizacao.CLINICA);
        em.persist(o);
        return o;
    }

    private AssinaturaTenant assinatura(Organizacao org, PlanoAssinatura plano, StatusAssinatura status,
                                        int valor, LocalDate vencimento) {
        AssinaturaTenant a = AssinaturaTenant.builder()
                .organizacao(org)
                .planoAssinatura(plano)
                .status(status)
                .dataInicio(LocalDate.of(2026, 1, 1))
                .dataVencimento(vencimento)
                .valorMensal(BigDecimal.valueOf(valor))
                .qtdAdminExtra(0)
                .qtdProfissionalExtra(0)
                .qtdSecretariaExtra(0)
                .valorAdicionalPerfis(BigDecimal.ZERO)
                .build();
        em.persist(a);
        return a;
    }

    private void atualizarCriadoEm(Long assinaturaId, LocalDateTime criadoEm) {
        String filtro = assinaturaId == null ? "" : " WHERE id = " + assinaturaId;
        em.createNativeQuery("UPDATE assinatura_tenant SET criado_em = ?1" + filtro)
                .setParameter(1, criadoEm).executeUpdate();
    }

    private void cobranca(AssinaturaTenant assinatura, int valor, StatusCobranca status, LocalDateTime pagoEm) {
        CobrancaTenant c = CobrancaTenant.builder()
                .assinaturaTenant(assinatura)
                .organizacao(assinatura.getOrganizacao())
                .valorTotal(BigDecimal.valueOf(valor))
                .status(status)
                .dataPagamento(pagoEm)
                .build();
        em.persist(c);
    }

    private Profissional profissional(Organizacao org, String nome, StatusProfissional status) {
        Profissional p = Profissional.builder()
                .organizacao(org)
                .tipoProfissional(em.getReference(TipoProfissional.class, 1L))
                .nome(nome)
                .registroConselho("REG-" + nome)
                .status(status)
                .build();
        em.persist(p);
        return p;
    }

    private void consulta(Organizacao org, Profissional profissional, StatusConsulta status, LocalDateTime dataHora) {
        em.persist(Consulta.builder()
                .organizacao(org)
                .profissional(profissional)
                .paciente(em.getReference(Paciente.class, 1L))
                .dataHora(dataHora)
                .status(status)
                .build());
    }

    private void usuario(String login, TipoUsuarioNovo perfil, StatusUsuario status, Organizacao org) {
        em.persist(Usuario.builder()
                .login(login)
                .senha("hash")
                .tipoUsuarioNovo(perfil)
                .status(status)
                .organizacao(org)
                .build());
    }

    private void webhook(String dataId, boolean requerAtencao, LocalDateTime recebidoEm) {
        em.persist(MpWebhookEvento.builder()
                .tipo("payment")
                .dataId(dataId)
                .recebidoEm(recebidoEm)
                .requerAtencao(requerAtencao)
                .motivoAtencao(requerAtencao ? MpWebhookEvento.MOTIVO_EVENTO_EXPIRADO : null)
                .build());
    }
}
