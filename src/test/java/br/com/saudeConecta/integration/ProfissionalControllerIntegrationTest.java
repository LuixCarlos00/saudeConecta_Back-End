package br.com.saudeConecta.integration;

import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.profissional.StatusProfissional;
import br.com.saudeConecta.service.ProfissionalService;
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
@DisplayName("ProfissionalController - Testes de integração")
class ProfissionalControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProfissionalService profissionalService;

    private ObjectMapper objectMapper;
    private Profissional profissionalMock;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        Organizacao org = Organizacao.builder().nome("Clínica Teste").build();
        org.setId(1L);

        profissionalMock = Profissional.builder()
                .nome("Dr. Teste").cpf("12345678900").email("dr@teste.com")
                .registroConselho("CRM-12345").status(StatusProfissional.ATIVO)
                .organizacao(org)
                .build();
        profissionalMock.setId(10L);
    }

    // ========== GET /profissionais/buscarClinicoIdByOrg/{id} ==========

    @Nested
    @DisplayName("GET /profissionais/buscarClinicoIdByOrg/{id}")
    class BuscarClinicoIdByOrg {

        @Test
        @WithMockUser(roles = "ADMIN_ORG")
        @DisplayName("Deve retornar 200 e profissional quando encontrado")
        void deveRetornar200QuandoEncontrado() throws Exception {
            when(profissionalService.buscarClinicoIdByOrg(10L)).thenReturn(Optional.of(profissionalMock));

            mockMvc.perform(get("/profissionais/buscarClinicoIdByOrg/10"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @WithMockUser(roles = "ADMIN_ORG")
        @DisplayName("Deve retornar 404 quando profissional não existir")
        void deveRetornar404QuandoNaoExistir() throws Exception {
            when(profissionalService.buscarClinicoIdByOrg(999L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/profissionais/buscarClinicoIdByOrg/999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Deve retornar 403 sem autenticação")
        void deveRetornar403SemAutenticacao() throws Exception {
            mockMvc.perform(get("/profissionais/buscarClinicoIdByOrg/10"))
                    .andExpect(status().isForbidden());
        }
    }

    // ========== DELETE /profissionais/deletarClinicoIdByOrg/{id} ==========

    @Nested
    @DisplayName("DELETE /profissionais/deletarClinicoIdByOrg/{id}")
    class DeletarClinicoIdByOrg {

        @Test
        @WithMockUser(roles = "ADMIN_ORG")
        @DisplayName("Deve retornar 200 ao deletar profissional com sucesso")
        void deveRetornar200AoDeletarComSucesso() throws Exception {
            doNothing().when(profissionalService).deletarClinicoIdByOrg(10L);

            mockMvc.perform(delete("/profissionais/deletarClinicoIdByOrg/10")
                            .with(csrf()))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "ADMIN_ORG")
        @DisplayName("Deve retornar 404 quando profissional não for encontrado")
        void deveRetornar404QuandoNaoEncontrado() throws Exception {
            doThrow(new IllegalArgumentException("nao encontrado"))
                    .when(profissionalService).deletarClinicoIdByOrg(999L);

            mockMvc.perform(delete("/profissionais/deletarClinicoIdByOrg/999")
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(roles = "ADMIN_ORG")
        @DisplayName("Deve retornar 409 quando houver relacionamentos existentes")
        void deveRetornar409QuandoHouverRelacionamentos() throws Exception {
            doThrow(new IllegalStateException("relacionamentos existentes"))
                    .when(profissionalService).deletarClinicoIdByOrg(10L);

            mockMvc.perform(delete("/profissionais/deletarClinicoIdByOrg/10")
                            .with(csrf()))
                    .andExpect(status().isConflict());
        }
    }

    // ========== GET /profissionais/organizacao/{organizacaoId} ==========

    @Nested
    @DisplayName("GET /profissionais/organizacao/{organizacaoId}")
    class ListarPorOrganizacao {

        @Test
        @WithMockUser(roles = "ADMIN_ORG")
        @DisplayName("Deve retornar 200 com lista de profissionais da organização")
        void deveRetornar200ComListaDeProfissionaisDaOrg() throws Exception {
            when(profissionalService.buscarPorOrganizacao(1L)).thenReturn(List.of(profissionalMock));

            mockMvc.perform(get("/profissionais/organizacao/1"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @WithMockUser(roles = "ADMIN_ORG")
        @DisplayName("Deve retornar 200 com lista vazia quando não houver profissionais")
        void deveRetornar200ComListaVazia() throws Exception {
            when(profissionalService.buscarPorOrganizacao(99L)).thenReturn(List.of());

            mockMvc.perform(get("/profissionais/organizacao/99"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }
    }
}
