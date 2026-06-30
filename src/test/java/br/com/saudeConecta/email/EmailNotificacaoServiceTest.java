package br.com.saudeConecta.email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailNotificacaoService - Testes unitários")
class EmailNotificacaoServiceTest {

    @Mock private EmailTemplateService emailTemplateService;
    @Mock private EmailRetryService emailRetryService;

    private EmailNotificacaoService emailNotificacaoService;

    // Executor síncrono para testes — executa imediatamente na mesma thread
    private final Executor executorSincrono = Runnable::run;

    @BeforeEach
    void setUp() {
        emailNotificacaoService = new EmailNotificacaoService(
                emailTemplateService, emailRetryService, executorSincrono);
    }

    // ========== isEmailHabilitado ==========

    @Nested
    @DisplayName("isEmailHabilitado")
    class IsEmailHabilitado {

        @Test
        @DisplayName("Deve retornar true quando email está habilitado")
        void deveRetornarTrueQuandoHabilitado() {
            ReflectionTestUtils.setField(emailNotificacaoService, "emailHabilitado", true);
            assertThat(emailNotificacaoService.isEmailHabilitado()).isTrue();
        }

        @Test
        @DisplayName("Deve retornar false quando email está desabilitado")
        void deveRetornarFalseQuandoDesabilitado() {
            ReflectionTestUtils.setField(emailNotificacaoService, "emailHabilitado", false);
            assertThat(emailNotificacaoService.isEmailHabilitado()).isFalse();
        }
    }

    // ========== enviarCredenciaisAdministrador ==========

    @Nested
    @DisplayName("enviarCredenciaisAdministrador")
    class EnviarCredenciaisAdministrador {

        @Test
        @DisplayName("Deve enviar email quando habilitado")
        void deveEnviarQuandoHabilitado() throws Exception {
            ReflectionTestUtils.setField(emailNotificacaoService, "emailHabilitado", true);
            when(emailTemplateService.renderizar(anyString(), anyMap())).thenReturn("<html>corpo</html>");

            emailNotificacaoService.enviarCredenciaisAdministrador(
                    "admin@org.com", "Admin", "12345678000190", "senha123", 1L, 10L);

            verify(emailRetryService).executarComRetry(
                    eq("admin@org.com"), eq("Admin"), anyString(), anyString(),
                    eq("administrador"), eq(1L), eq(10L));
        }

        @Test
        @DisplayName("Não deve enviar email quando desabilitado")
        void naoDeveEnviarQuandoDesabilitado() {
            ReflectionTestUtils.setField(emailNotificacaoService, "emailHabilitado", false);

            emailNotificacaoService.enviarCredenciaisAdministrador(
                    "admin@org.com", "Admin", "12345678000190", "senha123", 1L, 10L);

            verifyNoInteractions(emailRetryService);
        }
    }

    // ========== enviarCredenciaisClinico ==========

    @Nested
    @DisplayName("enviarCredenciaisClinico")
    class EnviarCredenciaisClinico {

        @Test
        @DisplayName("Deve enviar email com tipo 'medico' quando habilitado")
        void deveEnviarQuandoHabilitado() throws Exception {
            ReflectionTestUtils.setField(emailNotificacaoService, "emailHabilitado", true);
            when(emailTemplateService.renderizar(anyString(), anyMap())).thenReturn("<html>corpo</html>");

            emailNotificacaoService.enviarCredenciaisClinico(
                    "dr@clinica.com", "Dr. João", "12345678900", "senha456",
                    "Clínica A", 1L, 20L);

            verify(emailRetryService).executarComRetry(
                    eq("dr@clinica.com"), eq("Dr. João"), anyString(), anyString(),
                    eq("medico"), eq(1L), eq(20L));
        }

        @Test
        @DisplayName("Não deve enviar quando desabilitado")
        void naoDeveEnviarQuandoDesabilitado() {
            ReflectionTestUtils.setField(emailNotificacaoService, "emailHabilitado", false);

            emailNotificacaoService.enviarCredenciaisClinico(
                    "dr@clinica.com", "Dr. João", "12345678900", "senha456",
                    "Clínica A", 1L, 20L);

            verifyNoInteractions(emailRetryService);
        }
    }

    // ========== enviarCredenciaisSecretaria ==========

    @Nested
    @DisplayName("enviarCredenciaisSecretaria")
    class EnviarCredenciaisSecretaria {

        @Test
        @DisplayName("Deve enviar email com tipo 'secretaria' quando habilitado")
        void deveEnviarQuandoHabilitado() throws Exception {
            ReflectionTestUtils.setField(emailNotificacaoService, "emailHabilitado", true);
            when(emailTemplateService.renderizar(anyString(), anyMap())).thenReturn("<html>corpo</html>");

            emailNotificacaoService.enviarCredenciaisSecretaria(
                    "sec@clinica.com", "Ana", "98765432100", "senha789", 1L, 30L);

            verify(emailRetryService).executarComRetry(
                    eq("sec@clinica.com"), eq("Ana"), anyString(), anyString(),
                    eq("secretaria"), eq(1L), eq(30L));
        }

        @Test
        @DisplayName("Não deve enviar quando desabilitado")
        void naoDeveEnviarQuandoDesabilitado() {
            ReflectionTestUtils.setField(emailNotificacaoService, "emailHabilitado", false);

            emailNotificacaoService.enviarCredenciaisSecretaria(
                    "sec@clinica.com", "Ana", "98765432100", "senha789", 1L, 30L);

            verifyNoInteractions(emailRetryService);
        }
    }

    // ========== enviarCredenciaisPaciente ==========

    @Nested
    @DisplayName("enviarCredenciaisPaciente")
    class EnviarCredenciaisPaciente {

        @Test
        @DisplayName("Deve enviar email com tipo 'paciente' quando habilitado")
        void deveEnviarQuandoHabilitado() throws Exception {
            ReflectionTestUtils.setField(emailNotificacaoService, "emailHabilitado", true);
            when(emailTemplateService.renderizar(anyString(), anyMap())).thenReturn("<html>corpo</html>");

            emailNotificacaoService.enviarCredenciaisPaciente(
                    "pac@email.com", "Maria", "11122233344", "senhaP", 1L, 40L);

            verify(emailRetryService).executarComRetry(
                    eq("pac@email.com"), eq("Maria"), anyString(), anyString(),
                    eq("paciente"), eq(1L), eq(40L));
        }

        @Test
        @DisplayName("Não deve enviar quando desabilitado")
        void naoDeveEnviarQuandoDesabilitado() {
            ReflectionTestUtils.setField(emailNotificacaoService, "emailHabilitado", false);

            emailNotificacaoService.enviarCredenciaisPaciente(
                    "pac@email.com", "Maria", "11122233344", "senhaP", 1L, 40L);

            verifyNoInteractions(emailRetryService);
        }
    }

    // ========== enviarRecuperacaoSenha ==========

    @Nested
    @DisplayName("enviarRecuperacaoSenha")
    class EnviarRecuperacaoSenha {

        @Test
        @DisplayName("Deve enviar email de recuperação quando habilitado")
        void deveEnviarQuandoHabilitado() throws Exception {
            ReflectionTestUtils.setField(emailNotificacaoService, "emailHabilitado", true);
            when(emailTemplateService.renderizar(anyString(), anyMap())).thenReturn("<html>recuperacao</html>");

            emailNotificacaoService.enviarRecuperacaoSenha(
                    "user@email.com", "Carlos", "carlos@email.com", "novaSenha1", 1L);

            verify(emailRetryService).executarComRetry(
                    eq("user@email.com"), eq("Carlos"), anyString(), anyString(),
                    eq("recuperacao"), eq(1L), isNull());
        }

        @Test
        @DisplayName("Não deve enviar quando desabilitado")
        void naoDeveEnviarQuandoDesabilitado() {
            ReflectionTestUtils.setField(emailNotificacaoService, "emailHabilitado", false);

            emailNotificacaoService.enviarRecuperacaoSenha(
                    "user@email.com", "Carlos", "carlos@email.com", "novaSenha1", 1L);

            verifyNoInteractions(emailRetryService);
        }
    }

    // ========== reenviarEmail ==========

    @Nested
    @DisplayName("reenviarEmail")
    class ReenviarEmail {

        @Test
        @DisplayName("Deve reenviar email quando habilitado")
        void deveReenviarQuandoHabilitado() {
            ReflectionTestUtils.setField(emailNotificacaoService, "emailHabilitado", true);

            emailNotificacaoService.reenviarEmail(99L, "dest@email.com", "Assunto", "<html>corpo</html>");

            verify(emailRetryService).executarReenvioComRetry(
                    eq(99L), eq("dest@email.com"), eq("Assunto"), eq("<html>corpo</html>"));
        }

        @Test
        @DisplayName("Não deve reenviar quando desabilitado")
        void naoDeveReenviarQuandoDesabilitado() {
            ReflectionTestUtils.setField(emailNotificacaoService, "emailHabilitado", false);

            emailNotificacaoService.reenviarEmail(99L, "dest@email.com", "Assunto", "<html>corpo</html>");

            verifyNoInteractions(emailRetryService);
        }
    }
}
