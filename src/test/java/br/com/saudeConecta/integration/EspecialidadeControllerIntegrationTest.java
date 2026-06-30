package br.com.saudeConecta.integration;

import br.com.saudeConecta.domain.profissional.Especialidade;
import br.com.saudeConecta.service.EspecialidadeService;
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
import java.util.Optional;

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

    // ========== GET /especialidades/{id} ==========

    @Nested
    @DisplayName("GET /especialidades/{id}")
    class BuscarPorId {

        @Test
        @WithMockUser
        @DisplayName("Deve retornar 200 quando especialidade existe")
        void deveRetornar200QuandoExiste() throws Exception {
            when(service.buscarPorId(1L))
                    .thenReturn(Optional.of(criarEspecialidade(1L, "Cardiologia")));

            mockMvc.perform(get("/especialidades/1"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser
        @DisplayName("Deve retornar 404 quando não existe")
        void deveRetornar404QuandoNaoExiste() throws Exception {
            when(service.buscarPorId(99L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/especialidades/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Deve retornar 403 sem autenticação")
        void deveRetornar403SemAutenticacao() throws Exception {
            mockMvc.perform(get("/especialidades/1"))
                    .andExpect(status().isForbidden());
        }
    }

    // ========== GET /especialidades/medicas e /odontologicas ==========

    @Nested
    @DisplayName("GET /especialidades/medicas e /odontologicas")
    class ListarPorTipo {

        @Test
        @WithMockUser
        @DisplayName("Deve retornar 200 para especialidades médicas")
        void deveRetornar200Medicas() throws Exception {
            when(service.listarMedicas())
                    .thenReturn(List.of(criarEspecialidade(1L, "Cardiologia")));

            mockMvc.perform(get("/especialidades/medicas"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser
        @DisplayName("Deve retornar 200 para especialidades odontológicas")
        void deveRetornar200Odontologicas() throws Exception {
            when(service.listarOdontologicas())
                    .thenReturn(List.of(criarEspecialidade(2L, "Ortodontia")));

            mockMvc.perform(get("/especialidades/odontologicas"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser
        @DisplayName("Deve retornar 200 para especialidades por tipo código")
        void deveRetornar200PorCodigoTipo() throws Exception {
            when(service.listarPorTipo("MED"))
                    .thenReturn(List.of(criarEspecialidade(1L, "Cardiologia")));

            mockMvc.perform(get("/especialidades/tipo/MED"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser
        @DisplayName("Deve retornar 200 para especialidades por tipo ID")
        void deveRetornar200PorIdTipo() throws Exception {
            when(service.listarPorTipoId(1L))
                    .thenReturn(List.of(criarEspecialidade(1L, "Cardiologia")));

            mockMvc.perform(get("/especialidades/tipo-id/1"))
                    .andExpect(status().isOk());
        }
    }

    // ========== DELETE /especialidades/{id} ==========

    @Nested
    @DisplayName("DELETE /especialidades/{id}")
    class Deletar {

        @Test
        @WithMockUser
        @DisplayName("Deve retornar 204 ao deletar com sucesso")
        void deveRetornar204AoDeletar() throws Exception {
            doNothing().when(service).deletar(1L);

            mockMvc.perform(delete("/especialidades/1").with(csrf()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser
        @DisplayName("Deve retornar 404 quando especialidade não encontrada")
        void deveRetornar404QuandoNaoEncontrada() throws Exception {
            doThrow(new IllegalArgumentException("não encontrada"))
                    .when(service).deletar(99L);

            mockMvc.perform(delete("/especialidades/99").with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }
}
