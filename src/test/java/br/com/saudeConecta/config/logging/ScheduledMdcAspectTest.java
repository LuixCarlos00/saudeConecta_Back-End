package br.com.saudeConecta.config.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("ScheduledMdcAspect - requestId por execução de job agendado")
class ScheduledMdcAspectTest {

    @Test
    @DisplayName("Cada execução ganha um requestId próprio, limpo ao final")
    void geraRequestIdPorExecucao() throws Throwable {
        ProceedingJoinPoint jp = mock(ProceedingJoinPoint.class);
        AtomicReference<String> visto = new AtomicReference<>();
        when(jp.proceed()).thenAnswer(inv -> {
            visto.set(MDC.get(MdcChaves.REQUEST_ID));
            return null;
        });

        new ScheduledMdcAspect().comRequestId(jp);

        assertThat(visto.get()).startsWith("job-");
        assertThat(MDC.get(MdcChaves.REQUEST_ID)).isNull();
    }
}
