package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.consulta.StatusConsulta;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.prontuario.PlanejamentoTerapeutico;
import br.com.saudeConecta.domain.prontuario.Prontuario;
import br.com.saudeConecta.domain.prontuario.ProntuarioDentista;
import br.com.saudeConecta.domain.prontuario.TermoAutorizacao;
import br.com.saudeConecta.domain.relatorio.TipoDocumentoRelatorio;
import br.com.saudeConecta.infra.tenant.RequiresTenant;
import br.com.saudeConecta.infra.tenant.TenantHelper;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.RelatorioRepository;
import br.com.saudeConecta.presentation.dto.relatorio.ConsultaRelatorioResponse;
import br.com.saudeConecta.presentation.dto.relatorio.DocumentoRelatorioResponse;
import br.com.saudeConecta.presentation.dto.relatorio.PacienteAtendidoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Monta os relatorios disponiveis por paciente a partir dos dados de atendimento.
 *
 * Os documentos nao possuem tabela propria: sao derivados do prontuario medico,
 * do prontuario odontologico, dos planejamentos terapeuticos, do termo de
 * autorizacao e do status de pagamento da consulta.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RelatorioPacienteService {

    private final RelatorioRepository relatorioRepository;
    private final ProfissionalRepository profissionalRepository;
    private final TenantHelper tenantHelper;

    /**
     * Lista os pacientes atendidos com as consultas e documentos disponiveis.
     *
     * @param profissionalId profissional a filtrar; nulo considera toda a organizacao
     * @param pacienteId     paciente a filtrar; nulo considera todos os pacientes
     * @param termo          busca por nome, cpf ou telefone; nulo ou vazio ignora o filtro
     * @param dataInicio     inicio do periodo; nulo ignora o limite inferior
     * @param dataFim        fim do periodo; nulo ignora o limite superior
     * @return pacientes ordenados do atendimento mais recente para o mais antigo
     */
    @RequiresTenant
    @Transactional(readOnly = true)
    public List<PacienteAtendidoResponse> buscarPacientesAtendidos(Long profissionalId,
                                                                   Long pacienteId,
                                                                   String termo,
                                                                   LocalDate dataInicio,
                                                                   LocalDate dataFim) {
        Long orgId = tenantHelper.getCurrentTenantId();
        String termoNormalizado = (termo == null || termo.isBlank()) ? null : termo.trim();
        LocalDateTime inicio = dataInicio != null ? dataInicio.atStartOfDay() : null;
        LocalDateTime fim = dataFim != null ? dataFim.plusDays(1).atStartOfDay() : null;

        log.debug("Montando relatorios - orgId: {}, profissionalId: {}, pacienteId: {}, termo: {}",
                orgId, profissionalId, pacienteId, termoNormalizado);

        List<Consulta> consultas = relatorioRepository.buscarConsultasParaRelatorio(
                orgId, profissionalId, pacienteId, termoNormalizado, inicio, fim);

        if (consultas.isEmpty()) {
            return List.of();
        }

        Map<Long, List<DocumentoRelatorioResponse>> documentosPorConsulta = new LinkedHashMap<>();
        Map<Long, String> diagnosticoPorConsulta = new LinkedHashMap<>();

        coletarDocumentosDeProntuariosMedicos(orgId, profissionalId, pacienteId, documentosPorConsulta, diagnosticoPorConsulta);
        coletarDocumentosDeProntuariosDentista(orgId, profissionalId, pacienteId, documentosPorConsulta, diagnosticoPorConsulta);
        coletarDocumentosDePlanejamentos(orgId, profissionalId, pacienteId, documentosPorConsulta);
        coletarDocumentosDeTermos(orgId, profissionalId, pacienteId, documentosPorConsulta);

        return agruparPorPaciente(consultas, documentosPorConsulta, diagnosticoPorConsulta);
    }

    /**
     * Busca os relatorios de um unico paciente.
     *
     * @param pacienteId     paciente desejado
     * @param profissionalId profissional a filtrar; nulo considera toda a organizacao
     * @return paciente com consultas e documentos, quando houver atendimento
     */
    @RequiresTenant
    @Transactional(readOnly = true)
    public Optional<PacienteAtendidoResponse> buscarRelatoriosDoPaciente(Long pacienteId, Long profissionalId) {
        return buscarPacientesAtendidos(profissionalId, pacienteId, null, null, null)
                .stream()
                .findFirst();
    }

    /**
     * Resolve o profissional vinculado a um usuario logado.
     *
     * @param usuarioId usuario autenticado
     * @return id do profissional ou vazio quando o usuario nao for um clinico
     */
    @Transactional(readOnly = true)
    public Optional<Long> buscarProfissionalDoUsuario(Long usuarioId) {
        return profissionalRepository.findByUsuario_Id(usuarioId)
                .map(Profissional::getId);
    }

    // ── Coleta de documentos ───────────────────────────────

    private void coletarDocumentosDeProntuariosMedicos(Long orgId,
                                                       Long profissionalId,
                                                       Long pacienteId,
                                                       Map<Long, List<DocumentoRelatorioResponse>> documentos,
                                                       Map<Long, String> diagnosticos) {
        for (Prontuario prontuario : relatorioRepository.buscarProntuariosMedicos(orgId, profissionalId, pacienteId)) {
            Long consultaId = prontuario.getConsulta() != null ? prontuario.getConsulta().getId() : null;
            if (consultaId == null) {
                continue;
            }

            LocalDateTime emitidoEm = converterParaDataHora(prontuario.getProntDataFinalizado(),
                    prontuario.getConsulta().getDataHora());
            Long id = prontuario.getProntCodigoProntuario();

            registrarDiagnostico(diagnosticos, consultaId, prontuario.getProntDiagnostico());

            adicionar(documentos, consultaId, new DocumentoRelatorioResponse(
                    id,
                    TipoDocumentoRelatorio.REGISTRO_CONSULTA,
                    "Registro da consulta",
                    resumir(prontuario.getProntDiagnostico(), TipoDocumentoRelatorio.REGISTRO_CONSULTA.getDescricaoPadrao()),
                    emitidoEm,
                    false,
                    consultaId));

            if (preenchido(prontuario.getProntPrescricao())) {
                adicionar(documentos, consultaId, new DocumentoRelatorioResponse(
                        id,
                        TipoDocumentoRelatorio.PRESCRICAO,
                        preenchido(prontuario.getProntTituloPrescricao())
                                ? prontuario.getProntTituloPrescricao()
                                : "Receituario",
                        resumir(prontuario.getProntPrescricao(), TipoDocumentoRelatorio.PRESCRICAO.getDescricaoPadrao()),
                        emitidoEm,
                        false,
                        consultaId));
            }

            if (preenchido(prontuario.getProntSolicitacaoExameTexto())) {
                adicionar(documentos, consultaId, new DocumentoRelatorioResponse(
                        id,
                        TipoDocumentoRelatorio.EXAMES,
                        "Solicitacao de exames",
                        resumir(prontuario.getProntSolicitacaoExameTexto(), TipoDocumentoRelatorio.EXAMES.getDescricaoPadrao()),
                        emitidoEm,
                        false,
                        consultaId));
            }
        }
    }

    private void coletarDocumentosDeProntuariosDentista(Long orgId,
                                                        Long profissionalId,
                                                        Long pacienteId,
                                                        Map<Long, List<DocumentoRelatorioResponse>> documentos,
                                                        Map<Long, String> diagnosticos) {
        for (ProntuarioDentista prontuario : relatorioRepository.buscarProntuariosDentista(orgId, profissionalId, pacienteId)) {
            Long consultaId = prontuario.getConsulta() != null ? prontuario.getConsulta().getId() : null;
            if (consultaId == null) {
                continue;
            }

            LocalDateTime emitidoEm = prontuario.getDataFinalizado() != null
                    ? prontuario.getDataFinalizado().atStartOfDay()
                    : prontuario.getConsulta().getDataHora();
            Long id = prontuario.getCodigo();

            registrarDiagnostico(diagnosticos, consultaId, prontuario.getDiagnostico());

            adicionar(documentos, consultaId, new DocumentoRelatorioResponse(
                    id,
                    TipoDocumentoRelatorio.REGISTRO_CONSULTA,
                    "Registro da consulta",
                    resumir(prontuario.getDiagnostico(), TipoDocumentoRelatorio.REGISTRO_CONSULTA.getDescricaoPadrao()),
                    emitidoEm,
                    false,
                    consultaId));

            if (preenchido(prontuario.getPrescricao())) {
                adicionar(documentos, consultaId, new DocumentoRelatorioResponse(
                        id,
                        TipoDocumentoRelatorio.PRESCRICAO,
                        preenchido(prontuario.getTituloPrescricao()) ? prontuario.getTituloPrescricao() : "Receituario",
                        resumir(prontuario.getPrescricao(), TipoDocumentoRelatorio.PRESCRICAO.getDescricaoPadrao()),
                        emitidoEm,
                        false,
                        consultaId));
            }

            if (preenchido(prontuario.getSolicitacaoExameTexto())) {
                adicionar(documentos, consultaId, new DocumentoRelatorioResponse(
                        id,
                        TipoDocumentoRelatorio.EXAMES,
                        preenchido(prontuario.getTituloExame()) ? prontuario.getTituloExame() : "Solicitacao de exames",
                        resumir(prontuario.getSolicitacaoExameTexto(), TipoDocumentoRelatorio.EXAMES.getDescricaoPadrao()),
                        emitidoEm,
                        false,
                        consultaId));
            }
        }
    }

    /**
     * Agrupa os planejamentos de cada consulta em um unico documento.
     */
    private void coletarDocumentosDePlanejamentos(Long orgId,
                                                  Long profissionalId,
                                                  Long pacienteId,
                                                  Map<Long, List<DocumentoRelatorioResponse>> documentos) {
        Map<Long, List<PlanejamentoTerapeutico>> porConsulta = new LinkedHashMap<>();

        for (PlanejamentoTerapeutico planejamento : relatorioRepository.buscarPlanejamentos(orgId, profissionalId, pacienteId)) {
            Long consultaId = planejamento.getConsulta() != null ? planejamento.getConsulta().getId() : null;
            if (consultaId == null) {
                continue;
            }
            porConsulta.computeIfAbsent(consultaId, chave -> new ArrayList<>()).add(planejamento);
        }

        porConsulta.forEach((consultaId, planejamentos) -> {
            PlanejamentoTerapeutico primeiro = planejamentos.get(0);
            boolean assinado = planejamentos.stream().anyMatch(PlanejamentoTerapeutico::isAssinado);

            LocalDateTime emitidoEm = primeiro.getCreatedAt() != null
                    ? primeiro.getCreatedAt()
                    : primeiro.getDataProcedimento().atStartOfDay();

            String descricao = planejamentos.size() == 1
                    ? resumir(primeiro.getProcedimentoRealizado(), TipoDocumentoRelatorio.PLANEJAMENTO.getDescricaoPadrao())
                    : planejamentos.size() + " procedimentos planejados";

            adicionar(documentos, consultaId, new DocumentoRelatorioResponse(
                    primeiro.getId(),
                    TipoDocumentoRelatorio.PLANEJAMENTO,
                    "Planejamento terapeutico",
                    descricao,
                    emitidoEm,
                    assinado,
                    consultaId));
        });
    }

    private void coletarDocumentosDeTermos(Long orgId,
                                           Long profissionalId,
                                           Long pacienteId,
                                           Map<Long, List<DocumentoRelatorioResponse>> documentos) {
        for (TermoAutorizacao termo : relatorioRepository.buscarTermosAutorizacao(orgId, profissionalId, pacienteId)) {
            Long consultaId = termo.getConsulta() != null ? termo.getConsulta().getId() : null;
            if (consultaId == null) {
                continue;
            }

            LocalDateTime emitidoEm = termo.getDataAssinatura() != null ? termo.getDataAssinatura() : termo.getCreatedAt();

            adicionar(documentos, consultaId, new DocumentoRelatorioResponse(
                    termo.getId(),
                    TipoDocumentoRelatorio.QUESTIONARIO_SAUDE,
                    "Questionario de saude",
                    termo.isAssinado() ? "Respondido e assinado pelo paciente" : "Aguardando assinatura do paciente",
                    emitidoEm,
                    termo.isAssinado(),
                    consultaId));
        }
    }

    // ── Agrupamento ────────────────────────────────────────

    private List<PacienteAtendidoResponse> agruparPorPaciente(List<Consulta> consultas,
                                                              Map<Long, List<DocumentoRelatorioResponse>> documentosPorConsulta,
                                                              Map<Long, String> diagnosticoPorConsulta) {
        Map<Long, List<ConsultaRelatorioResponse>> consultasPorPaciente = new LinkedHashMap<>();
        Map<Long, Paciente> pacientes = new LinkedHashMap<>();
        Map<Long, Consulta> consultaMaisRecente = new LinkedHashMap<>();

        for (Consulta consulta : consultas) {
            Paciente paciente = consulta.getPaciente();
            if (paciente == null) {
                continue;
            }

            Long pacienteId = paciente.getPaciCodigo();
            pacientes.putIfAbsent(pacienteId, paciente);
            consultaMaisRecente.putIfAbsent(pacienteId, consulta);

            consultasPorPaciente
                    .computeIfAbsent(pacienteId, chave -> new ArrayList<>())
                    .add(montarConsulta(consulta, documentosPorConsulta, diagnosticoPorConsulta));
        }

        List<PacienteAtendidoResponse> resposta = new ArrayList<>();

        pacientes.forEach((pacienteId, paciente) -> {
            List<ConsultaRelatorioResponse> consultasDoPaciente = consultasPorPaciente.get(pacienteId);
            Consulta recente = consultaMaisRecente.get(pacienteId);
            Profissional profissional = recente.getProfissional();

            int totalDocumentos = consultasDoPaciente.stream()
                    .mapToInt(c -> c.documentos().size())
                    .sum();

            resposta.add(new PacienteAtendidoResponse(
                    pacienteId,
                    paciente.getPaciNome(),
                    paciente.getPaciCpf(),
                    paciente.getPaciTelefone(),
                    paciente.getPaciEmail(),
                    paciente.getPaciDataNacimento() != null ? paciente.getPaciDataNacimento().toLocalDate() : null,
                    paciente.getPaciSexo(),
                    profissional != null ? profissional.getId() : null,
                    profissional != null ? profissional.getNome() : null,
                    recente.getDataHora(),
                    consultasDoPaciente.size(),
                    totalDocumentos,
                    consultasDoPaciente));
        });

        resposta.sort(Comparator.comparing(PacienteAtendidoResponse::ultimoAtendimento).reversed());
        return resposta;
    }

    private ConsultaRelatorioResponse montarConsulta(Consulta consulta,
                                                     Map<Long, List<DocumentoRelatorioResponse>> documentosPorConsulta,
                                                     Map<Long, String> diagnosticoPorConsulta) {
        List<DocumentoRelatorioResponse> documentos =
                new ArrayList<>(documentosPorConsulta.getOrDefault(consulta.getId(), List.of()));

        if (StatusConsulta.PAGO.equals(consulta.getStatus())) {
            documentos.add(new DocumentoRelatorioResponse(
                    consulta.getId(),
                    TipoDocumentoRelatorio.COMPROVANTE_PAGAMENTO,
                    "Comprovante de pagamento",
                    montarDescricaoPagamento(consulta),
                    consulta.getUpdatedAt() != null ? consulta.getUpdatedAt() : consulta.getDataHora(),
                    false,
                    consulta.getId()));
        }

        return new ConsultaRelatorioResponse(
                consulta.getId(),
                consulta.getDataHora(),
                consulta.getStatus() != null ? consulta.getStatus().name() : null,
                consulta.getEspecialidade() != null ? consulta.getEspecialidade().getNome() : null,
                consulta.getProfissional() != null ? consulta.getProfissional().getId() : null,
                consulta.getProfissional() != null ? consulta.getProfissional().getNome() : null,
                diagnosticoPorConsulta.get(consulta.getId()),
                documentos);
    }

    // ── Utilitarios ────────────────────────────────────────

    private void adicionar(Map<Long, List<DocumentoRelatorioResponse>> documentos,
                           Long consultaId,
                           DocumentoRelatorioResponse documento) {
        documentos.computeIfAbsent(consultaId, chave -> new ArrayList<>()).add(documento);
    }

    private void registrarDiagnostico(Map<Long, String> diagnosticos, Long consultaId, String diagnostico) {
        if (preenchido(diagnostico)) {
            diagnosticos.putIfAbsent(consultaId, diagnostico.trim());
        }
    }

    private String montarDescricaoPagamento(Consulta consulta) {
        String forma = consulta.getFormaPagamento() != null ? consulta.getFormaPagamento().getNome() : "Nao informado";
        return consulta.getValor() != null ? forma + " - R$ " + consulta.getValor() : forma;
    }

    /**
     * Converte a data de finalizacao do prontuario medico, que e persistida como data simples.
     *
     * @param dataFinalizado data registrada no prontuario
     * @param fallback       data usada quando o prontuario nao possui data
     * @return data e hora de emissao do documento
     */
    private LocalDateTime converterParaDataHora(java.sql.Date dataFinalizado, LocalDateTime fallback) {
        return dataFinalizado != null ? dataFinalizado.toLocalDate().atStartOfDay() : fallback;
    }

    private boolean preenchido(String valor) {
        return valor != null && !valor.isBlank();
    }

    /**
     * Reduz um texto longo para uso como descricao do card de documento.
     *
     * @param texto  conteudo original
     * @param padrao texto usado quando o conteudo esta vazio
     * @return descricao com no maximo 120 caracteres
     */
    private String resumir(String texto, String padrao) {
        if (!preenchido(texto)) {
            return padrao;
        }
        String limpo = texto.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
        return limpo.length() <= 120 ? limpo : limpo.substring(0, 117) + "...";
    }
}
