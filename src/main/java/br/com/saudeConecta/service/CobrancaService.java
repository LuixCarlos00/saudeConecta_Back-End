package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.planos.AssinaturaTenant;
import br.com.saudeConecta.domain.planos.CobrancaTenant;
import br.com.saudeConecta.domain.planos.StatusAssinatura;
import br.com.saudeConecta.domain.planos.StatusCobranca;
import br.com.saudeConecta.infra.exceptions.BusinessException;
import br.com.saudeConecta.infrastructure.persistence.repository.AssinaturaTenantRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.CobrancaTenantRepository;
import br.com.saudeConecta.presentation.dto.planos.CobrancaTenantResponse;
import br.com.saudeConecta.service.pix.PixResponse;
import br.com.saudeConecta.service.pix.PixServiceEstatico;
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
    private final PixServiceEstatico pixServiceEstatico;

    public CobrancaService(
            CobrancaTenantRepository cobrancaTenantRepository,
            AssinaturaTenantRepository assinaturaTenantRepository,
            AssinaturaTenantService assinaturaTenantService,
            PixServiceEstatico pixServiceEstatico) {
        this.cobrancaTenantRepository = cobrancaTenantRepository;
        this.assinaturaTenantRepository = assinaturaTenantRepository;
        this.assinaturaTenantService = assinaturaTenantService;
        this.pixServiceEstatico = pixServiceEstatico;
    }

    /**
     * Gera uma nova cobrança Pix estática para a assinatura.
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



        PixResponse pix = pixServiceEstatico.gerarCobrancaPix(assinatura.getValorMensal() );

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
        log.info("Cobrança Pix gerada: id={}, txid={}, org={}, valor=R${}",
                salva.getId(), salva.getTxid(),
                assinatura.getOrganizacao().getId(), salva.getValorTotal());

        return CobrancaTenantResponse.fromEntity(salva);
    }

    /**
     * Confirma pagamento manualmente (SuperAdmin).
     * Marca cobrança como PAGA e renova a assinatura.
     * Idempotente — ignora se já estiver pago.
     *
     * @param cobrancaId ID da cobrança
     * @return CobrancaTenantResponse atualizada
     */
    @Transactional
    public CobrancaTenantResponse confirmarPagamentoManual(Long cobrancaId) {
        CobrancaTenant cobranca = cobrancaTenantRepository.findById(cobrancaId)
                .orElseThrow(() -> new BusinessException(
                        "Cobrança não encontrada: " + cobrancaId,
                        HttpStatus.NOT_FOUND));

        if (cobranca.isPago()) {
            log.info("Cobrança já paga, ignorando: id={}", cobrancaId);
            return CobrancaTenantResponse.fromEntity(cobranca);
        }

        cobranca.setStatus(StatusCobranca.PAGO);
        cobranca.setDataPagamento(LocalDateTime.now());
        cobrancaTenantRepository.save(cobranca);

        assinaturaTenantService.renovarAposPagamento(cobranca.getAssinaturaTenant().getId());

        log.info("Pagamento confirmado manualmente: cobrancaId={}, org={}, valor=R${}",
                cobrancaId, cobranca.getOrganizacao().getId(), cobranca.getValorTotal());

        return CobrancaTenantResponse.fromEntity(cobranca);
    }

    /**
     * Verifica se existe cobrança pendente para uma assinatura.
     * Usado pelo scheduler para evitar duplicação de cobranças.
     *
     * @param assinaturaId ID da assinatura
     * @return true se existe cobrança pendente
     */
    public boolean existeCobrancaPendente(Long assinaturaId) {
        return cobrancaTenantRepository.existsByAssinaturaTenantIdAndStatus(
                assinaturaId, StatusCobranca.PENDENTE);
    }

    /**
     * Confirma pagamento via txid (mantido para compatibilidade com scheduler).
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
        return confirmarPagamentoManual(cobranca.getId());
    }

    /**
     * Lista todas as cobranças pendentes de todas as organizações (SuperAdmin).
     *
     * @return lista de CobrancaTenantResponse pendentes
     */
    @Transactional(readOnly = true)
    public List<CobrancaTenantResponse> listarTodasPendentes() {
        return cobrancaTenantRepository.findByStatusOrderByCriadaEmDesc(StatusCobranca.PENDENTE)
                .stream()
                .map(CobrancaTenantResponse::fromEntity)
                .toList();
    }

    /**
     * Busca a cobrança pendente mais recente da organização (AdminOrg).
     * Usado para exibir alerta no dashboard.
     *
     * @param organizacaoId ID da organização
     * @return CobrancaTenantResponse ou null se não houver pendente
     */
    public CobrancaTenantResponse buscarCobrancaPendenteAtual(Long organizacaoId) {
        return cobrancaTenantRepository
                .findTopByOrganizacaoIdAndStatusOrderByCriadaEmDesc(organizacaoId, StatusCobranca.PENDENTE)
                .map(CobrancaTenantResponse::fromEntity)
                .orElse(null);
    }

    /**
     * Lista assinaturas ativas para geração de cobrança (usado pelo scheduler e testes).
     *
     * @return lista de assinaturas ATIVAS e TRIAL
     */
    public List<AssinaturaTenant> listarAssinaturasAtivasParaCobranca() {
        return assinaturaTenantRepository.findByStatusIn(List.of(
                StatusAssinatura.ATIVA,
                StatusAssinatura.TRIAL
        ));
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
