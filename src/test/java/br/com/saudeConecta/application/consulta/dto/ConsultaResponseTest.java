package br.com.saudeConecta.application.consulta.dto;

import br.com.saudeConecta.domain.consulta.CanceladoPor;
import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.consulta.StatusConsulta;
import br.com.saudeConecta.domain.pagamento.FormaPagamento;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.domain.profissional.Especialidade;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.profissional.TipoProfissional;
import br.com.saudeConecta.domain.usuario.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ConsultaResponse.fromEntity - mapeamento com relações opcionais")
class ConsultaResponseTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2025, 3, 10, 9, 0);

    @Test
    @DisplayName("Consulta sem relações e sem status preenche só os campos escalares")
    void semRelacoes() {
        Consulta c = Consulta.builder().id(5L).dataHora(INICIO).duracaoMinutos(45).observacoes("obs")
                .valor(new BigDecimal("10.50")).build();

        ConsultaResponse r = ConsultaResponse.fromEntity(c);

        assertThat(r.id()).isEqualTo(5L);
        assertThat(r.profissionalId()).isNull();
        assertThat(r.profissionalNome()).isNull();
        assertThat(r.profissionalConselho()).isNull();
        assertThat(r.profissionalUsuarioId()).isNull();
        assertThat(r.tipoProfissionalId()).isNull();
        assertThat(r.tipoProfissionalNome()).isNull();
        assertThat(r.pacienteId()).isNull();
        assertThat(r.pacienteNome()).isNull();
        assertThat(r.pacienteTelefone()).isNull();
        assertThat(r.especialidadeId()).isNull();
        assertThat(r.especialidadeNome()).isNull();
        assertThat(r.formaPagamentoId()).isNull();
        assertThat(r.formaPagamentoNome()).isNull();
        assertThat(r.status()).isNull();
        assertThat(r.canceladoPor()).isNull();
        assertThat(r.dataHora()).isEqualTo(INICIO);
        assertThat(r.duracaoMinutos()).isEqualTo(45);
        assertThat(r.observacoes()).isEqualTo("obs");
        assertThat(r.valor()).isEqualByComparingTo("10.50");
    }

    @Test
    @DisplayName("Profissional sem usuário nem tipo mantém só id, nome e conselho")
    void profissionalSemUsuarioNemTipo() {
        Profissional p = Profissional.builder().id(7L).nome("Dra. Ana").registroConselho("CRM-1").build();
        Consulta c = Consulta.builder().id(1L).dataHora(INICIO).profissional(p).build();

        ConsultaResponse r = ConsultaResponse.fromEntity(c);

        assertThat(r.profissionalId()).isEqualTo(7L);
        assertThat(r.profissionalNome()).isEqualTo("Dra. Ana");
        assertThat(r.profissionalConselho()).isEqualTo("CRM-1");
        assertThat(r.profissionalUsuarioId()).isNull();
        assertThat(r.tipoProfissionalId()).isNull();
        assertThat(r.tipoProfissionalNome()).isNull();
    }

    @Test
    @DisplayName("Todas as relações preenchidas são mapeadas")
    void todasRelacoes() {
        Usuario u = Usuario.builder().login("x").senha("y").build();
        u.setId(99L);
        TipoProfissional tipo = TipoProfissional.builder().id(2L).codigo("DENTISTA").build();
        Profissional p = Profissional.builder().id(7L).nome("Dr. Bia").registroConselho("CRO-9")
                .usuario(u).tipoProfissional(tipo).build();
        Paciente pac = Paciente.builder().paciCodigo(3L).paciNome("Carlos").paciTelefone("1199").build();
        Especialidade esp = Especialidade.builder().id(4L).nome("Ortodontia").build();
        FormaPagamento fp = FormaPagamento.builder().id(6L).nome("PIX").build();
        Consulta c = Consulta.builder().id(1L).dataHora(INICIO).profissional(p).paciente(pac).especialidade(esp).formaPagamento(fp)
                .status(StatusConsulta.CANCELADA).canceladoPor(CanceladoPor.PACIENTE)
                .motivoCancelamento("mudou").createdAt(INICIO).build();

        ConsultaResponse r = ConsultaResponse.fromEntity(c);

        assertThat(r.profissionalUsuarioId()).isEqualTo(99L);
        assertThat(r.tipoProfissionalId()).isEqualTo(2L);
        assertThat(r.tipoProfissionalNome()).isEqualTo("DENTISTA");
        assertThat(r.pacienteId()).isEqualTo(3L);
        assertThat(r.pacienteNome()).isEqualTo("Carlos");
        assertThat(r.pacienteTelefone()).isEqualTo("1199");
        assertThat(r.especialidadeId()).isEqualTo(4L);
        assertThat(r.especialidadeNome()).isEqualTo("Ortodontia");
        assertThat(r.formaPagamentoId()).isEqualTo(6L);
        assertThat(r.formaPagamentoNome()).isEqualTo("PIX");
        assertThat(r.status()).isEqualTo("CANCELADA");
        assertThat(r.canceladoPor()).isEqualTo("PACIENTE");
        assertThat(r.motivoCancelamento()).isEqualTo("mudou");
        assertThat(r.createdAt()).isEqualTo(INICIO);
    }
}
