package br.com.saudeConecta.integration;

import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.consulta.StatusConsulta;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.infrastructure.persistence.repository.ConsultaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prova, contra H2, as queries da agenda (candidatas a conflito de horário) e do saldo financeiro.
 */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
@ActiveProfiles("test")
@DisplayName("ConsultaRepository - agenda e saldo")
class ConsultaAgendaSaldoRepositoryTest {

    private static final Long ORG = 1L;
    private static final Long OUTRA_ORG = 2L;
    private static final Long PROF = 1L;
    private static final LocalDateTime DIA = LocalDateTime.of(2030, 5, 10, 0, 0);

    @Autowired private EntityManager em;
    @Autowired private ConsultaRepository consultaRepository;

    @BeforeEach
    void desligarIntegridadeReferencial() {
        em.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
    }

    private Consulta consulta(Long org, Long prof, LocalDateTime dataHora, int duracao,
                              StatusConsulta status, BigDecimal valor) {
        Consulta c = Consulta.builder()
                .organizacao(em.getReference(Organizacao.class, org))
                .profissional(em.getReference(Profissional.class, prof))
                .paciente(em.getReference(Paciente.class, 1L))
                .dataHora(dataHora)
                .duracaoMinutos(duracao)
                .status(status)
                .valor(valor)
                .build();
        em.persist(c);
        return c;
    }

    @Test
    @DisplayName("Saldo soma consultas REALIZADA e PAGO, ignorando as demais")
    void saldoSomaRealizadaEPago() {
        consulta(ORG, PROF, DIA.withHour(9), 30, StatusConsulta.REALIZADA, new BigDecimal("100.00"));
        consulta(ORG, PROF, DIA.withHour(10), 30, StatusConsulta.PAGO, new BigDecimal("50.00"));
        consulta(ORG, PROF, DIA.withHour(11), 30, StatusConsulta.CANCELADA, new BigDecimal("999.00"));
        consulta(ORG, PROF, DIA.withHour(12), 30, StatusConsulta.AGENDADA, new BigDecimal("999.00"));
        em.flush();
        em.clear();

        List<Object[]> total = consultaRepository.somarValorConsultasRealizadasPorOrganizacao(
                ORG, DIA, DIA.plusDays(1));
        assertThat(((BigDecimal) total.get(0)[0])).isEqualByComparingTo("150.00");
        assertThat(((Number) total.get(0)[1]).longValue()).isEqualTo(2L);

        List<Object[]> porMes = consultaRepository.somarValorConsultasAgrupadasPorMes(
                ORG, DIA, DIA.plusDays(1));
        assertThat(((BigDecimal) porMes.get(0)[2])).isEqualByComparingTo("150.00");

        List<Object[]> porSemana = consultaRepository.somarValorConsultasAgrupadasPorSemana(
                ORG, DIA, DIA.plusDays(1));
        assertThat(((BigDecimal) porSemana.get(0)[2])).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("Candidatas a conflito: só AGENDADA e CONFIRMADA do profissional e da org, dentro da janela")
    void candidatasAConflito() {
        Consulta agendada = consulta(ORG, PROF, DIA.withHour(9), 60, StatusConsulta.AGENDADA, null);
        Consulta confirmada = consulta(ORG, PROF, DIA.withHour(10), 60, StatusConsulta.CONFIRMADA, null);
        consulta(ORG, PROF, DIA.withHour(9).withMinute(30), 30, StatusConsulta.CANCELADA, null);
        consulta(ORG, PROF, DIA.withHour(9).withMinute(45), 30, StatusConsulta.REALIZADA, null);
        consulta(ORG, 2L, DIA.withHour(9), 60, StatusConsulta.AGENDADA, null);
        consulta(OUTRA_ORG, PROF, DIA.withHour(9), 60, StatusConsulta.AGENDADA, null);
        consulta(ORG, PROF, DIA.plusDays(3).withHour(9), 60, StatusConsulta.AGENDADA, null);
        em.flush();
        em.clear();

        List<Consulta> candidatas = consultaRepository.findAtivasDoProfissionalNoPeriodo(
                ORG, PROF, DIA, DIA.plusDays(1));

        assertThat(candidatas).extracting(Consulta::getId)
                .containsExactlyInAnyOrder(agendada.getId(), confirmada.getId());
    }
}
