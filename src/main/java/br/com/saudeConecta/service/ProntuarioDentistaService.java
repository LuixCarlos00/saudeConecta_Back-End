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
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import jakarta.persistence.EntityManager;
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
    private final EntityManager entityManager;

    // =========================================================================
    // CADASTRO
    // =========================================================================

    @Transactional
    public ProntuarioDentista cadastrarProntuarioByOrg(CadastrarProntuarioDentistaRequest request) {
        log.info("Cadastrando prontuario odontologico — consulta={}", request.getConsulta());

        Profissional profissional = profissionalRepository
                .findById(request.getCodigoMedico())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Profissional nao encontrado: " + request.getCodigoMedico()));

        Consulta consulta = consultaRepository
                .findById(request.getConsulta())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Consulta nao encontrada: " + request.getConsulta()));

        // ── Monta Prontuario principal ────────────────────────────────────────
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
                .exameOutros(request.getExameOutros())
                // TUSS e CID
                .tussTexto(request.getTussTexto())
                .cidTexto(request.getCidTexto())
                .solicitacaoExameTexto(request.getSolicitacaoExameTexto())
                // relacionamentos
                .profissional(profissional)
                .consulta(consulta)
                .build();

        // ── Adiciona dentes do odontograma ────────────────────────────────────
        if (!CollectionUtils.isEmpty(request.getOdontograma())) {
            for (CadastrarProntuarioDentistaRequest.DenteRequest dr : request.getOdontograma()) {
                // só persiste dentes alterados (sadio sem observação nao agrega informação)
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
        log.info("Prontuario odontologico salvo — id={}, dentes={}",
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
    // ATUALIZAÇÃO
    // =========================================================================

    /**
     * Atualiza um Prontuario odontologico existente.
     *
     * @param id      ID do Prontuario a ser atualizado
     * @param request Dados atualizados do Prontuario
     */
    @Transactional
    public void atualizarProntuario(Long id, CadastrarProntuarioDentistaRequest request) {
        log.info("Atualizando prontuario odontologico — id={}", id);

        ProntuarioDentista prontuario = prontuarioDentistaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Prontuario odontologico nao encontrado: " + id));

        // ── Anamnese ──
        prontuario.setQueixaPrincipal(request.getQueixaPrincipal());
        prontuario.setAnamnese(request.getAnamnese());
        prontuario.setObservacao(request.getObservacao());

        // ── Exame Clínico ──
        prontuario.setHigieneBucal(request.getHigieneBucal());
        prontuario.setCondicaoGengival(request.getCondicaoGengival());
        prontuario.setOclusal(request.getOclusal());
        prontuario.setAtm(request.getAtm());

        // ── Diagnóstico ──
        prontuario.setDiagnostico(request.getDiagnostico());
        prontuario.setPlanoTratamento(request.getPlanoTratamento());

        // ── Prescrição ──
        prontuario.setTituloPrescricao(request.getTituloPrescricao());
        prontuario.setDataPrescricao(request.getDataPrescricao());
        prontuario.setPrescricao(request.getPrescricao());

        // ── Procedimentos ──
        prontuario.setTituloExame(request.getTituloExame());
        prontuario.setDataExame(request.getDataExame());
        prontuario.setProcedimentos(request.getProcedimentos());
        prontuario.setOrientacoes(request.getOrientacoes());

        // ── TUSS e CID ──
        prontuario.setTussTexto(request.getTussTexto());
        prontuario.setCidTexto(request.getCidTexto());
        prontuario.setSolicitacaoExameTexto(request.getSolicitacaoExameTexto());

        // ── Identificação ──
        prontuario.setResponsavel(request.getResponsavel());

        // ── Sinais Vitais ──
        prontuario.setPressaoArterial(request.getPressaoArterial());
        prontuario.setPulso(request.getPulso());
        prontuario.setAltura(request.getAltura());
        prontuario.setTemperatura(request.getTemperatura());
        prontuario.setPeso(request.getPeso());
        prontuario.setEdema(request.getEdema());
        prontuario.setFacies(request.getFacies());
        prontuario.setLinfonodos(request.getLinfonodos());
        prontuario.setLabios(request.getLabios());
        prontuario.setMucosas(request.getMucosas());
        prontuario.setSoalhoBucal(request.getSoalhoBucal());
        prontuario.setPalato(request.getPalato());
        prontuario.setOrofaringe(request.getOrofaringe());

        // ── Exame Intrabucal ──
        prontuario.setLingua(request.getLingua());
        prontuario.setGengiva(request.getGengiva());
        prontuario.setHabitosNocivos(request.getHabitosNocivos());
        prontuario.setPortadorAparelho(request.getPortadorAparelho());
        prontuario.setExameOutros(request.getExameOutros());

        // ── Atualiza odontograma (limpa e recria) ──
        prontuario.getDentes().clear();
        prontuarioDentistaRepository.saveAndFlush(prontuario);

        if (!CollectionUtils.isEmpty(request.getOdontograma())) {
            for (CadastrarProntuarioDentistaRequest.DenteRequest dr : request.getOdontograma()) {
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

        // ── Atualiza planejamentos terapêuticos (limpa e recria) ──
        prontuario.getPlanejamentos().clear();
        prontuarioDentistaRepository.saveAndFlush(prontuario);

        if (!CollectionUtils.isEmpty(request.getPlanejamentos())) {
            Long orgId = TenantContext.getCurrentTenant();
            Organizacao organizacao = new Organizacao();
            organizacao.setId(orgId);

            // Busca consulta e profissional apenas uma vez
            Consulta consulta = prontuario.getConsulta();
            Profissional profissional = prontuario.getProfissional();

            for (CadastrarProntuarioDentistaRequest.PlanejamentoItem item : request.getPlanejamentos()) {
                Paciente paciente = null;
                if (item.getPacienteId() != null) {
                    paciente = pacienteRepository.findById(item.getPacienteId()).orElse(null);
                }

                PlanejamentoTerapeutico planejamento = PlanejamentoTerapeutico.builder()
                        .prontuarioDentista(prontuario)
                        .consulta(consulta)
                        .paciente(paciente)
                        .profissional(profissional)
                        .organizacao(organizacao)
                        .dataProcedimento(parseData(item.getDataProcedimento()))
                        .procedimentoRealizado(item.getProcedimentoRealizado())
                        .valor(item.getValor())
                        .statusAssinatura("PENDENTE")
                        .build();
                prontuario.addPlanejamento(planejamento);
            }
            log.info("Planejamentos atualizados — total={}", request.getPlanejamentos().size());
        }

        prontuarioDentistaRepository.save(prontuario);
        log.info("Prontuario odontologico atualizado — id={}", id);
    }

    // =========================================================================
    // CONSULTAS
    // =========================================================================

    @Transactional(readOnly = true)
    public ProntuarioDentista buscarPorId(Long id) {
        return prontuarioDentistaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Prontuario odontologico nao encontrado: " + id));
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
                        "Nenhum Prontuario odontologico encontrado para consulta: " + consultaId));

        ProntuarioDentista pd = prontuarioDentistaRepository
                .findByIdComDentes(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Prontuario odontologico nao encontrado: id=" + id));

        // Inicializa planejamentos em query separada para evitar cartesian product com dentes
        Hibernate.initialize(pd.getPlanejamentos());

        return pd;
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
        List<ProntuarioDentista> lista = prontuarioDentistaRepository.findByPacienteId(pacienteId);
        // Inicializa planejamentos em query separada para evitar cartesian product com dentes
        lista.forEach(pd -> Hibernate.initialize(pd.getPlanejamentos()));
        return lista;
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