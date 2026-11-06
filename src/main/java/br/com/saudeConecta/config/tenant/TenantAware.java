package br.com.saudeConecta.config.tenant;

public interface TenantAware {
    Long getOrganizacaoId();
    void setOrganizacaoId(Long organizacaoId);
}
