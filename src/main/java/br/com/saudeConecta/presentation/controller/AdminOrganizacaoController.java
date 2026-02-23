package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.service.AdminOrganizacaoService;
import br.com.saudeConecta.domain.admin.AdminOrganizacao;
import br.com.saudeConecta.infra.tenant.TenantContext;
import br.com.saudeConecta.presentation.dto.admin.AtualizarAdminRequest;
import br.com.saudeConecta.presentation.dto.admin.CadastrarAdminOrgCompletoRequest;
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

    @GetMapping("/buscarrAdminByOrg/{id}")
    @Transactional
    @Description("Busca administrador por ID. Utilizado em: VisualizarEditarUsuarioComponent")
    public ResponseEntity<AdminOrganizacao> buscarrAdminByOrg(@PathVariable Long id) {
        log.debug("Buscando administrador por ID: {}", id);
        return adminOrganizacaoService.buscarrAdminByOrg(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/cadastrarAdminByOrg")
    @Transactional
    @Description("Cadastra novo administrador de uma organização . Utilizado em: CadastroAdmComponent")
    public ResponseEntity<?> cadastrarAdminByOrg(@RequestBody @Valid CadastrarAdminRequest request) {
        Long organizacaoId = TenantContext.getCurrentTenant();
        log.debug("Cadastrando administrador : {}", request.nome());

        if (organizacaoId == null) {
            log.warn("Organização não encontrada no contexto");
            return ResponseEntity.badRequest().body("Organização não identificada");
        }

        try {
            AdminOrganizacao admin = adminOrganizacaoService.cadastrarAdminByOrg(request, organizacaoId);
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
            AdminOrganizacao admin = adminOrganizacaoService.atualizarAdmByOrg(id, request.nome(), request.email());
            return ResponseEntity.ok(admin);
        } catch (IllegalArgumentException e) {
            log.warn("Erro ao atualizar administrador: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }


    @PostMapping("/cadastrarAdminOrgCompleto")
    @Transactional
    @Description("Cadastra Admin de Organização completo (com organização e endereço). Exclusivo para SUPER_ADMIN.")
    public ResponseEntity<?> cadastrarAdminOrgCompleto(@RequestBody @Valid CadastrarAdminOrgCompletoRequest request) {
        log.info("Cadastrando Admin Org completo: {}", request.nomeClinica());
        try {
            AdminOrganizacao admin = adminOrganizacaoService.cadastrarAdminOrgCompleto(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(admin);
        } catch (IllegalStateException e) {
            log.warn("Conflito ao cadastrar Admin Org: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Erro de validação ao cadastrar Admin Org: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/deletarAdmByOrg/{id}")
    @Transactional
    @Description("Deleta administrador por ID")
    public ResponseEntity<?> deletarAdmByOrg(@PathVariable Long id) {
        log.debug("Deletando administrador ID: {}", id);
        try {
            adminOrganizacaoService.deletarAdmByOrg(id);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            log.warn("Erro ao deletar administrador: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Administrador não encontrado: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }


}
