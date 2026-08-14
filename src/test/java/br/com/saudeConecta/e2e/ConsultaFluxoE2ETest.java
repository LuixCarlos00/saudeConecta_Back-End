package br.com.saudeConecta.e2e;

import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.consulta.StatusConsulta;
import br.com.saudeConecta.presentation.dto.consulta.AgendarConsultaRequest;
import br.com.saudeConecta.service.ConsultaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes ponta a ponta do fluxo completo de uma consulta:
 * Agendar → Confirmar → Realizar → Pagar
 *
 * Simula o ciclo de vida completo de uma consulta no sistema,
 * validando as transições de status e respostas HTTP de cada etapa.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("E2E - Fluxo completo de Consulta: Agendar → Confirmar → Realizar → Pagar")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConsultaFluxoE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConsultaService consultaService;

    private ObjectMapper objectMapper;

    private static final Long CONSULTA_ID = 1L;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    private Consulta buildConsultaMock(StatusConsulta status) {
        return Consulta.builder()
                .id(CONSULTA_ID)
                .status(status)
                .duracaoMinutos(30)
                .dataHora(LocalDateTime.now().plusDays(1))
                .build();
    }

    @Test
    @Order(1)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Passo 1 - Agendar consulta")
    void passo1_agendarConsulta() throws Exception {
        AgendarConsultaRequest request = new AgendarConsultaRequest(
                1L, 1L, null,
                LocalDateTime.now().plusDays(1),
                30, "Consulta de rotina", null, BigDecimal.valueOf(200)
        );

        when(consultaService.cadastrarConsultaByOrg(any())).thenReturn(buildConsultaMock(StatusConsulta.AGENDADA));

        mockMvc.perform(post("/consultas/cadastrarConsultaByOrg")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AGENDADA"));

        verify(consultaService).cadastrarConsultaByOrg(any());
    }

    @Test
    @Order(2)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Passo 2 - Confirmar consulta agendada")
    void passo2_confirmarConsulta() throws Exception {
        when(consultaService.atualizarStatus(eq(CONSULTA_ID), eq(StatusConsulta.CONFIRMADA), isNull()))
                .thenReturn(buildConsultaMock(StatusConsulta.CONFIRMADA));

        mockMvc.perform(patch("/consultas/{id}/status", CONSULTA_ID)
                        .with(csrf())
                        .param("status", "CONFIRMADA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMADA"));
    }

    @Test
    @Order(3)
    @WithMockUser(roles = "PROFISSIONAL")
    @DisplayName("Passo 3 - Realizar (concluir) consulta confirmada")
    void passo3_realizarConsulta() throws Exception {
        when(consultaService.concluirConsultabyOrg(CONSULTA_ID))
                .thenReturn(buildConsultaMock(StatusConsulta.REALIZADA));

        mockMvc.perform(put("/consultas/concluirConsultabyOrg/{id}", CONSULTA_ID)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REALIZADA"));

        verify(consultaService).concluirConsultabyOrg(CONSULTA_ID);
    }

    @Test
    @Order(4)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Passo 4 - Registrar pagamento da consulta realizada")
    void passo4_pagarConsulta() throws Exception {
        when(consultaService.atualizarStatus(eq(CONSULTA_ID), eq(StatusConsulta.PAGO), isNull()))
                .thenReturn(buildConsultaMock(StatusConsulta.PAGO));

        mockMvc.perform(patch("/consultas/{id}/status", CONSULTA_ID)
                        .with(csrf())
                        .param("status", "PAGO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAGO"));
    }

    @Test
    @Order(5)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Passo 5 - Tentativa de cancelar consulta PAGA deve retornar 422 (Unprocessable Entity)")
    void passo5_naoDeveCancelarConsultaPaga() throws Exception {
        when(consultaService.atualizarStatus(eq(CONSULTA_ID), eq(StatusConsulta.CANCELADA), eq("Tentativa inválida")))
                .thenThrow(new IllegalStateException("não podem ter o status alterado"));

        mockMvc.perform(patch("/consultas/{id}/status", CONSULTA_ID)
                        .with(csrf())
                        .param("status", "CANCELADA")
                        .param("motivo", "Tentativa inválida"))
                .andExpect(status().isUnprocessableEntity());
    }
}
