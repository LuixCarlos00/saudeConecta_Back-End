package br.com.saudeConecta.infra.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limitador de requisições em memória, por chave (normalmente o IP de origem).
 *
 * Suficiente para proteger endpoints públicos de abuso trivial em uma instância.
 * Em cenário multi-instância, substituir por um contador distribuído.
 */
@Component
public class RateLimiter {

    private static final Duration JANELA = Duration.ofHours(1);
    private static final int TAMANHO_MAXIMO = 10_000;

    private final Cache<String, AtomicInteger> contadores = Caffeine.newBuilder()
            .expireAfterWrite(JANELA)
            .maximumSize(TAMANHO_MAXIMO)
            .build();

    /**
     * Registra uma tentativa e informa se ela ainda está dentro do limite.
     *
     * @param chave          identificador do cliente (ex.: IP)
     * @param limitePorJanela número máximo de tentativas permitidas na janela
     * @return true se a requisição pode prosseguir
     */
    public boolean permitir(String chave, int limitePorJanela) {
        AtomicInteger contador = contadores.get(chave, k -> new AtomicInteger());
        return contador.incrementAndGet() <= limitePorJanela;
    }
}
