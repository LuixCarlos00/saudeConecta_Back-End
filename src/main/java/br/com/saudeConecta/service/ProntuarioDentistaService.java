package br.com.saudeConecta.service;

  import br.com.saudeConecta.domain.consulta.Consulta;
  import br.com.saudeConecta.domain.consulta.StatusConsulta;
  import br.com.saudeConecta.domain.profissional.Profissional;
  import br.com.saudeConecta.domain.prontuario.ProntuarioDentista;
import br.com.saudeConecta.domain.prontuario.ProntuarioDentistaDente;
  import br.com.saudeConecta.infrastructure.persistence.repository.ConsultaRepository;
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