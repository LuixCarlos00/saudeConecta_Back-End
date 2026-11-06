package br.com.saudeConecta.application.plataforma.dto;

import br.com.saudeConecta.domain.usuario.TipoUsuarioNovo;

/**
 * @param perfil     perfil do usuário (GESTOR, CLINICO, ASSISTENTE)
 * @param quantidade usuários ativos nesse perfil
 */
public record PerfilQuantidadeResponse(TipoUsuarioNovo perfil, long quantidade) {
}
