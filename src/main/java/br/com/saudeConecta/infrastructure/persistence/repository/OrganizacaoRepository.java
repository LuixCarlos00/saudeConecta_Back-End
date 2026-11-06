package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.organizacao.StatusOrganizacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrganizacaoRepository extends JpaRepository<Organizacao, Long> {
    
    List<Organizacao> findByStatus(StatusOrganizacao status);
    
    boolean existsByCnpj(String cnpj);
    
    Long countByStatus(StatusOrganizacao status);
}
