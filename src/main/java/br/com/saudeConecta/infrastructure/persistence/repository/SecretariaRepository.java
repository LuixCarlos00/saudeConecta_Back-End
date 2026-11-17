package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.secretaria.Secretaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SecretariaRepository extends JpaRepository<Secretaria, Long> {

    List<Secretaria> findByOrganizacao_Id(Long organizacaoId);
    
    @Query("SELECT s FROM Secretaria s LEFT JOIN FETCH s.usuario WHERE s.organizacao.id = :organizacaoId")
    List<Secretaria> findByOrganizacao_IdWithUsuario(@Param("organizacaoId") Long organizacaoId);


    Optional<Secretaria> findByIdAndOrganizacao_Id(Long id, Long organizacaoId);

    Optional<Secretaria> findByEmail(String email);

    /**
     * Todos os registros com o e-mail. A recuperação de senha usa a lista (e não o Optional de
     * findByEmail) para não estourar quando houver e-mail repetido e mandar um link por conta.
     *
     * @param email e-mail informado na tela "esqueci minha senha"
     * @return registros com o e-mail, vazio quando não há nenhum
     */
    List<Secretaria> findAllByEmail(String email);

    Optional<Secretaria> findByUsuario_Id(Long usuarioId);

    @Query("SELECT COUNT(s) FROM Secretaria s " +
           "WHERE s.organizacao.id = :orgId AND s.status IN ('ATIVO', 'INATIVO')")
    Long countAtivasByOrganizacaoId(@Param("orgId") Long organizacaoId);
}
