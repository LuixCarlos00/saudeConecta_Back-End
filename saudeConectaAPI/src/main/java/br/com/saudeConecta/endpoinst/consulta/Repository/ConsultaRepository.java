package br.com.saudeConecta.endpoinst.consulta.Repository;

import br.com.saudeConecta.endpoinst.consulta.Entity.Consulta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConsultaRepository extends JpaRepository<Consulta,Long> {
}
