package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.application.service.MedicoService;
import br.com.saudeConecta.domain.endereco.Endereco;
import br.com.saudeConecta.domain.medico.Medico;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infrastructure.persistence.repository.EnderecoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import br.com.saudeConecta.presentation.dto.medico.CadastrarMedicoRequest;
import br.com.saudeConecta.presentation.dto.medico.MedicoResponse;
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
@RequestMapping("/medico")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class MedicoController {

    private final MedicoService medicoService;
    private final UsuarioRepository usuarioRepository;
    private final EnderecoRepository enderecoRepository;

    @GetMapping("/buscarId/{id}")
    @Transactional
    public ResponseEntity<MedicoResponse> buscarPorId(@PathVariable Long id) {
        log.debug("Buscando médico por ID: {}", id);
        return medicoService.buscarPorId(id)
                .map(medico -> ResponseEntity.ok(new MedicoResponse(medico)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/buscarIdUsuario/{id}")
    @Transactional
    public ResponseEntity<MedicoResponse> buscarPorIdUsuario(@PathVariable Long id) {
        log.debug("Buscando médico por ID de usuário: {}", id);
        return medicoService.buscarPorIdUsuario(id)
                .map(medico -> ResponseEntity.ok(new MedicoResponse(medico)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/buscarEmail/{email}")
    @Transactional
    public ResponseEntity<MedicoResponse> buscarPorEmail(@PathVariable String email) {
        log.debug("Buscando médico por email: {}", email);
        return medicoService.buscarPorEmail(email)
                .map(medico -> ResponseEntity.ok(new MedicoResponse(medico)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/buscarCrm/{crm}")
    @Transactional
    public ResponseEntity<List<MedicoResponse>> buscarPorCrm(@PathVariable String crm) {
        log.debug("Buscando médicos por CRM: {}", crm);
        List<MedicoResponse> medicos = medicoService.buscarPorCrm(crm).stream()
                .map(MedicoResponse::new)
                .toList();
        return ResponseEntity.ok(medicos);
    }

    @GetMapping("/buscarNome/{nome}")
    @Transactional
    public ResponseEntity<List<MedicoResponse>> buscarPorNome(@PathVariable String nome) {
        log.debug("Buscando médicos por nome: {}", nome);
        List<MedicoResponse> medicos = medicoService.buscarPorNome(nome).stream()
                .map(MedicoResponse::new)
                .toList();
        return ResponseEntity.ok(medicos);
    }

    @GetMapping("/buscarEspecialidade/{especialidade}")
    @Transactional
    public ResponseEntity<List<MedicoResponse>> buscarPorEspecialidade(@PathVariable String especialidade) {
        log.debug("Buscando médicos por especialidade: {}", especialidade);
        List<MedicoResponse> medicos = medicoService.buscarPorEspecialidade(especialidade).stream()
                .map(MedicoResponse::new)
                .toList();
        return ResponseEntity.ok(medicos);
    }

    @GetMapping("/buscarMunicipio/{municipio}")
    @Transactional
    public ResponseEntity<List<MedicoResponse>> buscarPorMunicipio(@PathVariable String municipio) {
        log.debug("Buscando médicos por município: {}", municipio);
        List<MedicoResponse> medicos = medicoService.buscarPorMunicipio(municipio).stream()
                .map(MedicoResponse::new)
                .toList();
        return ResponseEntity.ok(medicos);
    }

    @GetMapping("/contarAtivos")
    @Transactional
    public ResponseEntity<Long> contarMedicosAtivos() {
        log.debug("Contando médicos ativos");
        Long count = medicoService.contarMedicosAtivos();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/listarTodos")
    @Transactional
    public ResponseEntity<List<MedicoResponse>> buscarTodos() {
        log.debug("Buscando todos os médicos");
        List<MedicoResponse> medicos = medicoService.buscarTodos().stream()
                .map(MedicoResponse::new)
                .toList();
        return ResponseEntity.ok(medicos);
    }

    @GetMapping("/pagina")
    @Transactional
    public ResponseEntity<Page<MedicoResponse>> buscarPorPaginas(
            @PageableDefault(size = 12, sort = {"medNome"}) Pageable paginacao) {
        log.debug("Buscando médicos com paginação");
        Page<MedicoResponse> medicos = medicoService.buscarTodos(paginacao)
                .map(MedicoResponse::new);
        return ResponseEntity.ok(medicos);
    }

    @PostMapping("/cadastrar")
    @Transactional
    public ResponseEntity<MedicoResponse> cadastrarMedico(
            @RequestBody @Valid CadastrarMedicoRequest dados,
            UriComponentsBuilder uriBuilder) {
        
        log.debug("Cadastrando médico: {}", dados.medNome());
        
        var usuarioOptional = usuarioRepository.findById(dados.usuario());
        if (usuarioOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var enderecoOptional = enderecoRepository.findById(dados.endereco());
        if (enderecoOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Usuario usuario = usuarioOptional.get();
        Endereco endereco = enderecoOptional.get();
        Medico medico = new Medico(dados, usuario, endereco);
        
        Medico medicoSalvo = medicoService.cadastrar(medico);

        URI uri = uriBuilder.path("/medico/buscarId/{id}")
                .buildAndExpand(medicoSalvo.getMedCodigo())
                .toUri();

        return ResponseEntity.created(uri).body(new MedicoResponse(medicoSalvo));
    }

    @DeleteMapping("/{id}")
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
}
