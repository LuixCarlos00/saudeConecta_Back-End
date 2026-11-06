package br.com.saudeConecta.application.consulta;

import br.com.saudeConecta.config.tenant.TenantContext;
import br.com.saudeConecta.config.tenant.TenantHelper;
import br.com.saudeConecta.domain.consulta.ConfiguracoesConsulta;
import br.com.saudeConecta.infrastructure.persistence.repository.ConfiguracoesConsultaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.OrganizacaoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfiguracoesConsultaService {

    private final ConfiguracoesConsultaRepository configuracoesConsultaRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final TenantHelper tenantHelper;

    /**
     * Busca a configuração de fluxo de consulta da organização atual.
     * Se não existir, retorna a configuração padrão (pularParaConfirmado = false).
     *
     * @return ConfiguracoesConsulta ou configuração padrão
     */
    @Transactional(readOnly = true)
    public ConfiguracoesConsulta buscarConfiguracaoPorOrganizacao(Long organizacaoId) {
        tenantHelper.validateTenant(organizacaoId);
        Optional<ConfiguracoesConsulta> config = configuracoesConsultaRepository.findByOrganizacaoId(organizacaoId);
        
        if (config.isEmpty()) {
            log.debug("consulta.configuracao buscar organizacaoId={} resultado=PADRAO", organizacaoId);
            return ConfiguracoesConsulta.builder()
                    .pularParaConfirmado(false)
                    .descricao("Configuração padrão - fluxo normal")
                    .build();
        }
        
        return config.get();
    }

    /**
     * Busca a configuração de fluxo de consulta da organização do contexto atual.
     *
     * @return ConfiguracoesConsulta ou configuração padrão
     */
    @Transactional(readOnly = true)
    public ConfiguracoesConsulta buscarConfiguracaoAtual() {
        Long organizacaoId = TenantContext.getCurrentTenant();
        if (organizacaoId == null) {
            log.warn("consulta.configuracao negado motivo=SEM_ORGANIZACAO acao=USA_PADRAO");
            return ConfiguracoesConsulta.builder()
                    .pularParaConfirmado(false)
                    .descricao("Configuração padrão - fluxo normal")
                    .build();
        }
        return buscarConfiguracaoPorOrganizacao(organizacaoId);
    }

    /**
     * Cria uma nova configuração de fluxo de consulta para a organização.
     *
     * @param organizacaoId ID da organização
     * @param pularParaConfirmado Se deve pular para status CONFIRMADA
     * @param descricao Descrição da configuração
     * @return ConfiguracoesConsulta criada
     */
    @Transactional
    public ConfiguracoesConsulta criarConfiguracao(Long organizacaoId, Boolean pularParaConfirmado, String descricao) {
        tenantHelper.validateTenant(organizacaoId);
        if (configuracoesConsultaRepository.existsByOrganizacaoId(organizacaoId)) {
            throw new IllegalStateException("Já existe uma configuração de fluxo para esta organização");
        }

        var organizacao = organizacaoRepository.findById(organizacaoId)
                .orElseThrow(() -> new EntityNotFoundException("Organização não encontrada"));

        ConfiguracoesConsulta config = ConfiguracoesConsulta.builder()
                .organizacao(organizacao)
                .pularParaConfirmado(Boolean.TRUE.equals(pularParaConfirmado))
                .descricao(descricao != null ? descricao : "Configuração de fluxo de consulta")
                .build();

        return configuracoesConsultaRepository.save(config);
    }

    /**
     * Atualiza a configuração de fluxo de consulta da organização atual.
     *
     * @param pularParaConfirmado Se deve pular para status CONFIRMADA
     * @param descricao Descrição da configuração
     * @return ConfiguracoesConsulta atualizada ou criada
     */
    @Transactional
    public ConfiguracoesConsulta atualizarConfiguracaoAtual(Boolean pularParaConfirmado, String descricao) {
        Long organizacaoId = TenantContext.getCurrentTenant();
        if (organizacaoId == null) {
            throw new IllegalStateException("Organização não identificada no contexto");
        }

        Optional<ConfiguracoesConsulta> configOpt = configuracoesConsultaRepository.findByOrganizacaoId(organizacaoId);

        if (configOpt.isEmpty()) {
            return criarConfiguracao(organizacaoId, pularParaConfirmado, descricao);
        }

        ConfiguracoesConsulta config = configOpt.get();
        if (pularParaConfirmado != null) {
            config.setPularParaConfirmado(pularParaConfirmado);
        }
        if (descricao != null) {
            config.setDescricao(descricao);
        }

        return configuracoesConsultaRepository.save(config);
    }

    /**
     * Verifica se a organização atual deve pular para o status CONFIRMADA.
     *
     * @return true se deve pular para CONFIRMADA, false caso contrário
     */
    @Transactional(readOnly = true)
    public boolean devePularParaConfirmado() {
        ConfiguracoesConsulta config = buscarConfiguracaoAtual();
        return config.getPularParaConfirmado();
    }
}
