package br.com.saudeConecta.domain.dashboard;

public enum TipoGraficoDashboard {
    CONSULTAS_POR_PERIODO("Consultas por Período"),
    AGENDAMENTOS_DIAS_SEMANA("Agendamentos por Dia da Semana"),
    SALDO_FINANCEIRO("Saldo Financeiro"),
    MEDICOS_POR_ESPECIALIDADE("Clínico por Especialidade"),

    MEDIA_TEMPO_CONSULTA("Média de Tempo de Consulta"),
    AGENDAMENTOS_MEDICO_PERIODO("Meus Agendamentos por Período");

    private final String descricao;
    
    TipoGraficoDashboard(String descricao) {
        this.descricao = descricao;
    }
    
    public String getDescricao() {
        return descricao;
    }
}
