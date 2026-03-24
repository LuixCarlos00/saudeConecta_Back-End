package br.com.saudeConecta.domain.mensageria;

/**
 * Tipo da mensagem registrada no sistema de mensageria.
 */
public enum TipoMensagem {
    EMAIL_CREDENCIAIS_CLINICO,
    EMAIL_CREDENCIAIS_SECRETARIA,
    EMAIL_CREDENCIAIS_ADMINISTRADOR,
    EMAIL_CREDENCIAIS_PACIENTE,
    EMAIL_RECUPERACAO_SENHA,
    EMAIL_GENERICO
}
