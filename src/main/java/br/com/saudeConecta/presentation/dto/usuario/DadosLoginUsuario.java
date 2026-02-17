package br.com.saudeConecta.presentation.dto.usuario;

public record DadosLoginUsuario(String login, String senha, Byte tipoUsuario, Byte Status) {
}
