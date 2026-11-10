package br.com.saudeConecta.application.assinatura;

import br.com.saudeConecta.application.assinatura.dto.LinkPagamentoResponse;
import br.com.saudeConecta.application.exception.BusinessException;
import br.com.saudeConecta.application.notificacao.DadosEmailAtivacaoAssinatura;
import br.com.saudeConecta.application.notificacao.EmailNotificacaoService;
import br.com.saudeConecta.domain.FusoHorario;
import br.com.saudeConecta.domain.admin.AdminOrganizacao;
import br.com.saudeConecta.domain.planos.AssinaturaTenant;
import br.com.saudeConecta.domain.planos.CobrancaTenant;
import br.com.saudeConecta.domain.planos.FormaPagamentoCobranca;
import br.com.saudeConecta.domain.planos.MpWebhookEvento;
import br.com.saudeConecta.domain.planos.OrigemCancelamento;
import br.com.saudeConecta.domain.planos.StatusAssinatura;
import br.com.saudeConecta.domain.planos.StatusCobranca;
import br.com.saudeConecta.infrastructure.mercadopago.MercadoPagoClient;
import br.com.saudeConecta.infrastructure.mercadopago.MercadoPagoProperties;
import br.com.saudeConecta.infrastructure.mercadopago.dto.AuthorizedPaymentResponse;
import br.com.saudeConecta.infrastructure.mercadopago.dto.PreapprovalResponse;
import br.com.saudeConecta.infrastructure.persistence.repository.AdminOrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.AssinaturaTenantRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.CobrancaTenantRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.MpWebhookEventoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Regras de negócio da assinatura recorrente no Mercado Pago.
 * Cuida da geração do link de pagamento, do cancelamento e da reconciliação
 * de status entre o Mercado Pago e a base local.
 */
@Service
public class AssinaturaMercadoPagoService {

    private static final Logger log = LoggerFactory.getLogger(AssinaturaMercadoPagoService.class);
    private static final String PREFIXO_EXTERNAL_REFERENCE = "ASSINATURA-";
    /** Código do erro 502 quando o MP não confirma o valor novo (troca de plano desfeita). */
    public static final String CODIGO_MP_VALOR_NAO_ATUALIZADO = "MP_VALOR_NAO_ATUALIZADO";
    /**
     * Código do erro 409 quando um preapproval substituído anterior está em pendência manual
     * (autorizado ou pago depois da troca) e ainda ocupa a coluna de substituído.
     */
    public static final String CODIGO_PREAPPROVAL_SUBSTITUIDO_EM_ANALISE = "PREAPPROVAL_SUBSTITUIDO_EM_ANALISE";
    private static final String PAGAMENTO_APROVADO = "approved";
    private static final String PAGAMENTO_RECUSADO = "rejected";

    /** O que aconteceu ao tentar cancelar um preapproval que saiu da assinatura. */
    private enum DesfechoCancelamento { CANCELADO, PENDENCIA_MANUAL }

    private final AssinaturaTenantRepository assinaturaTenantRepository;
    private final AdminOrganizacaoRepository adminOrganizacaoRepository;
    private final MercadoPagoClient mercadoPagoClient;
    private final MercadoPagoProperties properties;
    private final EmailNotificacaoService emailNotificacaoService;
    private final CobrancaTenantRepository cobrancaTenantRepository;
    /** Pendências manuais (preapproval substituído autorizado ou pago depois da troca). */
    private final MpWebhookEventoRepository eventoRepository;
    /** Transação própria das fases da troca de preapproval (commit independente do chamador). */
    private final TransactionTemplate transacaoNova;
    /** Suspende a transação corrente (usado no afterCommit). */
    private final TransactionTemplate semTransacao;

    public AssinaturaMercadoPagoService(
            AssinaturaTenantRepository assinaturaTenantRepository,
            AdminOrganizacaoRepository adminOrganizacaoRepository,
            MercadoPagoClient mercadoPagoClient,
            MercadoPagoProperties properties,
            EmailNotificacaoService emailNotificacaoService,
            CobrancaTenantRepository cobrancaTenantRepository,
            MpWebhookEventoRepository eventoRepository,
            PlatformTransactionManager transactionManager) {
        this.cobrancaTenantRepository = cobrancaTenantRepository;
        this.eventoRepository = eventoRepository;
        this.transacaoNova = new TransactionTemplate(transactionManager);
        this.transacaoNova.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.semTransacao = new TransactionTemplate(transactionManager);
        this.semTransacao.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
        this.assinaturaTenantRepository = assinaturaTenantRepository;
        this.adminOrganizacaoRepository = adminOrganizacaoRepository;
        this.mercadoPagoClient = mercadoPagoClient;
        this.properties = properties;
        this.emailNotificacaoService = emailNotificacaoService;
    }

    /**
     * Gera (ou reaproveita) o link do checkout hospedado para uma assinatura.
     *
     * Reutiliza o preapproval existente enquanto ele ainda estiver pendente (o init_point
     * continua servindo). Fora isso, troca o preapproval em duas fases:
     * <ol>
     *   <li>com a linha travada (FOR UPDATE), cria o preapproval novo (B) no MP, vincula B e
     *       guarda o antigo (A) em {@code mpPreapprovalSubstituidoId}; commit. Se o MP falhar
     *       ao criar B, nada muda e A segue intacto;</li>
     *   <li>depois do commit, cancela A no MP e limpa a coluna. Se falhar, a reconciliação
     *       tenta de novo enquanto a coluna estiver preenchida.</li>
     * </ol>
     * Como A só é cancelado com B já gravado, o webhook "cancelled" de A sempre encontra a
     * assinatura vinculada a B e é ignorado (OUTRO_PREAPPROVAL_VINCULADO).
     *
     * Não deve ser chamado dentro de uma transação do chamador: a fase 1 usa transação
     * própria (REQUIRES_NEW) e a fase 2 roda depois dela.
     *
     * Visível só no pacote: a entrada é o {@link LinkPagamentoAssinaturaService}, que antes
     * recusa o link novo quando o cliente já pagou o preapproval atual.
     *
     * @param assinatura assinatura do tenant
     * @return dados do checkout, incluindo o init_point
     */
    LinkPagamentoResponse gerarLinkPagamento(AssinaturaTenant assinatura) {
        validarParaLink(assinatura);
        if (linkPendenteReaproveitavel(assinatura)) {
            if (!pendenteCanceladoNoMp(assinatura.getMpPreapprovalId())) {
                log.info("mp.preapproval.criar ignorado motivo=PENDENTE_REAPROVEITADO preapprovalId={} assinaturaId={}",
                        assinatura.getMpPreapprovalId(), assinatura.getId());
                return montarResposta(assinatura);
            }
            marcarPendenteComoCancelado(assinatura.getId(), assinatura.getMpPreapprovalId());
        }

        Long assinaturaId = assinatura.getId();
        AtomicReference<String> criadoNoMp = new AtomicReference<>();
        AtomicReference<String> statusDoSubstituido = new AtomicReference<>();
        LinkPagamentoResponse link;
        try {
            link = transacaoNova.execute(status -> vincularNovoPreapproval(assinaturaId, criadoNoMp, statusDoSubstituido));
        } catch (RuntimeException e) {
            compensarPreapprovalNaoVinculado(criadoNoMp.get(), assinaturaId);
            throw e;
        }
        cancelarPreapprovalSubstituido(assinaturaId, statusDoSubstituido.get());
        return link;
    }

    /**
     * Leva ao Mercado Pago o valor mensal novo (troca de plano, plano personalizado). O valor
     * novo vale a partir do próximo ciclo, sem cobrança proporcional. Roda dentro da transação
     * do chamador, que já travou a linha e gravou o valor: se o MP falhar, a exceção desfaz a
     * troca inteira e nada muda no banco.
     * <ul>
     *   <li>preapproval {@code authorized} ou {@code paused}: atualiza o valor no MP;</li>
     *   <li>{@code pending}: abandona o link (o checkout mostraria o valor antigo). O preapproval
     *       é desvinculado e cancelado depois do commit; o próximo "Pagar" gera outro;</li>
     *   <li>sem preapproval, ou já cancelado: só o banco.</li>
     * </ul>
     *
     * @param assinatura    assinatura gerenciada, já com o valor novo
     * @param valorAnterior valor mensal antes da troca
     * @throws BusinessException 502 {@code MP_VALOR_NAO_ATUALIZADO} quando o MP não confirma
     */
    public void propagarNovoValor(AssinaturaTenant assinatura, BigDecimal valorAnterior) {
        if (!assinatura.temPreapproval() || assinatura.getValorMensal().compareTo(valorAnterior) == 0) {
            return;
        }
        String preapprovalId = assinatura.getMpPreapprovalId();
        try {
            PreapprovalResponse remoto = mercadoPagoClient.buscarPreapproval(preapprovalId);
            String statusMp = remoto != null ? remoto.status() : null;
            if (MercadoPagoClient.STATUS_AUTHORIZED.equals(statusMp) || MercadoPagoClient.STATUS_PAUSED.equals(statusMp)) {
                atualizarValorConfirmando(assinatura, preapprovalId);
                assinatura.setMpStatus(statusMp);
                assinatura.setMpUltimaSincronizacao(LocalDateTime.now(FusoHorario.ZONA));
                log.info("mp.preapproval.valor ok preapprovalId={} assinaturaId={} de={} para={}",
                        preapprovalId, assinatura.getId(), valorAnterior, assinatura.getValorMensal());
            } else if (MercadoPagoClient.STATUS_PENDING.equals(statusMp)) {
                desvincularECancelarAposCommit(assinatura, "LINK_PENDENTE_COM_VALOR_ANTIGO");
            }
        } catch (BusinessException e) {
            log.warn("mp.preapproval.valor negado motivo=MP_INDISPONIVEL preapprovalId={} assinaturaId={} acao=TROCA_DESFEITA",
                    preapprovalId, assinatura.getId());
            throw new BusinessException(
                    "Não foi possível atualizar o valor da assinatura no Mercado Pago. Nada foi alterado; tente novamente.",
                    HttpStatus.BAD_GATEWAY, CODIGO_MP_VALOR_NAO_ATUALIZADO);
        }
    }

    /**
     * PUT do valor novo. Se ele falhar ou estourar o timeout, o MP pode ter aplicado o valor
     * sem a resposta voltar: desfazer a troca deixaria o banco com o valor antigo e o MP
     * cobrando o novo. Por isso relê o preapproval e só desiste se o valor lá não for o novo.
     *
     * @throws BusinessException quando o valor novo não está confirmado no MP (o chamador
     *                           responde 502 e desfaz a troca)
     */
    private void atualizarValorConfirmando(AssinaturaTenant assinatura, String preapprovalId) {
        BigDecimal valorNovo = assinatura.getValorMensal();
        try {
            mercadoPagoClient.atualizarValor(preapprovalId, valorNovo);
        } catch (BusinessException falhaNoPut) {
            PreapprovalResponse relido;
            try {
                relido = mercadoPagoClient.buscarPreapproval(preapprovalId);
            } catch (BusinessException falhaNaReleitura) {
                // Sem saber o valor no MP: a troca é desfeita e a reconciliação acerta o MP pelo banco
                log.warn("mp.preapproval.valor falha motivo=VALOR_INCERTO preapprovalId={} assinaturaId={} valorNovo={} proximo=RECONCILIACAO",
                        preapprovalId, assinatura.getId(), valorNovo);
                throw falhaNoPut;
            }
            if (valorNoMp(relido) == null || valorNoMp(relido).compareTo(valorNovo) != 0) {
                throw falhaNoPut;
            }
            log.warn("mp.preapproval.valor confirmado motivo=RESPOSTA_PERDIDA preapprovalId={} assinaturaId={} valorNovo={}",
                    preapprovalId, assinatura.getId(), valorNovo);
        }
    }

    private static BigDecimal valorNoMp(PreapprovalResponse preapproval) {
        return preapproval != null && preapproval.autoRecurring() != null
                ? preapproval.autoRecurring().transactionAmount() : null;
    }

    /**
     * Desvincula o preapproval atual e agenda o cancelamento dele no MP para depois do commit
     * da transação do chamador (mesmo mecanismo da troca de link). Usado quando o preapproval
     * não deve mais cobrar: link pendente com valor antigo, assinatura que virou cortesia.
     *
     * @param assinatura assinatura gerenciada pela transação do chamador
     * @param motivo     código do motivo, para o log
     * @throws BusinessException se um substituído anterior ainda pendente não puder ser cancelado
     */
    public void desvincularECancelarAposCommit(AssinaturaTenant assinatura, String motivo) {
        desvincularECancelarAposCommit(assinatura, motivo, assinatura.getMpStatus());
    }

    /**
     * Limpeza da cortesia antiga que ainda tem preapproval vinculado (de antes de a cortesia
     * desvincular o preapproval): desvincula e cancela depois do commit. O status local desse
     * preapproval não é confiável, então um {@code authorized} vai para pendência manual em vez
     * de ser cancelado (o cliente pode estar sendo cobrado e o estorno é decisão do SUPER_ADMIN).
     *
     * @param assinatura cortesia gerenciada pela transação do chamador (linha travada)
     */
    public void limparPreapprovalDeCortesia(AssinaturaTenant assinatura) {
        if (!assinatura.isCortesia() || !assinatura.temPreapproval()) {
            return;
        }
        log.info("assinatura.cortesia.limpeza inicio preapprovalId={} assinaturaId={} statusMpLocal={}",
                assinatura.getMpPreapprovalId(), assinatura.getId(), assinatura.getMpStatus());
        desvincularECancelarAposCommit(assinatura, "CORTESIA_COM_PREAPPROVAL", null);
    }

    /**
     * @param statusNaTroca status do preapproval considerado na decisão de cancelar depois do
     *                      commit (null quando o status local não é confiável)
     */
    private void desvincularECancelarAposCommit(AssinaturaTenant assinatura, String motivo, String statusNaTroca) {
        if (!assinatura.temPreapproval()) {
            return;
        }
        if (assinatura.temPreapprovalSubstituidoPendente()) {
            // Só há uma coluna para o antigo: o substituído anterior precisa estar cancelado antes
            cancelarSubstituidoAnteriorOuRecusar(assinatura);
        }
        String preapprovalId = assinatura.getMpPreapprovalId();
        assinatura.desvincularPreapproval();
        Long assinaturaId = assinatura.getId();
        executarAposCommit("mp.preapproval.desvincular", assinaturaId,
                () -> cancelarPreapprovalSubstituido(assinaturaId, statusNaTroca));
        log.info("mp.preapproval.desvincular ok motivo={} preapprovalId={} assinaturaId={}",
                motivo, preapprovalId, assinaturaId);
    }

    /**
     * Roda a ação depois do commit da transação corrente (ou na hora, se não houver
     * transação), suspendendo a transação que acabou de fazer commit: dentro do
     * {@code afterCommit} o Spring ainda a mantém associada à thread e uma escrita com
     * REQUIRED entraria nela e seria perdida. Falhas são registradas e não sobem: o
     * commit já aconteceu.
     *
     * @param evento       nome do evento de log da operação chamadora
     * @param assinaturaId ID da assinatura (para o log)
     * @param acao         o que fazer depois do commit
     */
    public void executarAposCommit(String evento, Long assinaturaId, Runnable acao) {
        Runnable protegida = () -> {
            try {
                semTransacao.executeWithoutResult(status -> acao.run());
            } catch (RuntimeException e) {
                log.error("{} falha etapa=APOS_COMMIT assinaturaId={}", evento, assinaturaId, e);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    protegida.run();
                }
            });
        } else {
            protegida.run();
        }
    }

    /**
     * Compensação: o preapproval foi criado no MP mas a transação que o vincularia não
     * fez commit. Sem cancelar, ele ficaria vivo e sem dono, pronto para cobrar se o
     * cliente abrir o checkout.
     *
     * A exceção pode ter vindo depois do commit (ex.: falha de rede na confirmação do COMMIT):
     * antes de cancelar, relê a assinatura em transação nova e só cancela se o preapproval
     * criado não estiver vinculado. Na dúvida (releitura falhou) não cancela: cancelar um
     * preapproval vinculado quebraria a cobrança, e um pendente sem dono não cobra sozinho.
     */
    private void compensarPreapprovalNaoVinculado(String preapprovalId, Long assinaturaId) {
        if (preapprovalId == null) {
            return;
        }
        Boolean vinculado;
        try {
            vinculado = transacaoNova.execute(status -> assinaturaTenantRepository.findById(assinaturaId)
                    .map(atual -> preapprovalId.equals(atual.getMpPreapprovalId()))
                    .orElse(false));
        } catch (RuntimeException e) {
            log.error("mp.preapproval.compensar falha motivo=RELEITURA_FALHOU preapprovalId={} assinaturaId={} acao=CONFERIR_VINCULO_E_CANCELAR_MANUALMENTE",
                    preapprovalId, assinaturaId, e);
            return;
        }
        if (Boolean.TRUE.equals(vinculado)) {
            log.warn("mp.preapproval.compensar ignorado motivo=PREAPPROVAL_VINCULADO preapprovalId={} assinaturaId={}",
                    preapprovalId, assinaturaId);
            return;
        }
        try {
            mercadoPagoClient.cancelarPreapproval(preapprovalId);
            log.warn("mp.preapproval.criar compensado motivo=FALHA_ANTES_DO_COMMIT preapprovalId={} assinaturaId={}",
                    preapprovalId, assinaturaId);
        } catch (BusinessException e) {
            log.error("mp.preapproval.compensar falha preapprovalId={} assinaturaId={} acao=CANCELAR_MANUALMENTE_NO_MP",
                    preapprovalId, assinaturaId, e);
        }
    }

    /**
     * Fase 1 da troca de preapproval, dentro da transação nova. O FOR UPDATE serializa
     * gerações simultâneas e a troca de plano: quem chega depois relê o estado e reaproveita
     * o link que o primeiro acabou de criar.
     */
    private LinkPagamentoResponse vincularNovoPreapproval(Long assinaturaId, AtomicReference<String> criadoNoMp,
                                                         AtomicReference<String> statusDoSubstituido) {
        AssinaturaTenant assinatura = assinaturaTenantRepository.findByIdParaAtualizacao(assinaturaId)
                .orElseThrow(() -> new BusinessException(
                        "Assinatura não encontrada: " + assinaturaId, HttpStatus.NOT_FOUND));
        validarParaLink(assinatura);
        if (linkPendenteReaproveitavel(assinatura)) {
            log.info("mp.preapproval.criar ignorado motivo=PENDENTE_REAPROVEITADO preapprovalId={} assinaturaId={}",
                    assinatura.getMpPreapprovalId(), assinaturaId);
            return montarResposta(assinatura);
        }
        if (assinatura.temPreapprovalSubstituidoPendente()) {
            // Só há uma coluna para o antigo: o substituído anterior precisa estar cancelado antes.
            // Mesmo sem preapproval vinculado: se o antigo foi autorizado ou pago depois da troca,
            // um link novo levaria o cliente a pagar duas vezes (409 até o SUPER_ADMIN decidir)
            cancelarSubstituidoAnteriorOuRecusar(assinatura);
            assinatura.setMpPreapprovalSubstituidoId(null);
        }
        statusDoSubstituido.set(assinatura.getMpStatus());

        String payerEmail = resolverPayerEmail(assinatura);
        String reason = properties.getReasonPrefix() + " — Plano "
                + assinatura.getPlanoAssinatura().getNome();

        PreapprovalResponse preapproval = mercadoPagoClient.criarPreapprovalPendente(
                reason,
                PREFIXO_EXTERNAL_REFERENCE + assinaturaId,
                payerEmail,
                assinatura.getValorMensal());
        if (preapproval != null) {
            criadoNoMp.set(preapproval.id());
        }

        if (preapproval == null || preapproval.initPoint() == null) {
            throw new BusinessException(
                    "O Mercado Pago não retornou o link de pagamento. Tente novamente.",
                    HttpStatus.BAD_GATEWAY);
        }

        String anterior = assinatura.getMpPreapprovalId();
        assinatura.vincularNovoPreapproval(preapproval.id(), preapproval.status(), preapproval.initPoint(), payerEmail);
        assinatura.setMpUltimaSincronizacao(LocalDateTime.now(FusoHorario.ZONA));
        assinaturaTenantRepository.save(assinatura);

        log.info("mp.preapproval.criar ok preapprovalId={} substituidoId={} assinaturaId={} organizacaoId={}",
                preapproval.id(), anterior, assinaturaId, assinatura.getOrganizacao().getId());

        return montarResposta(assinatura);
    }

    static void validarParaLink(AssinaturaTenant assinatura) {
        if (assinatura.isCortesia()) {
            throw new BusinessException(
                    "Assinaturas de cortesia não possuem cobrança no Mercado Pago",
                    HttpStatus.CONFLICT);
        }
        if (StatusAssinatura.ATIVA.equals(assinatura.getStatus()) && assinatura.temPreapproval()) {
            throw new BusinessException("Esta assinatura já está ativa", HttpStatus.CONFLICT);
        }
    }

    /**
     * O MP cancela sozinho o preapproval quando o cartão é recusado no checkout, e esse
     * cancelamento nem sempre gera webhook. Antes de reaproveitar o link pendente, confere se ele
     * ainda vale: um init_point cancelado levaria o cliente a um checkout morto.
     *
     * @param preapprovalId preapproval pendente vinculado
     * @return true só quando o MP confirma que ele está cancelado; sem resposta, o link é
     *         reaproveitado como antes
     */
    boolean pendenteCanceladoNoMp(String preapprovalId) {
        try {
            PreapprovalResponse remoto = mercadoPagoClient.buscarPreapproval(preapprovalId);
            return remoto != null && preapprovalId.equals(remoto.id())
                    && MercadoPagoClient.STATUS_CANCELLED.equals(remoto.status());
        } catch (BusinessException e) {
            log.warn("mp.preapproval.criar consulta falha motivo=MP_INDISPONIVEL preapprovalId={} acao=REAPROVEITAR_LINK",
                    preapprovalId);
            return false;
        }
    }

    /**
     * Grava o cancelamento remoto do preapproval pendente, com a linha travada, para que a
     * fase 1 não reaproveite o link morto e trate o preapproval como substituído (já cancelado,
     * a fase 2 só limpa a coluna).
     */
    private void marcarPendenteComoCancelado(Long assinaturaId, String preapprovalId) {
        transacaoNova.executeWithoutResult(status -> assinaturaTenantRepository.findByIdParaAtualizacao(assinaturaId)
                .filter(a -> preapprovalId.equals(a.getMpPreapprovalId())
                        && MercadoPagoClient.STATUS_PENDING.equals(a.getMpStatus()))
                .ifPresentOrElse(a -> {
                    // O webhook da recusa costuma chegar depois do novo "Pagar": com o preapproval
                    // já trocado, ele não acharia a assinatura e a recusa sumiria do histórico.
                    registrarRecusasSemRegistro(a, preapprovalId);
                    a.setMpStatus(MercadoPagoClient.STATUS_CANCELLED);
                    a.setMpUltimaSincronizacao(LocalDateTime.now(FusoHorario.ZONA));
                    assinaturaTenantRepository.save(a);
                    log.info("mp.preapproval.criar link descartado motivo=PENDENTE_CANCELADO_NO_MP preapprovalId={} assinaturaId={}",
                            preapprovalId, assinaturaId);
                }, () -> log.info("mp.preapproval.criar link nao descartado motivo=VINCULO_MUDOU preapprovalId={} assinaturaId={}",
                        preapprovalId, assinaturaId)));
    }

    static boolean linkPendenteReaproveitavel(AssinaturaTenant assinatura) {
        return assinatura.temPreapproval() && assinatura.getMpInitPoint() != null
                && MercadoPagoClient.STATUS_PENDING.equals(assinatura.getMpStatus());
    }

    /**
     * Fase 2 da troca: cancela no MP o preapproval substituído e limpa a coluna. Também é
     * a nova tentativa da reconciliação. Não lança exceção: se o MP falhar, a coluna fica
     * preenchida e a reconciliação tenta de novo.
     *
     * Deve rodar fora de transação (ou depois do commit da troca): a chamada HTTP não segura
     * a linha e a limpeza da coluna usa transação própria.
     *
     * Sem saber como o substituído estava na troca (nova tentativa da reconciliação), um
     * {@code authorized} vai para pendência manual em vez de ser cancelado.
     *
     * @param assinaturaId ID da assinatura
     * @return true se o substituído foi confirmado como cancelado e a coluna foi limpa
     */
    public boolean cancelarPreapprovalSubstituido(Long assinaturaId) {
        return cancelarPreapprovalSubstituido(assinaturaId, null);
    }

    /**
     * @param statusNaTroca status local do substituído no momento da troca (null se desconhecido)
     */
    private boolean cancelarPreapprovalSubstituido(Long assinaturaId, String statusNaTroca) {
        // Leitura em transação nova: o chamador pode ter um contexto de persistência com a foto
        // anterior ao commit da troca
        String substituido = transacaoNova.execute(status -> assinaturaTenantRepository.findById(assinaturaId)
                .filter(AssinaturaTenant::temPreapprovalSubstituidoPendente)
                .map(AssinaturaTenant::getMpPreapprovalSubstituidoId)
                .orElse(null));
        if (substituido == null) {
            return false;
        }
        try {
            if (cancelarSeSeguro(assinaturaId, substituido, statusNaTroca) == DesfechoCancelamento.PENDENCIA_MANUAL) {
                // A coluna continua preenchida: os webhooks do substituído seguem ignorados
                return false;
            }
        } catch (BusinessException e) {
            log.warn("mp.preapproval.substituido falha motivo=MP_INDISPONIVEL preapprovalId={} assinaturaId={} proximo=RECONCILIACAO",
                    substituido, assinaturaId);
            return false;
        }

        transacaoNova.executeWithoutResult(status -> assinaturaTenantRepository.findByIdParaAtualizacao(assinaturaId)
                .filter(atual -> substituido.equals(atual.getMpPreapprovalSubstituidoId()))
                .ifPresent(atual -> {
                    atual.setMpPreapprovalSubstituidoId(null);
                    assinaturaTenantRepository.save(atual);
                }));
        log.info("mp.preapproval.substituido ok preapprovalId={} assinaturaId={} statusMp=cancelled",
                substituido, assinaturaId);
        return true;
    }

    /**
     * Cancela no MP um preapproval que saiu da assinatura, se ainda não estiver cancelado
     * (consultar antes evita repetir o PUT na nova tentativa). Não cancela às cegas: se ele foi
     * autorizado depois da troca (ex.: o cliente terminou o checkout do link abandonado) ou tem
     * fatura aprovada sem registro local, o cliente pagou por ele. Nesse caso registra uma
     * pendência manual e deixa a decisão para o SUPER_ADMIN.
     *
     * Um {@code authorized} que já era conhecido na troca (cortesia ou troca de cartão de quem
     * tinha cartão cadastrado) é abandonado de propósito e é cancelado normalmente.
     *
     * @param statusNaTroca status local do preapproval quando ele saiu da assinatura (null se desconhecido)
     * @throws BusinessException quando o Mercado Pago não responde
     */
    private DesfechoCancelamento cancelarSeSeguro(Long assinaturaId, String preapprovalId, String statusNaTroca) {
        PreapprovalResponse remoto = mercadoPagoClient.buscarPreapproval(preapprovalId);
        if (remoto == null || MercadoPagoClient.STATUS_CANCELLED.equals(remoto.status())) {
            return DesfechoCancelamento.CANCELADO;
        }
        boolean cartaoJaConhecido = MercadoPagoClient.STATUS_AUTHORIZED.equals(statusNaTroca)
                || MercadoPagoClient.STATUS_PAUSED.equals(statusNaTroca);
        boolean autorizadoDepoisDaTroca = MercadoPagoClient.STATUS_AUTHORIZED.equals(remoto.status()) && !cartaoJaConhecido;
        long faturasSemRegistro = autorizadoDepoisDaTroca ? 0 : contarFaturasAprovadasSemRegistro(preapprovalId);
        if (autorizadoDepoisDaTroca || faturasSemRegistro > 0) {
            registrarPendenciaSubstituido(assinaturaId, preapprovalId, remoto.status(), faturasSemRegistro);
            return DesfechoCancelamento.PENDENCIA_MANUAL;
        }
        mercadoPagoClient.cancelarPreapproval(preapprovalId);
        return DesfechoCancelamento.CANCELADO;
    }

    /**
     * Faturas aprovadas no MP que não estão registradas como PAGO aqui: a de um preapproval já
     * desvinculado vira {@code PAGAMENTO_SEM_ASSINATURA} no webhook e não gera cobrança local.
     */
    private long contarFaturasAprovadasSemRegistro(String preapprovalId) {
        return mercadoPagoClient.buscarAuthorizedPaymentsDoPreapproval(preapprovalId).stream()
                .filter(fatura -> fatura != null && fatura.id() != null
                        && PAGAMENTO_APROVADO.equals(fatura.statusPagamento()))
                .map(AuthorizedPaymentResponse::id)
                .filter(faturaId -> !cobrancaTenantRepository.findByMpAuthorizedPaymentId(faturaId)
                        .map(CobrancaTenant::isPago).orElse(false))
                .count();
    }

    /**
     * Registra (uma vez por preapproval, em transação própria) a pendência que aparece em
     * {@code /assinaturas/pendencias-manuais}. A nova tentativa da reconciliação não duplica.
     */
    private void registrarPendenciaSubstituido(Long assinaturaId, String preapprovalId, String statusMp,
                                               long faturasSemRegistro) {
        Boolean criada = transacaoNova.execute(status -> {
            if (eventoRepository.findByTipoAndDataId(MpWebhookEvento.TIPO_PENDENCIA_PREAPPROVAL_SUBSTITUIDO, preapprovalId).isPresent()) {
                return false;
            }
            MpWebhookEvento pendencia = MpWebhookEvento.builder()
                    .tipo(MpWebhookEvento.TIPO_PENDENCIA_PREAPPROVAL_SUBSTITUIDO)
                    .dataId(preapprovalId)
                    .processado(true)
                    .recebidoEm(LocalDateTime.now(FusoHorario.ZONA))
                    .build();
            pendencia.marcarParaAtencao(MpWebhookEvento.MOTIVO_PREAPPROVAL_SUBSTITUIDO_AUTORIZADO,
                    "preapproval " + preapprovalId + " saiu da assinatura " + assinaturaId + " e está " + statusMp
                            + " (faturas aprovadas sem registro: " + faturasSemRegistro
                            + ") — estornar e cancelar no MP, ou religar manualmente");
            assinaturaTenantRepository.findById(assinaturaId)
                    .map(a -> a.getOrganizacao() != null ? a.getOrganizacao().getId() : null)
                    .ifPresent(pendencia::setOrganizacaoId);
            eventoRepository.save(pendencia);
            return true;
        });
        if (Boolean.TRUE.equals(criada)) {
            log.warn("mp.preapproval.substituido negado motivo=PREAPPROVAL_SUBSTITUIDO_AUTORIZADO preapprovalId={} assinaturaId={} statusMp={} faturasSemRegistro={} acao=TRATAR_MANUALMENTE",
                    preapprovalId, assinaturaId, statusMp, faturasSemRegistro);
        } else {
            log.info("mp.preapproval.substituido ignorado motivo=PENDENCIA_JA_REGISTRADA preapprovalId={} assinaturaId={}",
                    preapprovalId, assinaturaId);
        }
    }

    /**
     * Antes de ocupar de novo a coluna de substituído, o anterior precisa estar cancelado. Se
     * ele está em pendência manual, a troca é recusada: sobrescrever a coluna faria os webhooks
     * dele deixarem de ser ignorados.
     *
     * @throws BusinessException 409 {@code PREAPPROVAL_SUBSTITUIDO_EM_ANALISE}, ou a falha do MP
     */
    private void cancelarSubstituidoAnteriorOuRecusar(AssinaturaTenant assinatura) {
        String anterior = assinatura.getMpPreapprovalSubstituidoId();
        if (cancelarSeSeguro(assinatura.getId(), anterior, null) == DesfechoCancelamento.PENDENCIA_MANUAL) {
            throw new BusinessException(
                    "Há um pagamento anterior desta assinatura em análise pelo suporte. Tente novamente mais tarde.",
                    HttpStatus.CONFLICT, CODIGO_PREAPPROVAL_SUBSTITUIDO_EM_ANALISE);
        }
    }

    /**
     * Cancela a assinatura a pedido do próprio cliente.
     * O cancelamento é propagado ao Mercado Pago antes de ser refletido localmente.
     *
     * @param organizacaoId ID da organização
     */
    @Transactional
    public void cancelarPeloCliente(Long organizacaoId) {
        log.info("assinatura.cancelar inicio origem=CLIENTE organizacaoId={}", organizacaoId);
        AssinaturaTenant assinatura = assinaturaTenantRepository
                .findAssinaturaAtivaByOrganizacaoId(organizacaoId)
                .orElseThrow(() -> new BusinessException(
                        "Nenhuma assinatura ativa encontrada para a organização",
                        HttpStatus.NOT_FOUND));

        cancelar(assinatura, OrigemCancelamento.CLIENTE);
    }

    /**
     * Cancela a assinatura local e, quando houver, também no Mercado Pago.
     *
     * @param assinatura assinatura a cancelar
     * @param origem     quem originou o cancelamento
     */
    @Transactional
    public void cancelar(AssinaturaTenant assinatura, OrigemCancelamento origem) {
        if (StatusAssinatura.CANCELADA.equals(assinatura.getStatus())) {
            log.info("assinatura.cancelar ignorado motivo=JA_CANCELADA assinaturaId={}", assinatura.getId());
            return;
        }

        if (assinatura.temPreapproval() && !MercadoPagoClient.STATUS_CANCELLED.equals(assinatura.getMpStatus())) {
            PreapprovalResponse resposta = mercadoPagoClient.cancelarPreapproval(assinatura.getMpPreapprovalId());
            assinatura.setMpStatus(resposta != null ? resposta.status() : MercadoPagoClient.STATUS_CANCELLED);
        }

        assinatura.setStatus(StatusAssinatura.CANCELADA);
        assinatura.setDataCancelamento(LocalDateTime.now(FusoHorario.ZONA));
        assinatura.setCanceladoPor(origem);
        assinatura.setMpUltimaSincronizacao(LocalDateTime.now(FusoHorario.ZONA));
        assinaturaTenantRepository.save(assinatura);

        log.info("assinatura.cancelar ok assinaturaId={} origem={} organizacaoId={} status={}",
                assinatura.getId(), origem, assinatura.getOrganizacao().getId(), assinatura.getStatus());
    }

    /**
     * Pausa a cobrança no Mercado Pago quando o tenant é suspenso por inadimplência.
     * Falhas de comunicação não interrompem a suspensão local.
     *
     * @param assinatura assinatura suspensa
     */
    @Transactional
    public void pausar(AssinaturaTenant assinatura) {
        if (!assinatura.temPreapproval() || MercadoPagoClient.STATUS_PAUSED.equals(assinatura.getMpStatus())) {
            return;
        }
        try {
            PreapprovalResponse resposta = mercadoPagoClient.pausarPreapproval(assinatura.getMpPreapprovalId());
            assinatura.setMpStatus(resposta != null ? resposta.status() : MercadoPagoClient.STATUS_PAUSED);
            assinatura.setMpUltimaSincronizacao(LocalDateTime.now(FusoHorario.ZONA));
            assinaturaTenantRepository.save(assinatura);
        } catch (BusinessException e) {
            log.error("mp.preapproval.pausar falha preapprovalId={} assinaturaId={}",
                    assinatura.getMpPreapprovalId(), assinatura.getId(), e);
        }
    }

    /**
     * Reativa no Mercado Pago o preapproval pausado (pela suspensão) depois que um
     * pagamento aprovado renovou a assinatura. Falha de comunicação não desfaz a
     * renovação: fica no log para reativação manual e a reconciliação mantém o espelho.
     *
     * @param assinatura assinatura já renovada (gerenciada pela transação corrente)
     */
    public void reativarSePausado(AssinaturaTenant assinatura) {
        if (!assinatura.temPreapproval() || !MercadoPagoClient.STATUS_PAUSED.equals(assinatura.getMpStatus())) {
            return;
        }
        try {
            PreapprovalResponse resposta = mercadoPagoClient.reativarPreapproval(assinatura.getMpPreapprovalId());
            assinatura.setMpStatus(resposta != null && resposta.status() != null
                    ? resposta.status() : MercadoPagoClient.STATUS_AUTHORIZED);
            assinatura.setMpUltimaSincronizacao(LocalDateTime.now(FusoHorario.ZONA));
            assinaturaTenantRepository.save(assinatura);
            log.info("mp.preapproval.reativar ok preapprovalId={} assinaturaId={}",
                    assinatura.getMpPreapprovalId(), assinatura.getId());
        } catch (BusinessException e) {
            log.error("mp.preapproval.reativar falha preapprovalId={} assinaturaId={} acao=REATIVAR_MANUALMENTE_NO_MP",
                    assinatura.getMpPreapprovalId(), assinatura.getId(), e);
        }
    }

    /**
     * Consulta no Mercado Pago a data da próxima cobrança de um preapproval com cartão
     * autorizado. Usada ao remover a cortesia de quem já tem cartão válido: o próprio MP
     * vai cobrar nessa data, então ela é o vencimento coerente.
     *
     * @param assinatura assinatura local
     * @return next_payment_date do preapproval, ou null se não há preapproval, ele não está
     *         authorized ou o MP não informou a data
     * @throws BusinessException quando o Mercado Pago não responde
     */
    public LocalDate buscarProximaCobrancaAutorizada(AssinaturaTenant assinatura) {
        if (!assinatura.temPreapproval()) {
            return null;
        }
        PreapprovalResponse remoto = mercadoPagoClient.buscarPreapproval(assinatura.getMpPreapprovalId());
        if (remoto == null || !MercadoPagoClient.STATUS_AUTHORIZED.equals(remoto.status())) {
            return null;
        }
        return remoto.proximaCobranca();
    }

    /**
     * Reconcilia o status local com o status atual no Mercado Pago.
     * Rede de segurança para webhooks perdidos.
     *
     * @param assinatura assinatura com preapproval vinculado
     * @return true se houve alteração no espelho do MP ou no status local
     */
    @Transactional
    public boolean sincronizar(AssinaturaTenant assinatura) {
        // Cortesia não é cobrada: a reconciliação não pode reativar nem suspender por ela
        if (!assinatura.temPreapproval() || assinatura.isCortesia()) {
            return false;
        }

        PreapprovalResponse remoto = mercadoPagoClient.buscarPreapproval(assinatura.getMpPreapprovalId());
        if (remoto == null || remoto.status() == null) {
            return false;
        }

        boolean mudou = !remoto.status().equals(assinatura.getMpStatus());
        assinatura.setMpStatus(remoto.status());
        assinatura.setMpUltimaSincronizacao(LocalDateTime.now(FusoHorario.ZONA));

        StatusAssinatura statusAnterior = assinatura.getStatus();
        if (aplicarStatusRemoto(assinatura, remoto.status())) {
            log.info("mp.reconciliar status assinaturaId={} de={} para={} statusMp={}",
                    assinatura.getId(), statusAnterior, assinatura.getStatus(), remoto.status());
            mudou = true;
        }

        assinaturaTenantRepository.save(assinatura);
        mudou = corrigirValorDivergente(assinatura, remoto) || mudou;

        // Retry da reativação: se ela falhou após o pagamento, a assinatura está em dia mas o
        // MP segue pausado e não cobraria o próximo mês
        if (MercadoPagoClient.STATUS_PAUSED.equals(remoto.status()) && ativaEmDia(assinatura)) {
            log.info("mp.reconciliar reativar motivo=ATIVA_COM_PREAPPROVAL_PAUSADO assinaturaId={}",
                    assinatura.getId());
            reativarSePausado(assinatura);
            mudou = mudou || !MercadoPagoClient.STATUS_PAUSED.equals(assinatura.getMpStatus());
        }
        return mudou;
    }

    /**
     * O banco é a fonte do valor mensal. Se o preapproval cobra outro valor (ex.: PUT da troca
     * de plano aplicado no MP sem a resposta voltar, troca desfeita no banco), leva ao MP o
     * valor do banco. Só para {@code authorized}/{@code paused}, como na troca de plano; falha
     * do MP fica para a próxima reconciliação.
     *
     * @return true se o valor foi corrigido no MP
     */
    private boolean corrigirValorDivergente(AssinaturaTenant assinatura, PreapprovalResponse remoto) {
        BigDecimal valorMp = valorNoMp(remoto);
        if (valorMp == null || assinatura.getValorMensal() == null
                || valorMp.compareTo(assinatura.getValorMensal()) == 0
                || !(MercadoPagoClient.STATUS_AUTHORIZED.equals(remoto.status())
                        || MercadoPagoClient.STATUS_PAUSED.equals(remoto.status()))) {
            return false;
        }
        try {
            mercadoPagoClient.atualizarValor(assinatura.getMpPreapprovalId(), assinatura.getValorMensal());
            log.warn("mp.reconciliar valor motivo=VALOR_DIVERGENTE preapprovalId={} assinaturaId={} valorMp={} valorBanco={} acao=MP_CORRIGIDO",
                    assinatura.getMpPreapprovalId(), assinatura.getId(), valorMp, assinatura.getValorMensal());
            return true;
        } catch (BusinessException e) {
            log.warn("mp.reconciliar valor falha motivo=VALOR_DIVERGENTE preapprovalId={} assinaturaId={} valorMp={} valorBanco={} proximo=RECONCILIACAO",
                    assinatura.getMpPreapprovalId(), assinatura.getId(), valorMp, assinatura.getValorMensal());
            return false;
        }
    }

    /**
     * Reflete no status local apenas as mudanças remotas que a régua não decide sozinha:
     * pausa (SUSPENSA) e cancelamento (CANCELADA).
     *
     * {@code authorized} no Mercado Pago significa "cartão autorizado", não "fatura paga":
     * não promove TRIAL, INADIMPLENTE nem SUSPENSA e não mexe no vencimento. Só a fatura
     * (authorized_payment) aprovada reativa e renova a assinatura. CANCELADA é terminal.
     *
     * @param assinatura assinatura local (não é salva aqui)
     * @param mpStatus   status atual do preapproval no Mercado Pago
     * @return true se o status local mudou
     */
    public boolean aplicarStatusRemoto(AssinaturaTenant assinatura, String mpStatus) {
        if (mpStatus == null || StatusAssinatura.CANCELADA.equals(assinatura.getStatus())) {
            return false;
        }
        switch (mpStatus) {
            case MercadoPagoClient.STATUS_PAUSED -> {
                if (StatusAssinatura.SUSPENSA.equals(assinatura.getStatus()) || ativaEmDia(assinatura)) {
                    return false;
                }
                assinatura.setStatus(StatusAssinatura.SUSPENSA);
                assinatura.setDataSuspensao(LocalDateTime.now(FusoHorario.ZONA));
                return true;
            }
            case MercadoPagoClient.STATUS_CANCELLED -> {
                // Decisão de 10/11/2026: no TRIAL, o cancelamento no MP (que o próprio MP faz ao
                // recusar o cartão no 1º checkout) não encerra a assinatura. Ela segue até o fim do
                // trial e depois entra na régua; o próximo "Pagar" gera um link novo.
                if (StatusAssinatura.TRIAL.equals(assinatura.getStatus())) {
                    desvincularCanceladoDoTrial(assinatura);
                    return false;
                }
                assinatura.setStatus(StatusAssinatura.CANCELADA);
                assinatura.setDataCancelamento(LocalDateTime.now(FusoHorario.ZONA));
                assinatura.setCanceladoPor(OrigemCancelamento.MERCADO_PAGO);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    /**
     * Tira do TRIAL o preapproval cancelado no MP. Se ele ficasse vinculado, a reconciliação o
     * acharia de novo depois que a régua levasse o trial vencido a INADIMPLENTE e aplicaria o
     * cancelamento, pulando a régua. Sem vínculo, o webhook atrasado cai em CANCELADO_SEM_VINCULO
     * e a rotina de trials manda um link novo. O preapproval não vira "substituído": já está
     * cancelado no MP.
     *
     * Uma fatura aprovada sem registro local (webhook perdido) mantém o vínculo: a recuperação
     * de faturas registra o pagamento e renova a assinatura antes de qualquer decisão.
     */
    private void desvincularCanceladoDoTrial(AssinaturaTenant assinatura) {
        String preapprovalId = assinatura.getMpPreapprovalId();
        if (preapprovalId == null) {
            return;
        }
        if (contarFaturasAprovadasSemRegistro(preapprovalId) > 0) {
            log.warn("mp.preapproval cancelado ignorado motivo=TRIAL_COM_FATURA_SEM_REGISTRO preapprovalId={} assinaturaId={} acao=AGUARDAR_RECUPERACAO",
                    preapprovalId, assinatura.getId());
            return;
        }
        registrarRecusasSemRegistro(assinatura, preapprovalId);
        assinatura.setMpPreapprovalId(null);
        assinatura.setMpStatus(null);
        assinatura.setMpInitPoint(null);
        log.info("mp.preapproval cancelado desvinculado motivo=TRIAL_SEGUE preapprovalId={} assinaturaId={}",
                preapprovalId, assinatura.getId());
    }

    /**
     * Sem o vínculo (preapproval desvinculado ou trocado por um link novo), o webhook da fatura
     * recusada que chegar depois não acha a assinatura e a recusa sumiria do histórico. Por isso
     * as recusas ainda sem registro são gravadas antes.
     */
    private void registrarRecusasSemRegistro(AssinaturaTenant assinatura, String preapprovalId) {
        for (AuthorizedPaymentResponse fatura : mercadoPagoClient.buscarAuthorizedPaymentsDoPreapproval(preapprovalId)) {
            if (fatura == null || fatura.id() == null || !PAGAMENTO_RECUSADO.equals(fatura.statusPagamento())
                    || cobrancaTenantRepository.findByMpAuthorizedPaymentId(fatura.id()).isPresent()) {
                continue;
            }
            cobrancaTenantRepository.save(CobrancaTenant.builder()
                    .assinaturaTenant(assinatura)
                    .organizacao(assinatura.getOrganizacao())
                    .valorTotal(fatura.transactionAmount() != null ? fatura.transactionAmount() : assinatura.getValorMensal())
                    .status(StatusCobranca.RECUSADO)
                    .motivoRecusa(fatura.motivoRecusa())
                    .formaPagamento(FormaPagamentoCobranca.MERCADO_PAGO)
                    .mpAuthorizedPaymentId(fatura.id())
                    .mpPaymentId(fatura.payment() != null && fatura.payment().id() != null
                            ? String.valueOf(fatura.payment().id()) : null)
                    .build());
            log.info("mp.fatura recusada registrada motivo=ANTES_DO_DESVINCULO assinaturaId={} faturaId={}",
                    assinatura.getId(), fatura.id());
        }
    }

    /**
     * ATIVA com vencimento hoje ou no futuro já teve a fatura do ciclo paga: uma pausa
     * remota (ex.: pausa antiga ainda não reativada) não pode bloquear quem está em dia.
     * Se a pausa persistir, a régua trata a assinatura quando ela vencer.
     */
    private static boolean ativaEmDia(AssinaturaTenant assinatura) {
        return StatusAssinatura.ATIVA.equals(assinatura.getStatus())
                && assinatura.getDataVencimento() != null
                && !assinatura.getDataVencimento().isBefore(LocalDate.now(FusoHorario.ZONA));
    }

    /**
     * Determina o e-mail do pagador: o do administrador owner da organização.
     * Esse e-mail precisa corresponder à conta Mercado Pago usada no checkout,
     * caso contrário o pagamento é recusado pelo próprio Mercado Pago.
     */
    private String resolverPayerEmail(AssinaturaTenant assinatura) {
        if (assinatura.getMpPayerEmail() != null && !assinatura.getMpPayerEmail().isBlank()) {
            return assinatura.getMpPayerEmail();
        }

        return adminOrganizacaoRepository
                .findOwnerByOrganizacaoId(assinatura.getOrganizacao().getId())
                .map(admin -> admin.getEmail())
                .filter(email -> email != null && !email.isBlank())
                .orElseThrow(() -> new BusinessException(
                        "Organização sem e-mail de administrador cadastrado para o pagamento",
                        HttpStatus.UNPROCESSABLE_ENTITY));
    }

    private LinkPagamentoResponse montarResposta(AssinaturaTenant assinatura) {
        return new LinkPagamentoResponse(
                assinatura.getMpPreapprovalId(),
                assinatura.getMpInitPoint(),
                assinatura.getMpPayerEmail(),
                assinatura.getValorMensal());
    }

    /**
     * Envia ao administrador owner o e-mail com o link, relendo a assinatura (fora de transação)
     * e só se o link ainda for o vinculado a ela.
     *
     * @param assinaturaId ID da assinatura
     * @param link         link gerado
     */
    void notificarLinkDePagamento(Long assinaturaId, LinkPagamentoResponse link) {
        // Releitura com plano e organização carregados: a chamada vem de fora de transação
        AssinaturaTenant assinatura = assinaturaTenantRepository.findByMpPreapprovalId(link.preapprovalId())
                .orElse(null);
        if (assinatura == null || !assinaturaId.equals(assinatura.getId())) {
            log.warn("mp.link.enviar negado motivo=LINK_NAO_VINCULADO assinaturaId={} preapprovalId={}",
                    assinaturaId, link.preapprovalId());
            return;
        }
        AdminOrganizacao owner = adminOrganizacaoRepository
                .findOwnerByOrganizacaoId(assinatura.getOrganizacao().getId())
                .orElse(null);

        if (owner == null || owner.getEmail() == null || owner.getEmail().isBlank()) {
            log.warn("mp.link.enviar negado motivo=SEM_ADMIN_COM_EMAIL assinaturaId={}", assinatura.getId());
            return;
        }

        emailNotificacaoService.enviarLinkAtivacaoAssinatura(new DadosEmailAtivacaoAssinatura(
                owner.getEmail(),
                owner.getNome(),
                assinatura.getPlanoAssinatura().getNome(),
                assinatura.getValorMensal(),
                link.initPoint(),
                link.payerEmail(),
                assinatura.getDataVencimento(),
                assinatura.getOrganizacao().getId(),
                owner.getId()));
    }
}
