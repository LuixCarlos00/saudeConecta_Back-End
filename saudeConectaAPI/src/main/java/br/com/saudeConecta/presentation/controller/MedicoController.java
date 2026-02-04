package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.application.service.MedicoService;
import br.com.saudeConecta.domain.medico.Medico;
import br.com.saudeConecta.presentation.dto.medico.CadastrarMedicoCompletoRequest;
import br.com.saudeConecta.presentation.dto.medico.MedicoResponse;
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
@RequestMapping("/medico")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
@Description ("Endpoints para gerenciamento de médicos")
public class MedicoController {

    private final MedicoService medicoService;

    @GetMapping("/buscarId/{id}")
    @Transactional
    @Description("Busca médico por ID. Utilizado em: DoctorDetailComponent, DoctorService")
    public ResponseEntity<MedicoResponse> buscarPorId(@PathVariable Long id) {
        log.debug("Buscando médico por ID: {}", id);
        return medicoService.buscarPorId(id)
                .map(medico -> ResponseEntity.ok(new MedicoResponse(medico)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/buscarIdUsuario/{id}")
    @Transactional
    @Description("Busca médico por ID do usuário. Utilizado em: DoctorProfileComponent, DoctorService")
    public ResponseEntity<MedicoResponse> buscarPorIdUsuario(@PathVariable Long id) {
        log.debug("Buscando médico por ID de usuário: {}", id);
        return medicoService.buscarPorIdUsuario(id)
                .map(medico -> ResponseEntity.ok(new MedicoResponse(medico)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/buscarEmail/{email}")
    @Transactional
    @Description("Busca médico por email. Utilizado em: DoctorSearchComponent, DoctorService")
    public ResponseEntity<MedicoResponse> buscarPorEmail(@PathVariable String email) {
        log.debug("Buscando médico por email: {}", email);
        return medicoService.buscarPorEmail(email)
                .map(medico -> ResponseEntity.ok(new MedicoResponse(medico)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/buscarCrm/{crm}")
    @Transactional
    @Description("Busca médicos por CRM. Utilizado em: DoctorSearchComponent, DoctorService")
    public ResponseEntity<List<MedicoResponse>> buscarPorCrm(@PathVariable String crm) {
        log.debug("Buscando médicos por CRM: {}", crm);
        List<MedicoResponse> medicos = medicoService.buscarPorCrm(crm).stream()
                .map(MedicoResponse::new)
                .toList();
        return ResponseEntity.ok(medicos);
    }

    @GetMapping("/buscarNome/{nome}")
    @Transactional
    @Description("Busca médicos por nome. Utilizado em: DoctorSearchComponent, DoctorService")
    public ResponseEntity<List<MedicoResponse>> buscarPorNome(@PathVariable String nome) {
        log.debug("Buscando médicos por nome: {}", nome);
        List<MedicoResponse> medicos = medicoService.buscarPorNome(nome).stream()
                .map(MedicoResponse::new)
                .toList();
        return ResponseEntity.ok(medicos);
    }

    @GetMapping("/buscarEspecialidade/{especialidade}")
    @Transactional
    @Description("Busca médicos por especialidade. Utilizado em: SpecialtyFilterComponent, DoctorService")
    public ResponseEntity<List<MedicoResponse>> buscarPorEspecialidade(@PathVariable String especialidade) {
        log.debug("Buscando médicos por especialidade: {}", especialidade);
        List<MedicoResponse> medicos = medicoService.buscarPorEspecialidade(especialidade).stream()
                .map(MedicoResponse::new)
                .toList();
        return ResponseEntity.ok(medicos);
    }

    @GetMapping("/buscarMunicipio/{municipio}")
    @Transactional
    @Description("Busca médicos por município. Utilizado em: LocationFilterComponent, DoctorService")
    public ResponseEntity<List<MedicoResponse>> buscarPorMunicipio(@PathVariable String municipio) {
        log.debug("Buscando médicos por município: {}", municipio);
        List<MedicoResponse> medicos = medicoService.buscarPorMunicipio(municipio).stream()
                .map(MedicoResponse::new)
                .toList();
        return ResponseEntity.ok(medicos);
    }

    @GetMapping("/contarAtivos")
    @Transactional
    @Description("Conta médicos ativos. Utilizado em: DashboardComponent, StatisticsService")
    public ResponseEntity<Long> contarMedicosAtivos() {
        log.debug("Contando médicos ativos");
        Long count = medicoService.contarMedicosAtivos();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/listarTodos")
    @Transactional
    @Description("Lista todos os médicos. Utilizado em: DoctorListComponent, DoctorService")
    public ResponseEntity<List<MedicoResponse>> buscarTodos() {
        log.debug("Buscando todos os médicos");
        List<MedicoResponse> medicos = medicoService.buscarTodos().stream()
                .map(MedicoResponse::new)
                .toList();
        return ResponseEntity.ok(medicos);
    }

    @GetMapping("/pagina")
    @Transactional
    @Description("Busca médicos com paginação. Utilizado em: DoctorTableComponent, DoctorService")
    public ResponseEntity<Page<MedicoResponse>> buscarPorPaginas(
            @PageableDefault(size = 12, sort = {"medNome"}) Pageable paginacao) {
        log.debug("Buscando médicos com paginação");
        Page<MedicoResponse> medicos = medicoService.buscarTodos(paginacao)
                .map(MedicoResponse::new);
        return ResponseEntity.ok(medicos);
    }

    @PostMapping("/cadastrar")
    @Description("Cadastra novo médico com criação automática de usuário (CPF como login, senha gerada e enviada por email)")
    public ResponseEntity<?> cadastrarMedico(
            @RequestBody @Valid CadastrarMedicoCompletoRequest dados,
            UriComponentsBuilder uriBuilder) {
        
        log.info("Recebida requisição de cadastro para médico: {}", dados.medNome());
        
        try {
            long startTime = System.currentTimeMillis();
            Medico medicoSalvo = medicoService.cadastrarCompleto(dados);
            long endTime = System.currentTimeMillis();
            
            log.info("Médico cadastrado com sucesso em {}ms. ID: {}", endTime - startTime, medicoSalvo.getMedCodigo());

            URI uri = uriBuilder.path("/medico/buscarId/{id}")
                    .buildAndExpand(medicoSalvo.getMedCodigo())
                    .toUri();

            return ResponseEntity.created(uri).body(new MedicoResponse(medicoSalvo));
        } catch (IllegalStateException e) {
            log.warn("CPF já cadastrado: {}", dados.medCpf());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            log.error("Erro ao cadastrar médico: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao cadastrar médico");
        }
    }

    @DeleteMapping("/{id}")
    @Description("Exclui médico por ID. Utilizado em: DoctorListComponent, DoctorService")
    public ResponseEntity<Void> deleteMedicoById(@PathVariable Long id) {
        log.debug("Deletando médico por ID: {}", id);
        try {
            medicoService.deletar(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Erro ao deletar médico ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Endpoint removido - duplicação de /listarTodos
}
