package br.com.saudeConecta.application.prontuario.dto;

import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.consulta.StatusConsulta;
import br.com.saudeConecta.domain.pagamento.FormaPagamento;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.prontuario.PlanejamentoTerapeutico;
import br.com.saudeConecta.domain.prontuario.Prontuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProntuarioCompletoResponse.fromEntity - mapeamento defensivo contra nulos")
class ProntuarioCompletoResponseTest {

    @Test
    @DisplayName("Prontuário nulo retorna nulo")
    void nulo() {
        assertThat(ProntuarioCompletoResponse.fromEntity(null)).isNull();
    }

    @Test
    @DisplayName("Prontuário sem relações: escalares copiados, relações e planejamentos nulos")
    void semRelacoes() {
        java.sql.Date finalizado = new java.sql.Date(1000L);
        Prontuario p = Prontuario.builder().prontCodigoProntuario(1L).prontPeso("70").prontAltura("170")
                .prontQueixaPricipal("dor").prontDiagnostico("dx").prontPrescricao("rx").prontDataFinalizado(finalizado)
                .prontResponsavel("mae").prontPulso("80").prontCidTexto("A00").planejamentos(null).build();

        ProntuarioCompletoResponse r = ProntuarioCompletoResponse.fromEntity(p);

        assertThat(r.getCodigoProntuario()).isEqualTo(1L);
        assertThat(r.getPeso()).isEqualTo("70");
        assertThat(r.getAltura()).isEqualTo("170");
        assertThat(r.getQueixaPrincipal()).isEqualTo("dor");
        assertThat(r.getDiagnostico()).isEqualTo("dx");
        assertThat(r.getPrescricao()).isEqualTo("rx");
        assertThat(r.getDataFinalizado()).isEqualTo(finalizado);
        assertThat(r.getResponsavel()).isEqualTo("mae");
        assertThat(r.getPulso()).isEqualTo("80");
        assertThat(r.getCidTexto()).isEqualTo("A00");
        assertThat(r.getProfissional()).isNull();
        assertThat(r.getConsulta()).isNull();
        assertThat(r.getPlanejamentos()).isNull();
    }

    @Test
    @DisplayName("Profissional e consulta sem paciente, sem datas e sem forma de pagamento")
    void consultaVazia() {
        Profissional prof = Profissional.builder().id(2L).nome("Dr. A").registroConselho("CRM").email("e@e.com")
                .telefone("11").build();
        Consulta consulta = Consulta.builder().id(3L).observacoes("obs").status(null).build();
        Prontuario p = Prontuario.builder().profissional(prof).consulta(consulta).planejamentos(List.of()).build();

        ProntuarioCompletoResponse r = ProntuarioCompletoResponse.fromEntity(p);

        assertThat(r.getProfissional().getId()).isEqualTo(2L);
        assertThat(r.getProfissional().getNome()).isEqualTo("Dr. A");
        assertThat(r.getProfissional().getConselho()).isEqualTo("CRM");
        assertThat(r.getProfissional().getEmail()).isEqualTo("e@e.com");
        assertThat(r.getProfissional().getTelefone()).isEqualTo("11");
        ProntuarioCompletoResponse.ConsultaResponse c = r.getConsulta();
        assertThat(c.getId()).isEqualTo(3L);
        assertThat(c.getObservacoes()).isEqualTo("obs");
        assertThat(c.getPacienteNome()).isNull();
        assertThat(c.getPacienteCpf()).isNull();
        assertThat(c.getDataHora()).isNull();
        assertThat(c.getStatus()).isNull();
        assertThat(c.getValor()).isNull();
        assertThat(c.getFormaPagamentoNome()).isNull();
        assertThat(c.getPaciente()).isNull();
        assertThat(r.getPlanejamentos()).isEmpty();
    }

    @Test
    @DisplayName("Consulta completa com paciente, forma de pagamento e planejamentos")
    void consultaCompleta() {
        java.sql.Date nasc = new java.sql.Date(0L);
        Paciente pac = Paciente.builder().paciCodigo(9L).paciNome("Zé").paciCpf("123").paciEmail("z@z.com")
                .paciTelefone("55").paciSexo("M").paciDataNacimento(nasc).build();
        LocalDateTime dh = LocalDateTime.of(2025, 1, 2, 3, 4);
        Consulta consulta = Consulta.builder().id(3L).paciente(pac).dataHora(dh).status(StatusConsulta.REALIZADA)
                .valor(new BigDecimal("12.5")).formaPagamento(FormaPagamento.builder().nome("PIX").build()).build();
        PlanejamentoTerapeutico completo = PlanejamentoTerapeutico.builder().id(1L)
                .dataProcedimento(LocalDate.of(2025, 5, 6)).procedimentoRealizado("limpeza")
                .valor(new BigDecimal("30.0")).statusAssinatura("ASSINADO").assinaturaBase64("b64")
                .dataAssinatura(dh).ipOrigem("1.1.1.1").build();
        PlanejamentoTerapeutico vazio = PlanejamentoTerapeutico.builder().id(2L).build();
        Prontuario p = Prontuario.builder().consulta(consulta).planejamentos(List.of(completo, vazio)).build();

        ProntuarioCompletoResponse r = ProntuarioCompletoResponse.fromEntity(p);

        ProntuarioCompletoResponse.ConsultaResponse c = r.getConsulta();
        assertThat(c.getPacienteNome()).isEqualTo("Zé");
        assertThat(c.getPacienteCpf()).isEqualTo("123");
        assertThat(c.getDataHora()).isEqualTo(dh.toString());
        assertThat(c.getStatus()).isEqualTo("REALIZADA");
        assertThat(c.getValor()).isEqualTo(12.5);
        assertThat(c.getFormaPagamentoNome()).isEqualTo("PIX");
        assertThat(c.getPaciente().getId()).isEqualTo(9L);
        assertThat(c.getPaciente().getNome()).isEqualTo("Zé");
        assertThat(c.getPaciente().getEmail()).isEqualTo("z@z.com");
        assertThat(c.getPaciente().getTelefone()).isEqualTo("55");
        assertThat(c.getPaciente().getSexo()).isEqualTo("M");
        assertThat(c.getPaciente().getDataNascimento()).isEqualTo(nasc.toString());

        assertThat(r.getPlanejamentos()).hasSize(2);
        ProntuarioCompletoResponse.PlanejamentoTerapeuticoResponse a = r.getPlanejamentos().get(0);
        assertThat(a.getId()).isEqualTo(1L);
        assertThat(a.getDataProcedimento()).isEqualTo("2025-05-06");
        assertThat(a.getProcedimentoRealizado()).isEqualTo("limpeza");
        assertThat(a.getValor()).isEqualTo(30.0);
        assertThat(a.getStatusAssinatura()).isEqualTo("ASSINADO");
        assertThat(a.getAssinaturaBase64()).isEqualTo("b64");
        assertThat(a.getDataAssinatura()).isEqualTo(dh.toString());
        assertThat(a.getIpOrigem()).isEqualTo("1.1.1.1");
        ProntuarioCompletoResponse.PlanejamentoTerapeuticoResponse b = r.getPlanejamentos().get(1);
        assertThat(b.getDataProcedimento()).isNull();
        assertThat(b.getValor()).isNull();
        assertThat(b.getDataAssinatura()).isNull();
        assertThat(b.getStatusAssinatura()).isEqualTo("PENDENTE");
    }
}
