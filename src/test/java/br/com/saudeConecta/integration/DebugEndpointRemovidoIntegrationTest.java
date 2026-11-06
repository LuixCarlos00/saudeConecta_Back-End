package br.com.saudeConecta.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Garante que os endpoints de debug (que listavam usuários de todos os tenants) não existem mais.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Endpoints /debug - removidos")
class DebugEndpointRemovidoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = {"SUPER_ADMIN", "ADMIN", "PROFISSIONAL", "RECEPCIONISTA"})
    @DisplayName("GET /debug/usuarios não deve existir")
    void debugUsuariosNaoDeveExistir() throws Exception {
        mockMvc.perform(get("/debug/usuarios"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = {"SUPER_ADMIN", "ADMIN", "PROFISSIONAL", "RECEPCIONISTA"})
    @DisplayName("GET /debug/usuario/{login} não deve existir")
    void debugUsuarioPorLoginNaoDeveExistir() throws Exception {
        mockMvc.perform(get("/debug/usuario/12345678900"))
                .andExpect(status().isNotFound());
    }
}
