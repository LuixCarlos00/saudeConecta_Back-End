package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.codigoverificacao.CodigoVerificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CodigoVerificacaoRepository extends JpaRepository<CodigoVerificacao,Long> {

        boolean existsByCodVerificacaoCodigo(String codVerificacaoCodigo);

    long deleteByCodVerificacaoCodigo(String codVerificacaoCodigo);
}
