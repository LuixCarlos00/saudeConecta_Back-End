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
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prova, contra um banco real (H2), as queries que alimentam a régua de cobrança.
 */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
@ActiveProfiles("test")
@DisplayName("AssinaturaTenantRepository - queries da régua de cobrança")
class AssinaturaTenantRepositoryTest {

    @Autowired private EntityManager em;
    @Autowired private AssinaturaTenantRepository repository;

    private Organizacao organizacao;
    private PlanoAssinatura plano;

    @BeforeEach
    void setUp() {
        organizacao = new Organizacao();
        organizacao.setNome("Clínica Régua");
        organizacao.setTipo(TipoOrganizacao.CLINICA);
        em.persist(organizacao);

        plano = new PlanoAssinatura();
        plano.setNome("Profissional");
        plano.setTipo(TipoPlano.PROFISSIONAL);
        plano.setValorMensal(BigDecimal.valueOf(279));
        plano.setAtivo(true);
        em.persist(plano);
    }

    private AssinaturaTenant assinatura(StatusAssinatura status, LocalDate vencimento) {
        AssinaturaTenant a = AssinaturaTenant.builder()
                .organizacao(organizacao)
                .planoAssinatura(plano)
                .status(status)
                .dataInicio(LocalDate.now().minusDays(60))
                .dataVencimento(vencimento)
                .valorMensal(BigDecimal.valueOf(279))
                .qtdAdminExtra(0)
                .qtdProfissionalExtra(0)
                .qtdSecretariaExtra(0)
                .valorAdicionalPerfis(BigDecimal.ZERO)
                .build();
        em.persist(a);
        return a;
    }

    @Test
    @DisplayName("2.1 - TRIAL vencido além da carência entra na régua; cortesia continua de fora")
    void trialVencidoEntraNaRegua() {
        LocalDate limite = LocalDate.now().minusDays(5);
        AssinaturaTenant trialVencido = assinatura(StatusAssinatura.TRIAL, limite.minusDays(1));
        AssinaturaTenant ativaVencida = assinatura(StatusAssinatura.ATIVA, limite.minusDays(1));
        AssinaturaTenant trialNoPrazo = assinatura(StatusAssinatura.TRIAL, limite.plusDays(1));
        AssinaturaTenant cortesia = assinatura(StatusAssinatura.TRIAL, limite.minusDays(1));
        cortesia.setCortesia(true);
        em.flush();
        em.clear();

        assertThat(repository.findVencidasNaoInadimplentes(limite))
                .extracting(AssinaturaTenant::getId)
                .containsExactlyInAnyOrder(trialVencido.getId(), ativaVencida.getId())
                .doesNotContain(trialNoPrazo.getId(), cortesia.getId());
    }

    @Test
    @DisplayName("2.3 - Suspensa é cancelada pela data da suspensão, não pela última atualização")
    void suspensaCanceladaPelaDataDaSuspensao() {
        LocalDateTime limite = LocalDate.now().minusDays(30).atStartOfDay();
        AssinaturaTenant suspensaAntiga = assinatura(StatusAssinatura.SUSPENSA, LocalDate.now().minusDays(60));
        suspensaAntiga.setDataSuspensao(limite.minusDays(1));
        AssinaturaTenant suspensaRecente = assinatura(StatusAssinatura.SUSPENSA, LocalDate.now().minusDays(20));
        suspensaRecente.setDataSuspensao(LocalDateTime.now().minusDays(2));
        em.flush();
        em.clear();

        // atualizadoEm de ambas é "agora": a query antiga nunca cancelava nenhuma das duas
        assertThat(repository.findSuspensasParaCancelar(limite))
                .extracting(AssinaturaTenant::getId)
                .containsExactly(suspensaAntiga.getId());
    }

    @Test
    @DisplayName("2.7 - Links de trial: pega quem vence até hoje+7 (inclui atrasados) e ignora quem já tem preapproval")
    void linksDeTrialAteADataAlvo() {
        LocalDate alvo = LocalDate.now().plusDays(7);
        AssinaturaTenant venceNoAlvo = assinatura(StatusAssinatura.TRIAL, alvo);
        AssinaturaTenant venceAntes = assinatura(StatusAssinatura.TRIAL, alvo.minusDays(3));
        AssinaturaTenant jaVencido = assinatura(StatusAssinatura.TRIAL, LocalDate.now().minusDays(1));
        AssinaturaTenant venceDepois = assinatura(StatusAssinatura.TRIAL, alvo.plusDays(1));
        AssinaturaTenant comLink = assinatura(StatusAssinatura.TRIAL, alvo.minusDays(1));
        comLink.setMpPreapprovalId("pa-ja-gerado");
        em.flush();
        em.clear();

        assertThat(repository.findTrialsSemLinkVencendoAte(alvo))
                .extracting(AssinaturaTenant::getId)
                .containsExactlyInAnyOrder(venceNoAlvo.getId(), venceAntes.getId(), jaVencido.getId())
                .doesNotContain(venceDepois.getId(), comLink.getId());
    }
}
