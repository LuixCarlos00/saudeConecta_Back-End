package br.com.saudeConecta.integration;

import br.com.saudeConecta.domain.planos.MpWebhookEvento;
import br.com.saudeConecta.infrastructure.persistence.repository.MpWebhookEventoRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prova, contra H2, que a pendência resolvida sai da lista de abertas e da contagem do dashboard da
 * plataforma, e entra na lista de resolvidas.
 */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
@ActiveProfiles("test")
@DisplayName("MpWebhookEventoRepository - pendências resolvidas")
class PendenciaManualRepositoryTest {

    @Autowired private EntityManager em;
    @Autowired private MpWebhookEventoRepository repository;

    private MpWebhookEvento pendencia(String dataId, LocalDateTime recebidoEm) {
        MpWebhookEvento e = MpWebhookEvento.builder().tipo("subscription_authorized_payment").dataId(dataId)
                .processado(true).recebidoEm(recebidoEm).build();
        e.marcarParaAtencao(MpWebhookEvento.MOTIVO_PAGAMENTO_EM_CORTESIA, "assinatura 1");
        em.persist(e);
        return e;
    }

    @Test
    @DisplayName("Resolvida não conta no dashboard nem aparece nas abertas; aparece nas resolvidas")
    void resolvidaSaiDasAbertas() {
        LocalDateTime base = LocalDateTime.of(2026, 11, 1, 10, 0);
        MpWebhookEvento aberta = pendencia("fat-1", base);
        MpWebhookEvento resolvida = pendencia("fat-2", base.plusHours(1));
        resolvida.resolver(99L, "estornado", base.plusDays(1));
        em.flush();
        em.clear();

        assertThat(repository.countByRequerAtencaoTrue()).isEqualTo(1);
        assertThat(repository.buscarPendentesManuais(PageRequest.of(0, 10)))
                .extracting(p -> p.getId()).containsExactly(aberta.getId());
        assertThat(repository.findByRequerAtencaoTrueOrderByRecebidoEmDesc())
                .extracting(MpWebhookEvento::getId).containsExactly(aberta.getId());
        assertThat(repository.findByResolvidoEmIsNotNullOrderByResolvidoEmDesc())
                .singleElement().satisfies(e -> {
                    assertThat(e.getId()).isEqualTo(resolvida.getId());
                    assertThat(e.getObservacaoResolucao()).isEqualTo("estornado");
                    assertThat(e.getResolvidoPorUsuarioId()).isEqualTo(99L);
                });
        assertThat(repository.findByIdParaAtualizacao(aberta.getId())).isPresent();
    }
}
