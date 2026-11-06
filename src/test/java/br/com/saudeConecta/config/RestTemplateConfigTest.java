package br.com.saudeConecta.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("N3 - Timeouts HTTP e pool do scheduler")
class RestTemplateConfigTest {

    @Configuration
    @EnableScheduling
    static class ComAgendamento {
    }

    @Test
    @DisplayName("Timeouts de conexão e leitura são aplicados na request factory")
    void timeoutsAplicadosNaRequestFactory() {
        RestTemplate restTemplate = new RestTemplateConfig().restTemplate(Duration.ofSeconds(5), Duration.ofSeconds(15));

        assertThat(restTemplate.getRequestFactory()).isInstanceOf(SimpleClientHttpRequestFactory.class);
        assertThat(ReflectionTestUtils.getField(restTemplate.getRequestFactory(), "connectTimeout")).isEqualTo(5_000);
        assertThat(ReflectionTestUtils.getField(restTemplate.getRequestFactory(), "readTimeout")).isEqualTo(15_000);
    }

    @Test
    @DisplayName("Sem as properties, o bean usa os padrões 5s/15s (app.http.*)")
    void beanUsaPadroesSemProperties() {
        new ApplicationContextRunner()
                // Igual ao SpringApplication: conversor de "5s" para Duration
                .withInitializer(ctx -> ctx.getBeanFactory()
                        .setConversionService(ApplicationConversionService.getSharedInstance()))
                .withUserConfiguration(RestTemplateConfig.class)
                .run(ctx -> {
                    RestTemplate restTemplate = ctx.getBean(RestTemplate.class);
                    assertThat(ReflectionTestUtils.getField(restTemplate.getRequestFactory(), "connectTimeout")).isEqualTo(5_000);
                    assertThat(ReflectionTestUtils.getField(restTemplate.getRequestFactory(), "readTimeout")).isEqualTo(15_000);
                });
    }

    @Test
    @DisplayName("application.properties configura pool do scheduler com mais de 1 thread")
    void poolDoSchedulerTemMaisDeUmaThread() throws Exception {
        Properties props = PropertiesLoaderUtils.loadProperties(new ClassPathResource("application.properties"));
        String poolSize = props.getProperty("spring.task.scheduling.pool.size");
        assertThat(poolSize).as("spring.task.scheduling.pool.size em application.properties").isNotNull();

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(TaskSchedulingAutoConfiguration.class))
                .withUserConfiguration(ComAgendamento.class)
                .withPropertyValues("spring.task.scheduling.pool.size=" + poolSize)
                .run(ctx -> assertThat(ctx.getBean(ThreadPoolTaskScheduler.class).getScheduledThreadPoolExecutor()
                        .getCorePoolSize()).isGreaterThan(1));
    }
}
