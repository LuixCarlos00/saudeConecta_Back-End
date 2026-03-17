package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.domain.prontuario.Prontuario;
import br.com.saudeConecta.presentation.dto.prontuario.CadastrarProntuarioRequest;
import br.com.saudeConecta.presentation.dto.prontuario.CadastrarProntuarioComPlanejamentosRequest;
import br.com.saudeConecta.presentation.dto.prontuario.ProntuarioCompletoResponse;
import br.com.saudeConecta.presentation.dto.prontuario.ProntuarioResponse;
import br.com.saudeConecta.presentation.dto.prontuario.PlanejamentoTerapeuticoRequest;
import br.com.saudeConecta.service.ProntuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/prontuario")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class ProntuarioController {

    private final ProntuarioService prontuarioService;

    /**
     * Endpoint para cadastrar um novo prontuário médico
     * 
     * POST /prontuario/cadastrar
     * 
     * @param request Dados do prontuário a ser cadastrado
     * @return Prontuário cadastrado com status 201 (CREATED)
     */
    @PostMapping("/cadastrarProntuarioMedico")
    public ResponseEntity<ProntuarioResponse> cadastrarProntuarioMedico(@Valid @RequestBody CadastrarProntuarioRequest request) {
        log.info("=== Requisição recebida: POST /prontuario/cadastrarProntuarioMedico ===");
        log.debug("Dados recebidos - Medico ID: {}, Consulta ID: {}", request.codigoMedico(), request.consulta());
        
        try {
            Prontuario prontuario = prontuarioService.cadastrarProntuarioMedico(request);
            log.info("Prontuário cadastrado com sucesso - ID: {}", prontuario.getProntCodigoProntuario());
            
            // Converter para DTO de resposta para evitar LazyInitializationException
            ProntuarioResponse response = ProntuarioResponse.fromEntity(prontuario);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Erro ao cadastrar prontuario: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Endpoint para buscar prontuário por ID da consulta
     * 
     * GET /prontuario/buscarProntuarioById/{consultaId}
     * 
     * @param consultaId ID da consulta
     * @return Prontuário encontrado
     */
    @GetMapping("/buscarProntuarioById/{consultaId}")
    public ResponseEntity<ProntuarioCompletoResponse> buscarProntuarioById(@PathVariable Long consultaId) {
        log.info("=== Requisição recebida: GET /prontuario/buscarProntuarioById/{} ===", consultaId);
        
        Prontuario prontuario = prontuarioService.buscarProntuarioById(consultaId);
        
        ProntuarioCompletoResponse response = ProntuarioCompletoResponse.fromEntity(prontuario);
        return ResponseEntity.ok(response);
    }

    /**
     * Busca todos os prontuários de um paciente (para aba Histórico)
     * 
     * @param pacienteId ID do paciente
     * @return Lista de prontuários do paciente
     */
    @GetMapping("/paciente/{pacienteId}")
    public ResponseEntity<List<ProntuarioCompletoResponse>> buscarPorPaciente(@PathVariable Long pacienteId) {
        log.info("=== Requisição recebida: GET /prontuario/paciente/{} ===", pacienteId);
        
        List<Prontuario> prontuarios = prontuarioService.buscarPorPaciente(pacienteId);
        List<ProntuarioCompletoResponse> response = prontuarios.stream()
                .map(ProntuarioCompletoResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Busca todos os prontuários de um profissional
     * 
     * @param profissionalId ID do profissional
     * @return Lista de prontuários do profissional
     */
    @GetMapping("/profissional/{profissionalId}")
    public ResponseEntity<List<ProntuarioCompletoResponse>> buscarPorProfissional(@PathVariable Long profissionalId) {
        log.info("=== Requisição recebida: GET /prontuario/profissional/{} ===", profissionalId);
        
        List<Prontuario> prontuarios = prontuarioService.buscarPorProfissional(profissionalId);
        List<ProntuarioCompletoResponse> response = prontuarios.stream()
                .map(ProntuarioCompletoResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para cadastrar um novo prontuário médico com planejamentos terapêuticos
     * 
     * POST /prontuario/cadastrarProntuarioMedicoComPlanejamentos
     * 
     * @param request Wrapper com dados do prontuário e planejamentos terapêuticos
     * @return Prontuário cadastrado com status 201 (CREATED)
     */
    @PostMapping("/cadastrarProntuarioMedicoComPlanejamentos")
    public ResponseEntity<ProntuarioResponse> cadastrarProntuarioMedicoComPlanejamentos(
            @Valid @RequestBody CadastrarProntuarioComPlanejamentosRequest request) {
        log.info("=== Requisição recebida: POST /prontuario/cadastrarProntuarioMedicoComPlanejamentos ===");
        
        CadastrarProntuarioRequest prontuarioRequest = request.getProntuario();
        List<PlanejamentoTerapeuticoRequest> planejamentos = request.getPlanejamentos();
        
        log.debug("Dados recebidos - Medico ID: {}, Consulta ID: {}, Planejamentos: {}", 
                prontuarioRequest.codigoMedico(), prontuarioRequest.consulta(), 
                planejamentos != null ? planejamentos.size() : 0);
        
        try {
            Prontuario prontuario = prontuarioService.cadastrarProntuarioMedico(prontuarioRequest, planejamentos);
            log.info("Prontuário cadastrado com sucesso - ID: {}", prontuario.getProntCodigoProntuario());
            
            // Converter para DTO de resposta para evitar LazyInitializationException
            ProntuarioResponse response = ProntuarioResponse.fromEntity(prontuario);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Erro ao cadastrar prontuario com planejamentos: {}", e.getMessage(), e);
            throw e;
        }
    }

}
