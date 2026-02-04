package br.com.saudeConecta.infra.tenant;

public interface TenantAware {
    Long getOrganizacaoId();
    void setOrganizacaoId(Long organizacaoId);
}
