package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.pagamento.FormaPagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FormaPagamentoRepository extends JpaRepository<FormaPagamento, Long> {
    
    List<FormaPagamento> findByStatus(Byte status);
    
    List<FormaPagamento> findByStatusOrderByNomeAsc(Byte status);
}
