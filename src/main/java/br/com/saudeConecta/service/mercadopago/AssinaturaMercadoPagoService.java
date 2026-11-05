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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Regras de negócio da assinatura recorrente no Mercado Pago.
 * Cuida da geração do link de pagamento, do cancelamento e da reconciliação
 * de status entre o Mercado Pago e a base local.
 */
@Service
public class AssinaturaMercadoPagoService {

    private static final Logger log = LoggerFactory.getLogger(AssinaturaMercadoPagoService.class);
    private static final String PREFIXO_EXTERNAL_REFERENCE = "ASSINATURA-";

    private final AssinaturaTenantRepository assinaturaTenantRepository;
    private final AdminOrganizacaoRepository adminOrganizacaoRepository;
    private final MercadoPagoClient mercadoPagoClient;
    private final MercadoPagoProperties properties;
    private final EmailNotificacaoService emailNotificacaoService;

    public AssinaturaMercadoPagoService(
            AssinaturaTenantRepository assinaturaTenantRepository,
            AdminOrganizacaoRepository adminOrganizacaoRepository,
            MercadoPagoClient mercadoPagoClient,
            MercadoPagoProperties properties,
            EmailNotificacaoService emailNotificacaoService) {
        this.assinaturaTenantRepository = assinaturaTenantRepository;
        this.adminOrganizacaoRepository = adminOrganizacaoRepository;
        this.mercadoPagoClient = mercadoPagoClient;
        this.properties = properties;
        this.emailNotificacaoService = emailNotificacaoService;
    }

    /**
     * Gera (ou reaproveita) o link do checkout hospedado para a organização logada.
     *
     * @param organizacaoId ID da organização
     * @return dados do checkout, incluindo o init_point
     */
    @Transactional
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
     * Reutiliza o preapproval existente enquanto ele ainda estiver pendente (o init_point
     * continua servindo), evitando criar assinaturas duplicadas no Mercado Pago.
     * Qualquer outro preapproval anterior (pausado, autorizado com cartão recusado etc.)
     * é cancelado no MP antes de criar o novo: sem isso ele fica órfão e pode voltar a
     * cobrar em paralelo ao novo.
     *
     * @param assinatura assinatura do tenant
     * @return dados do checkout, incluindo o init_point
     */
    @Transactional
    public LinkPagamentoResponse gerarLinkPagamento(AssinaturaTenant assinatura) {
        if (assinatura.isCortesia()) {
            throw new BusinessException(
                    "Assinaturas de cortesia não possuem cobrança no Mercado Pago",
                    HttpStatus.CONFLICT);
        }
        if (StatusAssinatura.ATIVA.equals(assinatura.getStatus()) && assinatura.temPreapproval()) {
            throw new BusinessException("Esta assinatura já está ativa", HttpStatus.CONFLICT);
        }

        if (assinatura.temPreapproval() && assinatura.getMpInitPoint() != null
                && MercadoPagoClient.STATUS_PENDING.equals(assinatura.getMpStatus())) {
            log.info("mp.preapproval.criar ignorado motivo=PENDENTE_REAPROVEITADO preapprovalId={} assinaturaId={}",
                    assinatura.getMpPreapprovalId(), assinatura.getId());
            return montarResposta(assinatura);
        }

        if (assinatura.temPreapproval()) {
            abandonarPreapprovalAnterior(assinatura);
        }

        String payerEmail = resolverPayerEmail(assinatura);
        String reason = properties.getReasonPrefix() + " — Plano "
                + assinatura.getPlanoAssinatura().getNome();

        PreapprovalResponse preapproval = mercadoPagoClient.criarPreapprovalPendente(
                reason,
                PREFIXO_EXTERNAL_REFERENCE + assinatura.getId(),
                payerEmail,
                assinatura.getValorMensal());

        if (preapproval == null || preapproval.initPoint() == null) {
            throw new BusinessException(
                    "O Mercado Pago não retornou o link de pagamento. Tente novamente.",
                    HttpStatus.BAD_GATEWAY);
        }

        assinatura.setMpPreapprovalId(preapproval.id());
        assinatura.setMpStatus(preapproval.status());
        assinatura.setMpInitPoint(preapproval.initPoint());
        assinatura.setMpPayerEmail(payerEmail);
        assinatura.setMpUltimaSincronizacao(LocalDateTime.now());
        assinaturaTenantRepository.save(assinatura);

        log.info("mp.preapproval.criar ok preapprovalId={} assinaturaId={} organizacaoId={}",
                preapproval.id(), assinatura.getId(), assinatura.getOrganizacao().getId());

        return montarResposta(assinatura);
    }

    /**
     * Cancela no MP o preapproval que será substituído. Consulta o status remoto antes
     * para que a nova tentativa (rotina diária de links) não tente cancelar de novo um
     * preapproval que a tentativa anterior já cancelou antes de falhar ao criar o novo.
     * Falha no cancelamento propaga: melhor não gerar o link agora do que deixar dois
     * preapprovals vivos.
     */
    private void abandonarPreapprovalAnterior(AssinaturaTenant assinatura) {
        String anterior = assinatura.getMpPreapprovalId();
        PreapprovalResponse remoto = mercadoPagoClient.buscarPreapproval(anterior);
        if (remoto == null || MercadoPagoClient.STATUS_CANCELLED.equals(remoto.status())) {
            assinatura.setMpStatus(MercadoPagoClient.STATUS_CANCELLED);
            return;
        }
        PreapprovalResponse cancelado = mercadoPagoClient.cancelarPreapproval(anterior);
        assinatura.setMpStatus(cancelado != null && cancelado.status() != null
                ? cancelado.status() : MercadoPagoClient.STATUS_CANCELLED);
        log.info("mp.preapproval.abandonar anterior preapprovalId={} statusMp={} assinaturaId={} motivo=SUBSTITUIDO_POR_NOVO_LINK",
                anterior, remoto.status(), assinatura.getId());
    }

    /**
     * Gera o link de pagamento de uma assinatura e o envia por e-mail ao administrador
     * owner da organização. Roda em transação própria para ser chamado item a item
     * pelo scheduler e após o commit da remoção de cortesia.
     *
     * @param assinaturaId ID da assinatura
     * @return dados do checkout, incluindo o init_point
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LinkPagamentoResponse gerarLinkENotificar(Long assinaturaId) {
        AssinaturaTenant assinatura = assinaturaTenantRepository.findById(assinaturaId)
                .orElseThrow(() -> new BusinessException(
                        "Assinatura não encontrada: " + assinaturaId, HttpStatus.NOT_FOUND));

        LinkPagamentoResponse link = gerarLinkPagamento(assinatura);
        notificarLinkDePagamento(assinatura, link);
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
        if (!assinatura.temPreapproval()) {
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

    private void notificarLinkDePagamento(AssinaturaTenant assinatura, LinkPagamentoResponse link) {
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
