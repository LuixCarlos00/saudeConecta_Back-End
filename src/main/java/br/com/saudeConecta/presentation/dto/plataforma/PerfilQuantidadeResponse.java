package br.com.saudeConecta.presentation.dto.plataforma;

import br.com.saudeConecta.domain.usuario.TipoUsuarioNovo;

/**
 * @param perfil     perfil do usuário (GESTOR, CLINICO, ASSISTENTE)
 * @param quantidade usuários ativos nesse perfil
 */
public record PerfilQuantidadeResponse(TipoUsuarioNovo perfil, long quantidade) {
}
