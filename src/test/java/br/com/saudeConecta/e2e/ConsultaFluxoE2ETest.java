package br.com.saudeConecta.e2e;

import br.com.saudeConecta.application.consulta.ConsultaAgendamentoService;
import br.com.saudeConecta.application.consulta.ConsultaBuscaService;
import br.com.saudeConecta.application.consulta.ConsultaEstatisticaService;
import br.com.saudeConecta.application.consulta.dto.AgendarConsultaRequest;
import br.com.saudeConecta.application.prontuario.ProntuarioService;
import br.com.saudeConecta.application.prontuario.dto.CadastrarProntuarioRequest;
import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.consulta.StatusConsulta;
import br.com.saudeConecta.domain.prontuario.Prontuario;
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
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes ponta a ponta do fluxo completo de uma consulta:
 * Agendar → Confirmar → Realizar (finalizar o prontuário) → Pagar
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
    private ConsultaAgendamentoService consultaAgendamentoService;

    @MockBean
    private ConsultaBuscaService consultaBuscaService;

    @MockBean
    private ConsultaEstatisticaService consultaEstatisticaService;

    @MockBean
    private ProntuarioService prontuarioService;

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

        when(consultaAgendamentoService.cadastrarConsultaByOrg(any())).thenReturn(buildConsultaMock(StatusConsulta.AGENDADA));

        mockMvc.perform(post("/consultas/cadastrarConsultaByOrg")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AGENDADA"));

        verify(consultaAgendamentoService).cadastrarConsultaByOrg(any());
    }

    @Test
    @Order(2)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Passo 2 - Confirmar consulta agendada")
    void passo2_confirmarConsulta() throws Exception {
        when(consultaAgendamentoService.atualizarStatus(eq(CONSULTA_ID), eq(StatusConsulta.CONFIRMADA), isNull()))
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
    @DisplayName("Passo 3 - Realizar: finalizar o prontuário marca a consulta como REALIZADA")
    void passo3_finalizarProntuario() throws Exception {
        // REALIZADA não se define pelo PATCH de status: é o cadastro do prontuário que conclui a consulta
        Consulta realizada = buildConsultaMock(StatusConsulta.REALIZADA);
        Prontuario prontuario = Prontuario.builder().prontCodigoProntuario(10L).consulta(realizada).build();
        when(prontuarioService.cadastrarProntuarioMedico(any())).thenReturn(prontuario);

        CadastrarProntuarioRequest request = new CadastrarProntuarioRequest();
        request.setConsulta(CONSULTA_ID);
        request.setCodigoMedico(1L);
        request.setDataFinalizado(Date.valueOf(LocalDate.now()));
        request.setDiagnostico("Consulta de rotina sem alterações");

        mockMvc.perform(post("/prontuario/cadastrarProntuarioMedico")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prontCodigoProntuario").value(10))
                .andExpect(jsonPath("$.consultaId").value(CONSULTA_ID));

        verify(prontuarioService).cadastrarProntuarioMedico(argThat(r -> CONSULTA_ID.equals(r.getConsulta())));
    }

    @Test
    @Order(4)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Passo 4 - Registrar pagamento da consulta realizada")
    void passo4_pagarConsulta() throws Exception {
        when(consultaAgendamentoService.atualizarStatus(eq(CONSULTA_ID), eq(StatusConsulta.PAGO), isNull()))
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
    @DisplayName("Passo 5 - Tentativa de cancelar consulta PAGA deve retornar 409 (Conflict)")
    void passo5_naoDeveCancelarConsultaPaga() throws Exception {
        when(consultaAgendamentoService.atualizarStatus(CONSULTA_ID, StatusConsulta.CANCELADA, "Tentativa inválida"))
                .thenThrow(new IllegalStateException("não podem ter o status alterado"));

        mockMvc.perform(patch("/consultas/{id}/status", CONSULTA_ID)
                        .with(csrf())
                        .param("status", "CANCELADA")
                        .param("motivo", "Tentativa inválida"))
                .andExpect(status().isConflict());
    }
}
