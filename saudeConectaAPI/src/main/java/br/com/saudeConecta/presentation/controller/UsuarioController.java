package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.service.UsuarioService;
import br.com.saudeConecta.infra.tenant.TenantContext;
import br.com.saudeConecta.presentation.dto.usuario.BloquearUsuarioRequest;
import br.com.saudeConecta.presentation.dto.usuario.TodosUsuariosAgrupadosResponse;
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
            log.warn("Organização não encontrada no contexto");
            return ResponseEntity.badRequest().build();
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



















//
//
//    @GetMapping("/perfil/{id}")
//    @Transactional
//    @Description("Busca perfil completo do usuário com Profissional, AdminOrganizacao e Endereco. Utilizado em: DadosPessoaisComponent")
//    public ResponseEntity<UsuarioPerfilCompletoResponse> buscarPerfilCompleto(@PathVariable Long id) {
//        log.debug("Buscando perfil completo do usuário ID: {}", id);
//        return usuarioService.buscarPerfilCompleto(id)
//                .map(ResponseEntity::ok)
//                .orElseGet(() -> ResponseEntity.notFound().build());
//    }
//
//    @GetMapping("/buscarId/{id}")
//    @Transactional
//    @Description( "Busca usuário por ID. Utilizado em: UserProfileComponent, UserService")
//    public ResponseEntity<UsuarioResponse> buscarPorId(@PathVariable Long id) {
//        log.debug("Buscando usuário por ID: {}", id);
//        return usuarioService.buscarPorId(id)
//                .map(usuario -> ResponseEntity.ok(new UsuarioResponse(usuario)))
//                .orElseGet(() -> ResponseEntity.notFound().build());
//    }

//    @GetMapping("/buscarLogin/{login}")
//    @Transactional
//    @Description( "Busca usuário por login. Utilizado em: LoginValidationComponent, UserService")
//    public ResponseEntity<UsuarioResponse> buscarPorLogin(@PathVariable String login) {
//        log.debug("Buscando usuário por login: {}", login);
//        return usuarioService.buscarPorLogin(login)
//                .map(usuario -> ResponseEntity.ok(new UsuarioResponse(usuario)))
//                .orElseGet(() -> ResponseEntity.notFound().build());
//    }
//
//    @GetMapping("/existeLogin/{login}")
//    @Transactional
//    @Description( "Verifica se login já existe. Utilizado em: RegisterComponent, ValidationService")
//    public ResponseEntity<Boolean> existePorLogin(@PathVariable String login) {
//        log.debug("Verificando existência de usuário por login: {}", login);
//        boolean existe = usuarioService.existePorLogin(login);
//        return ResponseEntity.ok(existe);
//    }
//
//    @GetMapping("/buscarUsuarioExistente/{login}")
//    @Description("Verifica se login está disponível (true = disponível). Utilizado em: CadastroUsuarioComponent")
//    public ResponseEntity<Boolean> verificarLoginDisponivel(@PathVariable String login) {
//        log.debug("Verificando disponibilidade de login: {}", login);
//        boolean existe = usuarioService.existePorLogin(login);
//        return ResponseEntity.ok(!existe);
//    }
//
////    @GetMapping("/listarPacientes")
////    @Transactional
////    @Description("Lista todos os pacientes. Utilizado em: GerenciamentoUsuariosComponent")
////    public ResponseEntity<List<PacienteResponse>> buscarTodosPacientes() {
////        log.debug("Buscando todos os pacientes");
////        return ResponseEntity.ok(usuarioService.buscarTodosPacientes());
////    }
//
//
//
//    @GetMapping("/pagina")
//    @Transactional
//    @Description( "Busca usuários com paginação. Utilizado em: UserTableComponent, UserService")
//    public ResponseEntity<Page<UsuarioResponse>> buscarPorPaginas(
//            @PageableDefault(size = 12, sort = {"login"}) Pageable paginacao) {
//        log.debug("Buscando usuários com paginação");
//        Page<UsuarioResponse> usuarios = usuarioService.buscarTodos(paginacao)
//                .map(UsuarioResponse::new);
//        return ResponseEntity.ok(usuarios);
//    }
//
//    @PostMapping("/cadastrar")
//    @Transactional
//    @Description( "Cadastra novo usuário. Utilizado em: RegisterComponent, UserService")
//    public ResponseEntity<UsuarioResponse> cadastrarUsuario(
//            @RequestBody @Valid CadastrarUsuarioRequest dados,
//            UriComponentsBuilder uriBuilder) {
//
//        log.debug("Cadastrando usuário: {}", dados.login());
//
//        try {
//            Usuario usuarioSalvo = usuarioService.cadastrarComValidacao(dados);
//
//            URI uri = uriBuilder.path("/usuario/buscarId/{id}")
//                    .buildAndExpand(usuarioSalvo.getId())
//                    .toUri();
//
//            return ResponseEntity.created(uri).body(new UsuarioResponse(usuarioSalvo));
//        } catch (IllegalStateException e) {
//            return ResponseEntity.status(HttpStatus.CONFLICT).build();
//        }
//    }
//
//    @DeleteMapping("/{id}")
//    @Description( "Exclui usuário por ID. Utilizado em: UserListComponent, UserService")
//    public ResponseEntity<Void> deleteUsuarioById(@PathVariable Long id) {
//        log.debug("Deletando usuário por ID: {}", id);
//        try {
//            usuarioService.deletar(id);
//            return ResponseEntity.noContent().build();
//        } catch (Exception e) {
//            log.error("Erro ao deletar usuário ID: {}", id, e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }



}
