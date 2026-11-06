package br.com.saudeConecta.application.cobranca;

import br.com.saudeConecta.domain.FusoHorario;
import br.com.saudeConecta.application.assinatura.AssinaturaTenantService;
import br.com.saudeConecta.application.assinatura.dto.CobrancaTenantResponse;
import br.com.saudeConecta.application.exception.BusinessException;
import br.com.saudeConecta.domain.planos.CobrancaTenant;
import br.com.saudeConecta.domain.planos.FormaPagamentoCobranca;
import br.com.saudeConecta.domain.planos.StatusCobranca;
import br.com.saudeConecta.infrastructure.persistence.repository.CobrancaTenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Consulta e conciliação das cobranças dos tenants.
 *
 * As cobranças são criadas pelo Mercado Pago (ver MercadoPagoWebhookService);
 * este serviço cuida apenas da leitura do histórico e da baixa manual usada
 * em casos excepcionais pelo SUPER_ADMIN.
 */
@Service
public class CobrancaService {

    private static final Logger log = LoggerFactory.getLogger(CobrancaService.class);

    private final CobrancaTenantRepository cobrancaTenantRepository;
    private final AssinaturaTenantService assinaturaTenantService;

    public CobrancaService(
            CobrancaTenantRepository cobrancaTenantRepository,
            AssinaturaTenantService assinaturaTenantService) {
        this.cobrancaTenantRepository = cobrancaTenantRepository;
        this.assinaturaTenantService = assinaturaTenantService;
    }

    /**
     * Confirma pagamento manualmente (SUPER_ADMIN).
     * Marca a cobrança como PAGA e renova a assinatura.
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
            log.info("cobranca.pagar ignorado motivo=JA_PAGA cobrancaId={}", cobrancaId);
            return CobrancaTenantResponse.fromEntity(cobranca);
        }

        cobranca.setStatus(StatusCobranca.PAGO);
        cobranca.setFormaPagamento(FormaPagamentoCobranca.MANUAL);
        cobranca.setDataPagamento(LocalDateTime.now(FusoHorario.ZONA));
        cobrancaTenantRepository.save(cobranca);

        assinaturaTenantService.renovarAposPagamento(cobranca.getAssinaturaTenant().getId());

        log.info("cobranca.pagar ok cobrancaId={} organizacaoId={} valor={}",
                cobrancaId, cobranca.getOrganizacao().getId(), cobranca.getValorTotal());

        return CobrancaTenantResponse.fromEntity(cobranca);
    }

    /**
     * Lista todas as cobranças pendentes de todas as organizações (SUPER_ADMIN).
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
}
