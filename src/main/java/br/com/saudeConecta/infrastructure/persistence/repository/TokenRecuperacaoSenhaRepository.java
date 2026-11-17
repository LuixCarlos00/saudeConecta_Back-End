package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.usuario.TokenRecuperacaoSenha;
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
public interface TokenRecuperacaoSenhaRepository extends JpaRepository<TokenRecuperacaoSenha, Long> {

    /**
     * Busca o token pelo hash, sem trava (só leitura, para a tela validar o link).
     *
     * @param tokenHash SHA-256 (hex) do token recebido
     * @return token encontrado, com o usuário já carregado
     */
    @Query("SELECT t FROM TokenRecuperacaoSenha t JOIN FETCH t.usuario WHERE t.tokenHash = :tokenHash")
    Optional<TokenRecuperacaoSenha> findByTokenHash(@Param("tokenHash") String tokenHash);

    /**
     * Busca o token pelo hash travando a linha até o fim da transação. Dois envios simultâneos
     * do mesmo link ficam em fila: o segundo já encontra o token usado.
     *
     * @param tokenHash SHA-256 (hex) do token recebido
     * @return token encontrado, com o usuário já carregado
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TokenRecuperacaoSenha t JOIN FETCH t.usuario WHERE t.tokenHash = :tokenHash")
    Optional<TokenRecuperacaoSenha> findByTokenHashParaUso(@Param("tokenHash") String tokenHash);

    /**
     * Invalida os tokens ainda pendentes do usuário (pedido novo ou senha já redefinida).
     *
     * @param usuarioId dono dos tokens
     * @param agora     momento gravado em usado_em
     * @return quantidade de tokens invalidados
     */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE TokenRecuperacaoSenha t SET t.usadoEm = :agora WHERE t.usuario.id = :usuarioId AND t.usadoEm IS NULL")
    int invalidarPendentes(@Param("usuarioId") Long usuarioId, @Param("agora") LocalDateTime agora);
}
