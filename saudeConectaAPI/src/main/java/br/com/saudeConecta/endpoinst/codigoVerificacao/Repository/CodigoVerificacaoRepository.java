package br.com.saudeConecta.endpoinst.codigoVerificacao.Repository;

import br.com.saudeConecta.endpoinst.codigoVerificacao.Entity.CodigoVerificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CodigoVerificacaoRepository extends JpaRepository<CodigoVerificacao,Long> {

        boolean existsByCodVerificacaoCodigo(String codVerificacaoCodigo);

    long deleteByCodVerificacaoCodigo(String codVerificacaoCodigo);
}
