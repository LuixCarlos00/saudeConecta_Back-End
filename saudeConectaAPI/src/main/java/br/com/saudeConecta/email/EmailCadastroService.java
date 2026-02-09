package br.com.saudeConecta.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class EmailCadastroService {

    private final CredenciaisEmailService credenciaisEmailService;
    private final Executor emailTaskExecutor;

    @Value("${app.email.enabled}")
    private boolean emailEnabled;

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long INITIAL_RETRY_DELAY_MS = 5000; // 5 segundos

    public EmailCadastroService(CredenciaisEmailService credenciaisEmailService, 
                               Executor emailTaskExecutor) {
        this.credenciaisEmailService = credenciaisEmailService;
        this.emailTaskExecutor = emailTaskExecutor;
    }

    /**
     * Envia email de credenciais para médico/clínico de forma assíncrona
     */
    @Async
    public void enviarCredenciaisClinicoAsync(String email, String nome, String cpf, String senha, String nomeOrganizacao) {
        if (emailEnabled) {
            CompletableFuture.runAsync(() -> {
                try {
                    credenciaisEmailService.enviarCredenciaisClinico(email, nome, cpf, senha, nomeOrganizacao);
                    log.info("Email de credenciais enviado para: {}", email);
                } catch (Exception e) {
                    log.error("Erro ao enviar email de credenciais: {}", e.getMessage());
                    agendarRetryEnvioEmail(email, nome, cpf, senha, nomeOrganizacao, "medico");
                }
            }, emailTaskExecutor);
        } else {
            log.warn("Envio de email desabilitado. Credenciais não enviadas para: {}", email);
            log.info("CREDENCIAIS MÉDICO - Login: {}, Senha: {}", cpf, senha);
        }
    }

    /**
     * Envia email de credenciais para secretária de forma assíncrona
     */
    @Async
    public void enviarCredenciaisSecretariaAsync(String email, String nome, String cpf, String senha) {
        if (emailEnabled) {
            CompletableFuture.runAsync(() -> {
                try {
                    credenciaisEmailService.enviarCredenciaisSecretaria(email, nome, cpf, senha);
                    log.info("Email de credenciais enviado para secretária: {}", email);
                } catch (Exception e) {
                    log.error("Erro ao enviar email de credenciais para secretária: {}", e.getMessage());
                    agendarRetryEnvioEmail(email, nome, cpf, senha, null, "secretaria");
                }
            }, emailTaskExecutor);
        } else {
            log.warn("Envio de email desabilitado. Credenciais não enviadas para secretária: {}", email);
            log.info("CREDENCIAIS SECRETÁRIA - Login: {}, Senha: {}", cpf, senha);
        }
    }

    /**
     * Envia email de credenciais para administrador de forma assíncrona
     */
    @Async
    public void enviarCredenciaisAdministradorAsync(String email, String nome, String cpf, String senha) {
        if (emailEnabled) {
            CompletableFuture.runAsync(() -> {
                try {
                    credenciaisEmailService.enviarCredenciaisAdministrador(email, nome, cpf, senha);
                    log.info("Email de credenciais enviado para administrador: {}", email);
                } catch (Exception e) {
                    log.error("Erro ao enviar email de credenciais para administrador: {}", e.getMessage());
                    agendarRetryEnvioEmail(email, nome, cpf, senha, null, "administrador");
                }
            }, emailTaskExecutor);
        } else {
            log.warn("Envio de email desabilitado. Credenciais não enviadas para administrador: {}", email);
            log.info("CREDENCIAIS ADMINISTRADOR - Login: {}, Senha: {}", cpf, senha);
        }
    }


    /**
     * Agenda retry de envio de email com backoff exponencial
     */
    private void agendarRetryEnvioEmail(String email, String nome, String cpf, String senha, 
                                       String nomeOrganizacao, String tipoUsuario) {
        agendarRetryEnvioEmail(email, nome, cpf, senha, nomeOrganizacao, tipoUsuario, 1);
    }

    /**
     * Agenda retry de envio de email com backoff exponencial
     */
    private void agendarRetryEnvioEmail(String email, String nome, String cpf, String senha, 
                                       String nomeOrganizacao, String tipoUsuario, int attempt) {
        if (attempt > MAX_RETRY_ATTEMPTS) {
            log.error("Falha ao enviar email após {} tentativas para: {}", MAX_RETRY_ATTEMPTS, email);
            return;
        }
        
        long delayMs = INITIAL_RETRY_DELAY_MS * (long) Math.pow(2, attempt - 1);
        
        CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS).execute(() -> {
            try {
                log.info("Tentativa {} de enviar email para: {} [{}]", attempt, email, tipoUsuario);
                
                switch (tipoUsuario.toLowerCase()) {
                    case "medico":
                        credenciaisEmailService.enviarCredenciaisClinico(email, nome, cpf, senha, nomeOrganizacao);
                        break;
                    case "secretaria":
                        credenciaisEmailService.enviarCredenciaisSecretaria(email, nome, cpf, senha);
                        break;
                    case "administrador":
                        credenciaisEmailService.enviarCredenciaisAdministrador(email, nome, cpf, senha);
                        break;
                    case "paciente":
                        //credenciaisEmailService.enviarCredenciaisPaciente(email, nome, cpf, senha);
                        break;
                    default:
                      //  credenciaisEmailService.enviarCredenciaisUsuario(email, nome, cpf, senha, tipoUsuario);
                        break;
                }
                
                log.info("Email enviado com sucesso na tentativa {} para: {} [{}]", attempt, email, tipoUsuario);
            } catch (Exception e) {
                log.error("Erro na tentativa {} de enviar email para {} [{}]: {}", attempt, email, tipoUsuario, e.getMessage());
                agendarRetryEnvioEmail(email, nome, cpf, senha, nomeOrganizacao, tipoUsuario, attempt + 1);
            }
        });
    }


}
