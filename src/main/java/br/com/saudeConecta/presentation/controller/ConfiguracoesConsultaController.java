package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.config.tenant.TenantContext;
import br.com.saudeConecta.domain.consulta.ConfiguracoesConsulta;
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
        ConfiguracoesConsulta config = configuracoesConsultaService.buscarConfiguracaoAtual();
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
        ConfiguracoesConsulta config = configuracoesConsultaService.atualizarConfiguracaoAtual(
                request.getPularParaConfirmado(),
                request.getDescricao()
        );
        return ResponseEntity.ok(toResponse(config));
    }

    private ConfiguracoesConsultaResponse toResponse(ConfiguracoesConsulta config) {
        return ConfiguracoesConsultaResponse.builder()
                .id(config.getId())
                .organizacaoId(config.getOrganizacao() != null ? config.getOrganizacao().getId() : null)
                .pularParaConfirmado(config.getPularParaConfirmado())
                .descricao(config.getDescricao())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}
