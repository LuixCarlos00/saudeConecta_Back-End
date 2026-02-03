package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.application.service.UsuarioService;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.presentation.dto.usuario.CadastrarUsuarioRequest;
import br.com.saudeConecta.presentation.dto.usuario.UsuarioResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jdk.jfr.Description;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/usuario")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
@Description(  "Endpoints para gerenciamento de usuários")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;

// Login removido - usar /Home/login


    @GetMapping("/buscarId/{id}")
    @Transactional
    @Description( "Busca usuário por ID. Utilizado em: UserProfileComponent, UserService")
    public ResponseEntity<UsuarioResponse> buscarPorId(@PathVariable Long id) {
        log.debug("Buscando usuário por ID: {}", id);
        return usuarioService.buscarPorId(id)
                .map(usuario -> ResponseEntity.ok(new UsuarioResponse(usuario)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/buscarLogin/{login}")
    @Transactional
    @Description( "Busca usuário por login. Utilizado em: LoginValidationComponent, UserService")
    public ResponseEntity<UsuarioResponse> buscarPorLogin(@PathVariable String login) {
        log.debug("Buscando usuário por login: {}", login);
        return usuarioService.buscarPorLogin(login)
                .map(usuario -> ResponseEntity.ok(new UsuarioResponse(usuario)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/existeLogin/{login}")
    @Transactional
    @Description( "Verifica se login já existe. Utilizado em: RegisterComponent, ValidationService")
    public ResponseEntity<Boolean> existePorLogin(@PathVariable String login) {
        log.debug("Verificando existência de usuário por login: {}", login);
        boolean existe = usuarioService.existePorLogin(login);
        return ResponseEntity.ok(existe);
    }

    @GetMapping("/listarTodos")
    @Transactional
    @Description( "Lista todos os usuários. Utilizado em: UserListComponent, UserService")
    public ResponseEntity<List<UsuarioResponse>> buscarTodos() {
        log.debug("Buscando todos os usuários");
        List<UsuarioResponse> usuarios = usuarioService.buscarTodos().stream()
                .map(UsuarioResponse::new)
                .toList();
        return ResponseEntity.ok(usuarios);
    }

    @GetMapping("/pagina")
    @Transactional
    @Description( "Busca usuários com paginação. Utilizado em: UserTableComponent, UserService")
    public ResponseEntity<Page<UsuarioResponse>> buscarPorPaginas(
            @PageableDefault(size = 12, sort = {"login"}) Pageable paginacao) {
        log.debug("Buscando usuários com paginação");
        Page<UsuarioResponse> usuarios = usuarioService.buscarTodos(paginacao)
                .map(UsuarioResponse::new);
        return ResponseEntity.ok(usuarios);
    }

    @PostMapping("/cadastrar")
    @Transactional
    @Description( "Cadastra novo usuário. Utilizado em: RegisterComponent, UserService")
    public ResponseEntity<UsuarioResponse> cadastrarUsuario(
            @RequestBody @Valid CadastrarUsuarioRequest dados,
            UriComponentsBuilder uriBuilder) {
        
        log.debug("Cadastrando usuário: {}", dados.login());
        
        if (usuarioService.existePorLogin(dados.login())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        String senhaCriptografada = passwordEncoder.encode(dados.senha());
        Usuario usuario = new Usuario(dados, senhaCriptografada);
        
        Usuario usuarioSalvo = usuarioService.cadastrar(usuario);

        URI uri = uriBuilder.path("/usuario/buscarId/{id}")
                .buildAndExpand(usuarioSalvo.getId())
                .toUri();

        return ResponseEntity.created(uri).body(new UsuarioResponse(usuarioSalvo));
    }

    @DeleteMapping("/{id}")
    @Description( "Exclui usuário por ID. Utilizado em: UserListComponent, UserService")
    public ResponseEntity<Void> deleteUsuarioById(@PathVariable Long id) {
        log.debug("Deletando usuário por ID: {}", id);
        try {
            usuarioService.deletar(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Erro ao deletar usuário ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
