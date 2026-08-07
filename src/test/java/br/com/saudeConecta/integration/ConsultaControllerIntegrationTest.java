package br.com.saudeConecta.integration;

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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("ConsultaController - Testes de integração")
class ConsultaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConsultaService consultaService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    // ========== POST /consultas/cadastrarConsultaByOrg ==========

    @Nested
    @DisplayName("POST /consultas/cadastrarConsultaByOrg")
    class CadastrarConsulta {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve retornar 200 ao cadastrar consulta válida")
        void deveRetornar200AoCadastrarConsultaValida() throws Exception {
            AgendarConsultaRequest request = new AgendarConsultaRequest(
                    1L, 1L, null,
                    LocalDateTime.now().plusDays(1),
                    30, "Observação", null, BigDecimal.TEN
            );

            Consulta consultaMock = Consulta.builder()
                    .id(100L).status(StatusConsulta.AGENDADA)
                    .duracaoMinutos(30)
                    .dataHora(LocalDateTime.now().plusDays(1))
                    .build();

            when(consultaService.cadastrarConsultaByOrg(any())).thenReturn(consultaMock);

            mockMvc.perform(post("/consultas/cadastrarConsultaByOrg")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Deve retornar 403 quando não autenticado")
        void deveRetornar403QuandoNaoAutenticado() throws Exception {
            AgendarConsultaRequest request = new AgendarConsultaRequest(
                    1L, 1L, null,
                    LocalDateTime.now().plusDays(1),
                    30, null, null, null
            );

            mockMvc.perform(post("/consultas/cadastrarConsultaByOrg")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // ========== GET /consultas/hoje ==========

    @Nested
    @DisplayName("GET /consultas/hoje")
    class ListarConsultasHoje {

        @Test
        @WithMockUser(roles = "PROFISSIONAL")
        @DisplayName("Deve retornar 200 com lista de consultas de hoje")
        void deveRetornar200ComListaDeConsultasHoje() throws Exception {
            when(consultaService.buscarConsultasHoje(null)).thenReturn(java.util.Collections.emptyList());

            mockMvc.perform(get("/consultas/hoje"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("Deve retornar 403 sem autenticação")
        void deveRetornar403SemAutenticacao() throws Exception {
            mockMvc.perform(get("/consultas/hoje"))
                    .andExpect(status().isForbidden());
        }
    }

    // ========== PUT /consultas/concluirConsultabyOrg/{id} ==========

    @Nested
    @DisplayName("PUT /consultas/concluirConsultabyOrg/{id}")
    class ConcluirConsulta {

        @Test
        @WithMockUser(roles = "PROFISSIONAL")
        @DisplayName("Deve retornar 200 ao concluir consulta existente")
        void deveRetornar200AoConcluirConsultaExistente() throws Exception {
            Consulta consultaMock = Consulta.builder()
                    .id(1L).status(StatusConsulta.REALIZADA)
                    .duracaoMinutos(30)
                    .dataHora(LocalDateTime.now())
                    .build();

            when(consultaService.concluirConsultabyOrg(1L)).thenReturn(consultaMock);

            mockMvc.perform(put("/consultas/concluirConsultabyOrg/1")
                            .with(csrf()))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "PROFISSIONAL")
        @DisplayName("Deve retornar 500 quando consulta não estiver em estado válido (sem try/catch no controller)")
        void deveRetornar500QuandoConsultaNaoAgendada() throws Exception {
            when(consultaService.concluirConsultabyOrg(1L))
                    .thenThrow(new IllegalStateException("Apenas consultas AGENDADAS podem ser concluídas"));

            mockMvc.perform(put("/consultas/concluirConsultabyOrg/1")
                            .with(csrf()))
                    .andExpect(status().isInternalServerError());
        }
    }

    // ========== GET /consultas/{id} ==========

    @Nested
    @DisplayName("GET /consultas/{id}")
    class BuscarConsultaPorId {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve retornar 200 quando consulta for encontrada")
        void deveRetornar200QuandoConsultaEncontrada() throws Exception {
            Consulta consultaMock = Consulta.builder()
                    .id(1L).status(StatusConsulta.AGENDADA)
                    .duracaoMinutos(30)
                    .dataHora(LocalDateTime.now().plusDays(1))
                    .build();

            when(consultaService.buscarPorId(1L)).thenReturn(java.util.Optional.of(consultaMock));

            mockMvc.perform(get("/consultas/1"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve retornar 404 quando consulta não existir")
        void deveRetornar404QuandoConsultaNaoExistir() throws Exception {
            when(consultaService.buscarPorId(999L)).thenReturn(java.util.Optional.empty());

            mockMvc.perform(get("/consultas/999"))
                    .andExpect(status().isNotFound());
        }
    }
}
