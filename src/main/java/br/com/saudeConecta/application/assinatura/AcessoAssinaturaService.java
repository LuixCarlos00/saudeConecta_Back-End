package br.com.saudeConecta.application.assinatura;

import br.com.saudeConecta.domain.planos.AssinaturaTenant;
import br.com.saudeConecta.infrastructure.persistence.listener.AssinaturaTenantCacheListener;
import br.com.saudeConecta.infrastructure.persistence.repository.AssinaturaTenantRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responde, por organização, se a assinatura ainda dá acesso ao sistema.
 *
 * Consultado pelo filtro de autenticação a cada requisição autenticada, por isso o resultado
 * fica em cache curto ({@value #CACHE_ACESSO_ASSINATURA}, TTL de 1 minuto em CacheConfig).
 * A mudança de status da assinatura invalida a chave via {@link AssinaturaTenantCacheListener},
 * e o TTL limita a janela caso alguma invalidação se perca.
 */
@Service
public class AcessoAssinaturaService {

    public static final String CACHE_ACESSO_ASSINATURA = "acesso-assinatura";

    private final AssinaturaTenantRepository assinaturaTenantRepository;

    public AcessoAssinaturaService(AssinaturaTenantRepository assinaturaTenantRepository) {
        this.assinaturaTenantRepository = assinaturaTenantRepository;
    }

    /**
     * Mesma regra do login (HomeController): a organização precisa ter assinatura TRIAL, ATIVA
     * ou INADIMPLENTE.
     *
     * @param organizacaoId ID da organização (tenant)
     * @return true se a assinatura permite acesso; false se suspensa, cancelada ou inexistente
     */
    @Cacheable(value = CACHE_ACESSO_ASSINATURA, key = "#organizacaoId")
    @Transactional(readOnly = true)
    public boolean organizacaoPermiteAcesso(Long organizacaoId) {
        return assinaturaTenantRepository.findAssinaturaAtivaByOrganizacaoId(organizacaoId)
                .map(AssinaturaTenant::permiteAcesso)
                .orElse(false);
    }
}
