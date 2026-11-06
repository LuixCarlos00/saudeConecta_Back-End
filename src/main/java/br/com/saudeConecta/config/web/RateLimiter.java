package br.com.saudeConecta.config.web;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limitador de requisições em memória, por chave (normalmente o IP de origem).
 *
 * Suficiente para proteger endpoints públicos de abuso trivial em uma instância.
 *
 * <p><b>Limitação conhecida (multi-instância):</b> os contadores vivem na memória de cada JVM.
 * Com N instâncias atrás do balanceador, o limite efetivo por IP vira até N vezes o configurado,
 * e um restart zera a contagem. Se a aplicação passar a rodar com mais de uma réplica, trocar por
 * um contador distribuído (ex.: Redis com INCR + EXPIRE) mantendo esta mesma interface.
 *
 * <p>A chave deve vir de {@code request.getRemoteAddr()}: com
 * {@code server.forward-headers-strategy=native} o Tomcat já resolve o IP real a partir do proxy
 * confiável. Ler {@code X-Forwarded-For} na mão permitiria ao cliente forjar o IP e burlar o limite.
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
