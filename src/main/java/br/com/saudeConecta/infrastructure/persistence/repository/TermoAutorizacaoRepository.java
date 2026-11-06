package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.prontuario.TermoAutorizacao;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TermoAutorizacaoRepository extends JpaRepository<TermoAutorizacao, Long> {

    Optional<TermoAutorizacao> findByToken(String token);

    Optional<TermoAutorizacao> findByConsultaId(Long consultaId);

    /**
     * Busca o termo de uma consulta somente se a consulta pertencer à organização informada.
     *
     * @param consultaId ID da consulta
     * @param orgId      ID da organização do token
     * @return termo, ou vazio se não existir ou for de outra clínica
     */
    @Query("SELECT t FROM TermoAutorizacao t WHERE t.consulta.id = :consultaId AND t.consulta.organizacao.id = :orgId")
    Optional<TermoAutorizacao> findByConsultaIdAndOrganizacaoId(@Param("consultaId") Long consultaId,
                                                                @Param("orgId") Long orgId);

    /**
     * Grava as respostas e a assinatura apenas se o termo ainda estiver PENDENTE e dentro da validade.
     * O UPDATE condicional é atômico no banco: duas submissões simultâneas do mesmo link não
     * conseguem assinar duas vezes (a segunda afeta 0 linhas).
     *
     * @param token      token público do questionário
     * @param respostas  respostas do questionário (JSON)
     * @param assinatura assinatura digital em base64
     * @param ipOrigem   IP de origem resolvido pelo servidor
     * @param agora      instante da assinatura (também usado para checar a expiração)
     * @return quantidade de linhas atualizadas (0 ou 1)
     */
    @Modifying
    @Query("UPDATE TermoAutorizacao t SET t.respostasQuestionario = :respostas, " +
           "t.assinaturaBase64 = :assinatura, t.ipOrigem = :ipOrigem, t.dataAssinatura = :agora, " +
           "t.status = 'ASSINADO' " +
           "WHERE t.token = :token AND t.status = 'PENDENTE' AND t.dataExpiracao > :agora")
    int responderSePendente(@Param("token") String token,
                            @Param("respostas") String respostas,
                            @Param("assinatura") String assinatura,
                            @Param("ipOrigem") String ipOrigem,
                            @Param("agora") LocalDateTime agora);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TermoAutorizacao t WHERE t.consulta.id = :consultaId")
    Optional<TermoAutorizacao> findByConsultaIdWithLock(@Param("consultaId") Long consultaId);

    @Query("SELECT t FROM TermoAutorizacao t " +
           "LEFT JOIN FETCH t.consulta c " +
           "LEFT JOIN FETCH c.organizacao o " +
           "LEFT JOIN FETCH c.profissional pr " +
           "LEFT JOIN FETCH t.paciente p " +
           "WHERE t.token = :token")
    Optional<TermoAutorizacao> findByTokenComRelacionamentos(@Param("token") String token);

    /**
     * Exclui todos os termos de autorização/questionários vinculados a uma consulta.
     */
    @Modifying
    @Query("DELETE FROM TermoAutorizacao t WHERE t.consulta.id = :consultaId")
    void deleteByConsultaId(@Param("consultaId") Long consultaId);
}
