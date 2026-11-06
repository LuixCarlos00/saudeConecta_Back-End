package br.com.saudeConecta.integration;

import br.com.saudeConecta.application.assinatura.AssinaturaMercadoPagoService;
import br.com.saudeConecta.application.assinatura.AssinaturaTenantService;
import br.com.saudeConecta.application.assinatura.LimitePlanoService;
import br.com.saudeConecta.application.assinatura.LinkPagamentoAssinaturaService;
import br.com.saudeConecta.application.assinatura.PendenciaManualService;
import br.com.saudeConecta.application.assinatura.dto.AssinaturaPlataformaResponse;
import br.com.saudeConecta.application.assinatura.dto.PendenciaManualResponse;
import br.com.saudeConecta.application.cobranca.CobrancaService;
import br.com.saudeConecta.application.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato HTTP das telas de assinatura do SUPER_ADMIN (CONTRATO-ADMIN-ASSINATURAS.md): permissão,
 * parâmetros, corpo e códigos de erro. Os services são mockados; as regras estão nos testes de service.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Assinaturas do SUPER_ADMIN - Testes de integração")
class AssinaturaAdminControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AssinaturaTenantService assinaturaTenantService;
    @MockBean private PendenciaManualService pendenciaManualService;
    @MockBean private AssinaturaMercadoPagoService assinaturaMercadoPagoService;
    @MockBean private LinkPagamentoAssinaturaService linkPagamentoAssinaturaService;
    @MockBean private LimitePlanoService limitePlanoService;
    @MockBean private CobrancaService cobrancaService;

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    static Stream<RequestBuilder> rotasDoSuperAdmin() {
        return Stream.of(
                get("/assinaturas"),
                get("/assinaturas/organizacao/7"),
                post("/assinaturas").with(csrf()).contentType(JSON).content("{\"organizacaoId\":7,\"planoId\":2}"),
                put("/assinaturas/organizacao/7/customizar").with(csrf()).contentType(JSON)
                        .content("{\"limiteProfissionalCustom\":5}"),
                patch("/assinaturas/31/cortesia?cortesia=true").with(csrf()),
                patch("/assinaturas/31/suspender").with(csrf()),
                patch("/assinaturas/31/cancelar").with(csrf()),
                get("/assinaturas/pendencias-manuais"),
                patch("/assinaturas/pendencias-manuais/5/resolver").with(csrf()).contentType(JSON)
                        .content("{\"observacao\":\"Estornado\"}"));
    }

    @ParameterizedTest
    @MethodSource("rotasDoSuperAdmin")
    @WithMockUser(roles = {"ADMIN", "PROFISSIONAL", "RECEPCIONISTA"})
    @DisplayName("Gestor de clínica (ADMIN) recebe 403 em toda rota do SUPER_ADMIN")
    void adminDeClinica403(RequestBuilder rota) throws Exception {
        mockMvc.perform(rota).andExpect(status().isForbidden());
        verifyNoInteractions(assinaturaTenantService, pendenciaManualService);
    }

    @ParameterizedTest
    @MethodSource("rotasDoSuperAdmin")
    @WithMockUser(roles = "SUPER_ADMIN")
    @DisplayName("SUPER_ADMIN recebe 2xx em toda rota da tela")
    void superAdmin2xx(RequestBuilder rota) throws Exception {
        when(assinaturaTenantService.listarParaPlataforma(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));
        mockMvc.perform(rota).andExpect(status().is2xxSuccessful());
    }

    @Nested
    @DisplayName("GET /assinaturas")
    class Listagem {

        @Test
        @WithMockUser(roles = "SUPER_ADMIN")
        @DisplayName("Filtros e paginação chegam ao service; resposta paginada sem dado pessoal")
        void filtrosEPagina() throws Exception {
            var linha = new AssinaturaPlataformaResponse(31L, 7L, "Clínica Alfa", 2L, "Profissional", "PROFISSIONAL",
                    "ATIVA", new BigDecimal("279.00"), false, LocalDate.of(2026, 12, 5), "authorized", true, false);
            when(assinaturaTenantService.listarParaPlataforma(any(), any(), any(), any(), any()))
                    .thenReturn(new PageImpl<>(List.of(linha), PageRequest.of(1, 10), 11));

            mockMvc.perform(get("/assinaturas?status=ATIVA&plano=PROFISSIONAL&cortesia=false&busca=alfa"
                            + "&page=1&size=10&sort=dataVencimento,desc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].assinaturaId").value(31))
                    .andExpect(jsonPath("$.content[0].organizacaoNome").value("Clínica Alfa"))
                    .andExpect(jsonPath("$.content[0].temPreapproval").value(true))
                    .andExpect(jsonPath("$.content[0].dataVencimento").value("2026-12-05"))
                    .andExpect(jsonPath("$.content[0].mpPayerEmail").doesNotExist())
                    .andExpect(jsonPath("$.content[0].mpPreapprovalId").doesNotExist())
                    .andExpect(jsonPath("$.totalElements").value(11))
                    .andExpect(jsonPath("$.totalPages").value(2))
                    .andExpect(jsonPath("$.number").value(1));

            verify(assinaturaTenantService).listarParaPlataforma(
                    eq(br.com.saudeConecta.domain.planos.StatusAssinatura.ATIVA),
                    eq(br.com.saudeConecta.domain.planos.TipoPlano.PROFISSIONAL), eq(false), eq("alfa"),
                    argThat((Pageable p) -> p.getPageNumber() == 1 && p.getPageSize() == 10
                            && p.getSort().getOrderFor("dataVencimento") != null));
        }

        @Test
        @WithMockUser(roles = "SUPER_ADMIN")
        @DisplayName("Sem parâmetros: filtros nulos e página 0 de 20")
        void semParametros() throws Exception {
            when(assinaturaTenantService.listarParaPlataforma(any(), any(), any(), any(), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/assinaturas")).andExpect(status().isOk());

            verify(assinaturaTenantService).listarParaPlataforma(isNull(), isNull(), isNull(), isNull(),
                    argThat((Pageable p) -> p.getPageNumber() == 0 && p.getPageSize() == 20));
        }

        @Test
        @WithMockUser(roles = "SUPER_ADMIN")
        @DisplayName("Status inválido: 400")
        void statusInvalido() throws Exception {
            mockMvc.perform(get("/assinaturas?status=XPTO")).andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "SUPER_ADMIN")
        @DisplayName("Ordenação fora da lista: 400 REQUISICAO_INVALIDA")
        void ordenacaoInvalida() throws Exception {
            when(assinaturaTenantService.listarParaPlataforma(any(), any(), any(), any(), any()))
                    .thenThrow(new IllegalArgumentException("Ordenação não permitida: mpPayerEmail"));

            mockMvc.perform(get("/assinaturas?sort=mpPayerEmail"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("REQUISICAO_INVALIDA"));
        }
    }

    @Nested
    @DisplayName("Pendências manuais")
    class Pendencias {

        private PendenciaManualResponse resolvida() {
            return new PendenciaManualResponse(5L, "subscription_authorized_payment", "fat-9",
                    "PAGAMENTO_EM_CORTESIA", "PAGAMENTO_EM_CORTESIA: assinatura 31", null, 0,
                    LocalDateTime.of(2026, 11, 4, 10, 0), null, 7L, "Clínica Alfa", true,
                    LocalDateTime.of(2026, 11, 5, 15, 30), 1L, "Estornado");
        }

        @Test
        @WithMockUser(roles = "SUPER_ADMIN")
        @DisplayName("?resolvidas=true chega ao service; padrão é false")
        void filtroResolvidas() throws Exception {
            when(pendenciaManualService.listar(true)).thenReturn(List.of(resolvida()));

            mockMvc.perform(get("/assinaturas/pendencias-manuais?resolvidas=true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].motivoCodigo").value("PAGAMENTO_EM_CORTESIA"))
                    .andExpect(jsonPath("$[0].organizacaoNome").value("Clínica Alfa"))
                    .andExpect(jsonPath("$[0].resolvida").value(true));
            mockMvc.perform(get("/assinaturas/pendencias-manuais")).andExpect(status().isOk());
            verify(pendenciaManualService).listar(false);
        }

        @Test
        @WithMockUser(roles = "SUPER_ADMIN")
        @DisplayName("Resolver devolve a pendência resolvida")
        void resolver() throws Exception {
            when(pendenciaManualService.resolver(eq(5L), any(), eq("Estornado"))).thenReturn(resolvida());

            mockMvc.perform(patch("/assinaturas/pendencias-manuais/5/resolver").with(csrf())
                            .contentType(JSON).content("{\"observacao\":\"Estornado\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resolvidoEm").value("2026-11-05T15:30:00"))
                    .andExpect(jsonPath("$.observacaoResolucao").value("Estornado"));
        }

        @Test
        @WithMockUser(roles = "SUPER_ADMIN")
        @DisplayName("Observação vazia ou ausente: 400 sem chamar o service")
        void observacaoObrigatoria() throws Exception {
            for (String corpo : List.of("{\"observacao\":\"  \"}", "{}")) {
                mockMvc.perform(patch("/assinaturas/pendencias-manuais/5/resolver").with(csrf())
                                .contentType(JSON).content(corpo))
                        .andExpect(status().isBadRequest());
            }
            verifyNoInteractions(pendenciaManualService);
        }

        @Test
        @WithMockUser(roles = "SUPER_ADMIN")
        @DisplayName("Já resolvida: 409 PENDENCIA_JA_RESOLVIDA")
        void jaResolvida() throws Exception {
            when(pendenciaManualService.resolver(eq(5L), any(), any())).thenThrow(new BusinessException(
                    "Esta pendência já foi resolvida.", HttpStatus.CONFLICT, PendenciaManualService.CODE_PENDENCIA_JA_RESOLVIDA));

            mockMvc.perform(patch("/assinaturas/pendencias-manuais/5/resolver").with(csrf())
                            .contentType(JSON).content("{\"observacao\":\"x\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("PENDENCIA_JA_RESOLVIDA"));
        }
    }

    @Nested
    @DisplayName("Associar plano, suspender, cancelar e cortesia")
    class Operacoes {

        @Test
        @WithMockUser(roles = "SUPER_ADMIN")
        @DisplayName("POST /assinaturas: troca que o MP não aceitou responde 502 MP_VALOR_NAO_ATUALIZADO")
        void associarPlano502() throws Exception {
            when(assinaturaTenantService.assinar(7L, 2L)).thenThrow(new BusinessException("MP recusou",
                    HttpStatus.BAD_GATEWAY, AssinaturaMercadoPagoService.CODIGO_MP_VALOR_NAO_ATUALIZADO));

            mockMvc.perform(post("/assinaturas").with(csrf()).contentType(JSON)
                            .content("{\"organizacaoId\":7,\"planoId\":2}"))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.code").value("MP_VALOR_NAO_ATUALIZADO"));
        }

        @Test
        @WithMockUser(roles = "SUPER_ADMIN")
        @DisplayName("POST /assinaturas: clínica SUSPENSA responde 409 ASSINATURA_SUSPENSA")
        void associarPlanoSuspensa() throws Exception {
            when(assinaturaTenantService.assinar(7L, 2L)).thenThrow(new BusinessException("suspensa",
                    HttpStatus.CONFLICT, AssinaturaTenantService.CODE_ASSINATURA_SUSPENSA));

            mockMvc.perform(post("/assinaturas").with(csrf()).contentType(JSON)
                            .content("{\"organizacaoId\":7,\"planoId\":2}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("ASSINATURA_SUSPENSA"));
        }

        @Test
        @WithMockUser(roles = "SUPER_ADMIN")
        @DisplayName("Suspender e cancelar usam as operações travadas do SUPER_ADMIN (204)")
        void suspenderECancelar() throws Exception {
            mockMvc.perform(patch("/assinaturas/31/suspender").with(csrf())).andExpect(status().isNoContent());
            mockMvc.perform(patch("/assinaturas/31/cancelar").with(csrf())).andExpect(status().isNoContent());

            verify(assinaturaTenantService).suspenderPeloSuperAdmin(31L);
            verify(assinaturaTenantService).cancelarPeloSuperAdmin(31L);
            verifyNoInteractions(assinaturaMercadoPagoService);
        }

        @Test
        @WithMockUser(roles = "SUPER_ADMIN")
        @DisplayName("Suspender CANCELADA: 409 ASSINATURA_CANCELADA")
        void suspenderCancelada() throws Exception {
            doThrow(new BusinessException("cancelada", HttpStatus.CONFLICT, AssinaturaTenantService.CODE_ASSINATURA_CANCELADA))
                    .when(assinaturaTenantService).suspenderPeloSuperAdmin(31L);

            mockMvc.perform(patch("/assinaturas/31/suspender").with(csrf()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("ASSINATURA_CANCELADA"));
        }

        @Test
        @WithMockUser(roles = "SUPER_ADMIN")
        @DisplayName("Cortesia com preapproval antigo em análise: 409 PREAPPROVAL_SUBSTITUIDO_EM_ANALISE")
        void cortesiaEmAnalise() throws Exception {
            when(assinaturaTenantService.definirCortesia(31L, true)).thenThrow(new BusinessException("análise",
                    HttpStatus.CONFLICT, AssinaturaMercadoPagoService.CODIGO_PREAPPROVAL_SUBSTITUIDO_EM_ANALISE));

            mockMvc.perform(patch("/assinaturas/31/cortesia?cortesia=true").with(csrf()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("PREAPPROVAL_SUBSTITUIDO_EM_ANALISE"));
        }
    }
}
