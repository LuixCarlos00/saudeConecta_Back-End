package br.com.saudeConecta.config.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MdcTaskDecorator - propaga o MDC para @Async")
class MdcTaskDecoratorTest {

    @AfterEach
    void limpar() {
        MDC.clear();
    }

    @Test
    @DisplayName("Tarefa assíncrona enxerga o requestId de quem submeteu e não deixa resto na thread do pool")
    void propagaELimpa() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            MDC.put(MdcChaves.REQUEST_ID, "req-1");
            MDC.put(MdcChaves.TENANT_ID, "7");
            AtomicReference<String> visto = new AtomicReference<>();
            AtomicReference<String> depois = new AtomicReference<>("nao-executado");

            pool.submit(new MdcTaskDecorator().decorate(() -> visto.set(MDC.get(MdcChaves.REQUEST_ID)))).get();
            pool.submit(() -> depois.set(MDC.get(MdcChaves.REQUEST_ID))).get();

            assertThat(visto.get()).isEqualTo("req-1");
            assertThat(depois.get()).isNull();
        } finally {
            pool.shutdown();
            pool.awaitTermination(1, TimeUnit.SECONDS);
        }
    }
}
