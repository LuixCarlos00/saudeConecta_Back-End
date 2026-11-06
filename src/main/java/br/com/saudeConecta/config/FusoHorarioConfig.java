package br.com.saudeConecta.config;

import br.com.saudeConecta.domain.FusoHorario;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Fuso de negócio da aplicação. Clínicas e cobrança operam no horário de Brasília: "hoje",
 * vencimentos e o dia âncora da cobrança não podem virar o dia às 21h só porque o servidor
 * (container Alpine) está em UTC.
 */
@Configuration
public class FusoHorarioConfig {

    /** Usado também no {@code zone} dos {@code @Scheduled}. */
    public static final String FUSO = FusoHorario.NOME;
    public static final ZoneId ZONA = FusoHorario.ZONA;

    /**
     * Relógio injetável dos services de cobrança; nos testes é trocado por {@link Clock#fixed}.
     *
     * @return relógio do sistema no fuso de São Paulo
     */
    @Bean
    public Clock clock() {
        return Clock.system(ZONA);
    }
}
