package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.application.service.AdministradorService;
import br.com.saudeConecta.domain.administrador.Administrador;
import br.com.saudeConecta.presentation.dto.administrador.AdministradorResponse;
import br.com.saudeConecta.presentation.dto.administrador.AtualizarAdministradorRequest;
import br.com.saudeConecta.presentation.dto.administrador.CadastrarAdministradorCompletoRequest;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/administrador")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
@Description(  "Endpoints para gerenciamento de administradores")
public class AdministradorController {

    private final AdministradorService administradorService;

    @GetMapping("/buscarId/{id}")
    @Transactional
    @Description( "Busca administrador por ID. Utilizado em: AdministradorDetailComponent, AdminService")
    public ResponseEntity<AdministradorResponse> buscarPorId(@PathVariable Long id) {
        log.debug("Buscando administrador por ID: {}", id);
        return administradorService.buscarPorId(id)
                .map(administrador -> ResponseEntity.ok(new AdministradorResponse(administrador)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/buscarIdUsuario/{id}")
    @Transactional
    @Description( "Busca administrador por ID do usuário. Utilizado em: UserProfileComponent, AdminService")
    public ResponseEntity<AdministradorResponse> buscarPorIdUsuario(@PathVariable Long id) {
        log.debug("Buscando administrador por ID de usuário: {}", id);
        return administradorService.buscarPorIdUsuario(id)
                .map(administrador -> ResponseEntity.ok(new AdministradorResponse(administrador)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/buscarPorEmail/{email}")
    @Transactional
    @Description( "Busca administrador por email para recuperação de senha. Utilizado em: ForgotPasswordComponent, PasswordRecoveryService")
    public ResponseEntity<Object> buscarPorEmailEnviarCodigo(@PathVariable String email) {
        log.debug("Buscando administrador por email: {}", email);
        // TODO: Implementar use cases para recuperação de senha
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @GetMapping("/verificarCodigo/{codigo}")
    @Transactional
    @Description( "Verifica código de recuperação de senha. Utilizado em: VerifyCodeComponent, PasswordRecoveryService")
    public ResponseEntity<ResponseEntity<Object>> verificarCodigoRecuperacao(@PathVariable String codigo) {
        log.debug("Verificando código de recuperação: {}", codigo);
        // TODO: Implementar use cases para verificação de código
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @PostMapping("/cadastrar")
    @Description("Cadastra novo administrador com criação automática de usuário (CPF como login, senha gerada e enviada por email)")
    public ResponseEntity<?> cadastrarAdministrador(
            @RequestBody @Valid CadastrarAdministradorCompletoRequest dados,
            UriComponentsBuilder uriBuilder) {
        
        log.info("Recebida requisição de cadastro para administrador: {}", dados.admNome());
        
        try {
            long startTime = System.currentTimeMillis();
            Administrador administradorSalvo = administradorService.cadastrarCompleto(dados);
            long endTime = System.currentTimeMillis();
            
            log.info("Administrador cadastrado com sucesso em {}ms. ID: {}", endTime - startTime, administradorSalvo.getAdmCodigo());

            URI uri = uriBuilder.path("/administrador/buscarId/{id}")
                    .buildAndExpand(administradorSalvo.getAdmCodigo())
                    .toUri();

            return ResponseEntity.created(uri).body(new AdministradorResponse(administradorSalvo));
        } catch (IllegalStateException e) {
            log.warn("CPF já cadastrado: {}", dados.admCpf());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            log.error("Erro ao cadastrar administrador: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao cadastrar administrador");
        }
    }

    @GetMapping("/pagina")
    @Description( "Busca administradores com paginação. Utilizado em: AdminListComponent, AdminService")
    public Page<AdministradorResponse> buscarPorPaginas(
            @PageableDefault(size = 12, sort = {"admCodigo"}) Pageable paginacao) {
        log.debug("Buscando administradores com paginação");
        return administradorService.buscarTodos(paginacao)
                .map(AdministradorResponse::new);
    }

    @GetMapping("/listarTodos")
    @Description( "Lista todos os administradores. Utilizado em: AdminSelectComponent, AdminService")
    public List<AdministradorResponse> buscarTodos() {
        log.debug("Buscando todos os administradores");
        return administradorService.buscarTodos().stream()
                .map(AdministradorResponse::new)
                .toList();
    }

    @DeleteMapping("/{id}")
    @Description( "Exclui administrador por ID. Utilizado em: AdminListComponent, AdminService")
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

    @PutMapping("/atualizar/{id}")
    @Transactional
    @Description("Atualiza dados do administrador por ID. Utilizado em: VisualizarEditarUsuarioComponent")
    public ResponseEntity<?> atualizarAdministrador(
            @PathVariable Long id,
            @RequestBody @Valid AtualizarAdministradorRequest dados) {
        log.debug("Atualizando administrador ID: {}", id);
        try {
            Administrador administradorAtualizado = administradorService.atualizar(id, dados);
            return ResponseEntity.ok(new AdministradorResponse(administradorAtualizado));
        } catch (IllegalArgumentException e) {
            log.warn("Administrador não encontrado para atualização ID: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Erro ao atualizar administrador ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao atualizar administrador");
        }
    }
}
