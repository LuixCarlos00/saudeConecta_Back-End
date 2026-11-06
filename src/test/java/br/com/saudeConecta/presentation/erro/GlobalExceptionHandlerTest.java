package br.com.saudeConecta.presentation.erro;

import br.com.saudeConecta.application.exception.BusinessException;
import br.com.saudeConecta.application.exception.LimitePlanoException;
import br.com.saudeConecta.application.exception.ResourceNotFoundException;
import br.com.saudeConecta.config.tenant.TenantAccessDeniedException;
import br.com.saudeConecta.config.tenant.TenantNotDefinedException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GlobalExceptionHandler - contrato único de erro")
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;
    private static Supplier<RuntimeException> proximaExcecao;

    @RestController
    static class ControllerFalso {
        @GetMapping("/falha")
        String falha() {
            throw proximaExcecao.get();
        }

        @PostMapping("/validar")
        String validar(@RequestBody @Valid Corpo corpo) {
            return "ok";
        }

        @GetMapping("/numero")
        String numero(@RequestParam Long valor) {
            return "ok";
        }
    }

    record Corpo(@NotBlank(message = "Nome e obrigatorio") String nome) {}

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ControllerFalso())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private ResultActions lancar(RuntimeException ex) throws Exception {
        proximaExcecao = () -> ex;
        return mockMvc.perform(get("/falha"));
    }

    @Test
    @DisplayName("IllegalStateException (regra de negócio) vira 409 com a mensagem")
    void illegalStateVira409() throws Exception {
        lancar(new IllegalStateException("Apenas consultas AGENDADAS podem ser concluidas"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("CONFLITO"))
                .andExpect(jsonPath("$.message").value("Apenas consultas AGENDADAS podem ser concluidas"))
                .andExpect(jsonPath("$.error").value("Apenas consultas AGENDADAS podem ser concluidas"))
                .andExpect(jsonPath("$.path").value("/falha"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("IllegalArgumentException vira 400")
    void illegalArgumentVira400() throws Exception {
        lancar(new IllegalArgumentException("Data invalida"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Data invalida"));
    }

    @Test
    @DisplayName("Lock otimista vira 409 pedindo para recarregar")
    void lockOtimistaVira409() throws Exception {
        lancar(new ObjectOptimisticLockingFailureException(Object.class, 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONCORRENCIA"))
                .andExpect(jsonPath("$.message", containsString("recarregue")));
    }

    @Test
    @DisplayName("Não encontrado (EntityNotFound, ResourceNotFound, NoSuchElement) vira 404")
    void naoEncontradoVira404() throws Exception {
        lancar(new EntityNotFoundException("Paciente nao encontrado"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Paciente nao encontrado"));
        lancar(new ResourceNotFoundException("Plano nao encontrado")).andExpect(status().isNotFound());
        lancar(new NoSuchElementException("No value present"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Recurso nao encontrado"));
    }

    @Test
    @DisplayName("EntityNotFound do Hibernate não expõe o nome da classe")
    void naoEncontradoHibernateNaoVaza() throws Exception {
        lancar(new EntityNotFoundException("Unable to find br.com.saudeConecta.domain.Paciente with id 5"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", not(containsString("br.com"))));
    }

    @Test
    @DisplayName("Tenant e AccessDenied viram 403 com message (lido pelo ErrorInterceptor)")
    void tenantEAcessoVira403() throws Exception {
        lancar(new TenantAccessDeniedException("Acesso negado a organizacao"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Acesso negado a organizacao"));
        lancar(new TenantNotDefinedException("Tenant nao definido")).andExpect(status().isForbidden());
        lancar(new AccessDeniedException("x")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("Credenciais inválidas viram 401 com error legível (lido pelo login)")
    void credenciaisInvalidasVira401() throws Exception {
        lancar(new BadCredentialsException("Bad credentials"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Credenciais invalidas"));
    }

    @Test
    @DisplayName("LimitePlanoException mantém 422")
    void limitePlanoVira422() throws Exception {
        lancar(new LimitePlanoException("PROFISSIONAL", 2, 2L))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("LIMITE_PLANO"));
    }

    @Test
    @DisplayName("BusinessException respeita o status e o código dela")
    void businessRespeitaStatus() throws Exception {
        lancar(new BusinessException("Ja existe", HttpStatus.CONFLICT, "DUPLICADO"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICADO"));
    }

    @Test
    @DisplayName("Violação de integridade vira 409 sem vazar a mensagem do banco")
    void integridadeNaoVazaBanco() throws Exception {
        lancar(new DataIntegrityViolationException("Duplicate entry '123' for key 'uk_cpf'"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INTEGRIDADE"))
                .andExpect(jsonPath("$.message", not(containsString("Duplicate"))));
    }

    @Test
    @DisplayName("SQLException encapsulada não vaza a mensagem do banco")
    void sqlExceptionNaoVaza() throws Exception {
        proximaExcecao = () -> new RuntimeException(new SQLException("Table 'x' doesn't exist"));
        mockMvc.perform(get("/falha"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Erro interno do servidor"));
    }

    @Test
    @DisplayName("Exception genérica vira 500 sem detalhes internos")
    void genericaVira500() throws Exception {
        lancar(new RuntimeException("NullPointer em ServicoX linha 42"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("ERRO_INTERNO"))
                .andExpect(jsonPath("$.message").value("Erro interno do servidor"));
    }

    @Test
    @DisplayName("Validação vira 400 com mapa campo->mensagem e message legível")
    void validacaoVira400ComMapa() throws Exception {
        mockMvc.perform(post("/validar").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACAO"))
                .andExpect(jsonPath("$.errors.nome").value("Nome e obrigatorio"))
                .andExpect(jsonPath("$.message").value("Nome e obrigatorio"));
    }

    @Test
    @DisplayName("JSON ilegível e type mismatch viram 400")
    void jsonIlegivelETypeMismatchVira400() throws Exception {
        mockMvc.perform(post("/validar").contentType(MediaType.APPLICATION_JSON).content("{nao-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("JSON_INVALIDO"));
        mockMvc.perform(get("/numero").param("valor", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PARAMETRO_INVALIDO"));
    }

    @Test
    @DisplayName("Erros do próprio Spring MVC mantêm o status 4xx (ex.: parâmetro obrigatório ausente)")
    void erroDoSpringMantemStatus() throws Exception {
        mockMvc.perform(get("/numero"))
                .andExpect(status().isBadRequest());
    }
}
