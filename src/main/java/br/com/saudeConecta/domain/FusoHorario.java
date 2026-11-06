package br.com.saudeConecta.domain;

import java.time.ZoneId;

/**
 * Fonte única do fuso de negócio (horário de Brasília). Fica no domínio porque é a camada que
 * todas as outras podem referenciar (as entidades não podem depender de {@code config}).
 * Passar a zona explicitamente a {@code now(ZONA)} deixa o resultado independente do fuso da JVM.
 */
public final class FusoHorario {

    /** Nome do fuso, utilizável em anotações como {@code @Scheduled(zone = ...)}. */
    public static final String NOME = "America/Sao_Paulo";

    /** Fuso de negócio da aplicação. */
    public static final ZoneId ZONA = ZoneId.of(NOME);

    private FusoHorario() {
    }
}
