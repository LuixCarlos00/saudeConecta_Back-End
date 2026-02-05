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

    List<Secretaria> findByOrganizacao_IdAndStatus(Long organizacaoId, StatusSecretaria status);

    Optional<Secretaria> findByIdAndOrganizacao_Id(Long id, Long organizacaoId);

    Optional<Secretaria> findByCpf(String cpf);

    boolean existsByCpf(String cpf);

    Optional<Secretaria> findByUsuario_Id(Long usuarioId);

    @Query("SELECT COUNT(s) FROM Secretaria s WHERE s.organizacao.id = :orgId AND s.status = 'ATIVO'")
    Long countAtivosByOrganizacaoId(@Param("orgId") Long organizacaoId);
}
