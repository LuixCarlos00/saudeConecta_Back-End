package br.com.saudeConecta.email;

import br.com.saudeConecta.util.MascaraLog;

import br.com.saudeConecta.domain.mensageria.TipoMensagem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Serviço responsável exclusivamente pela lógica de retry com backoff exponencial.
 * Tenta enviar o email até 3 vezes. Após cada resultado (sucesso ou falha final),
 * delega o registro à EmailMensageriaService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailRetryService {

    private static final int MAX_TENTATIVAS = 3;
    private static final long DELAY_INICIAL_MS = 5000;

    private final EmailRemetenteService emailRemetenteService;
    private final EmailMensageriaService emailMensageriaService;

    /**
     * Executa o envio de email HTML com retry e backoff exponencial.
     * Registra sucesso ou falha na mensageria após conclusão.
     *
     * @param destinatario   email do destinatário
     * @param nome           nome do destinatário
     * @param assunto        assunto do email
     * @param corpoHtml      HTML já renderizado
     * @param tipoUsuario    tipo do usuário (para resolver TipoMensagem)
     * @param organizacaoId  ID da organização (pode ser nulo)
     * @param entidadeId     ID da entidade destinatária (profissional, secretária, admin ou paciente)
     */
    public void executarComRetry(String destinatario, String nome, String assunto,
                                  String corpoHtml, String tipoUsuario,
                                  Long organizacaoId, Long entidadeId) {
        executarTentativa(destinatario, nome, assunto, corpoHtml, tipoUsuario,
                organizacaoId, entidadeId, 1);
    }

    /**
     * Executa uma tentativa de envio. Em caso de falha, agenda a próxima tentativa
     * com delay exponencial. Após esgotar tentativas, registra falha na mensageria.
     */
    private void executarTentativa(String destinatario, String nome, String assunto,
                                    String corpoHtml, String tipoUsuario,
                                    Long organizacaoId, Long entidadeId, int tentativa) {
        try {
            log.info("Tentativa {}/{} de envio para: {} [{}]", tentativa, MAX_TENTATIVAS, MascaraLog.email(destinatario), tipoUsuario);
            emailRemetenteService.enviarHtml(destinatario, assunto, corpoHtml);

            log.info("Email enviado com sucesso na tentativa {} para: {}", tentativa, MascaraLog.email(destinatario));
            TipoMensagem tipo = emailMensageriaService.resolverTipoMensagem(tipoUsuario);
            emailMensageriaService.registrarSucesso(
                    organizacaoId, entidadeId, destinatario, nome, assunto, corpoHtml, tipo, tentativa);

        } catch (Exception e) {
            log.warn("Erro na tentativa {}/{} para {} [{}]: {}",
                    tentativa, MAX_TENTATIVAS, MascaraLog.email(destinatario), tipoUsuario, e.getMessage());

            if (tentativa >= MAX_TENTATIVAS) {
                log.error("Falha definitiva após {} tentativas para: {}", MAX_TENTATIVAS, MascaraLog.email(destinatario), e);
                TipoMensagem tipo = emailMensageriaService.resolverTipoMensagem(tipoUsuario);
                emailMensageriaService.registrarFalha(
                        organizacaoId, entidadeId, destinatario, nome, assunto, corpoHtml,
                        tipo, "Falha no envio após " + MAX_TENTATIVAS + " tentativas: " + e.getMessage(),
                        MAX_TENTATIVAS);
                return;
            }

            long delayMs = DELAY_INICIAL_MS * (long) Math.pow(2, tentativa - 1);
            log.info("Próxima tentativa em {}ms para: {}", delayMs, MascaraLog.email(destinatario));

            CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS).execute(comMdc(() ->
                    executarTentativa(destinatario, nome, assunto, corpoHtml, tipoUsuario,
                            organizacaoId, entidadeId, tentativa + 1)));
        }
    }

    // ==========================================
    // REENVIO (atualiza registro existente)
    // ==========================================

    /**
     * Executa reenvio de email com retry, atualizando o registro existente na mensageria.
     * Diferente do executarComRetry que cria um novo registro, este atualiza o registro original.
     *
     * @param mensageriaId  ID do registro existente na mensageria
     * @param destinatario  email do destinatário
     * @param assunto       assunto do email
     * @param corpoHtml     HTML já renderizado
     */
    public void executarReenvioComRetry(Long mensageriaId, String destinatario,
                                         String assunto, String corpoHtml) {
        executarTentativaReenvio(mensageriaId, destinatario, assunto, corpoHtml, 1);
    }

    /**
     * Executa uma tentativa de reenvio. Atualiza o registro existente ao invés de criar novo.
     */
    private void executarTentativaReenvio(Long mensageriaId, String destinatario,
                                           String assunto, String corpoHtml, int tentativa) {
        try {
            log.info("Reenvio tentativa {}/{} para: {} [mensageriaId={}]",
                    tentativa, MAX_TENTATIVAS, MascaraLog.email(destinatario), mensageriaId);
            emailRemetenteService.enviarHtml(destinatario, assunto, corpoHtml);

            log.info("Reenvio com sucesso na tentativa {} para: {}", tentativa, MascaraLog.email(destinatario));
            emailMensageriaService.atualizarSucesso(mensageriaId, tentativa);

        } catch (Exception e) {
            log.warn("Erro no reenvio tentativa {}/{} para {} [mensageriaId={}]: {}",
                    tentativa, MAX_TENTATIVAS, MascaraLog.email(destinatario), mensageriaId, e.getMessage());

            if (tentativa >= MAX_TENTATIVAS) {
                log.error("Falha definitiva no reenvio após {} tentativas para: {}", MAX_TENTATIVAS, MascaraLog.email(destinatario), e);
                emailMensageriaService.atualizarFalha(mensageriaId,
                        "Falha no reenvio após " + MAX_TENTATIVAS + " tentativas: " + e.getMessage(),
                        tentativa);
                return;
            }

            long delayMs = DELAY_INICIAL_MS * (long) Math.pow(2, tentativa - 1);
            log.info("Próxima tentativa de reenvio em {}ms para: {}", delayMs, MascaraLog.email(destinatario));

            CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS).execute(comMdc(() ->
                    executarTentativaReenvio(mensageriaId, destinatario, assunto, corpoHtml, tentativa + 1)));
        }
    }

    /**
     * Envolve o Runnable copiando o MDC (requestId/tenantId/userId) da thread atual,
     * pois o delayedExecutor roda em outra thread e perderia o contexto de log.
     *
     * @param tarefa tarefa a executar com o MDC capturado
     * @return Runnable que restaura o MDC durante a execução e o limpa ao final
     */
    private static Runnable comMdc(Runnable tarefa) {
        Map<String, String> contexto = MDC.getCopyOfContextMap();
        return () -> {
            if (contexto != null) {
                MDC.setContextMap(contexto);
            }
            try {
                tarefa.run();
            } finally {
                MDC.clear();
            }
        };
    }
}
