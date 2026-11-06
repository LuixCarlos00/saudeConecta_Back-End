package br.com.saudeConecta.application.notificacao;

import br.com.saudeConecta.infrastructure.email.EmailRetryService;
import br.com.saudeConecta.infrastructure.email.EmailTemplateService;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
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
                    99L, "dest@email.com", "Assunto", "<html>corpo</html>");
        }

        @Test
        @DisplayName("Não deve reenviar quando desabilitado")
        void naoDeveReenviarQuandoDesabilitado() {
            ReflectionTestUtils.setField(emailNotificacaoService, "emailHabilitado", false);

            emailNotificacaoService.reenviarEmail(99L, "dest@email.com", "Assunto", "<html>corpo</html>");

            verifyNoInteractions(emailRetryService);
        }
    }

    // ========== logs com e-mail desabilitado ==========

    @Nested
    @DisplayName("Logs com e-mail desabilitado")
    class LogsComEmailDesabilitado {

        private static final String SENHA = "S3nh@Secreta!";
        private static final String EMAIL = "fulano.silva@clinica.com";

        private Logger logger;
        private ListAppender<ILoggingEvent> appender;

        @BeforeEach
        void capturarLogs() {
            ReflectionTestUtils.setField(emailNotificacaoService, "emailHabilitado", false);
            logger = (Logger) LoggerFactory.getLogger(EmailNotificacaoService.class);
            appender = new ListAppender<>();
            appender.start();
            logger.addAppender(appender);
        }

        @AfterEach
        void soltarLogs() {
            logger.detachAppender(appender);
        }

        private List<String> mensagens() {
            return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        }

        @Test
        @DisplayName("Credenciais: não deve logar a senha, o e-mail nem o CPF em claro (login mascarado)")
        void credenciaisNaoDevemLogarSenha() {
            emailNotificacaoService.enviarCredenciaisAdministrador(EMAIL, "Fulano", "12345678900", SENHA, 1L, 2L);

            assertThat(mensagens()).isNotEmpty();
            assertThat(mensagens()).noneMatch(m -> m.contains(SENHA));
            assertThat(mensagens()).noneMatch(m -> m.contains(EMAIL));
            assertThat(mensagens()).noneMatch(m -> m.contains("12345678900"));
            assertThat(mensagens()).anyMatch(m -> m.contains("***.***.***-00"));
        }

        @Test
        @DisplayName("Recuperação de senha: não deve logar a senha, o e-mail nem o CPF em claro (login mascarado)")
        void recuperacaoNaoDeveLogarSenha() {
            emailNotificacaoService.enviarRecuperacaoSenha(EMAIL, "Fulano", "12345678900", SENHA, 1L);

            assertThat(mensagens()).isNotEmpty();
            assertThat(mensagens()).noneMatch(m -> m.contains(SENHA));
            assertThat(mensagens()).noneMatch(m -> m.contains(EMAIL));
            assertThat(mensagens()).noneMatch(m -> m.contains("12345678900"));
            assertThat(mensagens()).anyMatch(m -> m.contains("***.***.***-00"));
        }
    }
}
