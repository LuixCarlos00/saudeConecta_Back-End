package br.com.saudeConecta.domain.usuario;

/**
 * Origem do bloqueio de um usuário. Permite que o desbloqueio em cascata da organização
 * (feito pelo ROOT) reative apenas quem foi bloqueado pela própria cascata, preservando
 * bloqueios individuais feitos pelo gestor.
 */
public enum MotivoBloqueio {
    /** Bloqueado individualmente (gestor ou ROOT bloqueando um usuário específico). */
    INDIVIDUAL,
    /** Bloqueado junto com a organização inteira (ROOT bloqueando o gestor). */
    CASCATA
}
