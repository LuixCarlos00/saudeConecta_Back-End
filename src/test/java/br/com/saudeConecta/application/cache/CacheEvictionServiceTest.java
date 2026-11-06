package br.com.saudeConecta.application.cache;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("CacheEvictionService - evicção após o commit")
class CacheEvictionServiceTest {

    private final CacheManager cacheManager = mock(CacheManager.class);
    private final Cache cache = mock(Cache.class);
    private final CacheEvictionService service = new CacheEvictionService(cacheManager);

    @AfterEach
    void limpar() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("Sem transação ativa: evict direto")
    void semTransacaoEvictDireto() {
        when(cacheManager.getCache(any())).thenReturn(cache);

        service.evictProntuarioRecente(1L, 2L);

        verify(cache).evict("org-1-2");
    }

    @Test
    @DisplayName("Com transação ativa: só evict depois do commit")
    void comTransacaoEvictAposCommit() {
        when(cacheManager.getCache(any())).thenReturn(cache);
        TransactionSynchronizationManager.initSynchronization();

        service.evictQuestionarioSaude(1L, 2L);
        verify(cache, never()).evict(any());

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(cache).evict("org-1-2");
    }
}
