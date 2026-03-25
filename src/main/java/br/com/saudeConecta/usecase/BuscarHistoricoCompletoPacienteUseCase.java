package br.com.saudeConecta.usecase;

import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.prontuario.ProntuarioDentista;
import br.com.saudeConecta.infra.tenant.RequiresTenant;
import br.com.saudeConecta.infra.tenant.TenantHelper;
import br.com.saudeConecta.infrastructure.persistence.repository.ConsultaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProntuarioDentistaRepository;
import br.com.saudeConecta.presentation.dto.consulta.HistoricoConsultaPacienteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Use Case responsável por buscar o histórico completo de consultas odontológicas de um paciente.
 * Retorna consultas REALIZADAS ou PAGAS que possuem prontuário dentista,
 * incluindo TODOS os campos do domínio ProntuarioDentista (dentes, planejamentos, exame intrabucal, etc.).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BuscarHistoricoCompletoPacienteUseCase {

    private final ConsultaRepository consultaRepository;
    private final ProntuarioDentistaRepository prontuarioDentistaRepository;
    private final TenantHelper tenantHelper;

    /**
     * Executa a busca do histórico completo de consultas odontológicas do paciente.
     *
     * @param pacienteId ID do paciente
     * @param profissionalId ID do profissional (opcional, null retorna todos)
     * @return Lista de DTOs com histórico completo (consulta + prontuário dentista)
     */
    @RequiresTenant
    @Transactional(readOnly = true)
    public List<HistoricoConsultaPacienteResponse> executar(Long pacienteId, Long profissionalId) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.info("Buscando histórico odontológico do paciente ID: {} profissionalId: {} na organização ID: {}", pacienteId, profissionalId, orgId);

        List<Consulta> consultas = consultaRepository.findHistoricoCompletoPaciente(pacienteId, orgId, profissionalId);
        log.debug("Encontradas {} consultas odontológicas para o paciente", consultas.size());

        return consultas.stream()
            .map(consulta -> {
                List<ProntuarioDentista> prontuarios = prontuarioDentistaRepository.findByConsultaId(consulta.getId());
                ProntuarioDentista prontuario = prontuarios.isEmpty() ? null : prontuarios.get(0);
                return mapearParaHistoricoResponse(consulta, prontuario);
            })
            .toList();
    }

    /**
     * Mapeia entidade Consulta e ProntuarioDentista para DTO de resposta.
     * Preenche TODOS os campos do domínio ProntuarioDentista, incluindo dentes e planejamentos.
     *
     * @param consulta Entidade Consulta
     * @param prontuario Entidade ProntuarioDentista (pode ser null)
     * @return DTO com dados do histórico odontológico
     */
    private HistoricoConsultaPacienteResponse mapearParaHistoricoResponse(Consulta consulta, ProntuarioDentista prontuario) {
        HistoricoConsultaPacienteResponse.HistoricoConsultaPacienteResponseBuilder builder =
            HistoricoConsultaPacienteResponse.builder();

        builder.tipoProntuario("DENTISTA");

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

        // TODOS os campos do Prontuário Dentista
        if (prontuario != null) {
            builder.prontuarioId(prontuario.getCodigo())
                   .codigoProntuario(prontuario.getCodigo().toString())

                   // Anamnese e Avaliação Odontológica
                   .queixaPrincipal(prontuario.getQueixaPrincipal())
                   .anamnese(prontuario.getAnamnese())
                   .observacao(prontuario.getObservacao())
                   .diagnostico(prontuario.getDiagnostico())

                   // Avaliação Bucal
                   .higieneBucal(prontuario.getHigieneBucal())
                   .condicaoGengival(prontuario.getCondicaoGengival())
                   .oclusal(prontuario.getOclusal())
                   .atm(prontuario.getAtm())

                   // Plano de Tratamento
                   .planoTratamento(prontuario.getPlanoTratamento())
                   .procedimentos(prontuario.getProcedimentos())
                   .orientacoes(prontuario.getOrientacoes())

                   // Prescrição
                   .tituloPrescricao(prontuario.getTituloPrescricao())
                   .dataPrescricao(prontuario.getDataPrescricao())
                   .prescricao(prontuario.getPrescricao())

                   // Exames
                   .tituloExame(prontuario.getTituloExame())
                   .dataExame(prontuario.getDataExame())
                   .tempoDuracao(prontuario.getTempoDuracao())

                   // Controle e Tratamento
                   .responsavel(prontuario.getResponsavel())
                     .dataFinalizadoDentista(prontuario.getDataFinalizado())

                   // Sinais Vitais
                   .pressao(prontuario.getPressaoArterial())
                   .pulso(prontuario.getPulso())
                   .altura(prontuario.getAltura())
                   .temperatura(prontuario.getTemperatura())
                   .peso(prontuario.getPeso())
                   .edema(prontuario.getEdema())
                   .facies(prontuario.getFacies())
                   .linfonodos(prontuario.getLinfonodos())
                   .labios(prontuario.getLabios())
                   .mucosas(prontuario.getMucosas())
                   .soalhoBucal(prontuario.getSoalhoBucal())
                   .palato(prontuario.getPalato())
                   .orofaringe(prontuario.getOrofaringe())

                   // Exame Intrabucal
                   .lingua(prontuario.getLingua())
                   .gengiva(prontuario.getGengiva())
                   .habitosNocivos(prontuario.getHabitosNocivos())
                   .portadorAparelho(prontuario.getPortadorAparelho())
                   .exameOutros(prontuario.getExameOutros())

                   // TUSS e CID
                   .tussTexto(prontuario.getTussTexto())
                   .cidTexto(prontuario.getCidTexto())
                   .solicitacaoExameTexto(prontuario.getSolicitacaoExameTexto())

                   // Odontograma — Dentes
                   .dentes(prontuario.getDentes() != null ?
                       prontuario.getDentes().stream()
                           .map(d -> HistoricoConsultaPacienteResponse.DenteResponse.builder()
                               .codigo(d.getCodigo())
                               .numeroFdi(d.getNumeroFdi())
                               .status(d.getStatus())
                               .observacao(d.getObservacao())
                               .build())
                           .toList() : List.of())

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
                           .toList() : List.of());
        }

        return builder.build();
    }
}
