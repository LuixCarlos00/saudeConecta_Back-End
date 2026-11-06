package br.com.saudeConecta.integration;

import br.com.saudeConecta.application.assinatura.AcessoAssinaturaService;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.organizacao.TipoOrganizacao;
import br.com.saudeConecta.domain.planos.AssinaturaTenant;
import br.com.saudeConecta.domain.planos.PlanoAssinatura;
import br.com.saudeConecta.domain.planos.StatusAssinatura;
import br.com.saudeConecta.domain.planos.TipoPlano;
import br.com.saudeConecta.infrastructure.persistence.repository.AssinaturaTenantRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 4.2 - O filtro de autenticação consulta o acesso da assinatura com cache curto. A mudança de
 * status da assinatura (por qualquer caminho: régua, webhook, ROOT) precisa invalidar a chave,
 * senão a clínica suspensa continua navegando até o TTL expirar (e a reativada fica bloqueada).
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
@ActiveProfiles("test")
@DisplayName("AcessoAssinaturaService - cache invalidado na mudança de status")
class AcessoAssinaturaCacheIntegrationTest {

    @Autowired private AcessoAssinaturaService acessoAssinaturaService;
    @Autowired private AssinaturaTenantRepository assinaturaTenantRepository;
    @Autowired private CacheManager cacheManager;
    @Autowired private EntityManager em;
    @Autowired private TransactionTemplate tx;

    private Long orgId;
    private Long planoId;
    private Long assinaturaId;

    @BeforeEach
    void setUp() {
        cacheManager.getCache(AcessoAssinaturaService.CACHE_ACESSO_ASSINATURA).clear();
        tx.executeWithoutResult(s -> {
            Organizacao org = new Organizacao();
            org.setNome("Clinica Cache Assinatura");
            org.setTipo(TipoOrganizacao.CLINICA);
            em.persist(org);

            PlanoAssinatura plano = new PlanoAssinatura();
            plano.setNome("Plano Cache");
            plano.setTipo(TipoPlano.PROFISSIONAL);
            plano.setValorMensal(BigDecimal.valueOf(279));
            plano.setAtivo(true);
            em.persist(plano);

            AssinaturaTenant a = AssinaturaTenant.builder()
                    .organizacao(org).planoAssinatura(plano).status(StatusAssinatura.ATIVA)
                    .dataInicio(LocalDate.now()).dataVencimento(LocalDate.now().plusDays(30))
                    .valorMensal(BigDecimal.valueOf(279))
                    .qtdAdminExtra(0).qtdProfissionalExtra(0).qtdSecretariaExtra(0)
                    .valorAdicionalPerfis(BigDecimal.ZERO)
                    .build();
            em.persist(a);
            orgId = org.getId();
            planoId = plano.getId();
            assinaturaId = a.getId();
        });
    }

    @AfterEach
    void tearDown() {
        tx.executeWithoutResult(s -> {
            em.remove(em.find(AssinaturaTenant.class, assinaturaId));
            em.remove(em.find(PlanoAssinatura.class, planoId));
            em.remove(em.find(Organizacao.class, orgId));
        });
    }

    @Test
    @DisplayName("Suspender a assinatura (dirty checking, sem save explícito) invalida o cache da organização")
    void suspenderInvalidaCache() {
        assertThat(acessoAssinaturaService.organizacaoPermiteAcesso(orgId)).isTrue();
        assertThat(cacheManager.getCache(AcessoAssinaturaService.CACHE_ACESSO_ASSINATURA).get(orgId)).isNotNull();

        tx.executeWithoutResult(s ->
                assinaturaTenantRepository.findById(assinaturaId).orElseThrow().setStatus(StatusAssinatura.SUSPENSA));

        assertThat(acessoAssinaturaService.organizacaoPermiteAcesso(orgId)).isFalse();
    }

    @Test
    @DisplayName("Reativar a assinatura libera o acesso sem esperar o TTL")
    void reativarInvalidaCache() {
        tx.executeWithoutResult(s ->
                assinaturaTenantRepository.findById(assinaturaId).orElseThrow().setStatus(StatusAssinatura.SUSPENSA));
        assertThat(acessoAssinaturaService.organizacaoPermiteAcesso(orgId)).isFalse();

        tx.executeWithoutResult(s -> {
            AssinaturaTenant a = assinaturaTenantRepository.findById(assinaturaId).orElseThrow();
            a.setStatus(StatusAssinatura.ATIVA);
            assinaturaTenantRepository.save(a);
        });

        assertThat(acessoAssinaturaService.organizacaoPermiteAcesso(orgId)).isTrue();
    }
}
