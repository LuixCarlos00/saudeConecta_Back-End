package br.com.saudeConecta.presentation.controller.organizacao;

import br.com.saudeConecta.application.organizacao.AdminOrganizacaoService;
import br.com.saudeConecta.application.organizacao.dto.AdminOrgCompletoResponse;
import br.com.saudeConecta.application.organizacao.dto.AtualizarAdminOrgCompletoRequest;
import br.com.saudeConecta.application.organizacao.dto.AtualizarAdminRequest;
import br.com.saudeConecta.application.organizacao.dto.CadastrarAdminOrgCompletoRequest;
import br.com.saudeConecta.application.organizacao.dto.CadastrarAdminRequest;
import br.com.saudeConecta.config.tenant.TenantContext;
import br.com.saudeConecta.domain.admin.AdminOrganizacao;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jdk.jfr.Description;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/administrador")
@RequiredArgsConstructor
@Slf4j
@Description("Endpoints para gerenciamento de administradores de organização")
public class AdminOrganizacaoController {

    private final AdminOrganizacaoService adminOrganizacaoService;

    @GetMapping("/buscarrAdminByOrg/{id}")
    @Transactional
    @Description("Busca administrador por ID. Utilizado em: VisualizarEditarUsuarioComponent")
    public ResponseEntity<AdminOrganizacao> buscarrAdminByOrg(@PathVariable Long id) {
        return adminOrganizacaoService.buscarrAdminByOrg(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/cadastrarAdminByOrg")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Description("Cadastra novo administrador de uma organização . Utilizado em: CadastroAdmComponent")
    public ResponseEntity<Object> cadastrarAdminByOrg(@RequestBody @Valid CadastrarAdminRequest request) {
        Long organizacaoId = TenantContext.getCurrentTenant();

        if (organizacaoId == null) {
            log.warn("adminOrg.cadastrar negado motivo=SEM_ORGANIZACAO");
            return ResponseEntity.badRequest().body("Organização não identificada");
        }

        try {
            AdminOrganizacao admin = adminOrganizacaoService.cadastrarAdminByOrg(request, organizacaoId);
            return ResponseEntity.status(HttpStatus.CREATED).body(admin);
        } catch (IllegalArgumentException e) {
            log.warn("adminOrg.cadastrar negado motivo=VALIDACAO organizacaoId={}", organizacaoId);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    @PutMapping("/atualizarAdmByOrg/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Description("Atualiza nome e email do administrador. Utilizado em: VisualizarEditarUsuarioComponent")
    public ResponseEntity<AdminOrganizacao> atualizarAdmByOrg(@PathVariable Long id, @RequestBody AtualizarAdminRequest request) {
        try {
            AdminOrganizacao admin = adminOrganizacaoService.atualizarAdmByOrg(id, request.nome(), request.email());
            return ResponseEntity.ok(admin);
        } catch (IllegalArgumentException e) {
            log.warn("adminOrg.atualizar negado motivo=NAO_ENCONTRADO adminId={}", id);
            return ResponseEntity.notFound().build();
        }
    }


    @PostMapping("/cadastrarAdminOrgCompleto")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    @Description("Cadastra Admin de Organização completo (com organização e endereço). Exclusivo para SUPER_ADMIN.")
    public ResponseEntity<Object> cadastrarAdminOrgCompleto(@RequestBody @Valid CadastrarAdminOrgCompletoRequest request) {
        try {
            AdminOrganizacao admin = adminOrganizacaoService.cadastrarAdminOrgCompleto(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(admin);
        } catch (IllegalArgumentException e) {
            log.warn("adminOrg.cadastrarCompleto negado motivo=VALIDACAO detalhe={}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/buscarAdminOrgCompleto/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    @Description("Busca dados completos do AdminOrg (admin + organização + endereço). Exclusivo para SUPER_ADMIN.")
    public ResponseEntity<AdminOrgCompletoResponse> buscarAdminOrgCompleto(@PathVariable Long id) {
        try {
            AdminOrgCompletoResponse response = adminOrganizacaoService.buscarAdminOrgCompleto(id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("adminOrg.buscarCompleto negado motivo=NAO_ENCONTRADO adminId={}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/atualizarAdminOrgCompleto/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    @Description("Atualiza dados completos do AdminOrg (admin + organização + endereço). Exclusivo para SUPER_ADMIN.")
    public ResponseEntity<AdminOrgCompletoResponse> atualizarAdminOrgCompleto(@PathVariable Long id, @RequestBody AtualizarAdminOrgCompletoRequest request) {
        try {
            AdminOrgCompletoResponse response = adminOrganizacaoService.atualizarAdminOrgCompleto(id, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("adminOrg.atualizarCompleto negado motivo=NAO_ENCONTRADO adminId={}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/atualizarMeusDados/{id}")
    @Transactional
    @Description("Atualiza dados pessoais do Admin Org logado + organização + endereço. Utilizado em: DadosPessoaisComponent")
    public ResponseEntity<Object> atualizarMeusDados(@PathVariable Long id, @RequestBody AtualizarAdminOrgCompletoRequest request) {
        try {
            adminOrganizacaoService.atualizarMeusDadosAdminOrg(id, request);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("adminOrg.atualizarMeusDados negado motivo=VALIDACAO adminId={}", id);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/deletarAdmByOrg/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Description("Deleta administrador por ID")
    public ResponseEntity<Void> deletarAdmByOrg(@PathVariable Long id) {
        try {
            adminOrganizacaoService.deletarAdmByOrg(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("adminOrg.deletar negado motivo=NAO_ENCONTRADO adminId={}", id);
            return ResponseEntity.notFound().build();
        }
    }


}
