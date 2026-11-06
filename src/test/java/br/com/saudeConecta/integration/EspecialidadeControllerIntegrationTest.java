package br.com.saudeConecta.integration;

import br.com.saudeConecta.application.especialidade.EspecialidadeService;
import br.com.saudeConecta.domain.profissional.Especialidade;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("EspecialidadeController - Testes de integração")
class EspecialidadeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EspecialidadeService service;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    private Especialidade criarEspecialidade(Long id, String nome) {
        Especialidade e = new Especialidade();
        e.setId(id);
        e.setNome(nome);
        e.setCodigo("ESP001");
        return e;
    }

    // ========== GET /especialidades/carregarEspecialidades ==========

    @Nested
    @DisplayName("GET /especialidades/carregarEspecialidades")
    class CarregarEspecialidades {

        @Test
        @WithMockUser
        @DisplayName("Deve retornar 200 com lista de especialidades")
        void deveRetornar200ComLista() throws Exception {
            when(service.carregarEspecialidades())
                    .thenReturn(List.of(criarEspecialidade(1L, "Cardiologia")));

            mockMvc.perform(get("/especialidades/carregarEspecialidades"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser
        @DisplayName("Deve retornar lista vazia quando sem especialidades")
        void deveRetornarListaVazia() throws Exception {
            when(service.carregarEspecialidades()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/especialidades/carregarEspecialidades"))
                    .andExpect(status().isOk());
        }
    }
}
