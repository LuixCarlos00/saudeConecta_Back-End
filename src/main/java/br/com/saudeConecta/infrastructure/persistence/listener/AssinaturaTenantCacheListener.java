package br.com.saudeConecta.infrastructure.persistence.listener;

import br.com.saudeConecta.application.cache.CacheEvictionService;
import br.com.saudeConecta.domain.planos.AssinaturaTenant;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Listener JPA que invalida o cache de acesso da assinatura sempre que uma AssinaturaTenant é
 * gravada. A assinatura muda de status por vários caminhos (régua de cobrança, webhook e
 * reconciliação do Mercado Pago, ações do ROOT), muitos só por dirty checking; centralizar aqui
 * evita depender de cada caminho lembrar do evict.
 *
 * O Hibernate instancia o listener pelo container do Spring (SpringBeanContainer), que injeta o
 * campo. Em contextos sem o CacheEvictionService (ex.: @DataJpaTest) a injeção fica nula e o
 * listener não faz nada.
 */
public class AssinaturaTenantCacheListener {

    @Autowired(required = false)
    private CacheEvictionService cacheEvictionService;

    /**
     * Invalida, após o commit, o acesso em cache da organização dona da assinatura.
     *
     * @param assinatura assinatura persistida, atualizada ou removida
     */
    @PostPersist
    @PostUpdate
    @PostRemove
    public void invalidarAcesso(AssinaturaTenant assinatura) {
        if (cacheEvictionService == null || assinatura.getOrganizacao() == null) {
            return;
        }
        cacheEvictionService.evictAcessoAssinatura(assinatura.getOrganizacao().getId());
    }
}
