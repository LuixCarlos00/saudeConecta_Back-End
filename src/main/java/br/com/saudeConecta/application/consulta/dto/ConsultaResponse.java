package br.com.saudeConecta.application.consulta.dto;

import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.pagamento.FormaPagamento;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.domain.profissional.Especialidade;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.profissional.TipoProfissional;
import br.com.saudeConecta.domain.usuario.Usuario;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.function.Function;

public record ConsultaResponse(
    Long id,
    Long profissionalId,
    String profissionalNome,
    String profissionalConselho,
    Long profissionalUsuarioId,
    Long tipoProfissionalId,
    String tipoProfissionalNome,
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
        Profissional prof = c.getProfissional();
        // Id do usuário dono do prontuário: o front compara com o logado para oferecer "Editar".
        // Só o id do proxy lazy é lido, então não há select a mais nem LazyInitializationException.
        Usuario usuarioProf = valorOuNulo(prof, Profissional::getUsuario);
        TipoProfissional tipoProf = valorOuNulo(prof, Profissional::getTipoProfissional);
        Paciente pac = c.getPaciente();
        Especialidade esp = c.getEspecialidade();
        FormaPagamento forma = c.getFormaPagamento();

        return new ConsultaResponse(
            c.getId(),
            valorOuNulo(prof, Profissional::getId),
            valorOuNulo(prof, Profissional::getNome),
            valorOuNulo(prof, Profissional::getRegistroConselho),
            valorOuNulo(usuarioProf, Usuario::getId),
            valorOuNulo(tipoProf, TipoProfissional::getId),
            valorOuNulo(tipoProf, TipoProfissional::getCodigo),
            valorOuNulo(pac, Paciente::getPaciCodigo),
            valorOuNulo(pac, Paciente::getPaciNome),
            valorOuNulo(pac, Paciente::getPaciTelefone),
            valorOuNulo(esp, Especialidade::getId),
            valorOuNulo(esp, Especialidade::getNome),
            c.getDataHora(),
            c.getDataHoraFim(),
            c.getDuracaoMinutos(),
            c.getObservacoes(),
            valorOuNulo(forma, FormaPagamento::getId),
            valorOuNulo(forma, FormaPagamento::getNome),
            c.getValor(),
            valorOuNulo(c.getStatus(), Enum::name),
            valorOuNulo(c.getCanceladoPor(), Enum::name),
            c.getMotivoCancelamento(),
            c.getCreatedAt()
        );
    }

    /** Aplica o extrator ao objeto, ou devolve nulo quando o objeto é nulo. */
    private static <T, R> R valorOuNulo(T origem, Function<T, R> extrator) {
        return origem != null ? extrator.apply(origem) : null;
    }
}
