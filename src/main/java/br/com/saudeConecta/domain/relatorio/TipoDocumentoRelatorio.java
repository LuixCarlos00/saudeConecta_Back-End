package br.com.saudeConecta.domain.relatorio;

/**
 * Tipos de documento que podem ser derivados dos atendimentos de um paciente.
 *
 * Cada tipo tem uma origem definida nos dados do prontuario, do planejamento
 * terapeutico, do termo de autorizacao ou da propria consulta.
 */
public enum TipoDocumentoRelatorio {

    REGISTRO_CONSULTA("Registro", "Registro completo do atendimento"),
    PRESCRICAO("Prescricao", "Receituario emitido no atendimento"),
    EXAMES("Exames", "Solicitacao de exames emitida no atendimento"),
    PLANEJAMENTO("Planejamento", "Planejamento terapeutico do paciente"),
    QUESTIONARIO_SAUDE("Questionario", "Questionario de saude respondido pelo paciente"),
    COMPROVANTE_PAGAMENTO("Comprovante", "Comprovante de pagamento da consulta");

    private final String rotulo;
    private final String descricaoPadrao;

    TipoDocumentoRelatorio(String rotulo, String descricaoPadrao) {
        this.rotulo = rotulo;
        this.descricaoPadrao = descricaoPadrao;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getDescricaoPadrao() {
        return descricaoPadrao;
    }
}
