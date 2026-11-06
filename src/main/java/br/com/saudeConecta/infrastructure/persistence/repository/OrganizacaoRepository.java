package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.organizacao.Organizacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizacaoRepository extends JpaRepository<Organizacao, Long> {
    
    boolean existsByCnpj(String cnpj);
    
}
