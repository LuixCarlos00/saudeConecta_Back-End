package br.com.saudeConecta.integration;

import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.organizacao.TipoOrganizacao;
import br.com.saudeConecta.domain.planos.AssinaturaTenant;
import br.com.saudeConecta.domain.planos.PlanoAssinatura;
import br.com.saudeConecta.domain.planos.StatusAssinatura;
import br.com.saudeConecta.domain.planos.TipoPlano;
import br.com.saudeConecta.infrastructure.persistence.repository.AssinaturaTenantRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prova, contra H2, a query da tela de assinaturas do SUPER_ADMIN: uma linha por clínica (a assinatura
 * mais recente), filtros opcionais, busca por nome com curinga escapado e paginação com total.
 */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
@ActiveProfiles("test")
@DisplayName("AssinaturaTenantRepository - listagem da plataforma")
class AssinaturaPlataformaRepositoryTest {

    @Autowired private EntityManager em;
    @Autowired private AssinaturaTenantRepository repository;

    private PlanoAssinatura starter;
    private PlanoAssinatura business;
    private Organizacao alfa;
    private Organizacao beta;
    private Organizacao gama;

    @BeforeEach
    void massa() {
        starter = plano(TipoPlano.STARTER);
        business = plano(TipoPlano.BUSINESS);
        alfa = org("Clínica Alfa");
        beta = org("Odonto 50% Beta");
        gama = org("Gama Saúde");
        assinatura(alfa, starter, StatusAssinatura.CANCELADA, false); // histórico: não aparece
        assinatura(alfa, business, StatusAssinatura.ATIVA, false);
        assinatura(beta, starter, StatusAssinatura.TRIAL, true);
        assinatura(gama, starter, StatusAssinatura.SUSPENSA, false);
        em.flush();
        em.clear();
    }

    private PlanoAssinatura plano(TipoPlano tipo) {
        PlanoAssinatura p = new PlanoAssinatura();
        p.setNome(tipo.getDescricao());
        p.setTipo(tipo);
        p.setValorMensal(BigDecimal.valueOf(100));
        p.setAtivo(true);
        em.persist(p);
        return p;
    }

    private Organizacao org(String nome) {
        Organizacao o = new Organizacao();
        o.setNome(nome);
        o.setTipo(TipoOrganizacao.CLINICA);
        em.persist(o);
        return o;
    }

    private void assinatura(Organizacao o, PlanoAssinatura p, StatusAssinatura status, boolean cortesia) {
        em.persist(AssinaturaTenant.builder()
                .organizacao(o).planoAssinatura(p).status(status).cortesia(cortesia)
                .dataInicio(LocalDate.of(2026, 10, 1)).dataVencimento(LocalDate.of(2026, 12, 1))
                .valorMensal(BigDecimal.valueOf(100)).qtdAdminExtra(0).qtdProfissionalExtra(0)
                .qtdSecretariaExtra(0).valorAdicionalPerfis(BigDecimal.ZERO).build());
    }

    private Page<AssinaturaTenant> listar(StatusAssinatura status, TipoPlano plano, Boolean cortesia, String busca) {
        return repository.listarVigentesParaPlataforma(status, plano, cortesia, busca,
                PageRequest.of(0, 10, Sort.by("organizacao.nome").and(Sort.by("id"))));
    }

    @Test
    @DisplayName("Sem filtro: uma linha por clínica, a mais recente, ordenada pelo nome")
    void umaLinhaPorClinica() {
        Page<AssinaturaTenant> pagina = listar(null, null, null, null);

        assertThat(pagina.getTotalElements()).isEqualTo(3);
        assertThat(pagina.getContent()).extracting(a -> a.getOrganizacao().getNome())
                .containsExactly("Clínica Alfa", "Gama Saúde", "Odonto 50% Beta");
        assertThat(pagina.getContent().get(0).getStatus()).isEqualTo(StatusAssinatura.ATIVA);
    }

    @Test
    @DisplayName("Filtros de status, plano e cortesia combinam")
    void filtros() {
        assertThat(listar(StatusAssinatura.SUSPENSA, null, null, null).getContent())
                .extracting(a -> a.getOrganizacao().getId()).containsExactly(gama.getId());
        assertThat(listar(null, TipoPlano.STARTER, null, null).getContent())
                .extracting(a -> a.getOrganizacao().getId()).containsExactlyInAnyOrder(beta.getId(), gama.getId());
        assertThat(listar(null, null, true, null).getContent())
                .extracting(a -> a.getOrganizacao().getId()).containsExactly(beta.getId());
        // A CANCELADA da Alfa é histórico: filtrar CANCELADA não traz a Alfa (vigente é ATIVA)
        assertThat(listar(StatusAssinatura.CANCELADA, null, null, null).getContent()).isEmpty();
    }

    @Test
    @DisplayName("Busca por nome sem diferenciar maiúsculas; % do usuário é literal")
    void busca() {
        assertThat(listar(null, null, null, "%gama%").getContent())
                .extracting(a -> a.getOrganizacao().getId()).containsExactly(gama.getId());
        assertThat(listar(null, null, null, "%50!%%").getContent())
                .extracting(a -> a.getOrganizacao().getId()).containsExactly(beta.getId());
    }

    @Test
    @DisplayName("Paginação devolve o total de clínicas")
    void paginacao() {
        Page<AssinaturaTenant> pagina = repository.listarVigentesParaPlataforma(null, null, null, null,
                PageRequest.of(1, 2, Sort.by("organizacao.nome")));

        assertThat(pagina.getTotalElements()).isEqualTo(3);
        assertThat(pagina.getTotalPages()).isEqualTo(2);
        assertThat(pagina.getContent()).hasSize(1);
    }
}
