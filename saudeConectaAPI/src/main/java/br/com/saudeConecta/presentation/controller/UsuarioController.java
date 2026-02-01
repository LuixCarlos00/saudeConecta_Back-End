package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.application.service.UsuarioService;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infra.configuracoesseguranca.TokenService;
import br.com.saudeConecta.presentation.dto.usuario.CadastrarUsuarioRequest;
import br.com.saudeConecta.presentation.dto.usuario.DadosLoginUsuario;
import br.com.saudeConecta.presentation.dto.usuario.DadosTokenJWT;
import br.com.saudeConecta.presentation.dto.usuario.UsuarioResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    @PostMapping("/login")
    public ResponseEntity<DadosTokenJWT> autenticar(@RequestBody @NotNull DadosLoginUsuario dados) {
        var authenticatetoken = new UsernamePasswordAuthenticationToken(dados.login(), dados.senha());
        var authentication = authenticationManager.authenticate(authenticatetoken);

        var TokenJWT = tokenService.gerarToken((Usuario) authentication.getPrincipal());

        return ResponseEntity.ok(new DadosTokenJWT(TokenJWT));
    }


    @GetMapping("/buscarId/{id}")
    @Transactional
    public ResponseEntity<UsuarioResponse> buscarPorId(@PathVariable Long id) {
        log.debug("Buscando usuário por ID: {}", id);
        return usuarioService.buscarPorId(id)
                .map(usuario -> ResponseEntity.ok(new UsuarioResponse(usuario)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/buscarLogin/{login}")
    @Transactional
    public ResponseEntity<UsuarioResponse> buscarPorLogin(@PathVariable String login) {
        log.debug("Buscando usuário por login: {}", login);
        return usuarioService.buscarPorLogin(login)
                .map(usuario -> ResponseEntity.ok(new UsuarioResponse(usuario)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/existeLogin/{login}")
    @Transactional
    public ResponseEntity<Boolean> existePorLogin(@PathVariable String login) {
        log.debug("Verificando existência de usuário por login: {}", login);
        boolean existe = usuarioService.existePorLogin(login);
        return ResponseEntity.ok(existe);
    }

    @GetMapping("/listarTodos")
    @Transactional
    public ResponseEntity<List<UsuarioResponse>> buscarTodos() {
        log.debug("Buscando todos os usuários");
        List<UsuarioResponse> usuarios = usuarioService.buscarTodos().stream()
                .map(UsuarioResponse::new)
                .toList();
        return ResponseEntity.ok(usuarios);
    }

    @GetMapping("/pagina")
    @Transactional
    public ResponseEntity<Page<UsuarioResponse>> buscarPorPaginas(
            @PageableDefault(size = 12, sort = {"login"}) Pageable paginacao) {
        log.debug("Buscando usuários com paginação");
        Page<UsuarioResponse> usuarios = usuarioService.buscarTodos(paginacao)
                .map(UsuarioResponse::new);
        return ResponseEntity.ok(usuarios);
    }

    @PostMapping("/cadastrar")
    @Transactional
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
