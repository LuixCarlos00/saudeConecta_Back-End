package br.com.saudeConecta.infrastructure.persistence.repository;

 import br.com.saudeConecta.domain.historicodadospessoais.EntidadeTipo;
 import br.com.saudeConecta.domain.historicodadospessoais.HistoricoDadosPessoais;
 import br.com.saudeConecta.domain.organizacao.Organizacao;
 import br.com.saudeConecta.domain.usuario.Usuario;
 import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoricoDadosPessoaisRepository extends JpaRepository<HistoricoDadosPessoais, Long> {


//    List<HistoricoDadosPessoais> findByOrganizacao_IdAndEntidadeAndIdEntidadeOrderByCriadoEmDesc(
//            Long organizacaoId, EntidadeTipo entidade, Long idEntidade);

    List<HistoricoDadosPessoais> findByOrganizacao_IdAndUsuario_IdOrderByCriadoEmDesc(
            Long organizacaoId, Long usuarioId);
}