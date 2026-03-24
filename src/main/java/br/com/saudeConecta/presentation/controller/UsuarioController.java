package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.service.UsuarioService;
import br.com.saudeConecta.infra.tenant.TenantContext;
import br.com.saudeConecta.presentation.dto.usuario.BloquearUsuarioRequest;
import br.com.saudeConecta.presentation.dto.usuario.TodosUsuariosAgrupadosResponse;
import br.com.saudeConecta.presentation.dto.usuario.UsuarioPerfilCompletoResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jdk.jfr.Description;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuario")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
@Description(  "Endpoints para gerenciamento de usuários")
public class UsuarioController {

    private final UsuarioService usuarioService;



    @PutMapping("/bloquearUsuariobyOrg")
    @Description("Bloqueia ou desbloqueia usuário por ID. Utilizado em: GerenciamentoUsuariosComponent")
    public ResponseEntity<Void> bloquearUsuariobyOrg(@RequestBody @Valid BloquearUsuarioRequest request) {
        log.debug("Alterando status do usuário ID: {}", request.codigoUsuario());
        try {
            usuarioService.bloquearUsuariobyOrg( request );
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Erro ao alterar status do usuário ID: {}", request.codigoUsuario(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }




    @GetMapping("/buscarTodosAgrupados")
    @Transactional
    @Description("Lista todos os usuários agrupados por tipo (paciente, medico, secretaria, administrador). Utilizado em: TabelaTodosUsuariosComponent")
    public ResponseEntity<TodosUsuariosAgrupadosResponse> buscarTodosAgrupados() {
        Long organizacaoId = TenantContext.getCurrentTenant();

        if (organizacaoId == null) {
            log.info("SUPER_ADMIN sem organização — buscando todos os AdminOrgs do sistema");
            return ResponseEntity.ok(usuarioService.buscarTodosAdminOrgsSuperAdmin());
        }

        return ResponseEntity.ok(usuarioService.buscarTodosAgrupados(organizacaoId));
    }


    @PutMapping("/trocarSenharUsuariobyOrg/{id}")
    @Description("Troca a senha do usuário por ID. Utilizado em: TrocaSenhaUsuariosComponent")
    public ResponseEntity<Void> trocarSenharUsuariobyOrg(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        log.debug("Trocando senha do usuário ID: {} organizacao: {}", id, TenantContext.getCurrentTenant());
        try {
            String novaSenha = body.get("senhaNova");
            if (novaSenha == null || novaSenha.isBlank()) {
                return ResponseEntity.badRequest().build();
            }
            usuarioService.trocarSenharUsuariobyOrg(id, novaSenha);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Erro ao trocar senha do usuário ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @GetMapping("/buscarPerfilCompleto/{id}")
    @Transactional
    @Description( "Busca perfil completo do usuário por ID com dados específicos baseados no tipo (PROFISSIONAL, ADMIN_ORG, RECEPCIONISTA) incluindo endereço quando existir. Utilizado em: UserProfileComponent, UserService")
    public ResponseEntity<UsuarioPerfilCompletoResponse> buscarPerfilCompleto(@PathVariable Long id) {
        log.debug("Buscando perfil completo do usuario por ID: {}", id);
        return usuarioService.buscarPerfilCompleto(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }







 


}
