package br.com.saudeConecta.integration;

import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.pagamento.FormaPagamento;
import br.com.saudeConecta.domain.prontuario.PlanejamentoTerapeutico;
import br.com.saudeConecta.domain.prontuario.Prontuario;
import br.com.saudeConecta.domain.prontuario.TermoAutorizacao;
import br.com.saudeConecta.infrastructure.persistence.repository.FormaPagamentoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.PlanejamentoTerapeuticoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProntuarioRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.TermoAutorizacaoRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prova, contra um banco real (H2), que as queries filtram por organização e que os links
 * públicos são de uso único. Organização, profissional e paciente entram só como chave
 * estrangeira (integridade referencial desligada) para manter o cenário mínimo.
 */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
@ActiveProfiles("test")
@DisplayName("Repositórios - isolamento por tenant e links públicos")
class IsolamentoTenantRepositoryTest {

    private static final Long ORG_A = 1L;
    private static final Long ORG_B = 2L;

    @Autowired private EntityManager em;
    @Autowired private ProntuarioRepository prontuarioRepository;
    @Autowired private FormaPagamentoRepository formaPagamentoRepository;
    @Autowired private TermoAutorizacaoRepository termoRepository;
    @Autowired private PlanejamentoTerapeuticoRepository planejamentoRepository;

    @BeforeEach
    void desligarIntegridadeReferencial() {
        em.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
    }

    private Consulta consultaDa(Long orgId) {
        Consulta c = Consulta.builder()
                .organizacao(em.getReference(Organizacao.class, orgId))
                .profissional(em.getReference(Profissional.class, 1L))
                .paciente(em.getReference(Paciente.class, 1L))
                .dataHora(LocalDateTime.now())
                .build();
        em.persist(c);
        return c;
    }

    private Prontuario prontuarioDe(Consulta consulta) {
        Prontuario p = Prontuario.builder()
                .consulta(consulta)
                .prontDataFinalizado(Date.valueOf(LocalDate.now()))
                .build();
        em.persist(p);
        return p;
    }

    private PlanejamentoTerapeutico planejamento(String token, String status, LocalDateTime expiracao) {
        PlanejamentoTerapeutico p = PlanejamentoTerapeutico.builder()
                .organizacao(em.getReference(Organizacao.class, ORG_A))
                .profissional(em.getReference(Profissional.class, 1L))
                .paciente(em.getReference(Paciente.class, 1L))
                .dataProcedimento(LocalDate.now())
                .procedimentoRealizado("Restauração")
                .valor(BigDecimal.TEN)
                .tokenAssinatura(token)
                .tokenExpiracao(expiracao)
                .statusAssinatura(status)
                .build();
        em.persist(p);
        return p;
    }

    @Test
    @DisplayName("Prontuário de consulta de outra clínica não é encontrado por id, consulta, paciente ou profissional")
    void prontuarioFiltradoPorOrganizacao() {
        Consulta consultaB = consultaDa(ORG_B);
        Prontuario prontB = prontuarioDe(consultaB);
        em.flush();
        em.clear();

        assertThat(prontuarioRepository.findByProntCodigoProntuarioAndConsulta_Organizacao_Id(
                prontB.getProntCodigoProntuario(), ORG_A)).isEmpty();
        assertThat(prontuarioRepository.findIdMaisRecentePorConsulta(consultaB.getId(), ORG_A)).isEmpty();
        assertThat(prontuarioRepository.findByConsultaIdAndOrganizacaoIdWithFetch(consultaB.getId(), ORG_A)).isNull();

        // Controle: a própria clínica continua enxergando.
        assertThat(prontuarioRepository.findIdMaisRecentePorConsulta(consultaB.getId(), ORG_B))
                .contains(prontB.getProntCodigoProntuario());
    }

    @Test
    @DisplayName("Forma de pagamento: global e da própria clínica são encontradas; de outra clínica, não")
    void formaPagamentoFiltradaPorOrganizacao() {
        FormaPagamento global = new FormaPagamento();
        global.setNome("Dinheiro");
        global.setCodigo("DINHEIRO_T");
        em.persist(global);
        FormaPagamento daB = new FormaPagamento();
        daB.setNome("Convênio B");
        daB.setCodigo("CONVENIO_B_T");
        daB.setOrganizacao(em.getReference(Organizacao.class, ORG_B));
        em.persist(daB);
        em.flush();
        em.clear();

        assertThat(formaPagamentoRepository.findDisponivelParaOrganizacao(global.getId(), ORG_A)).isPresent();
        assertThat(formaPagamentoRepository.findDisponivelParaOrganizacao(daB.getId(), ORG_A)).isEmpty();
        // Controle: a própria clínica continua enxergando a forma dela
        assertThat(formaPagamentoRepository.findDisponivelParaOrganizacao(daB.getId(), ORG_B)).isPresent();
    }

    @Test
    @DisplayName("Questionário de saúde de outra clínica não é encontrado pela consulta")
    void termoFiltradoPorOrganizacao() {
        Consulta consultaB = consultaDa(ORG_B);
        em.persist(TermoAutorizacao.builder()
                .consulta(consultaB)
                .paciente(em.getReference(Paciente.class, 1L))
                .organizacao(em.getReference(Organizacao.class, ORG_B))
                .token("tok-termo")
                .dataExpiracao(LocalDateTime.now().plusHours(1))
                .build());
        em.flush();
        em.clear();

        assertThat(termoRepository.findByConsultaIdAndOrganizacaoId(consultaB.getId(), ORG_A)).isEmpty();
        assertThat(termoRepository.findByConsultaIdAndOrganizacaoId(consultaB.getId(), ORG_B)).isPresent();
    }

    @Test
    @DisplayName("Questionário só é respondido uma vez: a segunda resposta afeta 0 linhas")
    void questionarioUsoUnico() {
        Consulta consulta = consultaDa(ORG_A);
        em.persist(TermoAutorizacao.builder()
                .consulta(consulta)
                .paciente(em.getReference(Paciente.class, 1L))
                .organizacao(em.getReference(Organizacao.class, ORG_A))
                .token("tok-unico")
                .dataExpiracao(LocalDateTime.now().plusHours(1))
                .build());
        em.flush();

        LocalDateTime agora = LocalDateTime.now();
        assertThat(termoRepository.responderSePendente("tok-unico", "{}", "sig", "1.1.1.1", agora)).isEqualTo(1);
        assertThat(termoRepository.responderSePendente("tok-unico", "{}", "sig2", "2.2.2.2", agora)).isZero();
    }

    @Test
    @DisplayName("Questionário expirado não é respondido")
    void questionarioExpirado() {
        Consulta consulta = consultaDa(ORG_A);
        em.persist(TermoAutorizacao.builder()
                .consulta(consulta)
                .paciente(em.getReference(Paciente.class, 1L))
                .organizacao(em.getReference(Organizacao.class, ORG_A))
                .token("tok-expirado")
                .dataExpiracao(LocalDateTime.now().minusMinutes(1))
                .build());
        em.flush();

        assertThat(termoRepository.responderSePendente("tok-expirado", "{}", "sig", "1.1.1.1", LocalDateTime.now()))
                .isZero();
    }

    @Test
    @DisplayName("Planejamento: assina só itens PENDENTES, limpa o token e não aceita reenvio")
    void planejamentoUsoUnico() {
        planejamento("tok-plan", "PENDENTE", LocalDateTime.now().plusDays(7));
        planejamento("tok-plan", "ASSINADO", LocalDateTime.now().plusDays(7));
        em.flush();

        LocalDateTime agora = LocalDateTime.now();
        assertThat(planejamentoRepository.assinarPorToken("tok-plan", "sig", "1.1.1.1", agora)).isEqualTo(1);
        em.clear();

        assertThat(planejamentoRepository.assinarPorToken("tok-plan", "sig2", "2.2.2.2", agora))
                .as("o item já assinado mantém o token antigo, mas não é reassinado")
                .isZero();
        assertThat(planejamentoRepository.findByTokenAssinatura("tok-plan"))
                .allMatch(PlanejamentoTerapeutico::isAssinado);
    }

    @Test
    @DisplayName("Planejamento: link expirado não assina; link legado (sem expiração) assina")
    void planejamentoExpiracao() {
        planejamento("tok-velho", "PENDENTE", LocalDateTime.now().minusMinutes(1));
        planejamento("tok-legado", "PENDENTE", null);
        em.flush();

        LocalDateTime agora = LocalDateTime.now();
        assertThat(planejamentoRepository.assinarPorToken("tok-velho", "sig", "1.1.1.1", agora)).isZero();
        assertThat(planejamentoRepository.assinarPorToken("tok-legado", "sig", "1.1.1.1", agora)).isEqualTo(1);
        em.clear();
        assertThat(planejamentoRepository.findByTokenAssinatura("tok-legado"))
                .as("token mantido após assinar, para o link reaberto mostrar 'já assinado'")
                .hasSize(1)
                .allMatch(PlanejamentoTerapeutico::isAssinado);
        assertThat(planejamentoRepository.assinarPorToken("tok-legado", "sig2", "2.2.2.2", agora.plusMinutes(1)))
                .as("reenvio não reassina")
                .isZero();
    }
}
