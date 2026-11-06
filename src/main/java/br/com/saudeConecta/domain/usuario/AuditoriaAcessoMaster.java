package br.com.saudeConecta.domain.usuario;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Trilha de auditoria do acesso via senha master: o ROOT entra na conta de outro usuário
 * (recurso intencional de suporte). Cada uso grava quem entrou, como quem, quando e de qual IP.
 * Registro só de inserção: nunca é alterado nem apagado pela aplicação.
 */
@Entity
@Table(name = "auditoria_acesso_master")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AuditoriaAcessoMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Usuário ROOT que usou a senha master. */
    @Column(name = "ator_usuario_id", nullable = false, updatable = false)
    private Long atorUsuarioId;

    @Column(name = "ator_login", nullable = false, length = 100, updatable = false)
    private String atorLogin;

    /** Usuário em nome de quem o ROOT entrou. */
    @Column(name = "alvo_usuario_id", nullable = false, updatable = false)
    private Long alvoUsuarioId;

    @Column(name = "alvo_login", nullable = false, length = 100, updatable = false)
    private String alvoLogin;

    @Column(name = "alvo_organizacao_id", updatable = false)
    private Long alvoOrganizacaoId;

    /** IP do cliente (já resolvido pelo RemoteIpValve atrás do proxy). */
    @Column(name = "ip", length = 45, updatable = false)
    private String ip;

    @Column(name = "data_hora", nullable = false, updatable = false)
    private LocalDateTime dataHora;
}
