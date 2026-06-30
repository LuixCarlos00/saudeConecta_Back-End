package br.com.saudeConecta.integration;

import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.organizacao.StatusOrganizacao;
import br.com.saudeConecta.service.OrganizacaoService;
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
@DisplayName("OrganizacaoController - Testes de integração")
class OrganizacaoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrganizacaoService organizacaoService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    private Organizacao criarOrganizacao(Long id, String nome) {
        Organizacao org = new Organizacao();
        org.setId(id);
        org.setNome(nome);
        org.setStatus(StatusOrganizacao.ATIVO);
        return org;
    }

    // ========== GET /api/v2/organizacoes/minha ==========

    @Nested
    @DisplayName("GET /api/v2/organizacoes/minha")
    class BuscarMinha {

        @Test
        @WithMockUser
        @DisplayName("Deve retornar 200 quando organização atual existe")
        void deveRetornar200QuandoExiste() throws Exception {
            when(organizacaoService.buscarAtual())
                    .thenReturn(Optional.of(criarOrganizacao(1L, "Clínica A")));

            mockMvc.perform(get("/api/v2/organizacoes/minha"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser
        @DisplayName("Deve retornar 404 quando organização não encontrada")
        void deveRetornar404QuandoNaoExiste() throws Exception {
            when(organizacaoService.buscarAtual()).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v2/organizacoes/minha"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Deve retornar 403 sem autenticação")
        void deveRetornar403SemAutenticacao() throws Exception {
            mockMvc.perform(get("/api/v2/organizacoes/minha"))
                    .andExpect(status().isForbidden());
        }
    }

    // ========== GET /api/v2/organizacoes ==========

    @Nested
    @DisplayName("GET /api/v2/organizacoes")
    class ListarTodas {

        @Test
        @WithMockUser
        @DisplayName("Deve retornar 200 com lista de organizações")
        void deveRetornar200ComLista() throws Exception {
            when(organizacaoService.listarTodas())
                    .thenReturn(List.of(criarOrganizacao(1L, "Clínica A")));

            mockMvc.perform(get("/api/v2/organizacoes"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser
        @DisplayName("Deve retornar lista vazia quando não há organizações")
        void deveRetornarListaVazia() throws Exception {
            when(organizacaoService.listarTodas()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v2/organizacoes"))
                    .andExpect(status().isOk());
        }
    }

    // ========== GET /api/v2/organizacoes/ativas ==========

    @Nested
    @DisplayName("GET /api/v2/organizacoes/ativas")
    class ListarAtivas {

        @Test
        @WithMockUser
        @DisplayName("Deve retornar 200 com organizações ativas")
        void deveRetornar200ComAtivas() throws Exception {
            when(organizacaoService.listarAtivas())
                    .thenReturn(List.of(criarOrganizacao(1L, "Clínica A")));

            mockMvc.perform(get("/api/v2/organizacoes/ativas"))
                    .andExpect(status().isOk());
        }
    }

    // ========== GET /api/v2/organizacoes/{id} ==========

    @Nested
    @DisplayName("GET /api/v2/organizacoes/{id}")
    class BuscarPorId {

        @Test
        @WithMockUser
        @DisplayName("Deve retornar 200 quando encontrada")
        void deveRetornar200QuandoEncontrada() throws Exception {
            when(organizacaoService.buscarPorId(1L))
                    .thenReturn(Optional.of(criarOrganizacao(1L, "Clínica A")));

            mockMvc.perform(get("/api/v2/organizacoes/1"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser
        @DisplayName("Deve retornar 404 quando não encontrada")
        void deveRetornar404QuandoNaoEncontrada() throws Exception {
            when(organizacaoService.buscarPorId(99L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v2/organizacoes/99"))
                    .andExpect(status().isNotFound());
        }
    }

    // ========== PUT /api/v2/organizacoes/{id}/ativar e inativar ==========

    @Nested
    @DisplayName("PUT /api/v2/organizacoes/{id}/ativar e /inativar")
    class AtivarInativar {

        @Test
        @WithMockUser
        @DisplayName("Deve retornar 200 ao ativar organização")
        void deveRetornar200AoAtivar() throws Exception {
            doNothing().when(organizacaoService).ativar(1L);

            mockMvc.perform(put("/api/v2/organizacoes/1/ativar").with(csrf()))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser
        @DisplayName("Deve retornar 200 ao inativar organização")
        void deveRetornar200AoInativar() throws Exception {
            doNothing().when(organizacaoService).inativar(1L);

            mockMvc.perform(put("/api/v2/organizacoes/1/inativar").with(csrf()))
                    .andExpect(status().isOk());
        }
    }

    // ========== GET /api/v2/organizacoes/estatisticas/total ==========

    @Nested
    @DisplayName("GET /api/v2/organizacoes/estatisticas/total")
    class ContarAtivas {

        @Test
        @WithMockUser
        @DisplayName("Deve retornar 200 com contagem de organizações ativas")
        void deveRetornar200ComContagem() throws Exception {
            when(organizacaoService.contarAtivas()).thenReturn(5L);

            mockMvc.perform(get("/api/v2/organizacoes/estatisticas/total"))
                    .andExpect(status().isOk());
        }
    }
}
