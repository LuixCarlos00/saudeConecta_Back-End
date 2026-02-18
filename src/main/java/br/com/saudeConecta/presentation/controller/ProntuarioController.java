package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.domain.prontuario.Prontuario;
import br.com.saudeConecta.presentation.dto.prontuario.CadastrarProntuarioRequest;
import br.com.saudeConecta.presentation.dto.prontuario.ProntuarioCompletoResponse;
import br.com.saudeConecta.presentation.dto.prontuario.ProntuarioResponse;
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
    public ResponseEntity<ProntuarioCompletoResponse> buscarProntuarioById(@PathVariable Long consultaId) {
        log.info("=== Requisição recebida: GET /prontuario/buscarProntuarioById/{} ===", consultaId);
        
        Prontuario prontuario = prontuarioService.buscarProntuarioById(consultaId);
        
        // Converter para DTO completo para evitar LazyInitializationException
        ProntuarioCompletoResponse response = ProntuarioCompletoResponse.fromEntity(prontuario);
        return ResponseEntity.ok(response);
    }






}
