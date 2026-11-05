package br.com.saudeConecta.e2e;

import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.organizacao.TipoOrganizacao;
import br.com.saudeConecta.domain.planos.*;
import br.com.saudeConecta.domain.usuario.StatusUsuario;
import br.com.saudeConecta.domain.usuario.TipoUsuarioNovo;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.email.EmailNotificacaoService;
import br.com.saudeConecta.infrastructure.persistence.repository.*;
import br.com.saudeConecta.service.mercadopago.MercadoPagoClient;
import br.com.saudeConecta.service.mercadopago.MercadoPagoWebhookService;
import br.com.saudeConecta.service.mercadopago.dto.AuthorizedPaymentResponse;
import br.com.saudeConecta.service.mercadopago.dto.PreapprovalResponse;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
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
@SpringBootTest(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
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
}
