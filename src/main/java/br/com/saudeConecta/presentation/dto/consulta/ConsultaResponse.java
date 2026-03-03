package br.com.saudeConecta.presentation.dto.consulta;

import br.com.saudeConecta.domain.consulta.Consulta;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ConsultaResponse(
    Long id,
    Long profissionalId,
    String profissionalNome,
    String profissionalConselho,
    Long pacienteId,
    String pacienteNome,
    String pacienteTelefone,
    Long especialidadeId,
    String especialidadeNome,
    LocalDateTime dataHora,
    LocalDateTime dataHoraFim,
    Integer duracaoMinutos,
    String observacoes,
    Long formaPagamentoId,
    String formaPagamentoNome,
    BigDecimal valor,
    String status,
    String canceladoPor,
    String motivoCancelamento,
    LocalDateTime createdAt
) {
    public static ConsultaResponse fromEntity(Consulta c) {
        return new ConsultaResponse(
            c.getId(),
            c.getProfissional() != null ? c.getProfissional().getId() : null,
            c.getProfissional() != null ? c.getProfissional().getNome() : null,
            c.getProfissional() != null ? c.getProfissional().getRegistroConselho() : null,
            c.getPaciente() != null ? c.getPaciente().getPaciCodigo() : null,
            c.getPaciente() != null ? c.getPaciente().getPaciNome() : null,
            c.getPaciente() != null ? c.getPaciente().getPaciTelefone() : null,
            c.getEspecialidade() != null ? c.getEspecialidade().getId() : null,
            c.getEspecialidade() != null ? c.getEspecialidade().getNome() : null,
            c.getDataHora(),
            c.getDataHoraFim(),
            c.getDuracaoMinutos(),
            c.getObservacoes(),
            c.getFormaPagamento() != null ? c.getFormaPagamento().getId() : null,
            c.getFormaPagamento() != null ? c.getFormaPagamento().getNome() : null,
            c.getValor(),
            c.getStatus() != null ? c.getStatus().name() : null,
            c.getCanceladoPor() != null ? c.getCanceladoPor().name() : null,
            c.getMotivoCancelamento(),
            c.getCreatedAt()
        );
    }
}
