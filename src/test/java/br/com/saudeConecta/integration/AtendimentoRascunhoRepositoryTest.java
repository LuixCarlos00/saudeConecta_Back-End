package br.com.saudeConecta.integration;

import br.com.saudeConecta.domain.atendimento.AtendimentoRascunho;
import br.com.saudeConecta.domain.atendimento.RascunhoAtendimentoResumo;
import br.com.saudeConecta.domain.atendimento.TipoAtendimentoRascunho;
import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.profissional.TipoProfissional;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infrastructure.persistence.repository.AtendimentoRascunhoRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Rascunho do atendimento contra um banco real (H2): UNIQUE por consulta, filtro por clínica e por
 * usuário do profissional, e o DELETE por consulta. Organização, tipo, usuário e paciente entram só
 * como chave estrangeira (integridade referencial desligada), como no {@link IsolamentoTenantRepositoryTest}.
 */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
@ActiveProfiles("test")
@DisplayName("AtendimentoRascunhoRepository - UNIQUE por consulta e isolamento")
class AtendimentoRascunhoRepositoryTest {

    private static final Long ORG_A = 1L;
    private static final Long ORG_B = 2L;
    private static final Long USUARIO_DONO = 50L;
    private static final Long OUTRO_USUARIO = 51L;

    @Autowired private EntityManager em;
    @Autowired private AtendimentoRascunhoRepository repository;

    @BeforeEach
    void desligarIntegridadeReferencial() {
        em.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
    }

    private Profissional profissional(Long orgId, Long usuarioId) {
        Profissional p = Profissional.builder()
                .organizacao(em.getReference(Organizacao.class, orgId))
                .tipoProfissional(em.getReference(TipoProfissional.class, 1L))
                .usuario(em.getReference(Usuario.class, usuarioId))
                .nome("Dra. Teste")
                .registroConselho("CRM-1")
                .build();
        em.persist(p);
        return p;
    }

    private Consulta consulta(Long orgId, Profissional profissional) {
        Consulta c = Consulta.builder()
                .organizacao(em.getReference(Organizacao.class, orgId))
                .profissional(profissional)
                .paciente(em.getReference(Paciente.class, 1L))
                .dataHora(LocalDateTime.now())
                .build();
        em.persist(c);
        return c;
    }

    private AtendimentoRascunho rascunho(Consulta consulta, int tempo) {
        return AtendimentoRascunho.builder()
                .consulta(consulta)
                .organizacao(consulta.getOrganizacao())
                .profissional(consulta.getProfissional())
                .tipo(TipoAtendimentoRascunho.MEDICO)
                .dados("{\"exame\":{}}")
                .tempoAcumuladoSegundos(tempo)
                .build();
    }

    @Test
    @DisplayName("Segundo rascunho da mesma consulta viola o UNIQUE de consulta_id")
    void uniquePorConsulta() {
        Consulta c = consulta(ORG_A, profissional(ORG_A, USUARIO_DONO));
        repository.saveAndFlush(rascunho(c, 1));

        var segundo = rascunho(c, 2);
        assertThatThrownBy(() -> repository.saveAndFlush(segundo))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Primeira gravação começa na versão 0 e cada alteração incrementa")
    void versaoIncrementa() {
        Consulta c = consulta(ORG_A, profissional(ORG_A, USUARIO_DONO));
        AtendimentoRascunho salvo = repository.saveAndFlush(rascunho(c, 1));
        assertThat(salvo.getVersao()).isZero();
        assertThat(salvo.getCriadoEm()).isNotNull().isEqualTo(salvo.getAtualizadoEm());

        salvo.setTempoAcumuladoSegundos(30);
        assertThat(repository.saveAndFlush(salvo).getVersao()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Busca por consulta respeita a clínica")
    void buscaPorConsultaFiltraClinica() {
        Consulta c = consulta(ORG_B, profissional(ORG_B, USUARIO_DONO));
        repository.saveAndFlush(rascunho(c, 1));
        em.clear();

        assertThat(repository.findByConsulta_IdAndOrganizacao_Id(c.getId(), ORG_A)).isEmpty();
        assertThat(repository.findByConsulta_IdAndOrganizacao_Id(c.getId(), ORG_B)).isPresent();
    }

    @Test
    @DisplayName("'meus' traz só os rascunhos do usuário logado na clínica do token, sem dados")
    void listarDoUsuarioFiltraProfissionalEClinica() {
        Consulta minha = consulta(ORG_A, profissional(ORG_A, USUARIO_DONO));
        Consulta deOutro = consulta(ORG_A, profissional(ORG_A, OUTRO_USUARIO));
        Consulta minhaOutraClinica = consulta(ORG_B, profissional(ORG_B, USUARIO_DONO));
        repository.saveAndFlush(rascunho(minha, 754));
        repository.saveAndFlush(rascunho(deOutro, 1));
        repository.saveAndFlush(rascunho(minhaOutraClinica, 2));
        em.clear();

        List<RascunhoAtendimentoResumo> meus = repository.listarDoUsuario(ORG_A, USUARIO_DONO);

        assertThat(meus).hasSize(1);
        assertThat(meus.get(0).getConsultaId()).isEqualTo(minha.getId());
        assertThat(meus.get(0).getTempoAcumuladoSegundos()).isEqualTo(754);
        assertThat(meus.get(0).getAtualizadoEm()).isNotNull();
        assertThat(repository.listarDoUsuario(ORG_A, 999L)).isEmpty();
    }

    @Test
    @DisplayName("Apagar por consulta remove só o rascunho dela e é idempotente")
    void apagarPorConsulta() {
        Profissional p = profissional(ORG_A, USUARIO_DONO);
        Consulta c1 = consulta(ORG_A, p);
        Consulta c2 = consulta(ORG_A, p);
        repository.saveAndFlush(rascunho(c1, 1));
        repository.saveAndFlush(rascunho(c2, 1));

        assertThat(repository.apagarPorConsulta(c1.getId())).isEqualTo(1);
        assertThat(repository.apagarPorConsulta(c1.getId())).isZero();
        em.clear();
        assertThat(repository.findByConsulta_IdAndOrganizacao_Id(c2.getId(), ORG_A)).isPresent();
    }
}
