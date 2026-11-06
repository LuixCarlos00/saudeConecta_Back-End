package br.com.saudeConecta.application.historico;

import br.com.saudeConecta.application.consulta.dto.HistoricoConsultaPacienteResponse;
import br.com.saudeConecta.config.tenant.RequiresTenant;
import br.com.saudeConecta.config.tenant.TenantHelper;
import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.prontuario.Prontuario;
import br.com.saudeConecta.infrastructure.persistence.repository.ConsultaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProntuarioRepository;
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
     * @param profissionalId ID do profissional (opcional, null retorna todos)
     * @return Lista de DTOs com histórico completo (consulta + prontuário médico)
     */
    @RequiresTenant
    @Transactional(readOnly = true)
    public List<HistoricoConsultaPacienteResponse> executar(Long pacienteId, Long profissionalId) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.debug("historico.paciente buscar perfil=MEDICO pacienteId={} profissionalId={}", pacienteId, profissionalId);

        List<Consulta> consultas = consultaRepository.findHistoricoCompletoPacienteMedico(pacienteId, orgId, profissionalId);
        log.debug("historico.paciente consultas perfil=MEDICO quantidade={}", consultas.size());

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
                   .profissionalCrm(consulta.getProfissional().getRegistroConselho())
                   // Para o front saber se o logado é o dono (só o dono edita o prontuário)
                   .profissionalUsuarioId(consulta.getProfissional().getUsuario() != null
                           ? consulta.getProfissional().getUsuario().getId() : null);

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
                   .pulso(prontuario.getProntPulso())
                   .responsavel(prontuario.getProntResponsavel())

                   // Anamnese e Avaliação
                   .queixaPrincipal(prontuario.getProntQueixaPricipal())
                   .anamnese(prontuario.getProntAnamnese())
                   .observacao(prontuario.getProntObservacao())
                   .diagnostico(prontuario.getProntDiagnostico())

                   // Orientações e campos adicionais
                   .orientacoes(prontuario.getProntOrientacoes())

                   // Codificação (CID/TUSS) e exames solicitados: estavam fora do histórico,
                   // embora apareçam no registro individual (/prontuario/buscarProntuarioById)
                   .cidTexto(prontuario.getProntCidTexto())
                   .tussTexto(prontuario.getProntTussTexto())
                   .solicitacaoExameTexto(prontuario.getProntSolicitacaoExameTexto())

                   // Prescrição Médica
                    .tituloPrescricao(prontuario.getProntTituloPrescricao())
                    .prescricao(prontuario.getProntPrescricao())

                   .tempoDuracao(prontuario.getProntTempoDuracao())

                   // Planejamentos Terapêuticos
                   .planejamentos(prontuario.getPlanejamentos() != null ?
                       prontuario.getPlanejamentos().stream()
                           .map(p -> HistoricoConsultaPacienteResponse.PlanejamentoResponse.builder()
                               .id(p.getId())
                               .dataProcedimento(p.getDataProcedimento())
                               .procedimentoRealizado(p.getProcedimentoRealizado())
                               .valor(p.getValor())
                               .statusAssinatura(p.getStatusAssinatura())
                               .build())
                           .toList() : List.of())

                   // Dados de Controle
                   .dataFinalizado(prontuario.getProntDataFinalizado());
        }

        return builder.build();
    }
}
