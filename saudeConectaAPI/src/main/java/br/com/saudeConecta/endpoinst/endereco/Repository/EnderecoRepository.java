package br.com.saudeConecta.endpoinst.endereco.Repository;

import br.com.saudeConecta.endpoinst.endereco.Entity.Endereco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EnderecoRepository extends JpaRepository<Endereco , Long > {
}
