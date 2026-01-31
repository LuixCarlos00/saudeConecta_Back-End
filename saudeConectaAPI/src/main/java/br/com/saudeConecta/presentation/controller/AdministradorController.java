package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.application.service.AdministradorService;
import br.com.saudeConecta.domain.administrador.Administrador;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import br.com.saudeConecta.presentation.dto.administrador.AdministradorResponse;
import br.com.saudeConecta.presentation.dto.administrador.CadastrarAdministradorRequest;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/administrador")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class AdministradorController {

    private final AdministradorService administradorService;
    private final UsuarioRepository usuarioRepository;

    @GetMapping("/buscarId/{id}")
    @Transactional
    public ResponseEntity<AdministradorResponse> buscarPorId(@PathVariable Long id) {
        log.debug("Buscando administrador por ID: {}", id);
        return administradorService.buscarPorId(id)
                .map(administrador -> ResponseEntity.ok(new AdministradorResponse(administrador)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/buscarIdDeUsusario/{id}")
    @Transactional
    public ResponseEntity<AdministradorResponse> buscarPorIdUsuario(@PathVariable Long id) {
        log.debug("Buscando administrador por ID de usuário: {}", id);
        return administradorService.buscarPorIdUsuario(id)
                .map(administrador -> ResponseEntity.ok(new AdministradorResponse(administrador)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/buscarPorEmail/{email}")
    @Transactional
    public ResponseEntity<Object> buscarPorEmailEnviarCodigo(@PathVariable String email) {
        log.debug("Buscando administrador por email: {}", email);
        // TODO: Implementar use cases para recuperação de senha
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @GetMapping("/InserirCodigo/{codigo}")
    @Transactional
    public ResponseEntity<ResponseEntity<Object>> verificarCodigoRecuperacao(@PathVariable String codigo) {
        log.debug("Verificando código de recuperação: {}", codigo);
        // TODO: Implementar use cases para verificação de código
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @PostMapping("/post")
    @Transactional
    public ResponseEntity<AdministradorResponse> cadastrarAdministrador(
            @RequestBody @Valid CadastrarAdministradorRequest dados,
            UriComponentsBuilder uriBuilder) {
        
        log.debug("Cadastrando administrador: {}", dados.admNome());
        
        var usuarioOptional = usuarioRepository.findById(dados.admUsuario());
        if (usuarioOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Usuario usuario = usuarioOptional.get();
        Administrador administrador = new Administrador(dados, usuario);
        
        Administrador administradorSalvo = administradorService.cadastrar(administrador);

        URI uri = uriBuilder.path("/administrador/buscarId/{id}")
                .buildAndExpand(administradorSalvo.getAdmCodigo())
                .toUri();

        return ResponseEntity.created(uri).body(new AdministradorResponse(administradorSalvo));
    }

    @GetMapping("/pacientepagina")
    public Page<AdministradorResponse> buscarPorPaginas(
            @PageableDefault(size = 12, sort = {"admCodigo"}) Pageable paginacao) {
        log.debug("Buscando administradores com paginação");
        return administradorService.buscarTodos(paginacao)
                .map(AdministradorResponse::new);
    }

    @GetMapping("/listatodospaciente")
    public List<AdministradorResponse> buscarTodos() {
        log.debug("Buscando todos os administradores");
        return administradorService.buscarTodos().stream()
                .map(AdministradorResponse::new)
                .toList();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAdministradorById(@PathVariable Long id) {
        log.debug("Deletando administrador por ID: {}", id);
        try {
            administradorService.deletar(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Erro ao deletar administrador ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
