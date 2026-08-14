package br.com.saudeConecta.presentation.dto.relatorio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Paciente atendido, com o resumo do historico e os relatorios disponiveis.
 *
 * @param id                identificador do paciente
 * @param nome              nome do paciente
 * @param cpf               cpf do paciente
 * @param telefone          telefone de contato
 * @param email             email de contato
 * @param dataNascimento    data de nascimento
 * @param sexo              sexo informado no cadastro
 * @param profissionalId    profissional do atendimento mais recente
 * @param profissionalNome  nome do profissional do atendimento mais recente
 * @param ultimoAtendimento data e hora do ultimo atendimento
 * @param totalConsultas    quantidade de consultas consideradas
 * @param totalDocumentos   quantidade de documentos disponiveis
 * @param consultas         consultas com os respectivos documentos
 */
public record PacienteAtendidoResponse(
        Long id,
        String nome,
        String cpf,
        String telefone,
        String email,
        LocalDate dataNascimento,
        String sexo,
        Long profissionalId,
        String profissionalNome,
        LocalDateTime ultimoAtendimento,
        int totalConsultas,
        int totalDocumentos,
        List<ConsultaRelatorioResponse> consultas
) {
}
