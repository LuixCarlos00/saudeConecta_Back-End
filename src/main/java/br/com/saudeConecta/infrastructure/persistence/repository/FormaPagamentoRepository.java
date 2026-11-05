package br.com.saudeConecta.infrastructure.persistence.repository;

import java.util.Optional;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import br.com.saudeConecta.domain.pagamento.FormaPagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FormaPagamentoRepository extends JpaRepository<FormaPagamento, Long> {

    /**
     * Forma de pagamento que a clínica pode usar: as globais (sem organização) e as dela.
     * Forma de outra clínica volta vazia, como se não existisse (evita IDOR por id).
     *
     * @param id    ID da forma de pagamento
     * @param orgId ID da organização do token
     * @return a forma de pagamento, ou vazio se não existir ou for de outra clínica
     */
    @Query("SELECT f FROM FormaPagamento f WHERE f.id = :id AND (f.organizacao IS NULL OR f.organizacao.id = :orgId)")
    Optional<FormaPagamento> findDisponivelParaOrganizacao(@Param("id") Long id, @Param("orgId") Long orgId);

}
