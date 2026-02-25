package br.com.saudeConecta.domain.dashboard;

public enum TipoCardDashboard {

    // ── Cards do AdminOrg ────────────────────────────────────────────────────
    CONSULTAS_HOJE("Total de Consultas Hoje"),
    CONSULTAS_ATENDIDAS("Atendidos"),
    CONSULTAS_AGUARDANDO("Aguardando"),
    MEDICOS_ATIVOS("Clínicos Ativos"),
    CONSULTAS_SEMANA("Esta Semana"),
    CANCELADOS_SEMANA("Cancelados (Semana)"),
    CONFIRMADOS_SEMANA("Confirmados (Semana)"),

    // ── Cards do SuperAdmin (esqueleto — implementação futura) ────────────────
    CONSULTAS_HOJE_GLOBAL("Total de Consultas Hoje (Global)"),
    CONSULTAS_SEMANA_GLOBAL("Consultas na Semana (Global)"),
    MEDICOS_ATIVOS_GLOBAL("Clínicos Ativos (Global)"),

    // ── Cards do Profissional (esqueleto — implementação futura) ─────────────
    MINHAS_CONSULTAS_HOJE("Minhas Consultas Hoje"),
    MINHAS_CONSULTAS_ATENDIDAS("Minhas Consultas Atendidas"),
    MINHAS_CONSULTAS_AGUARDANDO("Minhas Consultas Aguardando"),
    MINHAS_CONSULTAS_SEMANA("Minhas Consultas na Semana");

    private final String descricao;

    TipoCardDashboard(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
