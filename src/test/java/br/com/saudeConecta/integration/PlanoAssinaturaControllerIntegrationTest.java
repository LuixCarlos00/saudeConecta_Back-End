package br.com.saudeConecta.integration;

import br.com.saudeConecta.application.assinatura.PlanoAssinaturaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("PlanoAssinaturaController - Testes de integração (autorização)")
class PlanoAssinaturaControllerIntegrationTest {

    private static final String BODY_VALIDO = """
            {"nome":"Starter","tipo":"STARTER","valorMensal":99.90}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlanoAssinaturaService planoAssinaturaService;

    @Nested
    @DisplayName("GET /planos (público)")
    class Listar {

        @Test
        @DisplayName("Deve retornar 200 sem autenticação")
        void deveRetornar200SemAutenticacao() throws Exception {
            when(planoAssinaturaService.listarPlanosAtivos()).thenReturn(List.of());

            mockMvc.perform(get("/planos"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PUT /planos/{id}")
    class Atualizar {

        @Test
        @WithMockUser(roles = {"SUPER_ADMIN", "ADMIN", "PROFISSIONAL", "RECEPCIONISTA"})
        @DisplayName("Deve retornar 200 para SUPER_ADMIN")
        void deveRetornar200ParaSuperAdmin() throws Exception {
            mockMvc.perform(put("/planos/1").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
                    .andExpect(status().isOk());
            verify(planoAssinaturaService).atualizarPlano(eq(1L), any());
        }

        @Test
        @WithMockUser(roles = {"ADMIN", "PROFISSIONAL", "RECEPCIONISTA"})
        @DisplayName("Deve retornar 403 para GESTOR")
        void deveRetornar403ParaGestor() throws Exception {
            mockMvc.perform(put("/planos/1").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(planoAssinaturaService);
        }
    }

    @Nested
    @DisplayName("DELETE /planos/{id}")
    class Desativar {

        @Test
        @WithMockUser(roles = {"SUPER_ADMIN", "ADMIN", "PROFISSIONAL", "RECEPCIONISTA"})
        @DisplayName("Deve retornar 204 para SUPER_ADMIN")
        void deveRetornar204ParaSuperAdmin() throws Exception {
            mockMvc.perform(delete("/planos/1").with(csrf()))
                    .andExpect(status().isNoContent());
            verify(planoAssinaturaService).desativarPlano(1L);
        }

        @Test
        @WithMockUser(roles = {"ADMIN", "PROFISSIONAL", "RECEPCIONISTA"})
        @DisplayName("Deve retornar 403 para GESTOR")
        void deveRetornar403ParaGestor() throws Exception {
            mockMvc.perform(delete("/planos/1").with(csrf()))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(planoAssinaturaService);
        }
    }
}
