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

            // ── Caches de curta duração — TTL 2 minutos ──────────────────────
            // consultas-intervalo-org: absorve requisições duplicadas dos 2 gráficos do dashboard
            // que chamam o mesmo endpoint simultaneamente com os mesmos parâmetros.
            // TTL curto para refletir mudanças de agenda recentes.
            buildCache("consultas-intervalo-org", 2, 300),

            // Caches da tela de Agenda (/consultas/hoje, /semana-atual, /mes-atual, /ano-atual)
            // TTLs diferenciados por volatilidade do período: hoje muda a cada agendamento,
            // mês/ano mudam com menos frequência. Todos invalidados via @CacheEvict nos mutadores.
            buildCache("consultas-hoje-org",   2,  300),
            buildCache("consultas-semana-org", 5,  300),
            buildCache("consultas-mes-org",   10,  300),
            buildCache("consultas-ano-org",   15,  300),

            // horarios-ocupados-org: horários já agendados por profissional/data.
            // Consultado a cada seleção de médico ou data no formulário de nova/editar consulta.
            // TTL 2 min: curto o suficiente para refletir novos agendamentos, longo o suficiente
            // para absorver cliques rápidos do usuário. Invalidado via @CacheEvict nos mutadores.
            buildCache("horarios-ocupados-org",   2, 1000),

            // disponibilidade-consulta: verificação pontual de slot livre (profissional+data+horario).
            // Cache estava referenciado nos @CacheEvict mas não registrado — fix necessário.
            // TTL 2 min, mesmo ciclo que horarios-ocupados-org.
            buildCache("disponibilidade-consulta", 2, 1000),

            // mensageria-lista: listagem paginada com filtros — TTL 1 min.
            // Novos registros são criados automaticamente pelo sistema de email,
            // então o TTL é propositalmente curto. Invalidado via @CacheEvict
            // em marcarComoNotificado e reenviarMensagem.
            buildCache("mensageria-lista", 1, 200),

            // ── Caches estáveis — TTL 30 minutos ────────────────────────────
            buildCache("configuracoes-graficos", 30, 500),
            buildCache("configuracoes-cards",    30, 500),
            buildCache("planos",                 30, 100),
            buildCache("procedimentos-padrao",   30, 500),
            buildCache("especialidades",         30, 200),

            // disponibilidade-consulta: slot de horário verificado durante agendamento. TTL curto (2 min)
            // pois é invalidado via @CacheEvict em qualquer mutação de consulta.
            buildCache("disponibilidade-consulta",  2, 2000),

            // questionario-saude: dado imutável após assinado, invalidado via @CacheEvict
            // ao gerar novo link ou ao responder o questionário. TTL 60 min como safety net.
            buildCache("questionario-saude",     60, 500),

            // prontuario-recente: prontuário médico mais recente por consultaId.
            // Invalidado via @CacheEvict em cadastrarProntuarioMedico e atualizarProntuarioMedico.
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
