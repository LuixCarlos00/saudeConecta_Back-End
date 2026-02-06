package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.application.service.AdminOrganizacaoService;
import br.com.saudeConecta.domain.admin.AdminOrganizacao;
import br.com.saudeConecta.infra.tenant.TenantContext;
import br.com.saudeConecta.presentation.dto.admin.AtualizarAdminRequest;
import br.com.saudeConecta.presentation.dto.admin.CadastrarAdminRequest;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jdk.jfr.Description;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/administrador")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
@Description("Endpoints para gerenciamento de administradores de organização")
public class AdminOrganizacaoController {

    private final AdminOrganizacaoService adminOrganizacaoService;

    @GetMapping("/buscarId/{id}")
    @Transactional
    @Description("Busca administrador por ID. Utilizado em: VisualizarEditarUsuarioComponent")
    public ResponseEntity<AdminOrganizacao> buscarPorId(@PathVariable Long id) {
        log.debug("Buscando administrador por ID: {}", id);
        return adminOrganizacaoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/cadastrarAdminByOrg")
    @Transactional
    @Description("Cadastra novo administrador de uma organização . Utilizado em: CadastroAdmComponent")
    public ResponseEntity<?> cadastrarAdminByOrg(@RequestBody @Valid CadastrarAdminRequest request) {
        Long organizacaoId = TenantContext.getCurrentTenant();
        log.debug("Cadastrando administrador para organização ID: {}", organizacaoId);

        if (organizacaoId == null) {
            log.warn("Organização não encontrada no contexto");
            return ResponseEntity.badRequest().body("Organização não identificada");
        }

        try {
            AdminOrganizacao admin = adminOrganizacaoService.cadastrar(request, organizacaoId);
            return ResponseEntity.status(HttpStatus.CREATED).body(admin);
        } catch (IllegalStateException e) {
            log.warn("Erro ao cadastrar administrador: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Erro de validação: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    @PutMapping("/atualizarAdmByOrg/{id}")
    @Transactional
    @Description("Atualiza nome e email do administrador. Utilizado em: VisualizarEditarUsuarioComponent")
    public ResponseEntity<?> atualizarAdmByOrg(@PathVariable Long id, @RequestBody AtualizarAdminRequest request) {
        log.debug("Atualizando administrador ID: {}", id);
        try {
            AdminOrganizacao admin = adminOrganizacaoService.atualizarAdmByOrg(id, request.admNome(), request.admEmail());
            return ResponseEntity.ok(admin);
        } catch (IllegalArgumentException e) {
            log.warn("Erro ao atualizar administrador: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/bloquerAdminByOrg/{id}")
    @Transactional
    @Description("Bloqueia/Desbloqueia administrador por ID. Utilizado em: TabelaTodosUsuariosComponent")
    public ResponseEntity<Void> bloquearAdminByOrg(@PathVariable Long id) {
        log.debug("Bloqueando/Desbloqueando administrador ID: {}", id);
        try {
            adminOrganizacaoService.bloquearAdminByOrg(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("Erro ao bloquear/desbloquear administrador: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }


}
