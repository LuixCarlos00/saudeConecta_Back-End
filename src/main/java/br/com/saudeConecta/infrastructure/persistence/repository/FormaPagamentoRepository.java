package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.pagamento.FormaPagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FormaPagamentoRepository extends JpaRepository<FormaPagamento, Long> {

}
