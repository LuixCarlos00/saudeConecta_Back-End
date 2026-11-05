package br.com.saudeConecta.service.mercadopago;

import br.com.saudeConecta.domain.admin.AdminOrganizacao;
import br.com.saudeConecta.domain.planos.AssinaturaTenant;
import br.com.saudeConecta.domain.planos.OrigemCancelamento;
import br.com.saudeConecta.domain.planos.StatusAssinatura;
import br.com.saudeConecta.email.EmailNotificacaoService;
import br.com.saudeConecta.infra.exceptions.BusinessException;
import br.com.saudeConecta.infrastructure.persistence.repository.AdminOrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.AssinaturaTenantRepository;
import br.com.saudeConecta.presentation.dto.planos.LinkPagamentoResponse;
import br.com.saudeConecta.service.mercadopago.dto.PreapprovalResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
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
    /** Código do erro 409 quando a recuperação de faturas mostra que o cliente já pagou. */
    public static final String CODIGO_PAGAMENTO_JA_RECEBIDO = "PAGAMENTO_JA_RECEBIDO";
    /** Código do erro 502 quando o MP não confirma o valor novo (troca de plano desfeita). */
    public static final String CODIGO_MP_VALOR_NAO_ATUALIZADO = "MP_VALOR_NAO_ATUALIZADO";

    private final AssinaturaTenantRepository assinaturaTenantRepository;
    private final AdminOrganizacaoRepository adminOrganizacaoRepository;
    private final MercadoPagoClient mercadoPagoClient;
    private final MercadoPagoProperties properties;
    private final EmailNotificacaoService emailNotificacaoService;
    /** Transação própria das fases da troca de preapproval (commit independente do chamador). */
    private final TransactionTemplate transacaoNova;
    /** Suspende a transação corrente (usado no afterCommit). */
    private final TransactionTemplate semTransacao;
    /** Recuperação de faturas aprovadas; @Lazy porque o webhook também depende deste serviço. */
    private final MercadoPagoWebhookService webhookService;

    public AssinaturaMercadoPagoService(
            AssinaturaTenantRepository assinaturaTenantRepository,
            AdminOrganizacaoRepository adminOrganizacaoRepository,
            MercadoPagoClient mercadoPagoClient,
            MercadoPagoProperties properties,
            EmailNotificacaoService emailNotificacaoService,
            PlatformTransactionManager transactionManager,
            @Lazy MercadoPagoWebhookService webhookService) {
        this.transacaoNova = new TransactionTemplate(transactionManager);
        this.transacaoNova.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.semTransacao = new TransactionTemplate(transactionManager);
        this.semTransacao.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
        this.webhookService = webhookService;
        this.assinaturaTenantRepository = assinaturaTenantRepository;
        this.adminOrganizacaoRepository = adminOrganizacaoRepository;
        this.mercadoPagoClient = mercadoPagoClient;
        this.properties = properties;
        this.emailNotificacaoService = emailNotificacaoService;
    }

    /**
     * Gera (ou reaproveita) o link do checkout hospedado para a organização logada.
     * Não abre transação: a troca de preapproval tem fases com commits próprios
     * (ver {@link #gerarLinkPagamento(AssinaturaTenant)}).
     *
     * @param organizacaoId ID da organização
     * @return dados do checkout, incluindo o init_point
     */
    public LinkPagamentoResponse gerarLinkPagamento(Long organizacaoId) {
        log.info("mp.preapproval.criar inicio organizacaoId={}", organizacaoId);
        // Vigente inclui a SUSPENSA: é justamente quem mais precisa do link para voltar a pagar
        AssinaturaTenant assinatura = assinaturaTenantRepository
                .findAssinaturaVigenteByOrganizacaoId(organizacaoId)
                .orElseThrow(() -> new BusinessException(
                        "Nenhuma assinatura vigente encontrada para a organização",
                        HttpStatus.NOT_FOUND));

        return gerarLinkPagamento(assinatura);
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
     * @param assinatura assinatura do tenant
     * @return dados do checkout, incluindo o init_point
     */
    public LinkPagamentoResponse gerarLinkPagamento(AssinaturaTenant assinatura) {
        validarParaLink(assinatura);
        if (linkPendenteReaproveitavel(assinatura)) {
            log.info("mp.preapproval.criar ignorado motivo=PENDENTE_REAPROVEITADO preapprovalId={} assinaturaId={}",
                    assinatura.getMpPreapprovalId(), assinatura.getId());
            return montarResposta(assinatura);
        }

        Long assinaturaId = assinatura.getId();
        if (assinatura.temPreapproval()) {
            recusarSeJaPagou(assinatura);
        }
        AtomicReference<String> criadoNoMp = new AtomicReference<>();
        LinkPagamentoResponse link;
        try {
            link = transacaoNova.execute(status -> vincularNovoPreapproval(assinaturaId, criadoNoMp));
        } catch (RuntimeException e) {
            compensarPreapprovalNaoVinculado(criadoNoMp.get(), assinaturaId);
            throw e;
        }
        cancelarPreapprovalSubstituido(assinaturaId);
        return link;
    }

    /**
     * Antes de abandonar o preapproval atual, registra as faturas dele aprovadas no MP cujo
     * webhook se perdeu. Se alguma renovou a assinatura, o cliente já pagou: gerar outro link
     * abriria caminho para uma segunda cobrança do mesmo ciclo.
     *
     * @throws BusinessException 409 {@code PAGAMENTO_JA_RECEBIDO} quando houve pagamento recuperado
     */
    private void recusarSeJaPagou(AssinaturaTenant assinatura) {
        int recuperadas = webhookService.recuperarFaturasAprovadas(assinatura.getMpPreapprovalId());
        if (recuperadas > 0) {
            log.warn("mp.preapproval.criar negado motivo=PAGAMENTO_JA_RECEBIDO assinaturaId={} preapprovalId={} recuperadas={}",
                    assinatura.getId(), assinatura.getMpPreapprovalId(), recuperadas);
            throw new BusinessException(
                    "O pagamento desta assinatura já foi recebido e a assinatura foi renovada. Atualize a página.",
                    HttpStatus.CONFLICT, CODIGO_PAGAMENTO_JA_RECEBIDO);
        }
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
                mercadoPagoClient.atualizarValor(preapprovalId, assinatura.getValorMensal());
                assinatura.setMpStatus(statusMp);
                assinatura.setMpUltimaSincronizacao(LocalDateTime.now());
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
     * Desvincula o preapproval atual e agenda o cancelamento dele no MP para depois do commit
     * da transação do chamador (mesmo mecanismo da troca de link). Usado quando o preapproval
     * não deve mais cobrar: link pendente com valor antigo, assinatura que virou cortesia.
     *
     * @param assinatura assinatura gerenciada pela transação do chamador
     * @param motivo     código do motivo, para o log
     * @throws BusinessException se um substituído anterior ainda pendente não puder ser cancelado
     */
    public void desvincularECancelarAposCommit(AssinaturaTenant assinatura, String motivo) {
        if (!assinatura.temPreapproval()) {
            return;
        }
        if (assinatura.temPreapprovalSubstituidoPendente()) {
            // Só há uma coluna para o antigo: o substituído anterior precisa estar cancelado antes
            cancelarNoMercadoPago(assinatura.getMpPreapprovalSubstituidoId());
        }
        String preapprovalId = assinatura.getMpPreapprovalId();
        assinatura.desvincularPreapproval();
        Long assinaturaId = assinatura.getId();
        executarAposCommit("mp.preapproval.desvincular", assinaturaId, () -> cancelarPreapprovalSubstituido(assinaturaId));
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
     */
    private void compensarPreapprovalNaoVinculado(String preapprovalId, Long assinaturaId) {
        if (preapprovalId == null) {
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
    private LinkPagamentoResponse vincularNovoPreapproval(Long assinaturaId, AtomicReference<String> criadoNoMp) {
        AssinaturaTenant assinatura = assinaturaTenantRepository.findByIdParaAtualizacao(assinaturaId)
                .orElseThrow(() -> new BusinessException(
                        "Assinatura não encontrada: " + assinaturaId, HttpStatus.NOT_FOUND));
        validarParaLink(assinatura);
        if (linkPendenteReaproveitavel(assinatura)) {
            log.info("mp.preapproval.criar ignorado motivo=PENDENTE_REAPROVEITADO preapprovalId={} assinaturaId={}",
                    assinatura.getMpPreapprovalId(), assinaturaId);
            return montarResposta(assinatura);
        }
        if (assinatura.temPreapproval() && assinatura.temPreapprovalSubstituidoPendente()) {
            // Só há uma coluna para o antigo: o substituído anterior precisa estar cancelado antes
            cancelarNoMercadoPago(assinatura.getMpPreapprovalSubstituidoId());
            assinatura.setMpPreapprovalSubstituidoId(null);
        }

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
        assinatura.setMpUltimaSincronizacao(LocalDateTime.now());
        assinaturaTenantRepository.save(assinatura);

        log.info("mp.preapproval.criar ok preapprovalId={} substituidoId={} assinaturaId={} organizacaoId={}",
                preapproval.id(), anterior, assinaturaId, assinatura.getOrganizacao().getId());

        return montarResposta(assinatura);
    }

    private static void validarParaLink(AssinaturaTenant assinatura) {
        if (assinatura.isCortesia()) {
            throw new BusinessException(
                    "Assinaturas de cortesia não possuem cobrança no Mercado Pago",
                    HttpStatus.CONFLICT);
        }
        if (StatusAssinatura.ATIVA.equals(assinatura.getStatus()) && assinatura.temPreapproval()) {
            throw new BusinessException("Esta assinatura já está ativa", HttpStatus.CONFLICT);
        }
    }

    private static boolean linkPendenteReaproveitavel(AssinaturaTenant assinatura) {
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
     * @param assinaturaId ID da assinatura
     * @return true se o substituído foi confirmado como cancelado e a coluna foi limpa
     */
    public boolean cancelarPreapprovalSubstituido(Long assinaturaId) {
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
            cancelarNoMercadoPago(substituido);
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
     * Cancela o preapproval no MP se ele ainda não estiver cancelado. Consultar antes evita
     * repetir o PUT na nova tentativa.
     *
     * @throws BusinessException quando o Mercado Pago não responde
     */
    private void cancelarNoMercadoPago(String preapprovalId) {
        PreapprovalResponse remoto = mercadoPagoClient.buscarPreapproval(preapprovalId);
        if (remoto == null || MercadoPagoClient.STATUS_CANCELLED.equals(remoto.status())) {
            return;
        }
        mercadoPagoClient.cancelarPreapproval(preapprovalId);
    }

    /**
     * Gera o link de pagamento de uma assinatura e o envia por e-mail ao administrador
     * owner da organização. Chamado item a item pelo scheduler e após o commit da remoção
     * de cortesia.
     *
     * Não abre transação de propósito: a troca do preapproval faz commit nas próprias fases
     * e o e-mail só sai depois desse commit, para nunca levar um link que foi desfeito.
     *
     * @param assinaturaId ID da assinatura
     * @return dados do checkout, incluindo o init_point
     */
    public LinkPagamentoResponse gerarLinkENotificar(Long assinaturaId) {
        AssinaturaTenant assinatura = assinaturaTenantRepository.findById(assinaturaId)
                .orElseThrow(() -> new BusinessException(
                        "Assinatura não encontrada: " + assinaturaId, HttpStatus.NOT_FOUND));

        LinkPagamentoResponse link = gerarLinkPagamento(assinatura);
        notificarLinkDePagamento(assinaturaId, link);
        return link;
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
        assinatura.setDataCancelamento(LocalDateTime.now());
        assinatura.setCanceladoPor(origem);
        assinatura.setMpUltimaSincronizacao(LocalDateTime.now());
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
            assinatura.setMpUltimaSincronizacao(LocalDateTime.now());
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
            assinatura.setMpUltimaSincronizacao(LocalDateTime.now());
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
        assinatura.setMpUltimaSincronizacao(LocalDateTime.now());

        StatusAssinatura statusAnterior = assinatura.getStatus();
        if (aplicarStatusRemoto(assinatura, remoto.status())) {
            log.info("mp.reconciliar status assinaturaId={} de={} para={} statusMp={}",
                    assinatura.getId(), statusAnterior, assinatura.getStatus(), remoto.status());
            mudou = true;
        }

        assinaturaTenantRepository.save(assinatura);

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
                assinatura.setDataSuspensao(LocalDateTime.now());
                return true;
            }
            case MercadoPagoClient.STATUS_CANCELLED -> {
                assinatura.setStatus(StatusAssinatura.CANCELADA);
                assinatura.setDataCancelamento(LocalDateTime.now());
                assinatura.setCanceladoPor(OrigemCancelamento.MERCADO_PAGO);
                return true;
            }
            default -> {
                return false;
            }
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
                && !assinatura.getDataVencimento().isBefore(LocalDate.now());
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

    private void notificarLinkDePagamento(Long assinaturaId, LinkPagamentoResponse link) {
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

        emailNotificacaoService.enviarLinkAtivacaoAssinatura(
                owner.getEmail(),
                owner.getNome(),
                assinatura.getPlanoAssinatura().getNome(),
                assinatura.getValorMensal(),
                link.initPoint(),
                link.payerEmail(),
                assinatura.getDataVencimento(),
                assinatura.getOrganizacao().getId(),
                owner.getId());
    }
}
