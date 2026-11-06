package br.com.saudeConecta.presentation.controller.profissional;

import br.com.saudeConecta.application.profissional.ProfissionalService;
import br.com.saudeConecta.application.profissional.dto.AtualizarClinicoRequest;
import br.com.saudeConecta.application.profissional.dto.CadastrarClinicoRequest;
import br.com.saudeConecta.application.profissional.dto.ProfissionalResponse;
import br.com.saudeConecta.config.tenant.TenantHelper;
import br.com.saudeConecta.domain.profissional.Profissional;
import jakarta.validation.Valid;
import jdk.jfr.Description;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/profissionais")
@RequiredArgsConstructor
@Slf4j
public class ProfissionalController {
    
    private final ProfissionalService profissionalService;
    private final TenantHelper tenantHelper;

    @PutMapping("/atualizarClinicoIdByOrg/{id}")
    @Description("Atualiza médico/clínico por ID dentro da organização. Gestor ou o próprio clínico (regra no service)")
    public ResponseEntity<ProfissionalResponse> atualizarClinicoIdByOrg(@PathVariable Long id,
                                                     @Valid @RequestBody AtualizarClinicoRequest request) {
        try {
            Profissional atualizado = profissionalService.atualizarClinicoIdByOrg(id, request);
            return ResponseEntity.ok(ProfissionalResponse.fromEntity(atualizado));
        } catch (IllegalArgumentException e) {
            log.warn("profissional.atualizar negado motivo=NAO_ENCONTRADO profissionalId={}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/cadastraClinicoByOrg")
    @PreAuthorize("hasRole('ADMIN')")
    @Description("Cadastra médico/clínico com CPF como login e envia credenciais por email. Utilizado em: CadastroMedicoComponent")
    public ResponseEntity<ProfissionalResponse> cadastraClinicoByOrg(@Valid @RequestBody CadastrarClinicoRequest request) {
        Profissional profissional = profissionalService.cadastraClinicoByOrg(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ProfissionalResponse.fromEntity(profissional));
    }

    @GetMapping("/buscarClinicoIdByOrg/{id}")
    @Description("Busca médico/clínico por ID dentro da organização")
    public ResponseEntity<ProfissionalResponse> buscarClinicoIdByOrg(@PathVariable Long id) {
        return profissionalService.buscarClinicoIdByOrg(id)
                .map(ProfissionalResponse::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/deletarClinicoIdByOrg/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Description("Deleta médico/clínico por ID")
    public ResponseEntity<Void> deletarClinicoIdByOrg(@PathVariable Long id) {
        try {
            profissionalService.deletarClinicoIdByOrg(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("profissional.deletar negado motivo=NAO_ENCONTRADO profissionalId={}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/estatisticas/organizacao/{organizacaoId}/medicos-ativos")
    public ResponseEntity<Long> contarAtivosPorOrganizacao(@PathVariable Long organizacaoId) {
        // Validado antes do service: o @Cacheable usa o orgId como chave e responderia do cache sem checar.
        tenantHelper.validateTenant(organizacaoId);
        return ResponseEntity.ok(profissionalService.contarAtivosPorOrganizacao(organizacaoId));
    }

    @GetMapping("/estatisticas/medicos-ativos")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Long> getEstatisticasMedicosAtivosByOrg() {
        return ResponseEntity.ok(profissionalService.getEstatisticasMedicosAtivosByOrg());
    }

    // ==========================================
    // ESTATÍSTICAS POR ORGANIZAÇÃO
    // ==========================================

    @GetMapping("/organizacao/{organizacaoId}")
    public ResponseEntity<List<ProfissionalResponse>> listarPorOrganizacao(@PathVariable Long organizacaoId) {
        // A resposta traz CPF/RG/e-mail: só a própria clínica pode listar (validado antes do cache do service).
        tenantHelper.validateTenant(organizacaoId);
        List<Profissional> profissionais = profissionalService.buscarPorOrganizacao(organizacaoId);
        List<ProfissionalResponse> response = profissionais.stream()
                .map(ProfissionalResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    // ========== MÉTODOS DE BUSCA PARA AUTOCOMPLETE COM FILTRO OPCIONAL ==========
    
    @GetMapping
    public ResponseEntity<List<ProfissionalResponse>> buscarTodos(
            @RequestParam(required = false, defaultValue = "ALL") String filtro) {
        List<ProfissionalResponse> profissionais = profissionalService.buscarTodosComFiltro(filtro)
                .stream()
                .map(ProfissionalResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(profissionais);
    }
    
    @GetMapping("/buscar-por-nome")
    public ResponseEntity<List<ProfissionalResponse>> buscarPorNome(
            @RequestParam String nome, 
            @RequestParam(required = false, defaultValue = "ALL") String filtro) {
        List<ProfissionalResponse> profissionais = profissionalService.buscarPorNomeComFiltro(nome, filtro)
                .stream()
                .map(ProfissionalResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(profissionais);
    }
    
    @GetMapping("/buscar-por-crm")
    public ResponseEntity<List<ProfissionalResponse>> buscarPorCRM(
            @RequestParam String crm, 
            @RequestParam(required = false, defaultValue = "ALL") String filtro) {
        List<ProfissionalResponse> profissionais = profissionalService.buscarPorCRMComFiltro(crm, filtro)
                .stream()
                .map(ProfissionalResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(profissionais);
    }
    
    @GetMapping("/buscar-por-cidade")
    public ResponseEntity<List<ProfissionalResponse>> buscarPorCidade(
            @RequestParam String cidade, 
            @RequestParam(required = false, defaultValue = "ALL") String filtro) {
        List<ProfissionalResponse> profissionais = profissionalService.buscarPorCidadeComFiltro(cidade, filtro)
                .stream()
                .map(ProfissionalResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(profissionais);
    }
    
    @GetMapping("/buscar-por-especialidade")
    public ResponseEntity<List<ProfissionalResponse>> buscarPorEspecialidade(
            @RequestParam String especialidade, 
            @RequestParam(required = false, defaultValue = "ALL") String filtro) {
        List<ProfissionalResponse> profissionais = profissionalService.buscarPorEspecialidadeComFiltro(especialidade, filtro)
                .stream()
                .map(ProfissionalResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(profissionais);
    }
}
