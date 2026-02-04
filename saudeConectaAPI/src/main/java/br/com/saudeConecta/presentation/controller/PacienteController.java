package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.application.service.PacienteService;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.presentation.dto.paciente.CadastrarPacienteCompletoRequest;
import br.com.saudeConecta.presentation.dto.paciente.CadastrarPacienteRequest;
import br.com.saudeConecta.presentation.dto.paciente.PacienteResponse;
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
@RequestMapping("/paciente")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
@Description( "Endpoints para gerenciamento de pacientes")
public class PacienteController {

    private final PacienteService pacienteService;

    @GetMapping("/buscarId/{id}")
    @Transactional
    @Description( "Busca paciente por ID. Utilizado em: PatientDetailComponent, PatientService")
    public ResponseEntity<PacienteResponse> buscarPorId(@PathVariable Long id) {
        log.debug("Buscando paciente por ID: {}", id);
        return pacienteService.buscarPorId(id)
                .map(paciente -> ResponseEntity.ok(new PacienteResponse(paciente)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/buscarEmail/{email}")
    @Transactional
    @Description( "Busca paciente por email. Utilizado em: PatientSearchComponent, PatientService")
    public ResponseEntity<PacienteResponse> buscarPorEmail(@PathVariable String email) {
        log.debug("Buscando paciente por email: {}", email);
        return pacienteService.buscarPorEmail(email)
                .map(paciente -> ResponseEntity.ok(new PacienteResponse(paciente)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/buscarCpf/{cpf}")
    @Transactional
    @Description( "Busca pacientes por CPF. Utilizado em: PatientSearchComponent, PatientService")
    public ResponseEntity<List<PacienteResponse>> buscarPorCpf(@PathVariable String cpf) {
        log.debug("Buscando pacientes por CPF: {}", cpf);
        List<PacienteResponse> pacientes = pacienteService.buscarPorCpf(cpf).stream()
                .map(PacienteResponse::new)
                .toList();
        return ResponseEntity.ok(pacientes);
    }

    @GetMapping("/buscarRg/{rg}")
    @Transactional
    @Description( "Busca pacientes por RG. Utilizado em: PatientSearchComponent, PatientService")
    public ResponseEntity<List<PacienteResponse>> buscarPorRg(@PathVariable String rg) {
        log.debug("Buscando pacientes por RG: {}", rg);
        List<PacienteResponse> pacientes = pacienteService.buscarPorRg(rg).stream()
                .map(PacienteResponse::new)
                .toList();
        return ResponseEntity.ok(pacientes);
    }

    @GetMapping("/buscarTelefone/{telefone}")
    @Transactional
    @Description( "Busca pacientes por telefone. Utilizado em: PatientSearchComponent, PatientService")
    public ResponseEntity<List<PacienteResponse>> buscarPorTelefone(@PathVariable String telefone) {
        log.debug("Buscando pacientes por telefone: {}", telefone);
        List<PacienteResponse> pacientes = pacienteService.buscarPorTelefone(telefone).stream()
                .map(PacienteResponse::new)
                .toList();
        return ResponseEntity.ok(pacientes);
    }

    @GetMapping("/buscarNome/{nome}")
    @Transactional
    @Description( "Busca pacientes por nome. Utilizado em: PatientSearchComponent, PatientService")
    public ResponseEntity<List<PacienteResponse>> buscarPorNome(@PathVariable String nome) {
        log.debug("Buscando pacientes por nome: {}", nome);
        List<PacienteResponse> pacientes = pacienteService.buscarPorNome(nome).stream()
                .map(PacienteResponse::new)
                .toList();
        return ResponseEntity.ok(pacientes);
    }

    @GetMapping("/listarTodos")
    @Transactional
    @Description( "Lista todos os pacientes. Utilizado em: PatientListComponent, PatientService")
    public ResponseEntity<List<PacienteResponse>> buscarTodos() {
        log.debug("Buscando todos os pacientes");
        List<PacienteResponse> pacientes = pacienteService.buscarTodos().stream()
                .map(PacienteResponse::new)
                .toList();
        return ResponseEntity.ok(pacientes);
    }

    @GetMapping("/pagina")
    @Transactional
    @Description( "Busca pacientes com paginação. Utilizado em: PatientTableComponent, PatientService")
    public ResponseEntity<Page<PacienteResponse>> buscarPorPaginas(
            @PageableDefault(size = 12, sort = {"paciNome"}) Pageable paginacao) {
        log.debug("Buscando pacientes com paginação");
        Page<PacienteResponse> pacientes = pacienteService.buscarTodos(paginacao)
                .map(PacienteResponse::new);
        return ResponseEntity.ok(pacientes);
    }

    @PostMapping("/cadastrar")
    @Transactional
    @Description("Cadastra novo paciente com endereço completo. Paciente não faz login no sistema.")
    public ResponseEntity<?> cadastrarPaciente(
            @RequestBody @Valid CadastrarPacienteCompletoRequest dados,
            UriComponentsBuilder uriBuilder) {
        
        log.debug("Cadastrando paciente: {}", dados.paciNome());
        
        try {
            Paciente pacienteSalvo = pacienteService.cadastrarCompleto(dados);

            URI uri = uriBuilder.path("/paciente/buscarId/{id}")
                    .buildAndExpand(pacienteSalvo.getPaciCodigo())
                    .toUri();

            return ResponseEntity.created(uri).body(new PacienteResponse(pacienteSalvo));
        } catch (Exception e) {
            log.error("Erro ao cadastrar paciente: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao cadastrar paciente");
        }
    }

    @DeleteMapping("/{id}")
    @Description( "Exclui paciente por ID. Utilizado em: PatientListComponent, PatientService")
    public ResponseEntity<Void> deletePacienteById(@PathVariable Long id) {
        log.debug("Deletando paciente por ID: {}", id);
        try {
            pacienteService.deletar(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Erro ao deletar paciente ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/bloquear/{id}")
    @Description("Bloqueia ou desbloqueia paciente por ID. Utilizado em: GerenciamentoUsuariosComponent")
    public ResponseEntity<Void> bloquearPaciente(@PathVariable Long id, @RequestBody java.util.Map<String, Integer> body) {
        log.debug("Alterando status do paciente ID: {}", id);
        try {
            int status = body.get("status");
            pacienteService.bloquear(id, status);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Erro ao alterar status do paciente ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
