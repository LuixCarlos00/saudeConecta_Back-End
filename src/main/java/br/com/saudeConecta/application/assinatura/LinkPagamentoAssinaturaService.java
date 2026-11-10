package br.com.saudeConecta.application.assinatura;

import br.com.saudeConecta.application.assinatura.dto.LinkPagamentoResponse;
import br.com.saudeConecta.application.exception.BusinessException;
import br.com.saudeConecta.domain.planos.AssinaturaTenant;
import br.com.saudeConecta.infrastructure.persistence.repository.AssinaturaTenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Entrada da geração do link de pagamento. Fica acima do {@link AssinaturaMercadoPagoService}
 * e do {@link MercadoPagoWebhookService} porque, antes de trocar o preapproval, recupera as
 * faturas aprovadas dele pelo caminho do webhook; com essa regra dentro do
 * {@code AssinaturaMercadoPagoService} os dois serviços dependeriam um do outro.
 *
 * Nenhum método abre transação: a troca do preapproval faz commit nas próprias fases e o
 * e-mail só sai depois desse commit.
 */
@Service
public class LinkPagamentoAssinaturaService {

    private static final Logger log = LoggerFactory.getLogger(LinkPagamentoAssinaturaService.class);
    /** Código do erro 409 quando a recuperação de faturas mostra que o cliente já pagou. */
    public static final String CODIGO_PAGAMENTO_JA_RECEBIDO = "PAGAMENTO_JA_RECEBIDO";

    private final AssinaturaTenantRepository assinaturaTenantRepository;
    private final AssinaturaMercadoPagoService assinaturaMercadoPagoService;
    private final MercadoPagoWebhookService webhookService;

    public LinkPagamentoAssinaturaService(AssinaturaTenantRepository assinaturaTenantRepository,
                                          AssinaturaMercadoPagoService assinaturaMercadoPagoService,
                                          MercadoPagoWebhookService webhookService) {
        this.assinaturaTenantRepository = assinaturaTenantRepository;
        this.assinaturaMercadoPagoService = assinaturaMercadoPagoService;
        this.webhookService = webhookService;
    }

    /**
     * Gera (ou reaproveita) o link do checkout hospedado para a organização logada.
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
     * Gera (ou reaproveita) o link do checkout de uma assinatura. Quando o preapproval atual
     * seria trocado, antes recusa se o cliente já pagou por ele (ver {@link #recusarSeJaPagou}).
     * A troca em si está em {@link AssinaturaMercadoPagoService#gerarLinkPagamento(AssinaturaTenant)}.
     *
     * @param assinatura assinatura do tenant
     * @return dados do checkout, incluindo o init_point
     */
    public LinkPagamentoResponse gerarLinkPagamento(AssinaturaTenant assinatura) {
        AssinaturaMercadoPagoService.validarParaLink(assinatura);
        // O link pendente cancelado no MP também será trocado: um webhook perdido pode esconder
        // uma fatura dele já paga, então a checagem vale para ele também.
        if (assinatura.temPreapproval() && (!AssinaturaMercadoPagoService.linkPendenteReaproveitavel(assinatura)
                || assinaturaMercadoPagoService.pendenteCanceladoNoMp(assinatura.getMpPreapprovalId()))) {
            recusarSeJaPagou(assinatura);
        }
        return assinaturaMercadoPagoService.gerarLinkPagamento(assinatura);
    }

    /**
     * Gera o link de pagamento de uma assinatura e o envia por e-mail ao administrador owner
     * da organização. Chamado item a item pelo scheduler e após o commit da remoção de cortesia.
     *
     * @param assinaturaId ID da assinatura
     * @return dados do checkout, incluindo o init_point
     */
    public LinkPagamentoResponse gerarLinkENotificar(Long assinaturaId) {
        AssinaturaTenant assinatura = assinaturaTenantRepository.findById(assinaturaId)
                .orElseThrow(() -> new BusinessException(
                        "Assinatura não encontrada: " + assinaturaId, HttpStatus.NOT_FOUND));

        LinkPagamentoResponse link = gerarLinkPagamento(assinatura);
        assinaturaMercadoPagoService.notificarLinkDePagamento(assinaturaId, link);
        return link;
    }

    /**
     * Atende o pedido de link publicado por quem não pode depender deste serviço. Síncrono: a
     * falha volta para quem publicou.
     *
     * @param evento pedido com o ID da assinatura
     */
    @EventListener
    public void aoSolicitarLink(LinkPagamentoSolicitadoEvent evento) {
        gerarLinkENotificar(evento.assinaturaId());
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
}
