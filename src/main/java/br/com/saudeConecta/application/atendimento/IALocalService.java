package br.com.saudeConecta.application.atendimento;

import br.com.saudeConecta.domain.FusoHorario;
import br.com.saudeConecta.application.historico.BuscarHistoricoCompletoPacienteDentistaUseCase;
import br.com.saudeConecta.application.historico.BuscarHistoricoCompletoPacienteMedicoUseCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Serviço de IA local para resumo de históricos clínicos
 * Não requer API keys externas - usa algoritmos de processamento de linguagem local
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IALocalService {

    private final BuscarHistoricoCompletoPacienteMedicoUseCase historicoMedicoUseCase;
    private final BuscarHistoricoCompletoPacienteDentistaUseCase historicoDentistaUseCase;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String FORMATO_DATA = "dd/MM/yyyy";
    private static final String FORMATO_ITEM_LISTA = "• %s%n";
    private static final String CAMPO_DATA_FINALIZADO = "dataFinalizado";
    private static final String CAMPO_QUEIXA_PRINCIPAL = "queixaPrincipal";
    private static final String CAMPO_DIAGNOSTICO = "diagnostico";
    private static final String CAMPO_PRESCRICAO = "prescricao";
    private static final String CAMPO_ANAMNESE = "anamnese";

    // Palavras-chave médicas para análise
    private static final Set<String> PALAVRAS_CRONICAS = Set.of(
        "hipertensão", "diabetes", "asma", "artrite", "depressão", "ansiedade",
        "colesterol", "triglicerídeos", "osteoporose", "enxaqueca", "insônia",
        "gastrite", "refluxo", "rinite", "sinusite", "alergia", "fibromialgia"
    );

    private static final Set<String> SINTOMAS_URGENTES = Set.of(
        "dor forte", "febre alta", "dificuldade respirar", "pressão alta",
        "tontura", "desmaio", "sangramento", "vômito persistente"
    );

    private static final Set<String> MEDICAMENTOS_COMUNS = Set.of(
        "ibuprofeno", "paracetamol", "dipirona", "amoxicilina", "losartana",
        "metformina", "omeprazol", "atorvastatina", "salbutamol", "diazepam"
    );

    /**
     * Gera resumo do histórico usando IA local (sem API externa)
     */
    public String gerarResumoHistorico(Long pacienteId, String tipo) {
        try {
            // 1. Buscar histórico completo
            List<?> historico = buscarHistoricoPorTipo(pacienteId, tipo);
            
            if (historico.isEmpty()) {
                return "📋 **Resumo Clínico**\n\nNão foram encontradas consultas anteriores para este paciente.";
            }

            // 2. Analisar dados com algoritmos locais
            AnaliseHistorico analise = analisarHistoricoLocal(historico);
            
            // 3. Gerar resumo estruturado
            return gerarResumoEstruturado(analise, historico.size(), tipo);
            
        } catch (Exception e) {
            log.error("ia.resumo falha", e);
            return "📋 **Resumo Clínico**\n\nErro ao analisar o histórico. Tente novamente.";
        }
    }

    /**
     * Busca histórico conforme o tipo
     */
    private List<?> buscarHistoricoPorTipo(Long pacienteId, String tipo) {
        if ("dentista".equalsIgnoreCase(tipo)) {
            return historicoDentistaUseCase.executar(pacienteId, null);
        } else {
            return historicoMedicoUseCase.executar(pacienteId, null);
        }
    }

    /**
     * Analisa o histórico usando algoritmos locais de NLP
     */
    private AnaliseHistorico analisarHistoricoLocal(List<?> historico) {
        AnaliseHistorico analise = new AnaliseHistorico();
        analise.sintomasUrgentes = new ArrayList<>();
        analise.medicamentosUsados = new ArrayList<>();
        analise.frequenciaSintomas = new HashMap<>();
        analise.frequenciaDiagnosticos = new HashMap<>();
        analise.ultimaQueixa = "";
        analise.ultimoDiagnostico = "";
        Set<String> condicoesCronicas = new HashSet<>();

        for (Object consulta : historico) {
            JsonNode node = objectMapper.valueToTree(consulta);
            analisarPeriodo(node, analise);
            analisarQueixaPrincipal(node, analise, condicoesCronicas);
            analisarDiagnostico(node, analise);
            analisarPrescricao(node, analise);
            analisarAnamnese(node, condicoesCronicas);
        }

        // Calcular período total
        long totalDias = 0;
        if (analise.primeiraConsulta != null && analise.ultimaConsulta != null) {
            totalDias = java.time.temporal.ChronoUnit.DAYS.between(analise.primeiraConsulta, analise.ultimaConsulta);
        }

        // Preencher análise
        analise.totalConsultas = historico.size();
        analise.totalDiasAcompanhamento = totalDias;
        analise.condicoesCronicas = new ArrayList<>(condicoesCronicas);

        // Determinar padrão evolutivo
        analise.padraoEvolucao = determinarPadraoEvolucao(historico);

        return analise;
    }

    /** Campo presente no JSON da consulta, ou null quando ausente. */
    private String textoDoCampo(JsonNode node, String campo) {
        if (node.has(campo) && node.get(campo) != null) {
            return node.get(campo).asText();
        }
        return null;
    }

    /** Atualiza a primeira e a última data de consulta; data inválida é ignorada. */
    private void analisarPeriodo(JsonNode node, AnaliseHistorico analise) {
        String dataStr = textoDoCampo(node, CAMPO_DATA_FINALIZADO);
        if (dataStr == null) {
            return;
        }
        try {
            LocalDate data = LocalDate.parse(dataStr.split("T")[0]);
            if (analise.primeiraConsulta == null || data.isBefore(analise.primeiraConsulta)) {
                analise.primeiraConsulta = data;
            }
            if (analise.ultimaConsulta == null || data.isAfter(analise.ultimaConsulta)) {
                analise.ultimaConsulta = data;
            }
        } catch (Exception e) {
            log.debug("ia.resumo data invalida data={}", dataStr);
        }
    }

    /** Extrai condições crônicas e sintomas urgentes da queixa principal. */
    private void analisarQueixaPrincipal(JsonNode node, AnaliseHistorico analise, Set<String> condicoesCronicas) {
        String queixaOriginal = textoDoCampo(node, CAMPO_QUEIXA_PRINCIPAL);
        if (queixaOriginal == null) {
            return;
        }
        String queixa = queixaOriginal.toLowerCase();
        analise.ultimaQueixa = queixaOriginal;

        for (String palavra : PALAVRAS_CRONICAS) {
            if (queixa.contains(palavra)) {
                condicoesCronicas.add(palavra);
                analise.frequenciaSintomas.merge(palavra, 1, Integer::sum);
            }
        }

        for (String urgente : SINTOMAS_URGENTES) {
            if (queixa.contains(urgente)) {
                analise.sintomasUrgentes.add(urgente);
            }
        }
    }

    /** Conta os termos do diagnóstico (mais de 3 letras) e guarda o último diagnóstico. */
    private void analisarDiagnostico(JsonNode node, AnaliseHistorico analise) {
        String diagnosticoOriginal = textoDoCampo(node, CAMPO_DIAGNOSTICO);
        if (diagnosticoOriginal == null) {
            return;
        }
        analise.ultimoDiagnostico = diagnosticoOriginal;

        for (String termo : diagnosticoOriginal.toLowerCase().split("[\\s,.;]+")) {
            if (termo.length() > 3) {
                analise.frequenciaDiagnosticos.merge(termo, 1, Integer::sum);
            }
        }
    }

    /** Identifica medicamentos comuns citados na prescrição. */
    private void analisarPrescricao(JsonNode node, AnaliseHistorico analise) {
        String prescricao = textoDoCampo(node, CAMPO_PRESCRICAO);
        if (prescricao == null) {
            return;
        }
        String texto = prescricao.toLowerCase();
        for (String medicamento : MEDICAMENTOS_COMUNS) {
            if (texto.contains(medicamento)) {
                analise.medicamentosUsados.add(medicamento);
            }
        }
    }

    /** Busca condições crônicas adicionais citadas na anamnese. */
    private void analisarAnamnese(JsonNode node, Set<String> condicoesCronicas) {
        String anamnese = textoDoCampo(node, CAMPO_ANAMNESE);
        if (anamnese == null) {
            return;
        }
        String texto = anamnese.toLowerCase();
        for (String palavra : PALAVRAS_CRONICAS) {
            if (texto.contains(palavra)) {
                condicoesCronicas.add(palavra);
            }
        }
    }

    /**
     * Determina o padrão evolutivo do paciente
     */
    private String determinarPadraoEvolucao(List<?> historico) {
        if (historico.size() < 2) return "Dados insuficientes para análise";
        
        // Simples análise de frequência
        long consultasUltimos6Meses = historico.stream()
            .filter(consulta -> {
                JsonNode node = objectMapper.valueToTree(consulta);
                if (node.has(CAMPO_DATA_FINALIZADO) && node.get(CAMPO_DATA_FINALIZADO) != null) {
                    String dataStr = node.get(CAMPO_DATA_FINALIZADO).asText();
                    try {
                        LocalDate data = LocalDate.parse(dataStr.split("T")[0]);
                        return data.isAfter(LocalDate.now(FusoHorario.ZONA).minusMonths(6));
                    } catch (Exception e) {
                        return false;
                    }
                }
                return false;
            })
            .count();
            
        if (consultasUltimos6Meses > 3) {
            return "Alta frequência de consultas recentes - possível quadro agudo ou descompensação";
        } else if (consultasUltimos6Meses >= 1) {
            return "Frequência moderada de consultas - acompanhamento regular";
        } else {
            return "Baixa frequência de consultas recentes - paciente estável";
        }
    }

    /**
     * Gera resumo estruturado baseado na análise
     */
    private String gerarResumoEstruturado(AnaliseHistorico analise, int totalConsultas, String tipo) {
        StringBuilder resumo = new StringBuilder();

        resumo.append("🧠 **Resumo Clínico Inteligente**\n\n");

        adicionarVisaoGeral(resumo, analise, totalConsultas, tipo);
        adicionarCondicoesCronicas(resumo, analise);
        adicionarListaCapitalizada(resumo, "\n⚠️ **Alertas Clínicos**\n", analise.sintomasUrgentes);
        adicionarListaCapitalizada(resumo, "\n💊 **Medicamentos Identificados**\n", analise.medicamentosUsados);

        // Padrão evolutivo
        resumo.append("\n📈 **Padrão Evolutivo**\n");
        resumo.append(String.format(FORMATO_ITEM_LISTA, analise.padraoEvolucao));

        adicionarConsultaMaisRecente(resumo, analise);
        adicionarObservacoesClinicas(resumo, analise, totalConsultas);

        resumo.append("\n---\n");
        resumo.append("*Resumo gerado por análise inteligente local - ");
        resumo.append(LocalDate.now(FusoHorario.ZONA).format(DateTimeFormatter.ofPattern(FORMATO_DATA)));

        return resumo.toString();
    }

    private void adicionarVisaoGeral(StringBuilder resumo, AnaliseHistorico analise, int totalConsultas, String tipo) {
        resumo.append("📊 **Visão Geral**\n");
        resumo.append(String.format("• **Total de consultas**: %d %s%n", totalConsultas,
            tipo.equals("dentista") ? "odontológicas" : "médicas"));

        if (analise.primeiraConsulta == null || analise.ultimaConsulta == null) {
            return;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(FORMATO_DATA);
        resumo.append(String.format("• **Período de acompanhamento**: %s a %s%n",
            analise.primeiraConsulta.format(formatter),
            analise.ultimaConsulta.format(formatter)));

        if (analise.totalDiasAcompanhamento > 0) {
            long meses = analise.totalDiasAcompanhamento / 30;
            resumo.append(String.format("• **Tempo de acompanhamento**: %d meses%n", meses));
        }
    }

    private void adicionarCondicoesCronicas(StringBuilder resumo, AnaliseHistorico analise) {
        if (analise.condicoesCronicas.isEmpty()) {
            return;
        }
        resumo.append("\n🏥 **Condições Crônicas Identificadas**\n");
        for (String condicao : analise.condicoesCronicas) {
            int frequencia = analise.frequenciaSintomas.getOrDefault(condicao, 0);
            resumo.append(String.format("• **%s** (mencionado %d vezes)%n",
                capitalize(condicao), frequencia));
        }
    }

    /** Seção de itens capitalizados; omitida quando a lista está vazia. */
    private void adicionarListaCapitalizada(StringBuilder resumo, String titulo, List<String> itens) {
        if (itens.isEmpty()) {
            return;
        }
        resumo.append(titulo);
        for (String item : itens) {
            resumo.append(String.format(FORMATO_ITEM_LISTA, capitalize(item)));
        }
    }

    private void adicionarConsultaMaisRecente(StringBuilder resumo, AnaliseHistorico analise) {
        if (analise.ultimaQueixa.isEmpty()) {
            return;
        }
        resumo.append("\n🩺 **Consulta Mais Recente**\n");
        resumo.append(String.format("• **Queixa principal**: %s%n", analise.ultimaQueixa));
        if (!analise.ultimoDiagnostico.isEmpty()) {
            resumo.append(String.format("• **Diagnóstico**: %s%n", analise.ultimoDiagnostico));
        }
    }

    /** Recomendações baseadas na análise. */
    private void adicionarObservacoesClinicas(StringBuilder resumo, AnaliseHistorico analise, int totalConsultas) {
        resumo.append("\n💡 **Observações Clínicas**\n");

        if (analise.condicoesCronicas.size() > 2) {
            resumo.append("• **Paciente plurimórbido** - requer atenção integrada\n");
        }

        if (!analise.sintomasUrgentes.isEmpty()) {
            resumo.append("• **Atenção a sintomas agudos** - monitorar evolução\n");
        }

        if (analise.totalDiasAcompanhamento > 365 && totalConsultas < 3) {
            resumo.append("• **Seguimento irregular** - considerar reavaliação periódica\n");
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    /**
     * Classe interna para armazenar resultados da análise
     */
    private static class AnaliseHistorico {
        int totalConsultas;
        LocalDate primeiraConsulta;
        LocalDate ultimaConsulta;
        long totalDiasAcompanhamento;
        List<String> condicoesCronicas;
        List<String> sintomasUrgentes;
        List<String> medicamentosUsados;
        Map<String, Integer> frequenciaSintomas;
        Map<String, Integer> frequenciaDiagnosticos;
        String ultimaQueixa;
        String ultimoDiagnostico;
        String padraoEvolucao;
    }
}
