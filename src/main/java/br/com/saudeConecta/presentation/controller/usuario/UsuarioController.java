package br.com.saudeConecta.presentation.controller.usuario;

import br.com.saudeConecta.application.usuario.UsuarioService;
import br.com.saudeConecta.application.usuario.dto.BloquearUsuarioRequest;
import br.com.saudeConecta.application.usuario.dto.TodosUsuariosAgrupadosResponse;
import br.com.saudeConecta.application.usuario.dto.UsuarioPerfilCompletoResponse;
import br.com.saudeConecta.config.tenant.TenantContext;
import br.com.saudeConecta.config.tenant.TenantHelper;
import jakarta.validation.Valid;
import jdk.jfr.Description;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuario")
@RequiredArgsConstructor
@Slf4j
@Description(  "Endpoints para gerenciamento de usuários")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final TenantHelper tenantHelper;

    @PutMapping("/bloquearUsuariobyOrg")
    @PreAuthorize("hasRole('ADMIN')")
    @Description("Bloqueia ou desbloqueia usuário por ID. Utilizado em: GerenciamentoUsuariosComponent")
    public ResponseEntity<Void> bloquearUsuariobyOrg(@RequestBody @Valid BloquearUsuarioRequest request) {
        // LimitePlanoException (422) e erros inesperados (500) ficam com o GlobalExceptionHandler
        try {
            usuarioService.bloquearUsuariobyOrg( request );
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/buscarTodosAgrupados")
    @Description("Lista todos os usuários agrupados por tipo (paciente, medico, secretaria, administrador). Utilizado em: TabelaTodosUsuariosComponent")
    public ResponseEntity<TodosUsuariosAgrupadosResponse> buscarTodosAgrupados() {
        Long organizacaoId = TenantContext.getCurrentTenant();

        if (organizacaoId == null) {
            log.debug("usuario.agrupados buscar escopo=SUPER_ADMIN");
            return ResponseEntity.ok(usuarioService.buscarTodosAdminOrgsSuperAdmin());
        }

        return ResponseEntity.ok(usuarioService.buscarTodosAgrupados(organizacaoId));
    }

    @PutMapping("/trocarSenharUsuariobyOrg/{id}")
    @Description("Troca a senha do usuário por ID. Utilizado em: TrocaSenhaUsuariosComponent")
    public ResponseEntity<Void> trocarSenharUsuariobyOrg(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        try {
            String novaSenha = body.get("senhaNova");
            if (novaSenha == null || novaSenha.isBlank()) {
                return ResponseEntity.badRequest().build();
            }
            usuarioService.trocarSenharUsuariobyOrg(id, novaSenha);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            // TenantAccessDeniedException (403) e erros inesperados (500) ficam com o GlobalExceptionHandler
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/buscarPerfilCompleto/{id}")
    @Description( "Busca perfil completo do usuário por ID com dados específicos baseados no tipo (CLINICO, GESTOR, ASSISTENTE) incluindo endereço quando existir. Utilizado em: UserProfileComponent, UserService")
    public ResponseEntity<UsuarioPerfilCompletoResponse> buscarPerfilCompleto(@PathVariable Long id) {
        // Checado depois do service: o @Cacheable usa só o ID na chave e responderia do cache sem
        // olhar a organização. Usuário de outra clínica responde 404, como os demais IDOR.
        boolean superAdmin = tenantHelper.isSuperAdmin();
        Long organizacaoId = tenantHelper.getCurrentTenantIdOrNull();
        return usuarioService.buscarPerfilCompleto(id)
                .filter(perfil -> superAdmin
                        || (organizacaoId != null && organizacaoId.equals(perfil.organizacaoId())))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

 

}
