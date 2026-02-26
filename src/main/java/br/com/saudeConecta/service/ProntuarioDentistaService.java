package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.consulta.StatusConsulta;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.prontuario.PlanejamentoTerapeutico;
import br.com.saudeConecta.domain.prontuario.ProntuarioDentista;
import br.com.saudeConecta.domain.prontuario.ProntuarioDentistaDente;
import br.com.saudeConecta.infra.tenant.TenantContext;
import br.com.saudeConecta.infrastructure.persistence.repository.ConsultaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.PacienteRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProntuarioDentistaRepository;
import br.com.saudeConecta.presentation.dto.prontuario.CadastrarProntuarioDentistaRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProntuarioDentistaService {

    private final ProntuarioDentistaRepository prontuarioDentistaRepository;
    private final ProfissionalRepository profissionalRepository;
    private final ConsultaRepository consultaRepository;
    private final PacienteRepository pacienteRepository;

    // =========================================================================
    // CADASTRO
    // =========================================================================

    @Transactional
    public ProntuarioDentista cadastrarProntuarioByOrg(CadastrarProntuarioDentistaRequest request) {
        log.info("Cadastrando prontuário odontológico — consulta={}", request.getConsulta());

        Profissional profissional = profissionalRepository
                .findById(request.getCodigoMedico())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Profissional não encontrado: " + request.getCodigoMedico()));

        Consulta consulta = consultaRepository
                .findById(request.getConsulta())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Consulta não encontrada: " + request.getConsulta()));

        // ── Monta prontuário principal ────────────────────────────────────────
        ProntuarioDentista prontuario = ProntuarioDentista.builder()
                // anamnese
                .queixaPrincipal(request.getQueixaPrincipal())
                .anamnese(request.getAnamnese())
                .observacao(request.getObservacao())
                // exame clínico — campos separados
                .higieneBucal(request.getHigieneBucal())
                .condicaoGengival(request.getCondicaoGengival())
                .oclusal(request.getOclusal())
                .atm(request.getAtm())
                // diagnóstico
                .diagnostico(request.getDiagnostico())
                .planoTratamento(request.getPlanoTratamento())
                // prescrição
                .tituloPrescricao(request.getTituloPrescricao())
                .dataPrescricao(request.getDataPrescricao())
                .prescricao(request.getPrescricao())
                // procedimentos
                .tituloExame(request.getTituloExame())
                .dataExame(request.getDataExame())
                .procedimentos(request.getProcedimentos())
                .orientacoes(request.getOrientacoes())
                // controle
                .dataFinalizado(parseData(request.getDataFinalizado()))
                .tempoDuracao(request.getTempoDuracao())
                // identificação do paciente (endereço vem da entidade Paciente)
                .responsavel(request.getResponsavel())
                .inicioTratamento(parseData(request.getInicioTratamento()))
                .terminoTratamento(parseData(request.getTerminoTratamento()))
                .interrupcao(request.getInterrupcao())
                // exame objetivo — sinais vitais
                .pressaoArterial(request.getPressaoArterial())
                .pulso(request.getPulso())
                .altura(request.getAltura())
                .temperatura(request.getTemperatura())
                .peso(request.getPeso())
                .edema(request.getEdema())
                .facies(request.getFacies())
                .linfonodos(request.getLinfonodos())
                .labios(request.getLabios())
                .mucosas(request.getMucosas())
                .soalhoBucal(request.getSoalhoBucal())
                .palato(request.getPalato())
                .orofaringe(request.getOrofaringe())
                // exame objetivo — exame intrabucal
                .lingua(request.getLingua())
                .gengiva(request.getGengiva())
                .habitosNocivos(request.getHabitosNocivos())
                .portadorAparelho(request.getPortadorAparelho())
                .oclusao(request.getOclusao())
                .exameOutros(request.getExameOutros())
                // relacionamentos
                .profissional(profissional)
                .consulta(consulta)
                .build();

        // ── Adiciona dentes do odontograma ────────────────────────────────────
        if (!CollectionUtils.isEmpty(request.getOdontograma())) {
            for (CadastrarProntuarioDentistaRequest.DenteRequest dr : request.getOdontograma()) {
                // só persiste dentes alterados (sadio sem observação não agrega informação)
                if ("sadio".equals(dr.getStatus()) &&
                        (dr.getObservacao() == null || dr.getObservacao().isBlank())) {
                    continue;
                }
                ProntuarioDentistaDente dente = ProntuarioDentistaDente.builder()
                        .numeroFdi(dr.getNumeroFdi())
                        .status(dr.getStatus())
                        .observacao(dr.getObservacao())
                        .build();
                prontuario.addDente(dente);
            }
        }

        ProntuarioDentista salvo = prontuarioDentistaRepository.save(prontuario);
        log.info("Prontuário odontológico salvo — id={}, dentes={}",
                salvo.getCodigo(), salvo.getDentes().size());

        // ── Adiciona planejamentos terapêuticos ─────────────────────────────────
        if (!CollectionUtils.isEmpty(request.getPlanejamentos())) {
            Long orgId = TenantContext.getCurrentTenant();
            Organizacao organizacao = new Organizacao();
            organizacao.setId(orgId);

            for (CadastrarProntuarioDentistaRequest.PlanejamentoItem item : request.getPlanejamentos()) {
                Paciente paciente = null;
                if (item.getPacienteId() != null) {
                    paciente = pacienteRepository.findById(item.getPacienteId()).orElse(null);
                }

                PlanejamentoTerapeutico planejamento = PlanejamentoTerapeutico.builder()
                        .prontuarioDentista(salvo)
                        .consulta(consulta)
                        .paciente(paciente)
                        .profissional(profissional)
                        .organizacao(organizacao)
                        .dataProcedimento(parseData(item.getDataProcedimento()))
                        .procedimentoRealizado(item.getProcedimentoRealizado())
                        .valor(item.getValor())
                        .statusAssinatura("PENDENTE")
                        .build();
                salvo.addPlanejamento(planejamento);
            }
            prontuarioDentistaRepository.save(salvo);
            log.info("Planejamentos salvos — total={}", request.getPlanejamentos().size());
        }

        consulta.setStatus(StatusConsulta.REALIZADA);
        consultaRepository.save(consulta);
        log.info("Status da consulta ID: {} atualizado para REALIZADA", consulta.getId());

        return salvo;
    }

    // =========================================================================
    // CONSULTAS
    // =========================================================================

    @Transactional(readOnly = true)
    public ProntuarioDentista buscarPorId(Long id) {
        return prontuarioDentistaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Prontuário odontológico não encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public List<ProntuarioDentista> listarPorConsulta(Long consultaId) {
        return prontuarioDentistaRepository.findByConsultaId(consultaId);
    }

    @Transactional(readOnly = true)
    public ProntuarioDentista buscarMaisRecentePorConsulta(Long consultaId) {
        Long id = prontuarioDentistaRepository
                .findIdMaisRecentePorConsulta(consultaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Nenhum prontuário odontológico encontrado para consulta: " + consultaId));

        return prontuarioDentistaRepository
                .findByIdComDentes(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Prontuário odontológico não encontrado: id=" + id));
    }




    @Transactional(readOnly = true)
    public List<ProntuarioDentista> listarPorProfissional(Long profissionalId) {
        return prontuarioDentistaRepository.findByProfissionalId(profissionalId);
    }

    /**
     * Lista prontuários de um paciente (histórico).
     *
     * @param pacienteId ID do paciente
     * @return lista de prontuários do paciente
     */
    @Transactional(readOnly = true)
    public List<ProntuarioDentista> listarPorPaciente(Long pacienteId) {
        return prontuarioDentistaRepository.findByPacienteId(pacienteId);
    }

    // =========================================================================
    // UTILITÁRIO
    // =========================================================================

    private LocalDate parseData(String data) {
        if (data == null || data.isBlank()) return LocalDate.now();
        try {
            return LocalDate.parse(data);
        } catch (Exception e) {
            log.warn("Data inválida '{}', usando data atual.", data);
            return LocalDate.now();
        }
    }
}