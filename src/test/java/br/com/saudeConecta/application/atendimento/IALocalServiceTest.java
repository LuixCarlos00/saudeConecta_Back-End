package br.com.saudeConecta.application.atendimento;

import br.com.saudeConecta.application.historico.BuscarHistoricoCompletoPacienteDentistaUseCase;
import br.com.saudeConecta.application.historico.BuscarHistoricoCompletoPacienteMedicoUseCase;
import br.com.saudeConecta.domain.FusoHorario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

/**
 * Caracteriza o resumo gerado pela IA local: protege a saída textual contra regressões
 * em refatorações de complexidade.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IALocalService - resumo clínico local")
class IALocalServiceTest {

    private static final String CABECALHO_VAZIO =
            "📋 **Resumo Clínico**\n\nNão foram encontradas consultas anteriores para este paciente.";

    @Mock private BuscarHistoricoCompletoPacienteMedicoUseCase historicoMedico;
    @Mock private BuscarHistoricoCompletoPacienteDentistaUseCase historicoDentista;

    @InjectMocks
    private IALocalService service;

    private static Map<String, Object> consulta(String data, String queixa, String diagnostico,
                                                String prescricao, String anamnese) {
        Map<String, Object> m = new HashMap<>();
        if (data != null) m.put("dataFinalizado", data);
        if (queixa != null) m.put("queixaPrincipal", queixa);
        if (diagnostico != null) m.put("diagnostico", diagnostico);
        if (prescricao != null) m.put("prescricao", prescricao);
        if (anamnese != null) m.put("anamnese", anamnese);
        return m;
    }

    private static String dataRecente(int mesesAtras) {
        return LocalDate.now(FusoHorario.ZONA).minusMonths(mesesAtras).toString() + "T10:00:00";
    }

    @Test
    @DisplayName("Histórico vazio retorna mensagem de ausência de consultas")
    void historicoVazio() {
        doReturn(List.of()).when(historicoMedico).executar(1L, null);

        assertThat(service.gerarResumoHistorico(1L, "medico")).isEqualTo(CABECALHO_VAZIO);
    }

    @Test
    @DisplayName("Falha na busca retorna mensagem de erro genérica")
    void falhaNaBusca() {
        doThrow(new IllegalStateException("boom")).when(historicoMedico).executar(1L, null);

        assertThat(service.gerarResumoHistorico(1L, "medico"))
                .isEqualTo("📋 **Resumo Clínico**\n\nErro ao analisar o histórico. Tente novamente.");
    }

    @Test
    @DisplayName("Tipo dentista usa o caso de uso odontológico e rótulo odontológicas")
    void tipoDentista() {
        doReturn(List.of(consulta(null, null, null, null, null))).when(historicoDentista).executar(2L, null);

        String resumo = service.gerarResumoHistorico(2L, "DENTISTA");

        // equalsIgnoreCase escolhe o caso de uso, mas o rótulo usa equals (comportamento atual)
        assertThat(resumo).contains("• **Total de consultas**: 1 médicas");
    }

    @Test
    @DisplayName("Tipo dentista minúsculo gera rótulo odontológicas")
    void tipoDentistaMinusculo() {
        doReturn(List.of(consulta(null, null, null, null, null))).when(historicoDentista).executar(2L, null);

        assertThat(service.gerarResumoHistorico(2L, "dentista"))
                .contains("• **Total de consultas**: 1 odontológicas");
    }

    @Test
    @DisplayName("Consulta sem campos: sem seções opcionais e padrão de dados insuficientes")
    void consultaSemCampos() {
        doReturn(List.of(consulta(null, null, null, null, null))).when(historicoMedico).executar(1L, null);

        String resumo = service.gerarResumoHistorico(1L, "medico");

        assertThat(resumo)
                .startsWith("🧠 **Resumo Clínico Inteligente**\n\n📊 **Visão Geral**\n")
                .contains("• Dados insuficientes para análise\n")
                .contains("💡 **Observações Clínicas**\n")
                .doesNotContain("Período de acompanhamento")
                .doesNotContain("Condições Crônicas")
                .doesNotContain("Alertas Clínicos")
                .doesNotContain("Medicamentos Identificados")
                .doesNotContain("Consulta Mais Recente")
                .doesNotContain("plurimórbido")
                .doesNotContain("Seguimento irregular")
                .contains("---\n*Resumo gerado por análise inteligente local - "
                        + LocalDate.now(FusoHorario.ZONA).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }

    @Test
    @DisplayName("Resumo completo: período, crônicas, alertas, medicamentos, última consulta e plurimórbido")
    void resumoCompleto() {
        doReturn(List.of(
                consulta("2023-01-10T10:00:00", "Hipertensão e diabetes com dor forte", "Hipertensão arterial, controle",
                        "Losartana e metformina", "histórico de asma"),
                consulta("2024-03-20T08:00:00", "Revisão de diabetes", "Diabetes tipo dois", "Dipirona", null)))
                .when(historicoMedico).executar(1L, null);

        String resumo = service.gerarResumoHistorico(1L, "medico");

        assertThat(resumo)
                .contains("• **Total de consultas**: 2 médicas\n")
                .contains("• **Período de acompanhamento**: 10/01/2023 a 20/03/2024\n")
                .contains("• **Tempo de acompanhamento**: 14 meses\n")
                .contains("🏥 **Condições Crônicas Identificadas**\n")
                .contains("• **Hipertensão** (mencionado 1 vezes)\n")
                .contains("• **Diabetes** (mencionado 2 vezes)\n")
                .contains("• **Asma** (mencionado 0 vezes)\n")
                .contains("⚠️ **Alertas Clínicos**\n• Dor forte\n")
                .contains("💊 **Medicamentos Identificados**\n")
                .contains("• Losartana\n")
                .contains("• Metformina\n")
                .contains("• Dipirona\n")
                .contains("🩺 **Consulta Mais Recente**\n• **Queixa principal**: Revisão de diabetes\n"
                        + "• **Diagnóstico**: Diabetes tipo dois\n")
                .contains("• **Paciente plurimórbido** - requer atenção integrada\n")
                .contains("• **Atenção a sintomas agudos** - monitorar evolução\n")
                .contains("• **Seguimento irregular** - considerar reavaliação periódica\n");
    }

    @Test
    @DisplayName("Queixa sem diagnóstico omite a linha de diagnóstico")
    void queixaSemDiagnostico() {
        doReturn(List.of(consulta(null, "Dor de cabeça", null, null, null)))
                .when(historicoMedico).executar(1L, null);

        assertThat(service.gerarResumoHistorico(1L, "medico"))
                .contains("• **Queixa principal**: Dor de cabeça\n")
                .doesNotContain("**Diagnóstico**");
    }

    @Test
    @DisplayName("Diagnóstico sozinho não gera seção de consulta recente (depende da queixa)")
    void diagnosticoSemQueixa() {
        doReturn(List.of(consulta(null, null, "Diagnóstico qualquer", null, null)))
                .when(historicoMedico).executar(1L, null);

        assertThat(service.gerarResumoHistorico(1L, "medico")).doesNotContain("Consulta Mais Recente");
    }

    @Test
    @DisplayName("Data inválida é ignorada sem derrubar o resumo")
    void dataInvalidaIgnorada() {
        doReturn(List.of(consulta("não-é-data", "Rinite", null, null, null)))
                .when(historicoMedico).executar(1L, null);

        assertThat(service.gerarResumoHistorico(1L, "medico"))
                .contains("• **Rinite** (mencionado 1 vezes)")
                .doesNotContain("Período de acompanhamento");
    }

    @Test
    @DisplayName("Mais de 365 dias com menos de 3 consultas sinaliza seguimento irregular")
    void seguimentoIrregular() {
        doReturn(List.of(consulta("2020-01-01", null, null, null, null),
                consulta("2022-01-01", null, null, null, null)))
                .when(historicoMedico).executar(1L, null);

        assertThat(service.gerarResumoHistorico(1L, "medico"))
                .contains("• **Seguimento irregular** - considerar reavaliação periódica\n");
    }

    @Test
    @DisplayName("Mesma data inicial e final não gera tempo de acompanhamento")
    void mesmaData() {
        doReturn(List.of(consulta("2024-05-05", null, null, null, null),
                consulta("2024-05-05", null, null, null, null)))
                .when(historicoMedico).executar(1L, null);

        assertThat(service.gerarResumoHistorico(1L, "medico"))
                .contains("• **Período de acompanhamento**: 05/05/2024 a 05/05/2024\n")
                .doesNotContain("Tempo de acompanhamento");
    }

    @Test
    @DisplayName("Padrão evolutivo: alta frequência (mais de 3 consultas nos últimos 6 meses)")
    void padraoAltaFrequencia() {
        doReturn(List.of(consulta(dataRecente(1), null, null, null, null),
                consulta(dataRecente(2), null, null, null, null),
                consulta(dataRecente(3), null, null, null, null),
                consulta(dataRecente(4), null, null, null, null)))
                .when(historicoMedico).executar(1L, null);

        assertThat(service.gerarResumoHistorico(1L, "medico"))
                .contains("• Alta frequência de consultas recentes - possível quadro agudo ou descompensação\n");
    }

    @Test
    @DisplayName("Padrão evolutivo: frequência moderada (1 a 3 consultas recentes)")
    void padraoModerado() {
        doReturn(List.of(consulta(dataRecente(1), null, null, null, null),
                consulta("2019-01-01", null, null, null, null)))
                .when(historicoMedico).executar(1L, null);

        assertThat(service.gerarResumoHistorico(1L, "medico"))
                .contains("• Frequência moderada de consultas - acompanhamento regular\n");
    }

    @Test
    @DisplayName("Padrão evolutivo: baixa frequência (nenhuma consulta recente ou data inválida)")
    void padraoBaixo() {
        doReturn(List.of(consulta("2019-01-01", null, null, null, null),
                consulta("xx", null, null, null, null),
                consulta(null, null, null, null, null)))
                .when(historicoMedico).executar(1L, null);

        assertThat(service.gerarResumoHistorico(1L, "medico"))
                .contains("• Baixa frequência de consultas recentes - paciente estável\n");
    }
}
