package br.com.saudeConecta.config.logging;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * Copia o MDC (requestId, tenantId, userId) da thread que submete para a thread do pool
 * (@Async e e-mails), e restaura o estado anterior ao terminar para não vazar contexto
 * entre tarefas que reutilizam a mesma thread.
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable tarefa) {
        Map<String, String> contexto = MDC.getCopyOfContextMap();
        return () -> {
            Map<String, String> anterior = MDC.getCopyOfContextMap();
            if (contexto != null) {
                MDC.setContextMap(contexto);
            } else {
                MDC.clear();
            }
            try {
                tarefa.run();
            } finally {
                if (anterior != null) {
                    MDC.setContextMap(anterior);
                } else {
                    MDC.clear();
                }
            }
        };
    }
}
