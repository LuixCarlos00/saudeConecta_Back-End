package br.com.saudeConecta.integration;

import br.com.saudeConecta.domain.mensageria.Mensageria;
import br.com.saudeConecta.domain.mensageria.StatusMensagem;
import br.com.saudeConecta.domain.mensageria.TipoMensagem;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.organizacao.TipoOrganizacao;
import br.com.saudeConecta.infrastructure.persistence.repository.MensageriaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 4.6 - Apagar um profissional removia a mensageria de qualquer entidade, de qualquer clínica,
 * que tivesse o mesmo ID numérico. A busca agora filtra tipo da mensagem e organização.
 */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
@ActiveProfiles("test")
@DisplayName("MensageriaRepository - mensagens do destinatário filtradas por tipo e organização")
class MensageriaRepositoryTest {

    private static final Long ENTIDADE_ID = 42L;

    @Autowired private EntityManager em;
    @Autowired private MensageriaRepository repository;

    private Organizacao organizacao(String nome) {
        Organizacao o = new Organizacao();
        o.setNome(nome);
        o.setTipo(TipoOrganizacao.CLINICA);
        em.persist(o);
        return o;
    }

    private Mensageria mensagem(Organizacao org, TipoMensagem tipo) {
        Mensageria m = Mensageria.builder()
                .organizacao(org).destinatarioEntidadeId(ENTIDADE_ID)
                .destinatarioEmail("x@x.com").assunto("a").corpoMensagem("c")
                .tipoMensagem(tipo).status(StatusMensagem.ENVIADO)
                .build();
        em.persist(m);
        return m;
    }

    @Test
    @DisplayName("Só retorna mensagens de credenciais de clínico da mesma organização")
    void filtraPorTipoEOrganizacao() {
        Organizacao clinicaA = organizacao("Clinica A");
        Organizacao clinicaB = organizacao("Clinica B");
        Mensageria doProfissional = mensagem(clinicaA, TipoMensagem.EMAIL_CREDENCIAIS_CLINICO);
        Mensageria daSecretariaMesmoId = mensagem(clinicaA, TipoMensagem.EMAIL_CREDENCIAIS_SECRETARIA);
        Mensageria doPacienteMesmoId = mensagem(clinicaA, TipoMensagem.EMAIL_CREDENCIAIS_PACIENTE);
        Mensageria deOutraClinica = mensagem(clinicaB, TipoMensagem.EMAIL_CREDENCIAIS_CLINICO);
        em.flush();
        em.clear();

        assertThat(repository.findByDestinatarioEntidadeIdAndTipoMensagemAndOrganizacao_Id(
                        ENTIDADE_ID, TipoMensagem.EMAIL_CREDENCIAIS_CLINICO, clinicaA.getId()))
                .extracting(Mensageria::getId)
                .containsExactly(doProfissional.getId())
                .doesNotContain(daSecretariaMesmoId.getId(), doPacienteMesmoId.getId(), deOutraClinica.getId());
    }
}
