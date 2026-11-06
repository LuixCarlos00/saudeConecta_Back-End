package br.com.saudeConecta.integration;

import br.com.saudeConecta.application.atendimento.AtendimentoRascunhoService;
import br.com.saudeConecta.application.atendimento.dto.RascunhoAtendimentoResponse;
import br.com.saudeConecta.application.atendimento.dto.RascunhoAtendimentoResumoResponse;
import br.com.saudeConecta.application.atendimento.dto.SalvarRascunhoAtendimentoRequest;
import br.com.saudeConecta.application.exception.BusinessException;
import br.com.saudeConecta.application.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AtendimentoRascunhoController - Testes de integração")
class AtendimentoRascunhoControllerIntegrationTest {

    private static final String ROTA = "/atendimentos/822/rascunho";
    private static final String CORPO_PUT =
            "{\"tipo\":\"MEDICO\",\"dados\":{\"exame\":{\"peso\":70},\"codigos\":[\"A01\"]},"
                    + "\"tempoAcumuladoSegundos\":754,\"versao\":3}";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AtendimentoRascunhoService service;

    private RascunhoAtendimentoResponse resposta(long versao) throws Exception {
        return new RascunhoAtendimentoResponse(822L, "MEDICO",
                objectMapper.readTree("{\"exame\":{\"peso\":70},\"codigos\":[\"A01\"]}"),
                754, versao, LocalDateTime.of(2026, 11, 5, 14, 32, 10));
    }

    @Nested
    @DisplayName("GET /atendimentos/{consultaId}/rascunho")
    class Buscar {

        @Test
        @WithMockUser(roles = "PROFISSIONAL")
        @DisplayName("204 quando não há rascunho")
        void semRascunho() throws Exception {
            when(service.buscar(822L)).thenReturn(Optional.empty());

            mockMvc.perform(get(ROTA))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));
        }

        @Test
        @WithMockUser(roles = "PROFISSIONAL")
        @DisplayName("200 com dados como objeto JSON e atualizadoEm sem fração")
        void comRascunho() throws Exception {
            when(service.buscar(822L)).thenReturn(Optional.of(resposta(4)));

            mockMvc.perform(get(ROTA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.consultaId").value(822))
                    .andExpect(jsonPath("$.tipo").value("MEDICO"))
                    .andExpect(jsonPath("$.dados.exame.peso").value(70))
                    .andExpect(jsonPath("$.versao").value(4))
                    .andExpect(jsonPath("$.tempoAcumuladoSegundos").value(754))
                    .andExpect(jsonPath("$.atualizadoEm").value("2026-11-05T14:32:10"));
        }

        @Test
        @WithMockUser(roles = "PROFISSIONAL")
        @DisplayName("404 com ErrorResponse para consulta de outro profissional")
        void outroProfissional() throws Exception {
            when(service.buscar(822L)).thenThrow(new ResourceNotFoundException("Consulta nao encontrada"));

            mockMvc.perform(get(ROTA))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NAO_ENCONTRADO"))
                    .andExpect(jsonPath("$.path").value(ROTA));
        }

        @Test
        @DisplayName("403 sem autenticação")
        void semAutenticacao() throws Exception {
            mockMvc.perform(get(ROTA)).andExpect(status().isForbidden());
            verifyNoInteractions(service);
        }
    }

    @Nested
    @DisplayName("PUT /atendimentos/{consultaId}/rascunho")
    class Salvar {

        @Test
        @WithMockUser(roles = "PROFISSIONAL")
        @DisplayName("200 devolvendo dados como objeto (não string) e a versão nova")
        void salva() throws Exception {
            when(service.salvar(eq(822L), any())).thenReturn(resposta(4));

            mockMvc.perform(put(ROTA).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(CORPO_PUT))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.dados").isMap())
                    .andExpect(jsonPath("$.dados.codigos[0]").value("A01"))
                    .andExpect(jsonPath("$.versao").value(4));

            verify(service).salvar(eq(822L), argThat((SalvarRascunhoAtendimentoRequest r) ->
                    "MEDICO".equals(r.tipo()) && r.versao() == 3L && r.tempoAcumuladoSegundos() == 754
                            && r.dados().get("exame").get("peso").asInt() == 70));
        }

        @Test
        @WithMockUser(roles = "PROFISSIONAL")
        @DisplayName("409 code=RASCUNHO_DESATUALIZADO quando outra aba salvou depois")
        void desatualizado() throws Exception {
            when(service.salvar(eq(822L), any())).thenThrow(new BusinessException(
                    "O rascunho foi salvo em outra aba ou computador. Recarregue para continuar.",
                    HttpStatus.CONFLICT, "RASCUNHO_DESATUALIZADO"));

            mockMvc.perform(put(ROTA).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(CORPO_PUT))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("RASCUNHO_DESATUALIZADO"))
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @WithMockUser(roles = "PROFISSIONAL")
        @DisplayName("409 code=CONSULTA_ENCERRADA quando a consulta já foi finalizada")
        void consultaEncerrada() throws Exception {
            when(service.salvar(eq(822L), any())).thenThrow(new BusinessException(
                    "A consulta ja foi encerrada e nao aceita rascunho", HttpStatus.CONFLICT, "CONSULTA_ENCERRADA"));

            mockMvc.perform(put(ROTA).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(CORPO_PUT))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CONSULTA_ENCERRADA"));
        }

        @Test
        @WithMockUser(roles = "PROFISSIONAL")
        @DisplayName("422 com ErrorResponse para tipo inválido")
        void tipoInvalido() throws Exception {
            when(service.salvar(eq(822L), any())).thenThrow(new BusinessException(
                    "Tipo do rascunho invalido: use MEDICO ou DENTISTA", HttpStatus.UNPROCESSABLE_ENTITY, "RASCUNHO_INVALIDO"));

            mockMvc.perform(put(ROTA).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"tipo\":\"XPTO\",\"dados\":{}}"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("RASCUNHO_INVALIDO"));
        }

        @Test
        @WithMockUser(roles = "PROFISSIONAL")
        @DisplayName("404 para consulta de outro profissional")
        void outroProfissional() throws Exception {
            when(service.salvar(eq(822L), any())).thenThrow(new ResourceNotFoundException("Consulta nao encontrada"));

            mockMvc.perform(put(ROTA).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(CORPO_PUT))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NAO_ENCONTRADO"));
        }
    }

    @Nested
    @DisplayName("DELETE /atendimentos/{consultaId}/rascunho")
    class Descartar {

        @Test
        @WithMockUser(roles = "PROFISSIONAL")
        @DisplayName("204")
        void descarta() throws Exception {
            mockMvc.perform(delete(ROTA).with(csrf()))
                    .andExpect(status().isNoContent());
            verify(service).descartar(822L);
        }
    }

    @Nested
    @DisplayName("GET /atendimentos/rascunhos/meus")
    class Meus {

        @Test
        @WithMockUser(roles = "PROFISSIONAL")
        @DisplayName("200 com consultaId, atualizadoEm e tempoAcumuladoSegundos")
        void lista() throws Exception {
            when(service.listarMeus()).thenReturn(List.of(
                    new RascunhoAtendimentoResumoResponse(822L, LocalDateTime.of(2026, 11, 5, 14, 32, 10), 754)));

            mockMvc.perform(get("/atendimentos/rascunhos/meus"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].consultaId").value(822))
                    .andExpect(jsonPath("$[0].atualizadoEm").value("2026-11-05T14:32:10"))
                    .andExpect(jsonPath("$[0].tempoAcumuladoSegundos").value(754));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("200 com [] para usuário que não é profissional")
        void naoProfissional() throws Exception {
            when(service.listarMeus()).thenReturn(List.of());

            mockMvc.perform(get("/atendimentos/rascunhos/meus"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));
        }
    }
}
