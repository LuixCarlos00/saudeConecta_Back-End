package br.com.saudeConecta.e2e;

import br.com.saudeConecta.application.assinatura.MercadoPagoWebhookService;
import br.com.saudeConecta.application.cobranca.CobrancaScheduler;
import br.com.saudeConecta.application.exception.BusinessException;
import br.com.saudeConecta.application.notificacao.EmailNotificacaoService;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.organizacao.TipoOrganizacao;
import br.com.saudeConecta.domain.planos.*;
import br.com.saudeConecta.domain.usuario.StatusUsuario;
import br.com.saudeConecta.domain.usuario.TipoUsuarioNovo;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infrastructure.mercadopago.MercadoPagoClient;
import br.com.saudeConecta.infrastructure.mercadopago.dto.AuthorizedPaymentResponse;
import br.com.saudeConecta.infrastructure.mercadopago.dto.PreapprovalResponse;
import br.com.saudeConecta.infrastructure.persistence.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ponta a ponta do pagamento da assinatura, do login ao webhook, com banco real (H2),
 * segurança real (JWT + FiltroAcesso) e transações reais. Só o transporte HTTP do
 * Mercado Pago (MercadoPagoClient) e o e-mail são simulados.
 */
// Banco H2 próprio: o testdb compartilhado é recriado/derrubado (create-drop) por outros contextos da suíte
@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.datasource.url=jdbc:h2:mem:pagamentoe2e;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL;NON_KEYWORDS=VALUE"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("E2E - Pagamento da assinatura: login, link e fatura")
class PagamentoAssinaturaFluxoE2ETest {

    private static final String SENHA = "Senha@123";
    private static final String PAGADOR = "pagador@clinica-pagamento.com";

    @MockBean private MercadoPagoClient mercadoPagoClient;
    @MockBean private EmailNotificacaoService emailNotificacaoService;

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private MercadoPagoWebhookService webhookService;
    @Autowired private AssinaturaTenantRepository assinaturaTenantRepository;
    @Autowired private CobrancaTenantRepository cobrancaTenantRepository;
    @Autowired private MpWebhookEventoRepository eventoRepository;
    @Autowired private OrganizacaoRepository organizacaoRepository;
    @Autowired private PlanoAssinaturaRepository planoAssinaturaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CobrancaScheduler cobrancaScheduler;
    @Autowired private br.com.saudeConecta.application.assinatura.AssinaturaTenantService assinaturaTenantService;
    @Autowired private br.com.saudeConecta.application.assinatura.AssinaturaMercadoPagoService assinaturaMercadoPagoService;
    @Autowired private br.com.saudeConecta.application.assinatura.LinkPagamentoAssinaturaService linkPagamentoAssinaturaService;

    private Organizacao organizacao;
    private PlanoAssinatura plano;
    private Usuario gestor;

    @BeforeEach
    void setUp() {
        organizacao = new Organizacao();
        organizacao.setNome("Clínica E2E Pagamento");
        organizacao.setTipo(TipoOrganizacao.CLINICA);
        organizacao = organizacaoRepository.save(organizacao);

        plano = new PlanoAssinatura();
        plano.setNome("Profissional E2E Pagamento");
        plano.setTipo(TipoPlano.PROFISSIONAL);
        plano.setValorMensal(BigDecimal.valueOf(279));
        plano.setAtivo(true);
        plano = planoAssinaturaRepository.save(plano);

        gestor = usuarioRepository.save(Usuario.builder()
                .login("e2e-pagamento-" + System.nanoTime())
                .senha(passwordEncoder.encode(SENHA))
                .tipoUsuarioNovo(TipoUsuarioNovo.GESTOR)
                .status(StatusUsuario.ATIVO)
                .organizacao(organizacao)
                .build());
    }

    @AfterEach
    void limpar() {
        cobrancaTenantRepository.deleteAll(cobrancaTenantRepository.findAll().stream()
                .filter(c -> c.getOrganizacao().getId().equals(organizacao.getId())).toList());
        eventoRepository.deleteAll();
        assinaturaTenantRepository.deleteAll(assinaturaTenantRepository.findByOrganizacaoId(organizacao.getId()));
        usuarioRepository.delete(gestor);
        organizacaoRepository.delete(organizacao);
        planoAssinaturaRepository.delete(plano);
    }

    private AssinaturaTenant salvarAssinatura(StatusAssinatura status, String preapprovalId, String mpStatus,
                                              LocalDate vencimento) {
        return assinaturaTenantRepository.save(AssinaturaTenant.builder()
                .organizacao(organizacao)
                .planoAssinatura(plano)
                .status(status)
                .dataInicio(LocalDate.now().minusMonths(3))
                .dataVencimento(vencimento)
                .dataProximaCobranca(vencimento)
                .valorMensal(BigDecimal.valueOf(279))
                .qtdAdminExtra(0)
                .qtdProfissionalExtra(0)
                .qtdSecretariaExtra(0)
                .valorAdicionalPerfis(BigDecimal.ZERO)
                .mpPreapprovalId(preapprovalId)
                .mpStatus(mpStatus)
                .mpPayerEmail(PAGADOR)
                .build());
    }

    private AssinaturaTenant recarregar(AssinaturaTenant assinatura) {
        return assinaturaTenantRepository.findById(assinatura.getId()).orElseThrow();
    }

    private String login() throws Exception {
        String corpo = mockMvc.perform(post("/Home/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"" + gestor.getLogin() + "\",\"senha\":\"" + SENHA
                                + "\",\"perfil\":\"GESTOR\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(corpo);
        return "Bearer " + json.get("token").asText();
    }

    private static AuthorizedPaymentResponse faturaAprovada(String faturaId, String preapprovalId) {
        return new AuthorizedPaymentResponse(faturaId, preapprovalId, "processed", BigDecimal.valueOf(279),
                new AuthorizedPaymentResponse.Payment(91L, "approved", "accredited"));
    }

    @Test
    @DisplayName("C1 - SUSPENSA faz login, vê a assinatura, gera o link, paga e volta a ATIVA")
    void suspensaConsegueVoltarAPagar() throws Exception {
        AssinaturaTenant suspensa = salvarAssinatura(StatusAssinatura.SUSPENSA, null, null,
                LocalDate.now().minusDays(20));
        suspensa.setDataSuspensao(LocalDateTime.now().minusDays(5));
        suspensa = assinaturaTenantRepository.save(suspensa);
        String referencia = "ASSINATURA-" + suspensa.getId();
        when(mercadoPagoClient.criarPreapprovalPendente(anyString(), eq(referencia), eq(PAGADOR), any()))
                .thenReturn(new PreapprovalResponse("pa-c1-novo", "pending", "https://mp/checkout/c1",
                        PAGADOR, referencia, null, null));
        when(mercadoPagoClient.buscarAuthorizedPayment("fat-c1"))
                .thenReturn(faturaAprovada("fat-c1", "pa-c1-novo"));

        // 1) Login devolve o token mesmo com a assinatura suspensa
        String token = login();

        // 2) Fora das rotas de pagamento, o token continua barrado
        mockMvc.perform(put("/assinaturas/trocar-plano/" + plano.getId()).header("Authorization", token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PLANO_SEM_ACESSO"));

        // 3) Minha assinatura mostra a SUSPENSA
        mockMvc.perform(get("/assinaturas/minha-assinatura").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENSA"));

        // 4) Gera o link de pagamento
        mockMvc.perform(post("/assinaturas/minha-assinatura/gerar-link-pagamento").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.initPoint").value("https://mp/checkout/c1"));
        assertThat(recarregar(suspensa).getMpPreapprovalId()).isEqualTo("pa-c1-novo");

        // 5) A fatura do preapproval novo é aprovada: a assinatura volta a ATIVA, com vencimento no futuro
        webhookService.processar(MercadoPagoWebhookService.TOPICO_AUTHORIZED_PAYMENT, "fat-c1", "{}");

        AssinaturaTenant paga = recarregar(suspensa);
        assertThat(paga.getStatus()).isEqualTo(StatusAssinatura.ATIVA);
        assertThat(paga.getDataSuspensao()).isNull();
        assertThat(paga.getDataVencimento()).isAfter(LocalDate.now());

        // 6) Já com acesso, as demais rotas voltam a funcionar
        mockMvc.perform(get("/assinaturas/minha-assinatura").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATIVA"));
    }

    @Test
    @DisplayName("C1 - Token de clínica SUSPENSA: pacientes, prontuário e rota do profissional devolvem 403 PLANO_SEM_ACESSO")
    void suspensaSoAcessaRotasDePagamento() throws Exception {
        AssinaturaTenant suspensa = salvarAssinatura(StatusAssinatura.SUSPENSA, null, null, LocalDate.now().minusDays(20));
        suspensa.setDataSuspensao(LocalDateTime.now().minusDays(5));
        assinaturaTenantRepository.save(suspensa);
        Usuario profissional = usuarioRepository.save(Usuario.builder()
                .login("e2e-pagamento-prof-" + System.nanoTime())
                .senha(passwordEncoder.encode(SENHA))
                .tipoUsuarioNovo(TipoUsuarioNovo.CLINICO)
                .status(StatusUsuario.ATIVO)
                .organizacao(organizacao)
                .build());
        try {
            String tokenGestor = login();

            mockMvc.perform(get("/pacientes/buscar-por-nome").param("nome", "Maria").header("Authorization", tokenGestor))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("PLANO_SEM_ACESSO"));
            mockMvc.perform(get("/prontuario/buscarProntuarioById/1").header("Authorization", tokenGestor))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("PLANO_SEM_ACESSO"));

            String corpo = mockMvc.perform(post("/Home/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"login\":\"" + profissional.getLogin() + "\",\"senha\":\"" + SENHA
                                    + "\",\"perfil\":\"CLINICO\"}"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            String tokenProfissional = "Bearer " + objectMapper.readTree(corpo).get("token").asText();

            mockMvc.perform(get("/consultas/hoje").header("Authorization", tokenProfissional))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("PLANO_SEM_ACESSO"));
            mockMvc.perform(get("/prontuario/consulta/1/recente").header("Authorization", tokenProfissional))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("PLANO_SEM_ACESSO"));
        } finally {
            usuarioRepository.delete(profissional);
        }
    }

    @Test
    @DisplayName("C1 - CANCELADA continua sem token no login (403)")
    void canceladaNaoRecebeToken() throws Exception {
        salvarAssinatura(StatusAssinatura.CANCELADA, null, null, LocalDate.now().minusDays(60));

        mockMvc.perform(post("/Home/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"" + gestor.getLogin() + "\",\"senha\":\"" + SENHA
                                + "\",\"perfil\":\"GESTOR\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("C1 - Com histórico, a vigente é a mais recente não cancelada")
    void vigenteEhAMaisRecenteNaoCancelada() {
        salvarAssinatura(StatusAssinatura.CANCELADA, null, null, LocalDate.now().minusDays(90));
        AssinaturaTenant antiga = salvarAssinatura(StatusAssinatura.SUSPENSA, null, null, LocalDate.now().minusDays(40));
        AssinaturaTenant atual = salvarAssinatura(StatusAssinatura.TRIAL, null, null, LocalDate.now().plusDays(20));

        assertThat(assinaturaTenantRepository.findAssinaturaVigenteByOrganizacaoId(organizacao.getId()))
                .get().extracting(AssinaturaTenant::getId).isEqualTo(atual.getId());
        assertThat(antiga.getId()).isLessThan(atual.getId());
    }

    @Test
    @DisplayName("R6 - SUSPENSA antiga com CANCELADA mais recente: sem vigente e o login é negado (403)")
    void suspensaAntigaComCanceladaMaisRecenteNaoEhVigente() throws Exception {
        AssinaturaTenant suspensa = salvarAssinatura(StatusAssinatura.SUSPENSA, null, null, LocalDate.now().minusDays(40));
        AssinaturaTenant cancelada = salvarAssinatura(StatusAssinatura.CANCELADA, null, null, LocalDate.now().minusDays(10));
        assertThat(suspensa.getId()).isLessThan(cancelada.getId());

        assertThat(assinaturaTenantRepository.findAssinaturaVigenteByOrganizacaoId(organizacao.getId())).isEmpty();
        mockMvc.perform(post("/Home/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"" + gestor.getLogin() + "\",\"senha\":\"" + SENHA
                                + "\",\"perfil\":\"GESTOR\"}"))
                .andExpect(status().isForbidden());
    }

    private static PreapprovalResponse preapproval(String id, String status, String referencia) {
        return new PreapprovalResponse(id, status, "pending".equals(status) ? "https://mp/checkout/" + id : null,
                PAGADOR, referencia, null, null);
    }

    @Test
    @DisplayName("C3 - Link novo: o antigo só é cancelado depois do commit do novo; o webhook cancelled do antigo é ignorado")
    void antigoCanceladoSoDepoisDoCommit() {
        AssinaturaTenant suspensa = salvarAssinatura(StatusAssinatura.SUSPENSA, "pa-c3-a", "paused",
                LocalDate.now().minusDays(20));
        String referencia = "ASSINATURA-" + suspensa.getId();
        when(mercadoPagoClient.criarPreapprovalPendente(anyString(), eq(referencia), eq(PAGADOR), any()))
                .thenReturn(preapproval("pa-c3-b", "pending", referencia));
        when(mercadoPagoClient.buscarPreapproval("pa-c3-a")).thenReturn(preapproval("pa-c3-a", "paused", referencia));
        AtomicReference<AssinaturaTenant> vistaNoCancelamento = new AtomicReference<>();
        when(mercadoPagoClient.cancelarPreapproval("pa-c3-a")).thenAnswer(inv -> {
            // Lido fora da transação da troca: só enxerga o que já foi commitado
            vistaNoCancelamento.set(recarregar(suspensa));
            return preapproval("pa-c3-a", "cancelled", referencia);
        });

        gerarLink();

        assertThat(vistaNoCancelamento.get()).isNotNull();
        assertThat(vistaNoCancelamento.get().getMpPreapprovalId()).isEqualTo("pa-c3-b");
        assertThat(vistaNoCancelamento.get().getMpPreapprovalSubstituidoId()).isEqualTo("pa-c3-a");
        AssinaturaTenant depois = recarregar(suspensa);
        assertThat(depois.getMpPreapprovalId()).isEqualTo("pa-c3-b");
        assertThat(depois.getMpPreapprovalSubstituidoId()).isNull();

        // Webhook do antigo cancelado chega depois do commit: não mexe na assinatura
        when(mercadoPagoClient.buscarPreapproval("pa-c3-a")).thenReturn(preapproval("pa-c3-a", "cancelled", referencia));
        webhookService.processar(MercadoPagoWebhookService.TOPICO_PREAPPROVAL, "pa-c3-a", "{}");

        AssinaturaTenant aposWebhook = recarregar(suspensa);
        assertThat(aposWebhook.getStatus()).isEqualTo(StatusAssinatura.SUSPENSA);
        assertThat(aposWebhook.getMpPreapprovalId()).isEqualTo("pa-c3-b");
        assertThat(aposWebhook.getCanceladoPor()).isNull();
    }

    @Test
    @DisplayName("C3 - Falha no POST do novo: o antigo segue vinculado, não é cancelado e a assinatura não muda")
    void falhaAoCriarNovoMantemAntigo() {
        AssinaturaTenant suspensa = salvarAssinatura(StatusAssinatura.SUSPENSA, "pa-c3-a", "paused",
                LocalDate.now().minusDays(20));
        Long versao = suspensa.getVersao();
        when(mercadoPagoClient.criarPreapprovalPendente(anyString(), anyString(), anyString(), any()))
                .thenThrow(new BusinessException("MP fora do ar", HttpStatus.BAD_GATEWAY));

        org.assertj.core.api.Assertions.assertThatThrownBy(this::gerarLink)
                .isInstanceOf(BusinessException.class);

        AssinaturaTenant depois = recarregar(suspensa);
        assertThat(depois.getMpPreapprovalId()).isEqualTo("pa-c3-a");
        assertThat(depois.getMpStatus()).isEqualTo("paused");
        assertThat(depois.getMpPreapprovalSubstituidoId()).isNull();
        assertThat(depois.getStatus()).isEqualTo(StatusAssinatura.SUSPENSA);
        assertThat(depois.getVersao()).isEqualTo(versao);
        verify(mercadoPagoClient, never()).cancelarPreapproval(anyString());
    }

    @Test
    @DisplayName("C3 - Falha ao cancelar o antigo: fica pendente, o webhook dele é ignorado e a reconciliação cancela depois")
    void falhaAoCancelarAntigoReconciliacaoTentaDeNovo() {
        AssinaturaTenant suspensa = salvarAssinatura(StatusAssinatura.SUSPENSA, "pa-c3-a", "paused",
                LocalDate.now().minusDays(20));
        String referencia = "ASSINATURA-" + suspensa.getId();
        when(mercadoPagoClient.criarPreapprovalPendente(anyString(), eq(referencia), eq(PAGADOR), any()))
                .thenReturn(preapproval("pa-c3-b", "pending", referencia));
        when(mercadoPagoClient.buscarPreapproval("pa-c3-a")).thenReturn(preapproval("pa-c3-a", "paused", referencia));
        when(mercadoPagoClient.cancelarPreapproval("pa-c3-a"))
                .thenThrow(new BusinessException("MP fora do ar", HttpStatus.BAD_GATEWAY))
                .thenReturn(preapproval("pa-c3-a", "cancelled", referencia));

        gerarLink();

        AssinaturaTenant pendente = recarregar(suspensa);
        assertThat(pendente.getMpPreapprovalId()).isEqualTo("pa-c3-b");
        assertThat(pendente.getMpPreapprovalSubstituidoId()).isEqualTo("pa-c3-a");

        // Um webhook "paused" do antigo nesse meio tempo não mexe na assinatura
        webhookService.processar(MercadoPagoWebhookService.TOPICO_PREAPPROVAL, "pa-c3-a", "{}");
        assertThat(recarregar(suspensa).getMpPreapprovalId()).isEqualTo("pa-c3-b");

        cobrancaScheduler.reconciliarComMercadoPago();

        AssinaturaTenant reconciliada = recarregar(suspensa);
        assertThat(reconciliada.getMpPreapprovalSubstituidoId()).isNull();
        assertThat(reconciliada.getMpPreapprovalId()).isEqualTo("pa-c3-b");
        assertThat(reconciliada.getStatus()).isEqualTo(StatusAssinatura.SUSPENSA);
        verify(mercadoPagoClient, times(2)).cancelarPreapproval("pa-c3-a");
    }

    @Test
    @DisplayName("M1 - Dois cliques simultâneos em Pagar: o FOR UPDATE serializa e só um preapproval é criado")
    void cliquesSimultaneosCriamUmSoPreapproval() throws Exception {
        AssinaturaTenant trial = salvarAssinatura(StatusAssinatura.TRIAL, null, null, LocalDate.now().plusDays(5));
        String referencia = "ASSINATURA-" + trial.getId();
        java.util.concurrent.CountDownLatch primeiroNoMp = new java.util.concurrent.CountDownLatch(1);
        when(mercadoPagoClient.criarPreapprovalPendente(anyString(), eq(referencia), eq(PAGADOR), any()))
                .thenAnswer(inv -> {
                    primeiroNoMp.countDown();
                    Thread.sleep(300);
                    return preapproval("pa-m1", "pending", referencia);
                });

        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var primeiro = pool.submit(() -> linkPagamentoAssinaturaService.gerarLinkPagamento(organizacao.getId()));
            primeiroNoMp.await(5, java.util.concurrent.TimeUnit.SECONDS);
            var segundo = pool.submit(() -> linkPagamentoAssinaturaService.gerarLinkPagamento(organizacao.getId()));

            assertThat(primeiro.get(10, java.util.concurrent.TimeUnit.SECONDS).initPoint())
                    .isEqualTo("https://mp/checkout/pa-m1");
            assertThat(segundo.get(10, java.util.concurrent.TimeUnit.SECONDS).initPoint())
                    .isEqualTo("https://mp/checkout/pa-m1");
        } finally {
            pool.shutdownNow();
        }
        verify(mercadoPagoClient, times(1)).criarPreapprovalPendente(anyString(), anyString(), anyString(), any());
        assertThat(recarregar(trial).getMpPreapprovalId()).isEqualTo("pa-m1");
    }

    @Test
    @DisplayName("M2 - Pagar com fatura aprovada sem webhook: 409 PAGAMENTO_JA_RECEBIDO, assinatura renovada e nenhum link novo")
    void pagamentoJaRecebidoNaoGeraLinkNovo() throws Exception {
        AssinaturaTenant inadimplente = salvarAssinatura(StatusAssinatura.INADIMPLENTE, "pa-m2", "authorized",
                LocalDate.now().minusDays(8));
        when(mercadoPagoClient.buscarAuthorizedPaymentsDoPreapproval("pa-m2"))
                .thenReturn(java.util.List.of(faturaAprovada("fat-m2", "pa-m2")));
        when(mercadoPagoClient.buscarAuthorizedPayment("fat-m2")).thenReturn(faturaAprovada("fat-m2", "pa-m2"));
        String token = login();

        mockMvc.perform(post("/assinaturas/minha-assinatura/gerar-link-pagamento").header("Authorization", token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PAGAMENTO_JA_RECEBIDO"));

        AssinaturaTenant depois = recarregar(inadimplente);
        assertThat(depois.getStatus()).isEqualTo(StatusAssinatura.ATIVA);
        assertThat(depois.getDataVencimento()).isAfter(LocalDate.now());
        assertThat(depois.getMpPreapprovalId()).isEqualTo("pa-m2");
        verify(mercadoPagoClient, never()).criarPreapprovalPendente(anyString(), anyString(), anyString(), any());
        verify(mercadoPagoClient, never()).cancelarPreapproval(anyString());
    }

    private PlanoAssinatura salvarPlanoBusiness() {
        PlanoAssinatura business = new PlanoAssinatura();
        business.setNome("Business E2E Pagamento");
        business.setTipo(TipoPlano.BUSINESS);
        business.setValorMensal(BigDecimal.valueOf(499));
        business.setLimiteProfissional(10);
        business.setValorAdicionalProfissional(BigDecimal.valueOf(40));
        business.setAtivo(true);
        return planoAssinaturaRepository.save(business);
    }

    @Test
    @DisplayName("C2 - Troca de plano com preapproval authorized: MP falha → 502 MP_VALOR_NAO_ATUALIZADO e nada muda; MP ok → valor novo no MP e no banco")
    void trocaDePlanoAtualizaValorNoMp() throws Exception {
        PlanoAssinatura business = salvarPlanoBusiness();
        try {
            AssinaturaTenant ativa = salvarAssinatura(StatusAssinatura.ATIVA, "pa-c2", "authorized",
                    LocalDate.now().plusDays(10));
            ativa.setLimiteProfissionalCustom(12);
            ativa = assinaturaTenantRepository.save(ativa);
            when(mercadoPagoClient.buscarPreapproval("pa-c2")).thenReturn(preapproval("pa-c2", "authorized", null));
            when(mercadoPagoClient.atualizarValor(eq("pa-c2"), any()))
                    .thenThrow(new BusinessException("MP fora do ar", HttpStatus.BAD_GATEWAY))
                    .thenReturn(preapproval("pa-c2", "authorized", null));
            String token = login();

            mockMvc.perform(put("/assinaturas/trocar-plano/" + business.getId()).header("Authorization", token))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.code").value("MP_VALOR_NAO_ATUALIZADO"));

            AssinaturaTenant semMudanca = recarregar(ativa);
            assertThat(semMudanca.getPlanoAssinatura().getId()).isEqualTo(plano.getId());
            assertThat(semMudanca.getValorMensal()).isEqualByComparingTo("279");

            mockMvc.perform(put("/assinaturas/trocar-plano/" + business.getId()).header("Authorization", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valorMensal").value(579));

            // 499 + (12 - 10) x 40: os limites personalizados continuam e são cobrados
            verify(mercadoPagoClient, times(2)).atualizarValor("pa-c2", new BigDecimal("579.00"));
            AssinaturaTenant trocada = recarregar(ativa);
            assertThat(trocada.getValorMensal()).isEqualByComparingTo("579");
            assertThat(trocada.getMpPreapprovalId()).isEqualTo("pa-c2");
        } finally {
            assinaturaTenantRepository.deleteAll(assinaturaTenantRepository.findByOrganizacaoId(organizacao.getId()));
            planoAssinaturaRepository.delete(business);
        }
    }

    @Test
    @DisplayName("C2 - Troca de plano com link pendente: o link antigo é abandonado e cancelado após o commit; o próximo Pagar cobra o valor novo")
    void trocaDePlanoComLinkPendenteAbandonaLink() throws Exception {
        PlanoAssinatura business = salvarPlanoBusiness();
        try {
            AssinaturaTenant trial = salvarAssinatura(StatusAssinatura.TRIAL, "pa-c2-pend", "pending",
                    LocalDate.now().plusDays(5));
            trial.setMpInitPoint("https://mp/checkout/pa-c2-pend");
            trial = assinaturaTenantRepository.save(trial);
            String referencia = "ASSINATURA-" + trial.getId();
            when(mercadoPagoClient.buscarPreapproval("pa-c2-pend"))
                    .thenReturn(preapproval("pa-c2-pend", "pending", referencia));
            AtomicReference<AssinaturaTenant> vistaNoCancelamento = new AtomicReference<>();
            AssinaturaTenant trialFinal = trial;
            when(mercadoPagoClient.cancelarPreapproval("pa-c2-pend")).thenAnswer(inv -> {
                vistaNoCancelamento.set(recarregar(trialFinal));
                return preapproval("pa-c2-pend", "cancelled", referencia);
            });
            when(mercadoPagoClient.criarPreapprovalPendente(anyString(), eq(referencia), eq(PAGADOR), any()))
                    .thenReturn(preapproval("pa-c2-novo", "pending", referencia));
            String token = login();

            mockMvc.perform(put("/assinaturas/trocar-plano/" + business.getId()).header("Authorization", token))
                    .andExpect(status().isOk());

            assertThat(vistaNoCancelamento.get().getMpPreapprovalId()).isNull();
            assertThat(vistaNoCancelamento.get().getValorMensal()).isEqualByComparingTo("499");
            verify(mercadoPagoClient, never()).atualizarValor(anyString(), any());
            AssinaturaTenant depois = recarregar(trial);
            assertThat(depois.getMpPreapprovalId()).isNull();
            assertThat(depois.getMpInitPoint()).isNull();
            assertThat(depois.getMpPreapprovalSubstituidoId()).isNull();

            // Webhook cancelled do link abandonado não cancela a assinatura
            when(mercadoPagoClient.buscarPreapproval("pa-c2-pend"))
                    .thenReturn(preapproval("pa-c2-pend", "cancelled", referencia));
            webhookService.processar(MercadoPagoWebhookService.TOPICO_PREAPPROVAL, "pa-c2-pend", "{}");
            assertThat(recarregar(trial).getStatus()).isEqualTo(StatusAssinatura.TRIAL);

            mockMvc.perform(post("/assinaturas/minha-assinatura/gerar-link-pagamento").header("Authorization", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.initPoint").value("https://mp/checkout/pa-c2-novo"))
                    .andExpect(jsonPath("$.valorMensal").value(499));
            verify(mercadoPagoClient).criarPreapprovalPendente(anyString(), eq(referencia), eq(PAGADOR),
                    eq(new BigDecimal("499.00")));
        } finally {
            assinaturaTenantRepository.deleteAll(assinaturaTenantRepository.findByOrganizacaoId(organizacao.getId()));
            planoAssinaturaRepository.delete(business);
        }
    }

    @Test
    @DisplayName("A2 - Dar cortesia a INADIMPLENTE: ATIVA, preapproval cancelado no MP só após o commit, webhook dele ignorado e reconciliação não reativa")
    void cortesiaParaACobrancaNoMp() {
        AssinaturaTenant inadimplente = salvarAssinatura(StatusAssinatura.INADIMPLENTE, "pa-a2", "paused",
                LocalDate.now().minusDays(8));
        String referencia = "ASSINATURA-" + inadimplente.getId();
        when(mercadoPagoClient.buscarPreapproval("pa-a2")).thenReturn(preapproval("pa-a2", "paused", referencia));
        AtomicReference<AssinaturaTenant> vistaNoCancelamento = new AtomicReference<>();
        when(mercadoPagoClient.cancelarPreapproval("pa-a2")).thenAnswer(inv -> {
            vistaNoCancelamento.set(recarregar(inadimplente));
            return preapproval("pa-a2", "cancelled", referencia);
        });

        assinaturaTenantService.definirCortesia(inadimplente.getId(), true);

        assertThat(vistaNoCancelamento.get().isCortesia()).isTrue();
        assertThat(vistaNoCancelamento.get().getMpPreapprovalId()).isNull();
        AssinaturaTenant cortesia = recarregar(inadimplente);
        assertThat(cortesia.getStatus()).isEqualTo(StatusAssinatura.ATIVA);
        assertThat(cortesia.getMpPreapprovalSubstituidoId()).isNull();

        when(mercadoPagoClient.buscarPreapproval("pa-a2")).thenReturn(preapproval("pa-a2", "cancelled", referencia));
        webhookService.processar(MercadoPagoWebhookService.TOPICO_PREAPPROVAL, "pa-a2", "{}");
        cobrancaScheduler.reconciliarComMercadoPago();

        AssinaturaTenant depois = recarregar(inadimplente);
        assertThat(depois.getStatus()).isEqualTo(StatusAssinatura.ATIVA);
        assertThat(depois.getCanceladoPor()).isNull();
        verify(mercadoPagoClient, never()).reativarPreapproval(anyString());
        verify(mercadoPagoClient, times(1)).cancelarPreapproval("pa-a2");
    }

    @Test
    @DisplayName("R3 - Reconciliação não reativa a cortesia legada: desvincula o preapproval pausado e o cancela no MP")
    void reconciliacaoLimpaCortesiaLegada() {
        AssinaturaTenant legada = salvarAssinatura(StatusAssinatura.ATIVA, "pa-a2-legado", "paused",
                LocalDate.now().plusDays(20));
        legada.setCortesia(true);
        assinaturaTenantRepository.save(legada);
        String referencia = "ASSINATURA-" + legada.getId();
        when(mercadoPagoClient.buscarPreapproval("pa-a2-legado")).thenReturn(preapproval("pa-a2-legado", "paused", referencia));

        cobrancaScheduler.reconciliarComMercadoPago();

        AssinaturaTenant depois = recarregar(legada);
        assertThat(depois.getMpPreapprovalId()).isNull();
        assertThat(depois.getMpPreapprovalSubstituidoId()).isNull();
        assertThat(depois.getStatus()).isEqualTo(StatusAssinatura.ATIVA);
        assertThat(depois.isCortesia()).isTrue();
        verify(mercadoPagoClient).cancelarPreapproval("pa-a2-legado");
        verify(mercadoPagoClient, never()).reativarPreapproval(anyString());
    }

    @Test
    @DisplayName("R3 - Cortesia legada com preapproval authorized: desvincula, não cancela e vira pendência manual")
    void reconciliacaoCortesiaLegadaAutorizadaViraPendencia() {
        AssinaturaTenant legada = salvarAssinatura(StatusAssinatura.ATIVA, "pa-r3-auth", "authorized",
                LocalDate.now().plusDays(20));
        legada.setCortesia(true);
        assinaturaTenantRepository.save(legada);
        String referencia = "ASSINATURA-" + legada.getId();
        when(mercadoPagoClient.buscarPreapproval("pa-r3-auth")).thenReturn(preapproval("pa-r3-auth", "authorized", referencia));

        cobrancaScheduler.reconciliarComMercadoPago();
        // A nova tentativa (próxima reconciliação) não duplica a pendência
        cobrancaScheduler.reconciliarComMercadoPago();

        AssinaturaTenant depois = recarregar(legada);
        assertThat(depois.getMpPreapprovalId()).isNull();
        assertThat(depois.getMpPreapprovalSubstituidoId()).isEqualTo("pa-r3-auth");
        verify(mercadoPagoClient, never()).cancelarPreapproval("pa-r3-auth");
        var pendencias = eventoRepository.findByRequerAtencaoTrueOrderByRecebidoEmDesc();
        assertThat(pendencias).hasSize(1);
        assertThat(pendencias.get(0).getDataId()).isEqualTo("pa-r3-auth");
        assertThat(pendencias.get(0).getMotivoAtencao()).startsWith(MpWebhookEvento.MOTIVO_PREAPPROVAL_SUBSTITUIDO_AUTORIZADO);
        assertThat(pendencias.get(0).getOrganizacaoId()).isEqualTo(legada.getOrganizacao().getId());

        // O webhook do preapproval que saiu continua ignorado: não religa nem mexe na cortesia
        webhookService.processar(MercadoPagoWebhookService.TOPICO_PREAPPROVAL, "pa-r3-auth", "{}");
        assertThat(recarregar(legada).getMpPreapprovalId()).isNull();
    }

    private void gerarLink() {
        linkPagamentoAssinaturaService.gerarLinkPagamento(organizacao.getId());
    }
}
