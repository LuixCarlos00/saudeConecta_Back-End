package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.domain.prontuario.Prontuario;
import br.com.saudeConecta.presentation.dto.prontuario.CadastrarProntuarioRequest;
import br.com.saudeConecta.presentation.dto.prontuario.ProntuarioResponse;
import br.com.saudeConecta.service.ProntuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller responsável pelos endpoints de Prontuário Médico
 * 
 * @author Sistema SaúdeConecta
 * @version 1.0
 */
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
        log.debug("Dados recebidos - Médico ID: {}, Consulta ID: {}", request.prontCodigoMedico(), request.consulta());
        
        try {
            Prontuario prontuario = prontuarioService.cadastrarProntuarioMedico(request);
            log.info("Prontuário cadastrado com sucesso - ID: {}", prontuario.getProntCodigoProntuario());
            
            // Converter para DTO de resposta para evitar LazyInitializationException
            ProntuarioResponse response = ProntuarioResponse.fromEntity(prontuario);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Erro ao cadastrar prontuário: {}", e.getMessage(), e);
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
    public ResponseEntity<Prontuario> buscarProntuarioById(@PathVariable Long consultaId) {
        log.info("=== Requisição recebida: GET /prontuario/buscarProntuarioById/{} ===", consultaId);
        
        Prontuario prontuario = prontuarioService.buscarProntuarioById(consultaId);
        return ResponseEntity.ok(prontuario);
    }

    /**
     * Endpoint para buscar todos os prontuários de um paciente
     * 
     * GET /prontuario/paciente/{pacienteId}
     * 
     * @param pacienteId ID do paciente
     * @return Lista de prontuários do paciente
     */
    @GetMapping("/paciente/{pacienteId}")
    public ResponseEntity<List<Prontuario>> buscarPorPaciente(@PathVariable Long pacienteId) {
        log.info("=== Requisição recebida: GET /prontuario/paciente/{} ===", pacienteId);
        
        List<Prontuario> prontuarios = prontuarioService.buscarPorPaciente(pacienteId);
        return ResponseEntity.ok(prontuarios);
    }

    /**
     * Endpoint para buscar todos os prontuários de um profissional
     * 
     * GET /prontuario/profissional/{profissionalId}
     * 
     * @param profissionalId ID do profissional
     * @return Lista de prontuários do profissional
     */
    @GetMapping("/profissional/{profissionalId}")
    public ResponseEntity<List<Prontuario>> buscarPorProfissional(@PathVariable Long profissionalId) {
        log.info("=== Requisição recebida: GET /prontuario/profissional/{} ===", profissionalId);
        
        List<Prontuario> prontuarios = prontuarioService.buscarPorProfissional(profissionalId);
        return ResponseEntity.ok(prontuarios);
    }

    /**
     * Endpoint para buscar prontuário por ID
     * 
     * GET /prontuario/{id}
     * 
     * @param id ID do prontuário
     * @return Prontuário encontrado
     */
    @GetMapping("/{id}")
    public ResponseEntity<Prontuario> buscarPorId(@PathVariable Long id) {
        log.info("=== Requisição recebida: GET /prontuario/{} ===", id);
        
        Prontuario prontuario = prontuarioService.buscarPorId(id);
        return ResponseEntity.ok(prontuario);
    }
}
