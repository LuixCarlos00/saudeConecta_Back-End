package br.com.saudeConecta.integration;

import br.com.saudeConecta.application.exception.BusinessException;
import br.com.saudeConecta.application.exception.ResourceNotFoundException;
import br.com.saudeConecta.application.prontuario.ProntuarioDentistaService;
import br.com.saudeConecta.application.prontuario.ProntuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato HTTP da edição de prontuário (médico e dentista). A regra de dono fica no service
 * (provada em ProntuarioServiceTest e ProntuarioDentistaServiceTest); aqui o status e o corpo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Edição de prontuário - Testes de integração")
class ProntuarioEdicaoControllerIntegrationTest {

    private static final String ROTA_MEDICO = "/prontuario/atualizarProntuarioMedico/41";
    private static final String ROTA_DENTISTA = "/prontuario-dentista/42";
    private static final String CORPO = "{\"anamnese\":\"Revisada\",\"consulta\":999,\"codigoMedico\":888,"
            + "\"planejamentos\":[{\"id\":7,\"procedimentoRealizado\":\"Limpeza\",\"valor\":150}]}";

    @Autowired private MockMvc mockMvc;
    @MockBean private ProntuarioService prontuarioService;
    @MockBean private ProntuarioDentistaService prontuarioDentistaService;

    @Test
    @WithMockUser(roles = "PROFISSIONAL")
    @DisplayName("Médico dono: 200 e o id do planejamento chega ao service")
    void medicoDono200() throws Exception {
        mockMvc.perform(put(ROTA_MEDICO).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isOk());

        verify(prontuarioService).atualizarProntuarioMedico(eq(41L),
                argThat(r -> r.getPlanejamentos().get(0).getId() == 7L));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "PROFISSIONAL"})
    @DisplayName("Médico: não dono (inclusive gestor) ou outra clínica recebe 404 NAO_ENCONTRADO")
    void medicoNaoDono404() throws Exception {
        doThrow(new ResourceNotFoundException("Prontuário não encontrado: 41"))
                .when(prontuarioService).atualizarProntuarioMedico(eq(41L), any());

        mockMvc.perform(put(ROTA_MEDICO).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NAO_ENCONTRADO"));
    }

    @Test
    @WithMockUser(roles = "PROFISSIONAL")
    @DisplayName("Médico: alterar planejamento assinado recebe 409 PLANEJAMENTO_ASSINADO")
    void medicoAssinado409() throws Exception {
        doThrow(new BusinessException("assinado", HttpStatus.CONFLICT, "PLANEJAMENTO_ASSINADO"))
                .when(prontuarioService).atualizarProntuarioMedico(eq(41L), any());

        mockMvc.perform(put(ROTA_MEDICO).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PLANEJAMENTO_ASSINADO"));
    }

    @Test
    @WithMockUser(roles = "PROFISSIONAL")
    @DisplayName("Dentista dono: 200")
    void dentistaDono200() throws Exception {
        mockMvc.perform(put(ROTA_DENTISTA).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isOk());

        verify(prontuarioDentistaService).atualizarProntuario(eq(42L), any());
    }

    @Test
    @WithMockUser(roles = "RECEPCIONISTA")
    @DisplayName("Dentista: secretária (não dona) recebe 404")
    void dentistaSecretaria404() throws Exception {
        doThrow(new ResourceNotFoundException("Prontuario odontologico nao encontrado: 42"))
                .when(prontuarioDentistaService).atualizarProntuario(eq(42L), any());

        mockMvc.perform(put(ROTA_DENTISTA).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isNotFound());
    }
}
