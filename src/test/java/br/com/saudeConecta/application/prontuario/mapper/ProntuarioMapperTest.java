package br.com.saudeConecta.application.prontuario.mapper;

import br.com.saudeConecta.application.prontuario.dto.CadastrarProntuarioRequest;
import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.prontuario.Prontuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProntuarioMapper - pedido de cadastro para entidade")
class ProntuarioMapperTest {

    private static CadastrarProntuarioRequest pedidoCompleto() {
        CadastrarProntuarioRequest r = new CadastrarProntuarioRequest();
        r.setQueixaPrincipal("Dor de cabeça");
        r.setAnamnese("Há 3 dias");
        r.setObservacao("Sem febre");
        r.setPressao("12x8");
        r.setFrequenciaRespiratoria("16");
        r.setFrequenciaArterialSistolica("120");
        r.setFrequenciaArterialDiastolica("80");
        r.setPulso("72");
        r.setAltura("1,70");
        r.setTemperatura("36,5");
        r.setPeso("70");
        r.setSaturacao("98");
        r.setHemoglobina("14");
        r.setDiagnostico("Cefaleia tensional");
        r.setOrientacoes("Hidratação");
        r.setTituloPrescricao("Receita");
        r.setPrescricao("Dipirona 500mg");
        r.setDataFinalizado(Date.valueOf(LocalDate.of(2026, 11, 6)));
        r.setTempoDuracao("00:25:00");
        r.setResponsavel("Mãe");
        r.setTussTexto("10101012");
        r.setCidTexto("G44.2");
        r.setSolicitacaoExameTexto("Hemograma");
        return r;
    }

    @Test
    @DisplayName("copia todos os campos do atendimento e liga profissional e consulta")
    void copiaTodosOsCampos() {
        Profissional profissional = new Profissional();
        Consulta consulta = new Consulta();

        Prontuario p = ProntuarioMapper.paraEntidade(pedidoCompleto(), profissional, consulta);

        assertThat(p.getProntQueixaPricipal()).isEqualTo("Dor de cabeça");
        assertThat(p.getProntAnamnese()).isEqualTo("Há 3 dias");
        assertThat(p.getProntObservacao()).isEqualTo("Sem febre");
        assertThat(p.getProntPressao()).isEqualTo("12x8");
        assertThat(p.getProntFrequenciaRespiratoria()).isEqualTo("16");
        assertThat(p.getProntFrequenciaArterialSistolica()).isEqualTo("120");
        assertThat(p.getProntFrequenciaArterialDiastolica()).isEqualTo("80");
        assertThat(p.getProntPulso()).isEqualTo("72");
        assertThat(p.getProntAltura()).isEqualTo("1,70");
        assertThat(p.getProntTemperatura()).isEqualTo("36,5");
        assertThat(p.getProntPeso()).isEqualTo("70");
        assertThat(p.getProntSaturacao()).isEqualTo("98");
        assertThat(p.getProntHemoglobina()).isEqualTo("14");
        assertThat(p.getProntDiagnostico()).isEqualTo("Cefaleia tensional");
        assertThat(p.getProntOrientacoes()).isEqualTo("Hidratação");
        assertThat(p.getProntTituloPrescricao()).isEqualTo("Receita");
        assertThat(p.getProntPrescricao()).isEqualTo("Dipirona 500mg");
        assertThat(p.getProntDataFinalizado()).isEqualTo(Date.valueOf(LocalDate.of(2026, 11, 6)));
        assertThat(p.getProntTempoDuracao()).isEqualTo("00:25:00");
        assertThat(p.getProntResponsavel()).isEqualTo("Mãe");
        assertThat(p.getProntTussTexto()).isEqualTo("10101012");
        assertThat(p.getProntCidTexto()).isEqualTo("G44.2");
        assertThat(p.getProntSolicitacaoExameTexto()).isEqualTo("Hemograma");
        assertThat(p.getProfissional()).isSameAs(profissional);
        assertThat(p.getConsulta()).isSameAs(consulta);
    }

    @Test
    @DisplayName("entidade nova: sem id e com a lista de planejamentos vazia (o service adiciona depois)")
    void entidadeNovaSemPlanejamentos() {
        CadastrarProntuarioRequest pedido = pedidoCompleto();
        pedido.setPlanejamentos(java.util.List.of(new CadastrarProntuarioRequest.PlanejamentoItem()));

        Prontuario p = ProntuarioMapper.paraEntidade(pedido, new Profissional(), new Consulta());

        assertThat(p.getProntCodigoProntuario()).isNull();
        assertThat(p.getPlanejamentos()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("campos opcionais ausentes ficam nulos, sem valor inventado")
    void opcionaisNulos() {
        Prontuario p = ProntuarioMapper.paraEntidade(new CadastrarProntuarioRequest(), null, null);

        assertThat(p.getProntDiagnostico()).isNull();
        assertThat(p.getProntDataFinalizado()).isNull();
        assertThat(p.getProfissional()).isNull();
        assertThat(p.getConsulta()).isNull();
    }
}
