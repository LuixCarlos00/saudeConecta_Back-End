package br.com.saudeConecta.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * CacheManager principal com TTLs diferenciados por domínio.
     *
     * - Caches voláteis (dashboard, mensageria, profissionais): TTL 5 min — dados mudam frequentemente.
     * - Caches estáveis (configuracoes-graficos, configuracoes-cards, planos, especialidades): TTL 30 min
     *   — dados raramente alterados pelo usuário; invalidados via @CacheEvict em cada mutação.
     *
     * @return CacheManager com estratégias por cache
     */
    @Bean
    @Primary
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
            // ── Caches voláteis — TTL 5 minutos ─────────────────────────────
            buildCache("dashboard-admin-org",    5,  200),
            buildCache("dashboard-profissional", 5,  200),
            buildCache("dashboard-super-admin",  5,   50),
            buildCache("mensageria-contagem",    5,  500),
            buildCache("profissionais-org",      5,  500),
            buildCache("pacientes-org",          5,  500),
            buildCache("usuarios-agrupados",     5,  200),
            buildCache("perfil-usuario",         5,  500),
            buildCache("consultas-intervalo-org", 2, 300),
            buildCache("consultas-hoje-org",   2,  300),
            buildCache("consultas-semana-org", 5,  300),
            buildCache("consultas-mes-org",   10,  300),
            buildCache("consultas-ano-org",   15,  300),
            buildCache("horarios-ocupados-org",   2, 1000),
            buildCache("disponibilidade-consulta", 2, 1000),
            buildCache("mensageria-lista", 1, 200),
            buildCache("configuracoes-graficos", 30, 500),
            buildCache("configuracoes-cards",    30, 500),
            buildCache("planos",                 30, 100),
            buildCache("procedimentos-padrao",   30, 500),
            buildCache("especialidades",         30, 200),
            buildCache("disponibilidade-consulta",  2, 2000),
            buildCache("questionario-saude",     60, 500),
            buildCache("prontuario-recente",     30, 500)
        ));
        return manager;
    }

    /**
     * Cria um cache Caffeine com TTL e tamanho máximo configurados.
     *
     * @param name       nome do cache (deve coincidir com value no @Cacheable)
     * @param ttlMinutes tempo de expiração após escrita (minutos)
     * @param maxSize    número máximo de entradas
     * @return instância de CaffeineCache configurada
     */
    private CaffeineCache buildCache(String name, int ttlMinutes, int maxSize) {
        return new CaffeineCache(name,
            Caffeine.newBuilder()
                .expireAfterWrite(ttlMinutes, TimeUnit.MINUTES)
                .maximumSize(maxSize)
                .build());
    }
}
