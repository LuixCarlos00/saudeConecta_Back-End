package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.secretaria.Secretaria;
import br.com.saudeConecta.domain.secretaria.StatusSecretaria;
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

    List<Secretaria> findByOrganizacao_IdAndStatus(Long organizacaoId, StatusSecretaria status);

    Optional<Secretaria> findByIdAndOrganizacao_Id(Long id, Long organizacaoId);

    Optional<Secretaria> findByCpf(String cpf);

    boolean existsByCpf(String cpf);
    
    Optional<Secretaria> findByEmail(String email);

    Optional<Secretaria> findByUsuario_Id(Long usuarioId);

    Optional<Secretaria> findByOrganizacao_IdAndUsuario_Id(Long id, Long id1);

    @Query("SELECT COUNT(s) FROM Secretaria s " +
           "WHERE s.organizacao.id = :orgId AND s.status IN ('ATIVO', 'INATIVO')")
    Long countAtivasByOrganizacaoId(@Param("orgId") Long organizacaoId);
}
