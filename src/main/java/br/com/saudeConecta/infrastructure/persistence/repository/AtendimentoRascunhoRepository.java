package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.atendimento.AtendimentoRascunho;
import br.com.saudeConecta.domain.atendimento.RascunhoAtendimentoResumo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AtendimentoRascunhoRepository extends JpaRepository<AtendimentoRascunho, Long> {

    /**
     * Rascunho da consulta, só se for da clínica informada.
     *
     * @param consultaId    ID da consulta
     * @param organizacaoId clínica do token
     * @return rascunho, ou vazio se não existir (ou for de outra clínica)
     */
    Optional<AtendimentoRascunho> findByConsulta_IdAndOrganizacao_Id(Long consultaId, Long organizacaoId);

    /**
     * Rascunhos do profissional vinculado ao usuário, na clínica informada, do mais recente para o
     * mais antigo. Usuário sem cadastro de profissional não casa com nada e recebe lista vazia.
     *
     * @param organizacaoId clínica do token
     * @param usuarioId     usuário logado
     * @return resumo de cada rascunho (sem {@code dados})
     */
    @Query("SELECT r.consulta.id AS consultaId, r.atualizadoEm AS atualizadoEm, "
            + "r.tempoAcumuladoSegundos AS tempoAcumuladoSegundos "
            + "FROM AtendimentoRascunho r "
            + "WHERE r.organizacao.id = :organizacaoId AND r.profissional.usuario.id = :usuarioId "
            + "ORDER BY r.atualizadoEm DESC")
    List<RascunhoAtendimentoResumo> listarDoUsuario(@Param("organizacaoId") Long organizacaoId,
                                                     @Param("usuarioId") Long usuarioId);

    /**
     * Apaga o rascunho da consulta (no máximo um, pelo UNIQUE). Chamar só depois de validar que a
     * consulta é da clínica do token.
     *
     * <p>{@code flushAutomatically}: as alterações pendentes (o UPDATE da consulta para REALIZADA, na
     * finalização) vão ao banco antes do DELETE. Assim a linha da consulta já está travada quando o
     * DELETE roda e um PUT concorrente, que trava a mesma linha, ou termina antes (e o DELETE o vê) ou
     * espera e encontra a consulta encerrada.</p>
     *
     * @param consultaId ID da consulta
     * @return quantidade de linhas apagadas (0 ou 1)
     */
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM AtendimentoRascunho r WHERE r.consulta.id = :consultaId")
    int apagarPorConsulta(@Param("consultaId") Long consultaId);
}
