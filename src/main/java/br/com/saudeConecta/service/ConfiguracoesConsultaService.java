package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.consulta.ConfiguracoesConsulta;
import br.com.saudeConecta.infrastructure.persistence.repository.ConfiguracoesConsultaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.OrganizacaoRepository;
import br.com.saudeConecta.infra.tenant.TenantContext;
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

    /**
     * Busca a configuração de fluxo de consulta da organização atual.
     * Se não existir, retorna a configuração padrão (pularParaAgendado = false).
     *
     * @return ConfiguracoesConsulta ou configuração padrão
     */
    @Transactional(readOnly = true)
    public ConfiguracoesConsulta buscarConfiguracaoPorOrganizacao(Long organizacaoId) {
        Optional<ConfiguracoesConsulta> config = configuracoesConsultaRepository.findByOrganizacaoId(organizacaoId);
        
        if (config.isEmpty()) {
            log.info("Configuracao de fluxo nao encontrada para organizacao {}, retornando padrao", organizacaoId);
            return ConfiguracoesConsulta.builder()
                    .pularParaAgendado(false)
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
            log.warn("Organizacao nao identificada no contexto, retornando configuracao padrao");
            return ConfiguracoesConsulta.builder()
                    .pularParaAgendado(false)
                    .descricao("Configuração padrão - fluxo normal")
                    .build();
        }
        return buscarConfiguracaoPorOrganizacao(organizacaoId);
    }

    /**
     * Cria uma nova configuração de fluxo de consulta para a organização.
     *
     * @param organizacaoId ID da organização
     * @param pularParaAgendado Se deve pular para status AGENDADO
     * @param descricao Descrição da configuração
     * @return ConfiguracoesConsulta criada
     */
    @Transactional
    public ConfiguracoesConsulta criarConfiguracao(Long organizacaoId, Boolean pularParaAgendado, String descricao) {
        if (configuracoesConsultaRepository.existsByOrganizacaoId(organizacaoId)) {
            throw new IllegalStateException("Já existe uma configuração de fluxo para esta organização");
        }

        var organizacao = organizacaoRepository.findById(organizacaoId)
                .orElseThrow(() -> new EntityNotFoundException("Organização não encontrada"));

        ConfiguracoesConsulta config = ConfiguracoesConsulta.builder()
                .organizacao(organizacao)
                .pularParaAgendado(pularParaAgendado != null ? pularParaAgendado : false)
                .descricao(descricao != null ? descricao : "Configuração de fluxo de consulta")
                .build();

        return configuracoesConsultaRepository.save(config);
    }

    /**
     * Atualiza uma configuração de fluxo de consulta existente.
     *
     * @param id ID da configuração
     * @param pularParaAgendado Se deve pular para status AGENDADO
     * @param descricao Descrição da configuração
     * @return ConfiguracoesConsulta atualizada
     */
    @Transactional
    public ConfiguracoesConsulta atualizarConfiguracao(Long id, Boolean pularParaAgendado, String descricao) {
        ConfiguracoesConsulta config = configuracoesConsultaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Configuração de fluxo não encontrada"));

        if (pularParaAgendado != null) {
            config.setPularParaAgendado(pularParaAgendado);
        }
        if (descricao != null) {
            config.setDescricao(descricao);
        }

        return configuracoesConsultaRepository.save(config);
    }

    /**
     * Atualiza a configuração de fluxo de consulta da organização atual.
     *
     * @param pularParaAgendado Se deve pular para status AGENDADO
     * @param descricao Descrição da configuração
     * @return ConfiguracoesConsulta atualizada ou criada
     */
    @Transactional
    public ConfiguracoesConsulta atualizarConfiguracaoAtual(Boolean pularParaAgendado, String descricao) {
        Long organizacaoId = TenantContext.getCurrentTenant();
        if (organizacaoId == null) {
            throw new IllegalStateException("Organização não identificada no contexto");
        }

        Optional<ConfiguracoesConsulta> configOpt = configuracoesConsultaRepository.findByOrganizacaoId(organizacaoId);

        if (configOpt.isEmpty()) {
            return criarConfiguracao(organizacaoId, pularParaAgendado, descricao);
        }

        ConfiguracoesConsulta config = configOpt.get();
        if (pularParaAgendado != null) {
            config.setPularParaAgendado(pularParaAgendado);
        }
        if (descricao != null) {
            config.setDescricao(descricao);
        }

        return configuracoesConsultaRepository.save(config);
    }

    /**
     * Verifica se a organização atual deve pular para o status AGENDADO.
     *
     * @return true se deve pular para AGENDADO, false caso contrário
     */
    @Transactional(readOnly = true)
    public boolean devePularParaAgendado() {
        ConfiguracoesConsulta config = buscarConfiguracaoAtual();
        return config.getPularParaAgendado();
    }
}
