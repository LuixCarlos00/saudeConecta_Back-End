package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.dashboard.ConfiguracaoGraficoDashboard;
import br.com.saudeConecta.domain.dashboard.TipoGraficoDashboard;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.infra.tenant.TenantHelper;
import br.com.saudeConecta.infrastructure.persistence.repository.ConfiguracaoGraficoDashboardRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.OrganizacaoRepository;
import br.com.saudeConecta.presentation.dto.dashboard.AtualizarConfiguracaoGraficoRequest;
import br.com.saudeConecta.presentation.dto.dashboard.ConfiguracaoGraficoResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfiguracaoGraficoDashboardService {

    private final ConfiguracaoGraficoDashboardRepository configuracaoRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final TenantHelper tenantHelper;

    public List<ConfiguracaoGraficoResponse> listarConfiguracoes() {
        Long organizacaoId = tenantHelper.getCurrentTenantId();
        log.debug("Listando configurações de gráficos para organização: {}", organizacaoId);
        
        List<ConfiguracaoGraficoDashboard> configuracoes = 
            configuracaoRepository.findByOrganizacaoIdOrderByOrdemExibicaoAsc(organizacaoId);
        
        return configuracoes.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public List<ConfiguracaoGraficoResponse> listarGraficosAtivos() {
        Long organizacaoId = tenantHelper.getCurrentTenantId();
        log.debug("Listando gráficos ativos para organização: {}", organizacaoId);
        
        List<ConfiguracaoGraficoDashboard> configuracoes = 
            configuracaoRepository.findByOrganizacaoIdAndAtivoTrueOrderByOrdemExibicaoAsc(organizacaoId);
        
        return configuracoes.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public ConfiguracaoGraficoResponse atualizarConfiguracao(Long id, AtualizarConfiguracaoGraficoRequest request) {
        Long organizacaoId = tenantHelper.getCurrentTenantId();
        log.info("Atualizando configuração de gráfico ID: {} para organização: {}", id, organizacaoId);
        
        ConfiguracaoGraficoDashboard config = configuracaoRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Configuração não encontrada"));
        
        if (!config.getOrganizacao().getId().equals(organizacaoId)) {
            throw new IllegalArgumentException("Configuração não pertence à organização atual");
        }
        
        config.setAtivo(request.ativo());
        if (request.ordemExibicao() != null) {
            config.setOrdemExibicao(request.ordemExibicao());
        }
        
        ConfiguracaoGraficoDashboard saved = configuracaoRepository.save(config);
        log.info("Configuração atualizada com sucesso: {}", saved.getId());
        
        return toResponse(saved);
    }

    @Transactional
    public List<ConfiguracaoGraficoResponse> atualizarMultiplasConfiguracoes(
            List<AtualizarConfiguracaoGraficoRequest> requests) {
        Long organizacaoId = tenantHelper.getCurrentTenantId();
        log.info("Atualizando múltiplas configurações para organização: {}", organizacaoId);
        
        return requests.stream()
            .map(request -> {
                ConfiguracaoGraficoDashboard config = 
                    configuracaoRepository.findByOrganizacaoIdAndTipoGrafico(
                        organizacaoId, request.tipoGrafico())
                    .orElseThrow(() -> new EntityNotFoundException(
                        "Configuração não encontrada para tipo: " + request.tipoGrafico()));
                
                config.setAtivo(request.ativo());
                if (request.ordemExibicao() != null) {
                    config.setOrdemExibicao(request.ordemExibicao());
                }
                
                return configuracaoRepository.save(config);
            })
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public void inicializarConfiguracoesParaOrganizacao(Long organizacaoId) {
        log.info("Inicializando configurações de gráficos para organização: {}", organizacaoId);
        
        Organizacao organizacao = organizacaoRepository.findById(organizacaoId)
            .orElseThrow(() -> new EntityNotFoundException("Organização não encontrada"));
        
        int ordem = 1;
        for (TipoGraficoDashboard tipo : TipoGraficoDashboard.values()) {
            if (!configuracaoRepository.existsByOrganizacaoIdAndTipoGrafico(organizacaoId, tipo)) {
                ConfiguracaoGraficoDashboard config = ConfiguracaoGraficoDashboard.builder()
                    .organizacao(organizacao)
                    .tipoGrafico(tipo)
                    .ativo(true)
                    .ordemExibicao(ordem++)
                    .build();
                
                configuracaoRepository.save(config);
                log.debug("Configuração criada para tipo: {}", tipo);
            }
        }
    }

    @Transactional
    public void resetarConfiguracoesParaPadrao() {
        Long organizacaoId = tenantHelper.getCurrentTenantId();
        log.info("Resetando configurações para padrão - organização: {}", organizacaoId);
        
        List<ConfiguracaoGraficoDashboard> configuracoes = 
            configuracaoRepository.findByOrganizacaoIdOrderByOrdemExibicaoAsc(organizacaoId);
        
        int ordem = 1;
        for (ConfiguracaoGraficoDashboard config : configuracoes) {
            config.setAtivo(true);
            config.setOrdemExibicao(ordem++);
            configuracaoRepository.save(config);
        }
    }

    @Transactional
    public List<ConfiguracaoGraficoResponse> inicializarConfiguracoesPrimeiroAcesso() {
        Long organizacaoId = tenantHelper.getCurrentTenantId();
        log.info("Inicializando configurações de gráficos para primeiro acesso - organização: {}", organizacaoId);
        
        Organizacao organizacao = organizacaoRepository.findById(organizacaoId)
            .orElseThrow(() -> new EntityNotFoundException("Organização não encontrada"));
        
        int ordem = 1;
        for (TipoGraficoDashboard tipo : TipoGraficoDashboard.values()) {
            if (!configuracaoRepository.existsByOrganizacaoIdAndTipoGrafico(organizacaoId, tipo)) {
                ConfiguracaoGraficoDashboard config = ConfiguracaoGraficoDashboard.builder()
                    .organizacao(organizacao)
                    .tipoGrafico(tipo)
                    .ativo(false) // Inicia desativado para o usuário ativar
                    .ordemExibicao(ordem++)
                    .build();
                
                configuracaoRepository.save(config);
                log.debug("Configuração criada (desativada) para tipo: {}", tipo);
            }
        }
        
        // Retorna todas as configurações criadas
        return listarConfiguracoes();
    }

    private ConfiguracaoGraficoResponse toResponse(ConfiguracaoGraficoDashboard config) {
        return new ConfiguracaoGraficoResponse(
            config.getId(),
            config.getTipoGrafico(),
            config.getTipoGrafico().getDescricao(),
            config.getAtivo(),
            config.getOrdemExibicao()
        );
    }
}
