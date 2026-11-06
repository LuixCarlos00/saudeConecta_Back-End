package br.com.saudeConecta.application.notificacao;

import br.com.saudeConecta.config.logging.MascaraLog;
import br.com.saudeConecta.infrastructure.email.EmailRetryService;
import br.com.saudeConecta.infrastructure.email.EmailTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
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
    private static final String PERFIL_ADMINISTRADOR = "Administrador";

    private static final String TEMPLATE_CREDENCIAIS = "email-credenciais";
    private static final String TEMPLATE_RECUPERACAO = "email-recuperacao-senha";
    private static final String TEMPLATE_CHAMADO = "email-chamado-suporte";
    private static final String TEMPLATE_CHAMADO_STATUS = "email-chamado-status";
    private static final String TEMPLATE_ATIVACAO_ASSINATURA = "email-ativacao-assinatura";
    private static final String ASSUNTO_ATIVACAO_ASSINATURA = "Saúde Conecta - Ative sua assinatura";
    private static final String ASSUNTO_CREDENCIAIS = "Bem-vindo ao Saúde Conecta - Suas credenciais de acesso";
    private static final String ASSUNTO_RECUPERACAO = "Saúde Conecta - Recuperação de Senha";

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String COR_ADMINISTRADOR = "#1976D2";
    private static final String COR_MEDICO = "#4CAF50";
    private static final String COR_SECRETARIA = "#9C27B0";
    private static final String COR_PACIENTE = "#FF9800";

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
     * Envia credenciais de acesso para um administrador de forma assíncrona.
     *
     * @param email            email do destinatário
     * @param nome             nome do administrador
     * @param login            login de acesso (CPF ou CNPJ)
     * @param senha            senha gerada
     * @param organizacaoId    ID da organização
     * @param adminOrgId       ID do admin da organização
     */
    @Async
    public void enviarCredenciaisAdministrador(String email, String nome, String login,
                                                String senha, Long organizacaoId, Long adminOrgId) {
        enviarCredenciais(new CredenciaisEmail(email, nome, login, senha, PERFIL_ADMINISTRADOR,
                COR_ADMINISTRADOR, null, organizacaoId, adminOrgId));
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
        enviarCredenciais(new CredenciaisEmail(email, nome, login, senha, "Médico",
                COR_MEDICO, nomeOrganizacao, organizacaoId, profissionalId));
    }

    /**
     * Envia credenciais de acesso para uma secretária de forma assíncrona.
     *
     * @param email         email do destinatário
     * @param nome          nome da secretária
     * @param login         login de acesso (CPF)
     * @param senha         senha gerada
     * @param organizacaoId ID da organização
     * @param secretariaId  ID da secretária
     */
    @Async
    public void enviarCredenciaisSecretaria(String email, String nome, String login, String senha,
                                             Long organizacaoId, Long secretariaId) {
        enviarCredenciais(new CredenciaisEmail(email, nome, login, senha, "Secretária",
                COR_SECRETARIA, null, organizacaoId, secretariaId));
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
            log.warn("email.recuperacao negado motivo=EMAIL_DESABILITADO login={} email={}",
                    MascaraLog.cpf(login), MascaraLog.email(email));
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
                log.error("email.recuperacao falha email={}", MascaraLog.email(email), e);
            }
        }, emailTaskExecutor);
    }

    /**
     * Envia a confirmação de abertura de um chamado de suporte de forma assíncrona.
     *
     * @param dados dados do chamado e do destinatário
     */
    @Async
    public void enviarConfirmacaoChamadoSuporte(DadosEmailConfirmacaoChamado dados) {
        String email = dados.email();
        String nome = dados.nome();
        String protocolo = dados.protocolo();
        String previsaoAtendimento = dados.previsaoAtendimento();
        if (!emailHabilitado) {
            log.warn("email.chamado negado motivo=EMAIL_DESABILITADO email={}", MascaraLog.email(email));
            log.debug("email.chamado desabilitado protocolo={} previsao={}", protocolo, previsaoAtendimento);
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> variaveis = new HashMap<>();
                variaveis.put("nome", nome);
                variaveis.put("protocolo", protocolo);
                variaveis.put("titulo", dados.titulo());
                variaveis.put("corpo", dados.corpo());
                variaveis.put("categoria", dados.categoria());
                variaveis.put("prioridade", dados.prioridade());
                variaveis.put("status", dados.status());
                variaveis.put("prazoDias", dados.prazoDias());
                variaveis.put("previsaoAtendimento", previsaoAtendimento);
                variaveis.put("quantidadeAnexos", dados.quantidadeAnexos());

                String corpoHtml = emailTemplateService.renderizar(TEMPLATE_CHAMADO, variaveis);
                String assunto = "Saúde Conecta - Chamado " + protocolo + " registrado";

                emailRetryService.executarComRetry(
                        email, nome, assunto, corpoHtml,
                        "chamado", dados.organizacaoId(), null);

            } catch (Exception e) {
                log.error("email.chamado falha protocolo={} email={}", protocolo, MascaraLog.email(email), e);
            }
        }, emailTaskExecutor);
    }

    /**
     * Notifica o autor do chamado sobre a atualização de status de forma assíncrona.
     *
     * @param dados dados do chamado, da mudança de status e do destinatário
     */
    @Async
    public void enviarAtualizacaoStatusChamado(DadosEmailAtualizacaoStatusChamado dados) {
        String email = dados.email();
        String nome = dados.nome();
        String protocolo = dados.protocolo();
        String statusAnterior = dados.statusAnterior();
        String statusAtual = dados.statusAtual();
        if (!emailHabilitado) {
            log.warn("email.chamado.status negado motivo=EMAIL_DESABILITADO email={}", MascaraLog.email(email));
            log.debug("email.chamado.status desabilitado protocolo={} de={} para={}", protocolo, statusAnterior, statusAtual);
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> variaveis = new HashMap<>();
                variaveis.put("nome", nome);
                variaveis.put("protocolo", protocolo);
                variaveis.put("titulo", dados.titulo());
                variaveis.put("statusAnterior", statusAnterior);
                variaveis.put("statusAtual", statusAtual);
                variaveis.put("observacao", dados.observacao());

                String corpoHtml = emailTemplateService.renderizar(TEMPLATE_CHAMADO_STATUS, variaveis);
                String assunto = "Saúde Conecta - Chamado " + protocolo + ": " + statusAtual;

                emailRetryService.executarComRetry(
                        email, nome, assunto, corpoHtml,
                        "chamado", dados.organizacaoId(), null);

            } catch (Exception e) {
                log.error("email.chamado.status falha protocolo={} email={}",
                        protocolo, MascaraLog.email(email), e);
            }
        }, emailTaskExecutor);
    }

    /**
     * Envia o link do checkout hospedado do Mercado Pago antes do fim do trial.
     *
     * @param dados dados da assinatura, do link de checkout e do destinatário
     */
    @Async
    public void enviarLinkAtivacaoAssinatura(DadosEmailAtivacaoAssinatura dados) {
        String email = dados.email();
        String nome = dados.nome();
        String plano = dados.plano();
        String initPoint = dados.initPoint();
        LocalDate dataFimTrial = dados.dataFimTrial();
        if (!emailHabilitado) {
            log.warn("email.ativacao negado motivo=EMAIL_DESABILITADO email={}", MascaraLog.email(email));
            log.debug("email.ativacao desabilitado plano={} link={}", plano, MascaraLog.token(initPoint));
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> variaveis = new HashMap<>();
                variaveis.put("nome", nome);
                variaveis.put("plano", plano);
                variaveis.put("valorMensal", formatarMoeda(dados.valorMensal()));
                variaveis.put("initPoint", initPoint);
                variaveis.put("payerEmail", dados.payerEmail());
                variaveis.put("dataFimTrial", dataFimTrial != null
                        ? dataFimTrial.format(FORMATO_DATA) : "");

                String corpoHtml = emailTemplateService.renderizar(TEMPLATE_ATIVACAO_ASSINATURA, variaveis);

                emailRetryService.executarComRetry(
                        email, nome, ASSUNTO_ATIVACAO_ASSINATURA, corpoHtml,
                        PERFIL_ADMINISTRADOR, dados.organizacaoId(), dados.adminOrgId());

            } catch (Exception e) {
                log.error("email.ativacao falha email={}", MascaraLog.email(email), e);
            }
        }, emailTaskExecutor);
    }

    /**
     * Formata um valor monetário no padrão brasileiro para exibição no email.
     */
    private String formatarMoeda(BigDecimal valor) {
        if (valor == null) {
            return "R$ 0,00";
        }
        return NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(valor);
    }

    /**
     * Reenvia um email já registrado na mensageria, atualizando o registro existente.
     *
     * @param mensageriaId   ID do registro existente na mensageria
     * @param destinatario   email do destinatário
     * @param assunto        assunto do email
     * @param corpoHtml      corpo HTML já renderizado
     */
    @Async
    public void reenviarEmail(Long mensageriaId, String destinatario, String assunto, String corpoHtml) {
        if (!emailHabilitado) {
            log.warn("email.reenviar negado motivo=EMAIL_DESABILITADO email={}", MascaraLog.email(destinatario));
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                emailRetryService.executarReenvioComRetry(mensageriaId, destinatario, assunto, corpoHtml);
            } catch (Exception e) {
                log.error("email.reenviar falha email={}", MascaraLog.email(destinatario), e);
            }
        }, emailTaskExecutor);
    }

    /**
     * Envia credenciais de acesso para um paciente de forma assíncrona.
     *
     * @param email         email do destinatário
     * @param nome          nome do paciente
     * @param login         login de acesso (CPF)
     * @param senha         senha gerada
     * @param organizacaoId ID da organização
     * @param pacienteId    ID do paciente
     */
    @Async
    public void enviarCredenciaisPaciente(String email, String nome, String login, String senha,
                                           Long organizacaoId, Long pacienteId) {
        enviarCredenciais(new CredenciaisEmail(email, nome, login, senha, "Paciente",
                COR_PACIENTE, null, organizacaoId, pacienteId));
    }

    /**
     * Método interno que centraliza o envio de credenciais para qualquer tipo de usuário.
     * Renderiza o template unificado e delega ao retry service.
     */
    private void enviarCredenciais(CredenciaisEmail dados) {
        String email = dados.email();
        String nome = dados.nome();
        String login = dados.login();
        String senha = dados.senha();
        String tipoUsuario = dados.tipoUsuario();
        if (!emailHabilitado) {
            log.warn("email.credenciais negado motivo=EMAIL_DESABILITADO tipo={} login={} email={}",
                    tipoUsuario, MascaraLog.cpf(login), MascaraLog.email(email));
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> variaveis = new HashMap<>();
                variaveis.put("nome", nome);
                variaveis.put("login", login);
                variaveis.put("senha", senha);
                variaveis.put("tipoUsuario", tipoUsuario);
                variaveis.put("corTema", dados.corTema());
                variaveis.put("nomeOrganizacao", dados.nomeOrganizacao());

                String corpoHtml = emailTemplateService.renderizar(TEMPLATE_CREDENCIAIS, variaveis);

                String tipoMensageria = resolverTipoMensageria(tipoUsuario);
                emailRetryService.executarComRetry(
                        email, nome, ASSUNTO_CREDENCIAIS, corpoHtml,
                        tipoMensageria, dados.organizacaoId(), dados.entidadeId());

            } catch (Exception e) {
                log.error("email.credenciais falha tipo={} email={}",
                        tipoUsuario, MascaraLog.email(email), e);
            }
        }, emailTaskExecutor);
    }

    /**
     * Dados internos de um email de credenciais, agrupados para o método unificado de envio.
     *
     * @param email           email do destinatário
     * @param nome            nome do destinatário
     * @param login           login de acesso
     * @param senha           senha gerada
     * @param tipoUsuario     label amigável do perfil (Administrador, Médico, Secretária, Paciente)
     * @param corTema         cor do tema do email
     * @param nomeOrganizacao nome da organização (pode ser nulo)
     * @param organizacaoId   ID da organização
     * @param entidadeId      ID da entidade destinatária
     */
    private record CredenciaisEmail(String email, String nome, String login, String senha,
                                    String tipoUsuario, String corTema, String nomeOrganizacao,
                                    Long organizacaoId, Long entidadeId) {
    }

    /**
     * Converte o label amigável do tipo de usuário para a chave usada na mensageria.
     */
    private String resolverTipoMensageria(String tipoUsuario) {
        return switch (tipoUsuario) {
            case PERFIL_ADMINISTRADOR -> "administrador";
            case "Médico" -> "medico";
            case "Secretária" -> "secretaria";
            case "Paciente" -> "paciente";
            default -> "generico";
        };
    }
}
