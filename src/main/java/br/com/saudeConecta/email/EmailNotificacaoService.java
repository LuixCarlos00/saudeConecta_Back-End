package br.com.saudeConecta.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Orquestrador central de envio de emails.
 * Único ponto de entrada para os serviços de domínio (cadastro, recuperação, etc.).
 * Coordena: renderização de template → envio assíncrono → retry → registro na mensageria.
 *
 * Todos os emails são assíncronos, com 3 tentativas de retry e registro na mensageria.
 */
@Service
@Slf4j
public class EmailNotificacaoService {

    private static final String TEMPLATE_CREDENCIAIS = "email-credenciais";
    private static final String TEMPLATE_RECUPERACAO = "email-recuperacao-senha";
    private static final String ASSUNTO_CREDENCIAIS = "Bem-vindo ao Saúde Conecta - Suas credenciais de acesso";
    private static final String ASSUNTO_RECUPERACAO = "Saúde Conecta - Recuperação de Senha";

    private static final String COR_ADMINISTRADOR = "#1976D2";
    private static final String COR_MEDICO = "#4CAF50";
    private static final String COR_SECRETARIA = "#9C27B0";

    private final EmailTemplateService emailTemplateService;
    private final EmailRetryService emailRetryService;
    private final Executor emailTaskExecutor;

    @Value("${app.email.enabled}")
    private boolean emailHabilitado;

    public EmailNotificacaoService(EmailTemplateService emailTemplateService,
                                    EmailRetryService emailRetryService,
                                    @Qualifier("emailTaskExecutor") Executor emailTaskExecutor) {
        this.emailTemplateService = emailTemplateService;
        this.emailRetryService = emailRetryService;
        this.emailTaskExecutor = emailTaskExecutor;
    }

    /**
     * Verifica se o envio de email está habilitado na configuração.
     *
     * @return true se o envio está habilitado
     */
    public boolean isEmailHabilitado() {
        return emailHabilitado;
    }

    /**
     * Envia credenciais de acesso para um administrador de forma assíncrona.
     *
     * @param email         email do destinatário
     * @param nome          nome do administrador
     * @param login         login de acesso (CPF ou CNPJ)
     * @param senha         senha gerada
     * @param organizacaoId ID da organização
     */
    @Async
    public void enviarCredenciaisAdministrador(String email, String nome, String login,
                                                String senha, Long organizacaoId) {
        enviarCredenciais(email, nome, login, senha, "Administrador",
                COR_ADMINISTRADOR, null, organizacaoId, null);
    }

    /**
     * Envia credenciais de acesso para um médico/clínico de forma assíncrona.
     *
     * @param email            email do destinatário
     * @param nome             nome do profissional
     * @param login            login de acesso (CPF)
     * @param senha            senha gerada
     * @param nomeOrganizacao  nome da organização
     * @param organizacaoId    ID da organização
     * @param profissionalId   ID do profissional
     */
    @Async
    public void enviarCredenciaisClinico(String email, String nome, String login, String senha,
                                          String nomeOrganizacao, Long organizacaoId, Long profissionalId) {
        enviarCredenciais(email, nome, login, senha, "Médico",
                COR_MEDICO, nomeOrganizacao, organizacaoId, profissionalId);
    }

    /**
     * Envia credenciais de acesso para uma secretária de forma assíncrona.
     *
     * @param email         email do destinatário
     * @param nome          nome da secretária
     * @param login         login de acesso (CPF)
     * @param senha         senha gerada
     * @param organizacaoId ID da organização
     * @param secretariaId  ID da secretária (usado como profissionalId na mensageria)
     */
    @Async
    public void enviarCredenciaisSecretaria(String email, String nome, String login, String senha,
                                             Long organizacaoId, Long secretariaId) {
        enviarCredenciais(email, nome, login, senha, "Secretária",
                COR_SECRETARIA, null, organizacaoId, secretariaId);
    }

    /**
     * Envia email de recuperação de senha de forma assíncrona.
     *
     * @param email         email do destinatário
     * @param nome          nome do usuário
     * @param login         login do usuário
     * @param novaSenha     nova senha gerada
     * @param organizacaoId ID da organização (pode ser nulo)
     */
    @Async
    public void enviarRecuperacaoSenha(String email, String nome, String login,
                                        String novaSenha, Long organizacaoId) {
        if (!emailHabilitado) {
            log.warn("Email desabilitado. Recuperação não enviada para: {}", email);
            log.info("RECUPERAÇÃO SENHA - Login: {}, Senha: {}", login, novaSenha);
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> variaveis = new HashMap<>();
                variaveis.put("nome", nome);
                variaveis.put("login", login);
                variaveis.put("senha", novaSenha);

                String corpoHtml = emailTemplateService.renderizar(TEMPLATE_RECUPERACAO, variaveis);

                emailRetryService.executarComRetry(
                        email, nome, ASSUNTO_RECUPERACAO, corpoHtml,
                        "recuperacao", organizacaoId, null);

            } catch (Exception e) {
                log.error("Erro ao preparar email de recuperação para {}: {}", email, e.getMessage());
            }
        }, emailTaskExecutor);
    }

    /**
     * Reenvia um email cujo corpo HTML já está renderizado (ex.: reenvio de mensageria).
     * Segue o mesmo fluxo: assíncrono → retry (3 tentativas) → registro na mensageria.
     *
     * @param destinatario   email do destinatário
     * @param nome           nome do destinatário
     * @param assunto        assunto do email
     * @param corpoHtml      HTML já renderizado (armazenado na tabela de mensageria)
     * @param tipoUsuario    tipo do usuário para resolver TipoMensagem
     * @param organizacaoId  ID da organização
     * @param profissionalId ID do profissional (pode ser nulo)
     */
    @Async
    public void reenviarEmail(String destinatario, String nome, String assunto,
                               String corpoHtml, String tipoUsuario,
                               Long organizacaoId, Long profissionalId) {
        if (!emailHabilitado) {
            log.warn("Email desabilitado. Reenvio não realizado para: {}", destinatario);
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                emailRetryService.executarComRetry(
                        destinatario, nome, assunto, corpoHtml,
                        tipoUsuario, organizacaoId, profissionalId);
            } catch (Exception e) {
                log.error("Erro ao reenviar email para {}: {}", destinatario, e.getMessage());
            }
        }, emailTaskExecutor);
    }

    /**
     * Método interno que centraliza o envio de credenciais para qualquer tipo de usuário.
     * Renderiza o template unificado e delega ao retry service.
     */
    private void enviarCredenciais(String email, String nome, String login, String senha,
                                    String tipoUsuario, String corTema, String nomeOrganizacao,
                                    Long organizacaoId, Long profissionalId) {
        if (!emailHabilitado) {
            log.warn("Email desabilitado. Credenciais não enviadas para {}: {}", tipoUsuario, email);
            log.info("CREDENCIAIS {} - Login: {}, Senha: {}", tipoUsuario.toUpperCase(), login, senha);
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> variaveis = new HashMap<>();
                variaveis.put("nome", nome);
                variaveis.put("login", login);
                variaveis.put("senha", senha);
                variaveis.put("tipoUsuario", tipoUsuario);
                variaveis.put("corTema", corTema);
                variaveis.put("nomeOrganizacao", nomeOrganizacao);

                String corpoHtml = emailTemplateService.renderizar(TEMPLATE_CREDENCIAIS, variaveis);

                String tipoMensageria = resolverTipoMensageria(tipoUsuario);
                emailRetryService.executarComRetry(
                        email, nome, ASSUNTO_CREDENCIAIS, corpoHtml,
                        tipoMensageria, organizacaoId, profissionalId);

            } catch (Exception e) {
                log.error("Erro ao preparar email de credenciais {} para {}: {}",
                        tipoUsuario, email, e.getMessage());
            }
        }, emailTaskExecutor);
    }

    /**
     * Converte o label amigável do tipo de usuário para a chave usada na mensageria.
     */
    private String resolverTipoMensageria(String tipoUsuario) {
        return switch (tipoUsuario) {
            case "Administrador" -> "administrador";
            case "Médico" -> "medico";
            case "Secretária" -> "secretaria";
            default -> "generico";
        };
    }
}
