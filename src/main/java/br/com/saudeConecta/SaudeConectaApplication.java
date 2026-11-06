package br.com.saudeConecta;

import br.com.saudeConecta.domain.FusoHorario;
import br.com.saudeConecta.config.FusoHorarioConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
@EnableCaching
public class SaudeConectaApplication {

	public static void main(String[] args) {
		definirFusoPadrao();
		SpringApplication.run(SaudeConectaApplication.class, args);
	}

	/**
	 * Garante o fuso de Brasília mesmo onde o main não roda (testes @SpringBootTest) ou quando o
	 * ambiente esquece o -Duser.timezone. Datas sem zona (LocalDate/LocalDateTime.now(FusoHorario.ZONA)) e as
	 * colunas DATETIME passam a refletir a hora que o usuário vê.
	 */
	@PostConstruct
	void definirFusoPadraoNoContexto() {
		definirFusoPadrao();
	}

	private static void definirFusoPadrao() {
		TimeZone.setDefault(TimeZone.getTimeZone(FusoHorarioConfig.ZONA));
	}

}
