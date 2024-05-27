package br.com.saudeConecta.endpoinst.administrador.DTO;

public record DadosTrocaDeSenha(
        Long id,
        String senhaAntiga,
        String senhaNova,
        String email
) {

}
