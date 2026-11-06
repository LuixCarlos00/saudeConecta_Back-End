package br.com.saudeConecta.integration;

import br.com.saudeConecta.application.consulta.dto.ConsultaResponse;
import br.com.saudeConecta.domain.common.Periodo;
import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.profissional.TipoProfissional;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infrastructure.persistence.repository.ConsultaRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code ConsultaResponse.profissionalUsuarioId} montado fora da sessão, como no controller
 * (open-in-view desligado): o id sai do proxy lazy do usuário, sem select a mais.
 */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
@ActiveProfiles("test")
@DisplayName("ConsultaResponse - profissionalUsuarioId sem LazyInitializationException")
class ConsultaResponseProfissionalUsuarioTest {

    private static final Long ORG = 1L;

    @Autowired private EntityManager em;
    @Autowired private ConsultaRepository consultaRepository;

    private Long usuarioDoDonoId;
    private Long consultaComUsuarioId;
    private Long consultaSemUsuarioId;

    @BeforeEach
    void montarDados() {
        em.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();

        Usuario dono = Usuario.builder().login("dono-" + System.nanoTime()).senha("x").build();
        em.persist(dono);
        usuarioDoDonoId = dono.getId();

        consultaComUsuarioId = consultaDe(profissional("Dra. Ana", dono)).getId();
        consultaSemUsuarioId = consultaDe(profissional("Dr. Sem Login", null)).getId();
        em.flush();
        em.clear();
    }

    private Profissional profissional(String nome, Usuario usuario) {
        Profissional p = Profissional.builder()
                .organizacao(em.getReference(Organizacao.class, ORG))
                .tipoProfissional(em.getReference(TipoProfissional.class, 1L))
                .nome(nome)
                .registroConselho("CRM-" + System.nanoTime() % 100000)
                .usuario(usuario)
                .build();
        em.persist(p);
        return p;
    }

    private Consulta consultaDe(Profissional profissional) {
        Consulta c = Consulta.builder()
                .organizacao(em.getReference(Organizacao.class, ORG))
                .profissional(profissional)
                .paciente(em.getReference(Paciente.class, 1L))
                .dataHora(LocalDate.now().atTime(10, 0))
                .build();
        em.persist(c);
        return c;
    }

    private ConsultaResponse daLista(List<Consulta> consultas, Long consultaId) {
        Consulta c = consultas.stream().filter(x -> x.getId().equals(consultaId)).findFirst().orElseThrow();
        assertThat(Hibernate.isInitialized(c.getProfissional())).isTrue();
        return ConsultaResponse.fromEntity(c);
    }

    @Test
    @DisplayName("GET /consultas/hoje: a query do período devolve o id do usuário do profissional, sem carregar o usuário")
    void consultasDoDia() {
        Periodo hoje = Periodo.dia(LocalDate.now());
        List<Consulta> consultas = consultaRepository.findByOrganizacaoIdAndPeriodo(ORG, hoje.inicio(), hoje.fim());
        em.clear(); // DTO montado fora da sessão, como no controller

        ConsultaResponse comUsuario = daLista(consultas, consultaComUsuarioId);
        ConsultaResponse semUsuario = daLista(consultas, consultaSemUsuarioId);

        assertThat(comUsuario.profissionalUsuarioId()).isEqualTo(usuarioDoDonoId);
        assertThat(comUsuario.profissionalNome()).isEqualTo("Dra. Ana");
        assertThat(semUsuario.profissionalUsuarioId()).isNull();
        // Só o id do proxy foi lido: nenhum select extra por linha (sem N+1)
        Consulta c = consultas.stream().filter(x -> x.getId().equals(consultaComUsuarioId)).findFirst().orElseThrow();
        assertThat(Hibernate.isInitialized(c.getProfissional().getUsuario())).isFalse();
    }

    @Test
    @DisplayName("GET /consultas/hoje filtrado por profissional também devolve o id")
    void consultasDoDiaPorProfissional() {
        Periodo hoje = Periodo.dia(LocalDate.now());
        Long profissionalId = em.find(Consulta.class, consultaComUsuarioId).getProfissional().getId();
        em.clear();

        List<Consulta> consultas = consultaRepository.findByOrganizacaoIdAndPeriodo(
                ORG, profissionalId, hoje.inicio(), hoje.fim());
        em.clear();

        assertThat(daLista(consultas, consultaComUsuarioId).profissionalUsuarioId()).isEqualTo(usuarioDoDonoId);
    }

    @Test
    @DisplayName("GET /consultas/{id}: a busca por id carrega o profissional e devolve o id do usuário")
    void consultaPorId() {
        Consulta c = consultaRepository.findByIdAndOrganizacao_IdWithRelations(consultaComUsuarioId, ORG).orElseThrow();
        Consulta semUsuario = consultaRepository.findByIdAndOrganizacao_IdWithRelations(consultaSemUsuarioId, ORG).orElseThrow();
        em.clear();

        ConsultaResponse resposta = ConsultaResponse.fromEntity(c);

        assertThat(resposta.profissionalUsuarioId()).isEqualTo(usuarioDoDonoId);
        assertThat(resposta.profissionalNome()).isEqualTo("Dra. Ana");
        assertThat(ConsultaResponse.fromEntity(semUsuario).profissionalUsuarioId()).isNull();
    }
}
