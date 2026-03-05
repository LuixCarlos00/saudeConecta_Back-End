package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.planos.AssinaturaTenant;
import br.com.saudeConecta.domain.planos.CobrancaTenant;
import br.com.saudeConecta.domain.planos.StatusCobranca;
import br.com.saudeConecta.infra.exceptions.BusinessException;
import br.com.saudeConecta.infrastructure.persistence.repository.AssinaturaTenantRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.CobrancaTenantRepository;
import br.com.saudeConecta.presentation.dto.planos.CobrancaTenantResponse;
import br.com.saudeConecta.service.pix.PixResponse;
import br.com.saudeConecta.service.pix.PixService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CobrancaService {

    private static final Logger log = LoggerFactory.getLogger(CobrancaService.class);

    private final CobrancaTenantRepository cobrancaTenantRepository;
    private final AssinaturaTenantRepository assinaturaTenantRepository;
    private final AssinaturaTenantService assinaturaTenantService;
    private final PixService pixService;

    public CobrancaService(
            CobrancaTenantRepository cobrancaTenantRepository,
            AssinaturaTenantRepository assinaturaTenantRepository,
            AssinaturaTenantService assinaturaTenantService,
            PixService pixService) {
        this.cobrancaTenantRepository = cobrancaTenantRepository;
        this.assinaturaTenantRepository = assinaturaTenantRepository;
        this.assinaturaTenantService = assinaturaTenantService;
        this.pixService = pixService;
    }

    /**
     * Gera uma nova cobrança Pix para a assinatura.
     * Verifica se já existe cobrança PENDENTE antes de criar.
     *
     * @param assinaturaId ID da assinatura
     * @return CobrancaTenantResponse com dados do Pix
     */
    @Transactional
    public CobrancaTenantResponse gerarCobranca(Long assinaturaId) {
        AssinaturaTenant assinatura = assinaturaTenantRepository.findById(assinaturaId)
                .orElseThrow(() -> new BusinessException(
                        "Assinatura não encontrada: " + assinaturaId,
                        HttpStatus.NOT_FOUND));

        if (cobrancaTenantRepository.existsByAssinaturaTenantIdAndStatus(
                assinaturaId, StatusCobranca.PENDENTE)) {
            throw new BusinessException(
                    "Já existe uma cobrança pendente para esta assinatura",
                    HttpStatus.CONFLICT);
        }

        String descricao = String.format("Assinatura %s - %s",
                assinatura.getPlanoAssinatura().getNome(),
                assinatura.getOrganizacao().getNome());

        PixResponse pix = pixService.gerarCobrancaPix(assinatura.getValorMensal(), descricao);

        CobrancaTenant cobranca = CobrancaTenant.builder()
                .assinaturaTenant(assinatura)
                .organizacao(assinatura.getOrganizacao())
                .valorTotal(assinatura.getValorMensal())
                .status(StatusCobranca.PENDENTE)
                .pixCopiaECola(pix.pixCopiaECola())
                .pixQrcodeBase64(pix.qrcodeBase64())
                .txid(pix.txid())
                .dataVencimentoPix(pix.dataVencimento())
                .build();

        CobrancaTenant salva = cobrancaTenantRepository.save(cobranca);
        log.info("Cobrança gerada: id={}, txid={}, org={}, valor=R${}",
                salva.getId(), salva.getTxid(),
                assinatura.getOrganizacao().getId(), salva.getValorTotal());

        return CobrancaTenantResponse.fromEntity(salva);
    }

    /**
     * Confirma pagamento via webhook do PSP (idempotente pelo txid).
     * Marca cobrança como PAGA e renova a assinatura.
     *
     * @param txid identificador da transação Pix
     * @return CobrancaTenantResponse atualizada
     */
    @Transactional
    public CobrancaTenantResponse confirmarPagamento(String txid) {
        CobrancaTenant cobranca = cobrancaTenantRepository.findByTxid(txid)
                .orElseThrow(() -> new BusinessException(
                        "Cobrança não encontrada para txid: " + txid,
                        HttpStatus.NOT_FOUND));

        if (cobranca.isPago()) {
            log.info("Webhook duplicado ignorado para txid={}", txid);
            return CobrancaTenantResponse.fromEntity(cobranca);
        }

        cobranca.setStatus(StatusCobranca.PAGO);
        cobranca.setDataPagamento(LocalDateTime.now());
        cobrancaTenantRepository.save(cobranca);

        assinaturaTenantService.renovarAposPagamento(cobranca.getAssinaturaTenant().getId());

        log.info("Pagamento confirmado: txid={}, org={}, valor=R${}",
                txid, cobranca.getOrganizacao().getId(), cobranca.getValorTotal());

        return CobrancaTenantResponse.fromEntity(cobranca);
    }

    /**
     * Lista cobranças de uma organização (histórico).
     *
     * @param organizacaoId ID da organização
     * @return lista de CobrancaTenantResponse
     */
    @Transactional(readOnly = true)
    public List<CobrancaTenantResponse> listarPorOrganizacao(Long organizacaoId) {
        return cobrancaTenantRepository.findByOrganizacaoIdWithRelations(organizacaoId)
                .stream()
                .map(CobrancaTenantResponse::fromEntity)
                .toList();
    }

    /**
     * Busca cobrança pelo txid.
     *
     * @param txid identificador da transação
     * @return CobrancaTenantResponse
     */
    @Transactional(readOnly = true)
    public CobrancaTenantResponse buscarPorTxid(String txid) {
        CobrancaTenant cobranca = cobrancaTenantRepository.findByTxid(txid)
                .orElseThrow(() -> new BusinessException(
                        "Cobrança não encontrada para txid: " + txid,
                        HttpStatus.NOT_FOUND));
        return CobrancaTenantResponse.fromEntity(cobranca);
    }

    /**
     * Expira cobranças com Pix vencido (chamado pelo scheduler).
     *
     * @return quantidade de cobranças expiradas
     */
    @Transactional
    public int expirarCobrancasVencidas() {
        List<CobrancaTenant> expiradas = cobrancaTenantRepository.findPixExpirados(LocalDate.now());

        for (CobrancaTenant cobranca : expiradas) {
            cobranca.setStatus(StatusCobranca.EXPIRADO);
            cobrancaTenantRepository.save(cobranca);
            log.info("Cobrança expirada: id={}, txid={}, org={}",
                    cobranca.getId(), cobranca.getTxid(), cobranca.getOrganizacao().getId());
        }

        if (!expiradas.isEmpty()) {
            log.info("Total de cobranças expiradas: {}", expiradas.size());
        }

        return expiradas.size();
    }
}
