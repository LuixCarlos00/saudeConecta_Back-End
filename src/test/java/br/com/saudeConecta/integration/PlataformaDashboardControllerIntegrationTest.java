package br.com.saudeConecta.integration;

import br.com.saudeConecta.infrastructure.persistence.repository.AssinaturaTenantRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.CobrancaTenantRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ConsultaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.MpWebhookEventoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GET /plataforma/dashboard com segurança, controller, service real, cache e JSON. Os repositories são
 * mockados (o contexto de teste não cria schema no H2); as queries são provadas em
 * {@link PlataformaDashboardRepositoryTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("PlataformaDashboardController - Testes de integração")
class PlataformaDashboardControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AssinaturaTenantRepository assinaturaRepository;
    @MockBean private CobrancaTenantRepository cobrancaRepository;
    @MockBean private ConsultaRepository consultaRepository;
    @MockBean private UsuarioRepository usuarioRepository;
    @MockBean private ProfissionalRepository profissionalRepository;
    @MockBean private MpWebhookEventoRepository webhookEventoRepository;
    @Autowired private CacheManager cacheManager;

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    @DisplayName("200 para SUPER_ADMIN, com os campos do contrato e todos os status e planos")
    void superAdminRecebeOPainelDoContrato() throws Exception {
        mockMvc.perform(get("/plataforma/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.geradoEm").isString())
                .andExpect(jsonPath("$.resumo.clinicasTotal").isNumber())
                .andExpect(jsonPath("$.resumo.clinicasAtivas").isNumber())
                .andExpect(jsonPath("$.resumo.emTrial").isNumber())
                .andExpect(jsonPath("$.resumo.trialsVencendo7Dias").isNumber())
                .andExpect(jsonPath("$.resumo.inadimplentes").isNumber())
                .andExpect(jsonPath("$.resumo.suspensas").isNumber())
                .andExpect(jsonPath("$.resumo.canceladas").isNumber())
                .andExpect(jsonPath("$.resumo.cortesias").isNumber())
                .andExpect(jsonPath("$.resumo.mrr").isNumber())
                .andExpect(jsonPath("$.resumo.receitaMesAtual").isNumber())
                .andExpect(jsonPath("$.resumo.receitaMesAnterior").isNumber())
                .andExpect(jsonPath("$.resumo.ticketMedio").isNumber())
                .andExpect(jsonPath("$.resumo.pendenciasManuais").isNumber())
                .andExpect(jsonPath("$.resumo.usuariosAtivos").isNumber())
                .andExpect(jsonPath("$.resumo.consultasMesAtual").isNumber())
                .andExpect(jsonPath("$.porStatus", hasSize(5)))
                .andExpect(jsonPath("$.porStatus[0].status").value("TRIAL"))
                .andExpect(jsonPath("$.porStatus[0].quantidade").isNumber())
                .andExpect(jsonPath("$.porPlano", hasSize(3)))
                .andExpect(jsonPath("$.porPlano[0].plano").value("STARTER"))
                .andExpect(jsonPath("$.porPlano[0].mrr").isNumber())
                .andExpect(jsonPath("$.serieMensal", hasSize(12)))
                .andExpect(jsonPath("$.serieMensal[0].mes").isString())
                .andExpect(jsonPath("$.serieMensal[0].novasClinicas").isNumber())
                .andExpect(jsonPath("$.serieMensal[0].cancelamentos").isNumber())
                .andExpect(jsonPath("$.serieMensal[0].receitaRecebida").isNumber())
                .andExpect(jsonPath("$.serieMensal[0].consultas").isNumber())
                .andExpect(jsonPath("$.usuariosPorPerfil").isArray())
                .andExpect(jsonPath("$.topClinicasUso").isArray())
                .andExpect(jsonPath("$.atencao").isArray());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    @DisplayName("meses informado define o tamanho da série e o resultado vai para o cache plataforma-dashboard")
    void mesesDefineASerieEUsaCache() throws Exception {
        mockMvc.perform(get("/plataforma/dashboard").param("meses", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serieMensal", hasSize(3)));

        assertThat(cacheManager.getCache("plataforma-dashboard").get(3)).isNotNull();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("403 para ADMIN de clínica")
    void adminDeClinicaRecebe403() throws Exception {
        mockMvc.perform(get("/plataforma/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("403 sem autenticação")
    void semAutenticacaoRecebe403() throws Exception {
        mockMvc.perform(get("/plataforma/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    @DisplayName("422 com ErrorResponse padrão para meses=0")
    void mesesZeroRecebe422() throws Exception {
        mockMvc.perform(get("/plataforma/dashboard").param("meses", "0"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value("MESES_INVALIDO"))
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    @DisplayName("422 com ErrorResponse padrão para meses=25")
    void mesesVinteECincoRecebe422() throws Exception {
        mockMvc.perform(get("/plataforma/dashboard").param("meses", "25"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("MESES_INVALIDO"));
    }
}
