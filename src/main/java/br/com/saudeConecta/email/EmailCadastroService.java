package br.com.saudeConecta.email;

import br.com.saudeConecta.domain.mensageria.TipoMensagem;
import br.com.saudeConecta.service.MensageriaService;
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
    private final MensageriaService mensageriaService;

    @Value("${app.email.enabled}")
    private boolean emailEnabled;

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long INITIAL_RETRY_DELAY_MS = 5000; // 5 segundos

    public EmailCadastroService(CredenciaisEmailService credenciaisEmailService,
                               Executor emailTaskExecutor,
                               MensageriaService mensageriaService) {
        this.credenciaisEmailService = credenciaisEmailService;
        this.emailTaskExecutor = emailTaskExecutor;
        this.mensageriaService = mensageriaService;
    }

    /**
     * Envia email de credenciais para médico/clínico de forma assíncrona.
     * Em caso de falha após todas as tentativas, registra na tabela de mensageria.
     *
     * @param email           Email do destinatário
     * @param nome            Nome do profissional
     * @param cpf             CPF (usado como login)
     * @param senha           Senha gerada
     * @param nomeOrganizacao Nome da organização
     * @param organizacaoId   ID da organização (para registro de falha)
     * @param profissionalId  ID do profissional (para registro de falha)
     */
    @Async
    public void enviarCredenciaisClinicoAsync(String email, String nome, String cpf, String senha,
                                              String nomeOrganizacao, Long organizacaoId, Long profissionalId) {
        if (emailEnabled) {
            CompletableFuture.runAsync(() -> {
                try {
                    credenciaisEmailService.enviarCredenciaisClinico(email, nome, cpf, senha, nomeOrganizacao);
                    log.info("Email de credenciais enviado para: {}", email);
                    // Registra sucesso na mensageria
                    registrarSucessoNaMensageria(email, nome, cpf, senha, nomeOrganizacao, "medico",
                            organizacaoId, profissionalId, 1);
                } catch (Exception e) {
                    log.error("Erro ao enviar email de credenciais: {}", e.getMessage());
                    agendarRetryEnvioEmail(email, nome, cpf, senha, nomeOrganizacao, "medico",
                            organizacaoId, profissionalId);
                }
            }, emailTaskExecutor);
        } else {
            log.warn("Envio de email desabilitado. Credenciais não enviadas para: {}", email);
            log.info("CREDENCIAIS MÉDICO - Login: {}, Senha: {}", cpf, senha);
        }
    }

    /**
     * Envia email de credenciais para médico/clínico de forma assíncrona (sem IDs de rastreamento).
     * Mantido para compatibilidade com chamadas existentes.
     */
    @Async
    public void enviarCredenciaisClinicoAsync(String email, String nome, String cpf, String senha, String nomeOrganizacao) {
        enviarCredenciaisClinicoAsync(email, nome, cpf, senha, nomeOrganizacao, null, null);
    }

    /**
     * Envia email de credenciais para secretária de forma assíncrona
     */
    @Async
    public void enviarCredenciaisSecretariaAsync(String email, String nome, String cpf, String senha, Long organizacaoId, Long secretariaId) {
        if (emailEnabled) {
            CompletableFuture.runAsync(() -> {
                try {
                    credenciaisEmailService.enviarCredenciaisSecretaria(email, nome, cpf, senha);
                    log.info("Email de credenciais enviado para secretária: {}", email);
                    // Registra sucesso na mensageria
                    registrarSucessoNaMensageria(email, nome, cpf, senha, null, "secretaria",
                            organizacaoId, secretariaId, 1);
                } catch (Exception e) {
                    log.error("Erro ao enviar email de credenciais para secretária: {}", e.getMessage());
                    agendarRetryEnvioEmail(email, nome, cpf, senha, null, "secretaria", organizacaoId, secretariaId);
                }
            }, emailTaskExecutor);
        } else {
            log.warn("Envio de email desabilitado. Credenciais não enviadas para secretária: {}", email);
            log.info("CREDENCIAIS SECRETÁRIA - Login: {}, Senha: {}", cpf, senha);
        }
    }

    /**
     * Envia email de credenciais para secretária de forma assíncrona (sem IDs de rastreamento).
     * Mantido para compatibilidade com chamadas existentes.
     */
    @Async
    public void enviarCredenciaisSecretariaAsync(String email, String nome, String cpf, String senha) {
        enviarCredenciaisSecretariaAsync(email, nome, cpf, senha, null, null);
    }

    /**
     * Envia email de credenciais para administrador de forma assíncrona
     */
    @Async
    public void enviarCredenciaisAdministradorAsync(String email, String nome, String cpf, String senha, Long organizacaoId, Long administradorId) {
        if (emailEnabled) {
            CompletableFuture.runAsync(() -> {
                try {
                    credenciaisEmailService.enviarCredenciaisAdministrador(email, nome, cpf, senha);
                    log.info("Email de credenciais enviado para administrador: {}", email);
                    // Registra sucesso na mensageria
                    registrarSucessoNaMensageria(email, nome, cpf, senha, null, "administrador",
                            organizacaoId, administradorId, 1);
                } catch (Exception e) {
                    log.error("Erro ao enviar email de credenciais para administrador: {}", e.getMessage());
                    agendarRetryEnvioEmail(email, nome, cpf, senha, null, "administrador", organizacaoId, administradorId);
                }
            }, emailTaskExecutor);
        } else {
            log.warn("Envio de email desabilitado. Credenciais não enviadas para administrador: {}", email);
            log.info("CREDENCIAIS ADMINISTRADOR - Login: {}, Senha: {}", cpf, senha);
        }
    }

    /**
     * Envia email de credenciais para administrador de forma assíncrona (sem IDs de rastreamento).
     * Mantido para compatibilidade com chamadas existentes.
     */
    @Async
    public void enviarCredenciaisAdministradorAsync(String email, String nome, String cpf, String senha) {
        enviarCredenciaisAdministradorAsync(email, nome, cpf, senha, null, null);
    }


    /**
     * Agenda retry de envio de email com backoff exponencial
     */
    private void agendarRetryEnvioEmail(String email, String nome, String cpf, String senha,
                                       String nomeOrganizacao, String tipoUsuario,
                                       Long organizacaoId, Long profissionalId) {
        agendarRetryEnvioEmail(email, nome, cpf, senha, nomeOrganizacao, tipoUsuario, 1,
                organizacaoId, profissionalId);
    }

    /**
     * Agenda retry de envio de email com backoff exponencial.
     * Após esgotar as tentativas, registra a falha na tabela de mensageria.
     */
    private void agendarRetryEnvioEmail(String email, String nome, String cpf, String senha,
                                       String nomeOrganizacao, String tipoUsuario, int attempt,
                                       Long organizacaoId, Long profissionalId) {
        if (attempt > MAX_RETRY_ATTEMPTS) {
            log.error("Falha ao enviar email após {} tentativas para: {}", MAX_RETRY_ATTEMPTS, email);
            registrarFalhaNaMensageria(email, nome, cpf, senha, nomeOrganizacao, tipoUsuario,
                    organizacaoId, profissionalId, MAX_RETRY_ATTEMPTS);
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
                // Registra sucesso na mensageria após retry
                registrarSucessoNaMensageria(email, nome, cpf, senha, nomeOrganizacao, tipoUsuario,
                        organizacaoId, profissionalId, attempt);
            } catch (Exception e) {
                log.error("Erro na tentativa {} de enviar email para {} [{}]: {}", attempt, email, tipoUsuario, e.getMessage());
                agendarRetryEnvioEmail(email, nome, cpf, senha, nomeOrganizacao, tipoUsuario, attempt + 1,
                        organizacaoId, profissionalId);
            }
        });
    }

    /**
     * Registra a falha de envio de email na tabela de mensageria para notificação ao admin.
     *
     * @param email           Email do destinatário
     * @param nome            Nome do destinatário
     * @param cpf             CPF (login)
     * @param senha           Senha gerada
     * @param nomeOrganizacao Nome da organização
     * @param tipoUsuario     Tipo do usuário (medico, secretaria, administrador)
     * @param organizacaoId   ID da organização
     * @param profissionalId  ID do profissional (pode ser nulo)
     * @param tentativas      Número de tentativas realizadas
     */
    private void registrarFalhaNaMensageria(String email, String nome, String cpf, String senha,
                                            String nomeOrganizacao, String tipoUsuario,
                                            Long organizacaoId, Long profissionalId, int tentativas) {
        if (organizacaoId == null) {
            log.warn("Falha de email não registrada na mensageria: organizacaoId nulo para {}", email);
            return;
        }

        try {
            TipoMensagem tipo = resolverTipoMensagem(tipoUsuario);
            String corpoMensagem = construirCorpoMensagemFalha(nome, cpf, senha, nomeOrganizacao, tipoUsuario);
            String assunto = "Bem-vindo ao Saúde Conecta - Suas credenciais de acesso";

            mensageriaService.registrarFalhaEnvio(
                    organizacaoId,
                    profissionalId,
                    email,
                    nome,
                    assunto,
                    corpoMensagem,
                    tipo,
                    "Falha no envio após " + tentativas + " tentativas",
                    tentativas
            );
        } catch (Exception ex) {
            log.error("Erro ao registrar falha de email na mensageria para {}: {}", email, ex.getMessage());
        }
    }

    /**
     * Resolve o TipoMensagem com base no tipo de usuário.
     */
    private TipoMensagem resolverTipoMensagem(String tipoUsuario) {
        return switch (tipoUsuario.toLowerCase()) {
            case "medico" -> TipoMensagem.EMAIL_CREDENCIAIS_CLINICO;
            case "secretaria" -> TipoMensagem.EMAIL_CREDENCIAIS_SECRETARIA;
            case "administrador" -> TipoMensagem.EMAIL_CREDENCIAIS_ADMINISTRADOR;
            default -> TipoMensagem.EMAIL_GENERICO;
        };
    }

    /**
     * Constrói o corpo da mensagem para registro de falha.
     * Armazena as informações necessárias para reenvio manual pelo administrador.
     */
    private String construirCorpoMensagemFalha(String nome, String cpf, String senha,
                                               String nomeOrganizacao, String tipoUsuario) {
        return String.format(
                "REGISTRO DE FALHA DE ENVIO DE EMAIL\n\n" +
                "Destinatário: %s\n" +
                "Tipo de usuário: %s\n" +
                "Login (CPF): %s\n" +
                "Senha gerada: %s\n" +
                "Organização: %s\n\n" +
                "Este registro foi criado automaticamente após falha no envio do email de credenciais. " +
                "O administrador deve reenviar as credenciais manualmente ao profissional.",
                nome, tipoUsuario, cpf, senha,
                nomeOrganizacao != null ? nomeOrganizacao : "N/A"
        );
    }

    /**
     * Registra o sucesso de envio de email na tabela de mensageria para histórico completo.
     *
     * @param email           Email do destinatário
     * @param nome            Nome do destinatário
     * @param cpf             CPF (login)
     * @param senha           Senha gerada
     * @param nomeOrganizacao Nome da organização
     * @param tipoUsuario     Tipo do usuário (medico, secretaria, administrador)
     * @param organizacaoId   ID da organização
     * @param profissionalId  ID do profissional (pode ser nulo)
     * @param tentativas      Número de tentativas até o sucesso
     */
    private void registrarSucessoNaMensageria(String email, String nome, String cpf, String senha,
                                              String nomeOrganizacao, String tipoUsuario,
                                              Long organizacaoId, Long profissionalId, int tentativas) {
        if (organizacaoId == null) {
            log.debug("Sucesso de email não registrado na mensageria: organizacaoId nulo para {}", email);
            return;
        }

        try {
            TipoMensagem tipo = resolverTipoMensagem(tipoUsuario);
            String corpoMensagem = construirCorpoMensagemSucesso(nome, cpf, senha, nomeOrganizacao, tipoUsuario);
            String assunto = "Bem-vindo ao Saúde Conecta - Suas credenciais de acesso";

            mensageriaService.registrarEnvioSucesso(
                    organizacaoId,
                    profissionalId,
                    email,
                    nome,
                    assunto,
                    corpoMensagem,
                    tipo,
                    tentativas
            );
        } catch (Exception ex) {
            log.error("Erro ao registrar sucesso de email na mensageria para {}: {}", email, ex.getMessage());
        }
    }

    /**
     * Constrói o corpo da mensagem para registro de sucesso.
     * Armazena as informações do envio bem-sucedido para histórico.
     */
    private String construirCorpoMensagemSucesso(String nome, String cpf, String senha,
                                                 String nomeOrganizacao, String tipoUsuario) {
        return String.format(
                "REGISTRO DE ENVIO BEM-SUCEDIDO\n\n" +
                "Destinatário: %s\n" +
                "Tipo de usuário: %s\n" +
                "Login (CPF): %s\n" +
                "Senha gerada: %s\n" +
                "Organização: %s\n\n" +
                "Este registro foi criado automaticamente após o envio bem-sucedido do email de credenciais.",
                nome, tipoUsuario, cpf, senha,
                nomeOrganizacao != null ? nomeOrganizacao : "N/A"
        );
    }
}
