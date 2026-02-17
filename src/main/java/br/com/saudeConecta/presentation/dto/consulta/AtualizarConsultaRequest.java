package br.com.saudeConecta.presentation.dto.consulta;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AtualizarConsultaRequest(
    @NotNull(message = "Profissional é obrigatório")
    Long profissionalId,

    @NotNull(message = "Paciente é obrigatório")
    Long pacienteId,

    Long especialidadeId,

    @NotNull(message = "Data e hora são obrigatórios")
    LocalDateTime dataHora,

    Integer duracaoMinutos,

    String observacoes,

    Long formaPagamentoId,

    BigDecimal valor,

    String status
) {}
