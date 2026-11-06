package br.com.saudeConecta.integration;

import br.com.saudeConecta.application.cache.CacheEvictionService;
import br.com.saudeConecta.application.prontuario.PlanejamentoTerapeuticoService;
import br.com.saudeConecta.application.prontuario.ProntuarioDentistaService;
import br.com.saudeConecta.application.prontuario.ProntuarioService;
import br.com.saudeConecta.application.prontuario.dto.CadastrarProntuarioDentistaRequest;
import br.com.saudeConecta.application.prontuario.dto.CadastrarProntuarioRequest;
import br.com.saudeConecta.config.tenant.TenantContext;
import br.com.saudeConecta.config.tenant.TenantHelper;
import br.com.saudeConecta.domain.atendimento.AtendimentoRascunho;
import br.com.saudeConecta.domain.atendimento.TipoAtendimentoRascunho;
import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.consulta.StatusConsulta;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.profissional.TipoProfissional;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infrastructure.persistence.repository.AtendimentoRascunhoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ConsultaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;

/**
 * Finalizar o atendimento (prontuário médico ou odontológico) apaga o rascunho da consulta na mesma
 * transação. Roda com transações reais (o teste não abre transação própria) para provar o rollback:
 * se algo falha depois do DELETE, o rascunho continua.
 */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
@ActiveProfiles("test")
@Import({ProntuarioService.class, ProntuarioDentistaService.class, TenantHelper.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("Finalização do prontuário apaga o rascunho do atendimento")
class FinalizacaoApagaRascunhoIntegrationTest {

    private static final Long ORG = 1L;

    @Autowired private EntityManager em;
    @Autowired private PlatformTransactionManager txManager;
    @Autowired private ProntuarioService prontuarioService;
    @Autowired private ProntuarioDentistaService prontuarioDentistaService;
    @Autowired private AtendimentoRascunhoRepository rascunhoRepository;
    @Autowired private ConsultaRepository consultaRepository;

    @MockBean private PlanejamentoTerapeuticoService planejamentoService;
    @MockBean private CacheEvictionService cacheEvictionService;

    private TransactionTemplate tx;
    private Long profissionalId;
    private Long consultaId;

    @BeforeEach
    void prepararConsultaComRascunho() {
        tx = new TransactionTemplate(txManager);
        TenantContext.setCurrentTenant(ORG);
        tx.executeWithoutResult(s -> {
            em.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
            Profissional p = Profissional.builder()
                    .organizacao(em.getReference(Organizacao.class, ORG))
                    .tipoProfissional(em.getReference(TipoProfissional.class, 1L))
                    .usuario(em.getReference(Usuario.class, 50L))
                    .nome("Dra. Teste").registroConselho("CRM-1")
                    .build();
            em.persist(p);
            Consulta c = Consulta.builder()
                    .organizacao(em.getReference(Organizacao.class, ORG))
                    .profissional(p)
                    .paciente(em.getReference(Paciente.class, 1L))
                    .dataHora(LocalDateTime.now())
                    .status(StatusConsulta.AGENDADA)
                    .build();
            em.persist(c);
            em.persist(AtendimentoRascunho.builder()
                    .consulta(c).organizacao(c.getOrganizacao()).profissional(p)
                    .tipo(TipoAtendimentoRascunho.MEDICO).dados("{\"exame\":{}}").tempoAcumuladoSegundos(10)
                    .build());
            profissionalId = p.getId();
            consultaId = c.getId();
        });
    }

    @AfterEach
    void limpar() {
        TenantContext.clear();
        tx.executeWithoutResult(s -> {
            em.createQuery("DELETE FROM PlanejamentoTerapeutico").executeUpdate();
            em.createQuery("DELETE FROM ProntuarioDentistaDente").executeUpdate();
            em.createQuery("DELETE FROM ProntuarioDentista").executeUpdate();
            em.createQuery("DELETE FROM Prontuario").executeUpdate();
            em.createQuery("DELETE FROM AtendimentoRascunho").executeUpdate();
            em.createQuery("DELETE FROM Consulta").executeUpdate();
            em.createQuery("DELETE FROM Profissional").executeUpdate();
        });
    }

    private boolean temRascunho() {
        return rascunhoRepository.findByConsulta_IdAndOrganizacao_Id(consultaId, ORG).isPresent();
    }

    private StatusConsulta statusDaConsulta() {
        return consultaRepository.findById(consultaId).orElseThrow().getStatus();
    }

    private CadastrarProntuarioRequest requestMedico() {
        CadastrarProntuarioRequest r = new CadastrarProntuarioRequest();
        r.setConsulta(consultaId);
        r.setCodigoMedico(profissionalId);
        r.setDataFinalizado(Date.valueOf(LocalDate.now()));
        r.setPlanejamentos(Collections.emptyList());
        return r;
    }

    private CadastrarProntuarioDentistaRequest requestDentista() {
        CadastrarProntuarioDentistaRequest r = new CadastrarProntuarioDentistaRequest();
        r.setConsulta(consultaId);
        r.setCodigoMedico(profissionalId);
        r.setOdontograma(Collections.emptyList());
        r.setPlanejamentos(Collections.emptyList());
        return r;
    }

    @Test
    @DisplayName("Prontuário médico gravado: rascunho apagado e consulta REALIZADA")
    void medicoApagaRascunho() {
        assertThat(temRascunho()).isTrue();

        prontuarioService.cadastrarProntuarioMedico(requestMedico());

        assertThat(temRascunho()).isFalse();
        assertThat(statusDaConsulta()).isEqualTo(StatusConsulta.REALIZADA);
    }

    @Test
    @DisplayName("Prontuário médico falha depois do DELETE: rollback mantém o rascunho e a consulta aberta")
    void medicoFalhaMantemRascunho() {
        doThrow(new IllegalStateException("falha simulada")).when(cacheEvictionService)
                .evictProntuarioRecente(anyLong(), anyLong());

        assertThatThrownBy(() -> prontuarioService.cadastrarProntuarioMedico(requestMedico()))
                .hasMessage("falha simulada");

        assertThat(temRascunho()).isTrue();
        assertThat(statusDaConsulta()).isEqualTo(StatusConsulta.AGENDADA);
    }

    @Test
    @DisplayName("Prontuário odontológico gravado: rascunho apagado e consulta REALIZADA")
    void dentistaApagaRascunho() {
        prontuarioDentistaService.cadastrarProntuarioByOrg(requestDentista());

        assertThat(temRascunho()).isFalse();
        assertThat(statusDaConsulta()).isEqualTo(StatusConsulta.REALIZADA);
    }

    @Test
    @DisplayName("Prontuário odontológico em transação que falha: o DELETE participa dela e volta junto")
    void dentistaFalhaMantemRascunho() {
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> {
            prontuarioDentistaService.cadastrarProntuarioByOrg(requestDentista());
            throw new IllegalStateException("falha depois da gravacao");
        })).hasMessage("falha depois da gravacao");

        assertThat(temRascunho()).isTrue();
        assertThat(statusDaConsulta()).isEqualTo(StatusConsulta.AGENDADA);
    }
}
