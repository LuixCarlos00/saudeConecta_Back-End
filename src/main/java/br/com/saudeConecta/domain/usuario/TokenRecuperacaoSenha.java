package br.com.saudeConecta.domain.usuario;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Token de uso único do link "esqueci minha senha". Guarda só o SHA-256 do token: o valor em
 * claro existe apenas no e-mail enviado ao dono da conta, então quem lê o banco não consegue
 * montar um link válido.
 *
 * <p>Um token deixa de valer quando expira, quando é usado, ou quando um pedido mais novo do
 * mesmo usuário o substitui (os dois últimos preenchem {@code usadoEm}).</p>
 */
@Entity
@Table(name = "token_recuperacao_senha")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TokenRecuperacaoSenha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
    private Usuario usuario;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64, updatable = false)
    private String tokenHash;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "expira_em", nullable = false, updatable = false)
    private LocalDateTime expiraEm;

    @Column(name = "usado_em")
    private LocalDateTime usadoEm;

    /**
     * Cria um token pendente.
     *
     * @param usuario   dono da conta que vai redefinir a senha
     * @param tokenHash SHA-256 (hex) do token enviado no link
     * @param criadoEm  momento do pedido
     * @param expiraEm  momento a partir do qual o link deixa de valer
     */
    public TokenRecuperacaoSenha(Usuario usuario, String tokenHash, LocalDateTime criadoEm, LocalDateTime expiraEm) {
        this.usuario = usuario;
        this.tokenHash = tokenHash;
        this.criadoEm = criadoEm;
        this.expiraEm = expiraEm;
    }

    /**
     * Diz se o link ainda pode ser usado.
     *
     * @param agora momento da verificação
     * @return true quando não foi usado nem substituído e ainda não expirou
     */
    public boolean isValido(LocalDateTime agora) {
        return usadoEm == null && agora.isBefore(expiraEm);
    }

    /**
     * Marca o token como consumido; a partir daí o link não vale mais.
     *
     * @param agora momento do uso
     */
    public void marcarUsado(LocalDateTime agora) {
        this.usadoEm = agora;
    }
}
