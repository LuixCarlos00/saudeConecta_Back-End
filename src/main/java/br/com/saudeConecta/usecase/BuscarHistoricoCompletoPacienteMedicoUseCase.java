package br.com.saudeConecta.usecase;

import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.prontuario.Prontuario;
import br.com.saudeConecta.infra.tenant.RequiresTenant;
import br.com.saudeConecta.infra.tenant.TenantHelper;
import br.com.saudeConecta.infrastructure.persistence.repository.ConsultaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProntuarioRepository;
import br.com.saudeConecta.presentation.dto.consulta.HistoricoConsultaPacienteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Use Case responsável por buscar o histórico completo de consultas médicas de um paciente.
 * Retorna consultas REALIZADAS ou PAGAS que possuem prontuário médico,
 * incluindo TODOS os campos do domínio Prontuario.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BuscarHistoricoCompletoPacienteMedicoUseCase {

    private final ConsultaRepository consultaRepository;
    private final ProntuarioRepository prontuarioRepository;
    private final TenantHelper tenantHelper;

    /**
     * Executa a busca do histórico completo de consultas médicas do paciente.
     *
     * @param pacienteId ID do paciente
     * @return Lista de DTOs com histórico completo (consulta + prontuário médico)
     */
    @RequiresTenant
    @Transactional(readOnly = true)
    public List<HistoricoConsultaPacienteResponse> executar(Long pacienteId) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.info("Buscando histórico médico do paciente ID: {} na organização ID: {}", pacienteId, orgId);

        List<Consulta> consultas = consultaRepository.findHistoricoCompletoPacienteMedico(pacienteId, orgId);
        log.debug("Encontradas {} consultas médicas para o paciente", consultas.size());

        return consultas.stream()
            .map(consulta -> {
                Prontuario prontuario = prontuarioRepository.findByConsulta_Id(consulta.getId());
                return mapearParaHistoricoResponse(consulta, prontuario);
            })
            .toList();
    }

    /**
     * Mapeia entidade Consulta e Prontuario (médico) para DTO de resposta.
     * Preenche TODOS os campos do domínio Prontuario.
     *
     * @param consulta Entidade Consulta
     * @param prontuario Entidade Prontuario (pode ser null)
     * @return DTO com dados do histórico médico
     */
    private HistoricoConsultaPacienteResponse mapearParaHistoricoResponse(Consulta consulta, Prontuario prontuario) {
        HistoricoConsultaPacienteResponse.HistoricoConsultaPacienteResponseBuilder builder =
            HistoricoConsultaPacienteResponse.builder();

        builder.tipoProntuario("MEDICO");

        // Dados da Consulta
        builder.consultaId(consulta.getId())
               .dataHora(consulta.getDataHora())
               .duracaoMinutos(consulta.getDuracaoMinutos())
               .observacoes(consulta.getObservacoes())
               .valor(consulta.getValor())
               .status(consulta.getStatus() != null ? consulta.getStatus().name() : null)
               .motivoCancelamento(consulta.getMotivoCancelamento());

        // Dados do Paciente
        if (consulta.getPaciente() != null) {
            builder.pacienteId(consulta.getPaciente().getPaciCodigo())
                   .pacienteNome(consulta.getPaciente().getPaciNome())
                   .pacienteCpf(consulta.getPaciente().getPaciCpf())
                   .pacienteDataNascimento(consulta.getPaciente().getPaciDataNacimento())
                   .pacienteTelefone(consulta.getPaciente().getPaciTelefone());
        }

        // Dados do Profissional
        if (consulta.getProfissional() != null) {
            builder.profissionalId(consulta.getProfissional().getId())
                   .profissionalNome(consulta.getProfissional().getNome())
                   .profissionalCrm(consulta.getProfissional().getRegistroConselho());

            if (consulta.getEspecialidade() != null) {
                builder.profissionalEspecialidade(consulta.getEspecialidade().getNome());
            }
        }

        // TODOS os campos do Prontuário Médico
        if (prontuario != null) {
            builder.prontuarioId(prontuario.getProntCodigoProntuario())
                   .codigoProntuario(prontuario.getProntCodigoProntuario().toString())

                   // Dados Vitais e Antropométricos
                   .peso(prontuario.getProntPeso())
                   .altura(prontuario.getProntAltura())
                   .temperatura(prontuario.getProntTemperatura())
                   .pressao(prontuario.getProntPressao())
                   .saturacao(prontuario.getProntSaturacao())
                   .hemoglobina(prontuario.getProntHemoglobina())
                   .frequenciaRespiratoria(prontuario.getProntFrequenciaRespiratoria())
                   .frequenciaArterialSistolica(prontuario.getProntFrequenciaArterialSistolica())
                   .frequenciaArterialDiastolica(prontuario.getProntFrequenciaArterialDiastolica())


                   // Anamnese e Avaliação
                   .queixaPrincipal(prontuario.getProntQueixaPricipal())
                   .anamnese(prontuario.getProntAnamnese())
                   .observacao(prontuario.getProntObservacao())
                   .diagnostico(prontuario.getProntDiagnostico())

                   // Prescrição Médica
                    .tituloPrescricao(prontuario.getProntTituloPrescricao())
                    .prescricao(prontuario.getProntPrescricao())

                   .tempoDuracao(prontuario.getProntTempoDuracao())

                   // Dados de Controle
                   .dataFinalizado(prontuario.getProntDataFinalizado());
        }

        return builder.build();
    }
}
