package br.com.saudeConecta.config.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Dá um requestId próprio ("job-xxxxxxxx") a cada execução de método {@code @Scheduled}, para
 * separar no log as rodadas da régua de cobrança, reconciliação etc. Limpa o MDC ao final porque
 * as threads do scheduler são reaproveitadas.
 */
@Aspect
@Component
public class ScheduledMdcAspect {

    /**
     * Envolve a execução do job com o requestId no MDC.
     *
     * @param jp execução do método agendado
     * @return retorno do job
     * @throws Throwable o que o job lançar
     */
    @Around("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    public Object comRequestId(ProceedingJoinPoint jp) throws Throwable {
        MDC.put(MdcChaves.REQUEST_ID, "job-" + UUID.randomUUID().toString().substring(0, 8));
        try {
            return jp.proceed();
        } finally {
            MDC.clear();
        }
    }
}
