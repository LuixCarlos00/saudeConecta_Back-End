package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.organizacao.StatusOrganizacao;
import br.com.saudeConecta.domain.organizacao.TipoOrganizacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizacaoRepository extends JpaRepository<Organizacao, Long> {
    
    Optional<Organizacao> findByCnpj(String cnpj);
    
    List<Organizacao> findByStatus(StatusOrganizacao status);
    
    List<Organizacao> findByTipo(TipoOrganizacao tipo);
    
    List<Organizacao> findByStatusAndTipo(StatusOrganizacao status, TipoOrganizacao tipo);
    
    boolean existsByCnpj(String cnpj);
    
    Long countByStatus(StatusOrganizacao status);
}
