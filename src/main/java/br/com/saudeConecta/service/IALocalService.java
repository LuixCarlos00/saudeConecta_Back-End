package br.com.saudeConecta.service;

import br.com.saudeConecta.usecase.BuscarHistoricoCompletoPacienteMedicoUseCase;
import br.com.saudeConecta.usecase.BuscarHistoricoCompletoPacienteUseCase;
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
    private final BuscarHistoricoCompletoPacienteUseCase historicoDentistaUseCase;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
            AnaliseHistorico analise = analisarHistoricoLocal(historico, tipo);
            
            // 3. Gerar resumo estruturado
            return gerarResumoEstruturado(analise, historico.size(), tipo);
            
        } catch (Exception e) {
            log.error("Erro ao gerar resumo local: {}", e.getMessage(), e);
            return "📋 **Resumo Clínico**\n\nErro ao analisar o histórico. Tente novamente.";
        }
    }

    /**
     * Busca histórico conforme o tipo
     */
    private List<?> buscarHistoricoPorTipo(Long pacienteId, String tipo) {
        if ("dentista".equalsIgnoreCase(tipo)) {
            return historicoDentistaUseCase.executar(pacienteId);
        } else {
            return historicoMedicoUseCase.executar(pacienteId);
        }
    }

    /**
     * Analisa o histórico usando algoritmos locais de NLP
     */
    private AnaliseHistorico analisarHistoricoLocal(List<?> historico, String tipo) {
        AnaliseHistorico analise = new AnaliseHistorico();
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        Map<String, Integer> frequenciaSintomas = new HashMap<>();
        Map<String, Integer> frequenciaDiagnosticos = new HashMap<>();
        Set<String> condicoesCronicas = new HashSet<>();
        List<String> sintomasUrgentes = new ArrayList<>();
        List<String> medicamentosUsados = new ArrayList<>();
        String ultimaQueixa = "";
        String ultimoDiagnostico = "";
        
        // Dados temporais
        LocalDate primeiraData = null;
        LocalDate ultimaData = null;
        long totalDias = 0;

        for (Object consulta : historico) {
            JsonNode node = objectMapper.valueToTree(consulta);
            
            // Análise temporal
            if (node.has("dataFinalizado") && node.get("dataFinalizado") != null) {
                String dataStr = node.get("dataFinalizado").asText();
                try {
                    LocalDate data = LocalDate.parse(dataStr.split("T")[0]);
                    if (primeiraData == null || data.isBefore(primeiraData)) {
                        primeiraData = data;
                    }
                    if (ultimaData == null || data.isAfter(ultimaData)) {
                        ultimaData = data;
                    }
                } catch (Exception e) {
                    log.debug("Erro ao parsear data: {}", dataStr);
                }
            }

            // Análise de queixa principal
            if (node.has("queixaPrincipal") && node.get("queixaPrincipal") != null) {
                String queixa = node.get("queixaPrincipal").asText().toLowerCase();
                ultimaQueixa = node.get("queixaPrincipal").asText();
                
                // Extrair sintomas
                for (String palavra : PALAVRAS_CRONICAS) {
                    if (queixa.contains(palavra)) {
                        condicoesCronicas.add(palavra);
                        frequenciaSintomas.merge(palavra, 1, Integer::sum);
                    }
                }
                
                // Verificar sintomas urgentes
                for (String urgente : SINTOMAS_URGENTES) {
                    if (queixa.contains(urgente)) {
                        sintomasUrgentes.add(urgente);
                    }
                }
            }

            // Análise de diagnóstico
            if (node.has("diagnostico") && node.get("diagnostico") != null) {
                String diagnostico = node.get("diagnostico").asText().toLowerCase();
                ultimoDiagnostico = node.get("diagnostico").asText();
                
                // Extrair termos do diagnóstico
                String[] termos = diagnostico.split("[\\s,.;]+");
                for (String termo : termos) {
                    if (termo.length() > 3) {
                        frequenciaDiagnosticos.merge(termo, 1, Integer::sum);
                    }
                }
            }

            // Análise de prescrição
            if (node.has("prescricao") && node.get("prescricao") != null) {
                String prescricao = node.get("prescricao").asText().toLowerCase();
                
                for (String medicamento : MEDICAMENTOS_COMUNS) {
                    if (prescricao.contains(medicamento)) {
                        medicamentosUsados.add(medicamento);
                    }
                }
            }

            // Análise de anamnese
            if (node.has("anamnese") && node.get("anamnese") != null) {
                String anamnese = node.get("anamnese").asText().toLowerCase();
                
                // Buscar padrões adicionais
                for (String palavra : PALAVRAS_CRONICAS) {
                    if (anamnese.contains(palavra)) {
                        condicoesCronicas.add(palavra);
                    }
                }
            }
        }

        // Calcular período total
        if (primeiraData != null && ultimaData != null) {
            totalDias = java.time.temporal.ChronoUnit.DAYS.between(primeiraData, ultimaData);
        }

        // Preencher análise
        analise.totalConsultas = historico.size();
        analise.primeiraConsulta = primeiraData;
        analise.ultimaConsulta = ultimaData;
        analise.totalDiasAcompanhamento = totalDias;
        analise.condicoesCronicas = new ArrayList<>(condicoesCronicas);
        analise.sintomasUrgentes = sintomasUrgentes;
        analise.medicamentosUsados = medicamentosUsados;
        analise.frequenciaSintomas = frequenciaSintomas;
        analise.frequenciaDiagnosticos = frequenciaDiagnosticos;
        analise.ultimaQueixa = ultimaQueixa;
        analise.ultimoDiagnostico = ultimoDiagnostico;
        
        // Determinar padrão evolutivo
        analise.padraoEvolucao = determinarPadraoEvolucao(historico);
        
        return analise;
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
                if (node.has("dataFinalizado") && node.get("dataFinalizado") != null) {
                    String dataStr = node.get("dataFinalizado").asText();
                    try {
                        LocalDate data = LocalDate.parse(dataStr.split("T")[0]);
                        return data.isAfter(LocalDate.now().minusMonths(6));
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
        
        // Visão geral
        resumo.append("📊 **Visão Geral**\n");
        resumo.append(String.format("• **Total de consultas**: %d %s\n", totalConsultas, 
            tipo.equals("dentista") ? "odontológicas" : "médicas"));
        
        if (analise.primeiraConsulta != null && analise.ultimaConsulta != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            resumo.append(String.format("• **Período de acompanhamento**: %s a %s\n", 
                analise.primeiraConsulta.format(formatter), 
                analise.ultimaConsulta.format(formatter)));
            
            if (analise.totalDiasAcompanhamento > 0) {
                long meses = analise.totalDiasAcompanhamento / 30;
                resumo.append(String.format("• **Tempo de acompanhamento**: %d meses\n", meses));
            }
        }
        
        // Condições crônicas
        if (!analise.condicoesCronicas.isEmpty()) {
            resumo.append("\n🏥 **Condições Crônicas Identificadas**\n");
            for (String condicao : analise.condicoesCronicas) {
                int frequencia = analise.frequenciaSintomas.getOrDefault(condicao, 0);
                resumo.append(String.format("• **%s** (mencionado %d vezes)\n", 
                    capitalize(condicao), frequencia));
            }
        }
        
        // Sintomas urgentes (alerta)
        if (!analise.sintomasUrgentes.isEmpty()) {
            resumo.append("\n⚠️ **Alertas Clínicos**\n");
            for (String urgente : analise.sintomasUrgentes) {
                resumo.append(String.format("• %s\n", capitalize(urgente)));
            }
        }
        
        // Medicamentos em uso
        if (!analise.medicamentosUsados.isEmpty()) {
            resumo.append("\n💊 **Medicamentos Identificados**\n");
            for (String medicamento : analise.medicamentosUsados) {
                resumo.append(String.format("• %s\n", capitalize(medicamento)));
            }
        }
        
        // Padrão evolutivo
        resumo.append("\n📈 **Padrão Evolutivo**\n");
        resumo.append(String.format("• %s\n", analise.padraoEvolucao));
        
        // Consulta mais recente
        if (!analise.ultimaQueixa.isEmpty()) {
            resumo.append("\n🩺 **Consulta Mais Recente**\n");
            resumo.append(String.format("• **Queixa principal**: %s\n", analise.ultimaQueixa));
            if (!analise.ultimoDiagnostico.isEmpty()) {
                resumo.append(String.format("• **Diagnóstico**: %s\n", analise.ultimoDiagnostico));
            }
        }
        
        // Recomendações baseadas na análise
        resumo.append("\n💡 **Observações Clínicas**\n");
        
        if (analise.condicoesCronicas.size() > 2) {
            resumo.append("• **Paciente plurimórbido** - requer atenção integrada\n");
        }
        
        if (analise.sintomasUrgentes.size() > 0) {
            resumo.append("• **Atenção a sintomas agudos** - monitorar evolução\n");
        }
        
        if (analise.totalDiasAcompanhamento > 365 && totalConsultas < 3) {
            resumo.append("• **Seguimento irregular** - considerar reavaliação periódica\n");
        }
        
        resumo.append("\n---\n");
        resumo.append("*Resumo gerado por análise inteligente local - ");
        resumo.append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        
        return resumo.toString();
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
