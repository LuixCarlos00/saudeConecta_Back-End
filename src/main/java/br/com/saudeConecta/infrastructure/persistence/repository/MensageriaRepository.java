package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.mensageria.Mensageria;
import br.com.saudeConecta.domain.mensageria.StatusMensagem;
import br.com.saudeConecta.domain.mensageria.TipoMensagem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório para acesso aos dados de mensageria.
 */
@Repository
public interface MensageriaRepository extends JpaRepository<Mensageria, Long> {



    @Query("SELECT m FROM Mensageria m WHERE m.organizacao.id = :orgId " +
           "AND (:status IS NULL OR m.status = :status) " +
           "AND (:tipo IS NULL OR m.tipoMensagem = :tipo) " +
           "ORDER BY m.dataCriacao DESC")
    Page<Mensageria> findByOrganizacaoIdWithFilters(
            @Param("orgId") Long orgId,
            @Param("status") StatusMensagem status,
            @Param("tipo") TipoMensagem tipo,
            Pageable pageable);


    long countByOrganizacao_IdAndAdminNotificadoFalseAndStatus(Long organizacaoId, StatusMensagem status);


    @Query("SELECT m FROM Mensageria m " +
           "WHERE (:status IS NULL OR m.status = :status) " +
           "AND (:tipo IS NULL OR m.tipoMensagem = :tipo) " +
           "ORDER BY m.dataCriacao DESC")
    Page<Mensageria> findAllWithFilters(
            @Param("status") StatusMensagem status,
            @Param("tipo") TipoMensagem tipo,
            Pageable pageable);



    long countByAdminNotificadoFalseAndStatus(StatusMensagem status);


    /**
     * Mensagens de um destinatário específico. O ID da entidade sozinho não identifica o
     * destinatário: profissional, secretária, admin e paciente têm sequências próprias e cada
     * clínica tem os seus, então o filtro precisa do tipo da mensagem e da organização.
     *
     * @param entidadeId    ID da entidade destinatária (profissional, secretária, admin ou paciente)
     * @param tipoMensagem  tipo que liga a mensagem ao tipo de entidade (ex.: EMAIL_CREDENCIAIS_CLINICO)
     * @param organizacaoId organização dona da mensagem
     * @return mensagens daquele destinatário naquela organização
     */
    List<Mensageria> findByDestinatarioEntidadeIdAndTipoMensagemAndOrganizacao_Id(
            Long entidadeId, TipoMensagem tipoMensagem, Long organizacaoId);
}
