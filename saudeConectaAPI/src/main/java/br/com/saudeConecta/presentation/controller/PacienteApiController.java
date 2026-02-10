package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.service.PacienteService;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.presentation.dto.paciente.AtualizarPacienteRequest;
import br.com.saudeConecta.presentation.dto.paciente.CadastrarPacienteCompletoRequest;
import br.com.saudeConecta.presentation.dto.paciente.PacienteResponse;
import br.com.saudeConecta.presentation.dto.usuario.BloquearUsuarioRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pacientes")
@RequiredArgsConstructor
@Slf4j
public class PacienteApiController {

    private final PacienteService pacienteService;

    @PostMapping("/cadastrarPacientebyOrg")
    public ResponseEntity<?> cadastrarPacientebyOrg(
            @Valid @RequestBody CadastrarPacienteCompletoRequest request) {
        log.info("Cadastrando paciente: {} - Data Nascimento: {}",
                request.nome(), request.dataNacimento());
        try {
            Paciente paciente = pacienteService.cadastrarPacientebyOrg(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new PacienteResponse(paciente));
        } catch (IllegalStateException e) {
            log.warn("Erro ao cadastrar paciente: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            log.error("Erro inesperado ao cadastrar paciente", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao processar requisição: " + e.getMessage());
        }
    }

    @GetMapping("/buscarrPacientebyOrg/{id}")
    public ResponseEntity<PacienteResponse> buscarrPacientebyOrg(@PathVariable Long id) {
        log.debug("Buscando paciente por ID: {} no tenant", id);
        return pacienteService.buscarrPacientebyOrg(id)
                .map(p -> ResponseEntity.ok(new PacienteResponse(p)))
                .orElse(ResponseEntity.notFound().build());
    }


    @PutMapping("/atualizarPacientebyOrg/{id}")
    public ResponseEntity<PacienteResponse> buscarrPatualizarPacientebyOrgacientebyOrg(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarPacienteRequest request) {
        log.info("Atualizando paciente ID: {} no tenant", id);

        // Verifica se paciente pertence ao tenant
        if (pacienteService.buscarrPacientebyOrg(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Paciente paciente = pacienteService.atualizarPacientebyOrg(id, request);
        return ResponseEntity.ok(new PacienteResponse(paciente));
    }




    @PutMapping("/bloquearPacientebyOrg")
    public ResponseEntity<Void> bloquearPacientebyOrg(
            @RequestBody @Valid @NotNull BloquearUsuarioRequest request) {
        log.info("Alterando status do paciente ID: {} para {}", request.codigo(), request.status());

        // Verifica se paciente pertence ao tenant
        if (pacienteService.buscarrPacientebyOrg(request.codigo()).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        pacienteService.bloquearPacientebyOrg(request.codigo(), request.status());
        return ResponseEntity.ok().build();
    }




    @DeleteMapping("/deletarPacientebyOrg/{id}")
    public ResponseEntity<Void> deletarPacientebyOrg(@PathVariable Long id) {
        log.info("Deletando paciente ID: {} do tenant", id);

        // Verifica se paciente pertence ao tenant
        if (pacienteService.buscarrPacientebyOrg(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            pacienteService.deletarPacientebyOrg(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Erro ao deletar paciente: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }






//
//
//    @GetMapping
//    public ResponseEntity<List<PacienteResponse>> listarTodos() {
//        log.debug("Listando todos os pacientes do tenant");
//        List<PacienteResponse> pacientes = pacienteService.buscarTodosPorTenant()
//                .stream()
//                .map(PacienteResponse::new)
//                .toList();
//        return ResponseEntity.ok(pacientes);
//    }
//
//
//
//
//
//    @GetMapping("/pagina")
//    public ResponseEntity<Page<PacienteResponse>> listarPaginado(
//            @PageableDefault(size = 12, sort = {"paciNome"}) Pageable pageable) {
//        log.debug("Listando pacientes paginados do tenant");
//        Page<PacienteResponse> pacientes = pacienteService.buscarTodosPorTenant(pageable)
//            .map(PacienteResponse::new);
//        return ResponseEntity.ok(pacientes);
//    }
//
//
//
//    @GetMapping("/buscar")
//    public ResponseEntity<List<PacienteResponse>> buscarPorNome(@RequestParam String nome) {
//        log.debug("Buscando pacientes por nome: {} no tenant", nome);
//        List<PacienteResponse> pacientes = pacienteService.buscarPorNomeTenant(nome)
//            .stream()
//            .map(PacienteResponse::new)
//            .toList();
//        return ResponseEntity.ok(pacientes);
//    }
//
//    @GetMapping("/estatisticas/ativos")
//    public ResponseEntity<Long> contarAtivos() {
//        log.debug("Contando pacientes ativos no tenant");
//        return ResponseEntity.ok(pacienteService.contarAtivosTenant());
//    }
//
//    @GetMapping("/verificar-cpf")
//    public ResponseEntity<Boolean> verificarCpfExiste(@RequestParam String cpf) {
//        log.debug("Verificando se CPF existe no tenant: {}", cpf);
//        return ResponseEntity.ok(pacienteService.existeCpfNoTenant(cpf));
//    }







}
