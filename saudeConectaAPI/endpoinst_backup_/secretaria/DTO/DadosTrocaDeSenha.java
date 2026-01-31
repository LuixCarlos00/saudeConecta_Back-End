package br.com.saudeConecta.endpoinst.secretaria.DTO;

public record DadosTrocaDeSenha(
        Long id,
        String senhaAntiga,
        String senhaNova,
        String email
) {

}
