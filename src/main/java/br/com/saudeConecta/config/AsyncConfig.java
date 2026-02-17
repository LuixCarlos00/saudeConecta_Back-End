package br.com.saudeConecta.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "emailTaskExecutor")
    public Executor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Número de threads que ficam sempre ativas
        executor.setCorePoolSize(2);
        
        // Número máximo de threads
        executor.setMaxPoolSize(5);
        
        // Capacidade da fila de tarefas
        executor.setQueueCapacity(100);
        
        // Nome das threads para facilitar debug
        executor.setThreadNamePrefix("EmailSender-");
        
        // Tempo que threads extras ficam ociosas antes de serem removidas
        executor.setKeepAliveSeconds(60);
        
        // Política quando a fila está cheia e não há threads disponíveis
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        
        // Inicializa o executor
        executor.initialize();
        
        return executor;
    }
}
