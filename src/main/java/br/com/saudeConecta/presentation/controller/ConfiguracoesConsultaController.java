package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.domain.consulta.ConfiguracoesConsulta;
import br.com.saudeConecta.infra.tenant.TenantContext;
import br.com.saudeConecta.presentation.dto.consulta.ConfiguracoesConsultaRequest;
import br.com.saudeConecta.presentation.dto.consulta.ConfiguracoesConsultaResponse;
import br.com.saudeConecta.service.ConfiguracoesConsultaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/configuracoes-consulta")
@RequiredArgsConstructor
@Slf4j
public class ConfiguracoesConsultaController {

    private final ConfiguracoesConsultaService configuracoesConsultaService;

    /**
     * Busca a configuração de fluxo de consulta da organização atual.
     *
     * @return ConfiguracoesConsultaResponse
     */
    @GetMapping("/atual")
    public ResponseEntity<ConfiguracoesConsultaResponse> buscarConfiguracaoAtual() {
        log.info("Buscando configuracao de fluxo de consulta da organizacao atual");
        ConfiguracoesConsulta config = configuracoesConsultaService.buscarConfiguracaoAtual();
        return ResponseEntity.ok(toResponse(config));
    }

    /**
     * Busca a configuração de fluxo de consulta por organização ID.
     *
     * @param organizacaoId ID da organização
     * @return ConfiguracoesConsultaResponse
     */
    @GetMapping("/organizacao/{organizacaoId}")
    public ResponseEntity<ConfiguracoesConsultaResponse> buscarConfiguracaoPorOrganizacao(
            @PathVariable Long organizacaoId) {
        log.info("Buscando configuracao de fluxo de consulta para organizacao {}", organizacaoId);
        ConfiguracoesConsulta config = configuracoesConsultaService.buscarConfiguracaoPorOrganizacao(organizacaoId);
        return ResponseEntity.ok(toResponse(config));
    }

    /**
     * Cria uma nova configuração de fluxo de consulta.
     *
     * @param organizacaoId ID da organização
     * @param request Dados da configuração
     * @return ConfiguracoesConsultaResponse
     */
    @PostMapping("/organizacao/{organizacaoId}")
    public ResponseEntity<ConfiguracoesConsultaResponse> criarConfiguracao(
            @PathVariable Long organizacaoId,
            @Valid @RequestBody ConfiguracoesConsultaRequest request) {
        log.info("Criando configuracao de fluxo de consulta para organizacao {}", organizacaoId);
        ConfiguracoesConsulta config = configuracoesConsultaService.criarConfiguracao(
                organizacaoId,
                request.getPularParaAgendado(),
                request.getDescricao()
        );
        return ResponseEntity.ok(toResponse(config));
    }

    /**
     * Atualiza uma configuração de fluxo de consulta existente.
     *
     * @param id ID da configuração
     * @param request Dados da configuração
     * @return ConfiguracoesConsultaResponse
     */
    @PutMapping("/{id}")
    public ResponseEntity<ConfiguracoesConsultaResponse> atualizarConfiguracao(
            @PathVariable Long id,
            @Valid @RequestBody ConfiguracoesConsultaRequest request) {
        log.info("Atualizando configuracao de fluxo de consulta {}", id);
        ConfiguracoesConsulta config = configuracoesConsultaService.atualizarConfiguracao(
                id,
                request.getPularParaAgendado(),
                request.getDescricao()
        );
        return ResponseEntity.ok(toResponse(config));
    }

    /**
     * Atualiza a configuração de fluxo de consulta da organização atual.
     *
     * @param request Dados da configuração
     * @return ConfiguracoesConsultaResponse
     */
    @PutMapping("/atual")
    public ResponseEntity<ConfiguracoesConsultaResponse> atualizarConfiguracaoAtual(
            @Valid @RequestBody ConfiguracoesConsultaRequest request) {
        Long organizacaoId = TenantContext.getCurrentTenant();
        log.info("Atualizando configuracao de fluxo de consulta da organizacao atual {}", organizacaoId);
        ConfiguracoesConsulta config = configuracoesConsultaService.atualizarConfiguracaoAtual(
                request.getPularParaAgendado(),
                request.getDescricao()
        );
        return ResponseEntity.ok(toResponse(config));
    }

    /**
     * Verifica se a organização atual deve pular para o status AGENDADO.
     *
     * @return true se deve pular para AGENDADO, false caso contrário
     */
    @GetMapping("/deve-pular-agendado")
    public ResponseEntity<Boolean> devePularParaAgendado() {
        log.info("Verificando se organizacao atual deve pular para AGENDADO");
        boolean devePular = configuracoesConsultaService.devePularParaAgendado();
        return ResponseEntity.ok(devePular);
    }

    private ConfiguracoesConsultaResponse toResponse(ConfiguracoesConsulta config) {
        return ConfiguracoesConsultaResponse.builder()
                .id(config.getId())
                .organizacaoId(config.getOrganizacao() != null ? config.getOrganizacao().getId() : null)
                .pularParaAgendado(config.getPularParaAgendado())
                .descricao(config.getDescricao())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}
