package br.com.saudeConecta.endpoinst.usuario.Resource;

import br.com.saudeConecta.endpoinst.usuario.DTO.DadosTokenJWT;
import br.com.saudeConecta.endpoinst.usuario.DTO.DadosUsuarioView;

public class CadastroResponse {
    private DadosUsuarioView usuarioView;
    private String tokenJWT;

    public CadastroResponse(DadosUsuarioView usuarioView, String tokenJWT) {
        this.usuarioView = usuarioView;
        this.tokenJWT = tokenJWT;
    }

    public DadosUsuarioView getUsuarioView() {
        return usuarioView;
    }

    public String getTokenJWT() {
        return tokenJWT;
    }
}


