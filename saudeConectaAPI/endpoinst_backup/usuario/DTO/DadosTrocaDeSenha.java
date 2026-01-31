package br.com.saudeConecta.endpoinst.usuario.DTO;

public record DadosTrocaDeSenha(
        Long id,
        String senhaAntiga,
        String senhaNova,
        String email
) {

}
