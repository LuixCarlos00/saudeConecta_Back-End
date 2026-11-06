package br.com.saudeConecta.presentation.controller.prontuario;

import br.com.saudeConecta.application.prontuario.ProntuarioService;
import br.com.saudeConecta.application.prontuario.dto.CadastrarProntuarioRequest;
import br.com.saudeConecta.application.prontuario.dto.ProntuarioCompletoResponse;
import br.com.saudeConecta.application.prontuario.dto.ProntuarioResponse;
import br.com.saudeConecta.domain.prontuario.Prontuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/prontuario")
@RequiredArgsConstructor
@Slf4j
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
        
        Prontuario prontuario = prontuarioService.cadastrarProntuarioMedico(request);

        // Converter para DTO de resposta para evitar LazyInitializationException
        return ResponseEntity.status(HttpStatus.CREATED).body(ProntuarioResponse.fromEntity(prontuario));
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
        
        Prontuario prontuario = prontuarioService.buscarProntuarioById(consultaId);
        
        ProntuarioCompletoResponse response = ProntuarioCompletoResponse.fromEntity(prontuario);
        return ResponseEntity.ok(response);
    }

    /**
     * Retorna o prontuário médico mais recente de uma consulta.
     * 
     * GET /prontuario/consulta/{consultaId}/recente
     * 
     * @param consultaId ID da consulta
     * @return Prontuário mais recente encontrado
     */
    @GetMapping("/consulta/{consultaId}/recente")
    public ResponseEntity<ProntuarioCompletoResponse> buscarMaisRecente(@PathVariable Long consultaId) {

        Prontuario prontuario = prontuarioService.buscarMaisRecentePorConsulta(consultaId);
        ProntuarioCompletoResponse response = ProntuarioCompletoResponse.fromEntity(prontuario);
        return ResponseEntity.ok(response);
    }

    /**
     * Atualiza um prontuário médico existente.
     * PUT /prontuario/{id}
     */
    @PutMapping("/atualizarProntuarioMedico/{id}")
    public ResponseEntity<Void> atualizarProntuario(
            @PathVariable Long id,
            @RequestBody CadastrarProntuarioRequest request) {

        prontuarioService.atualizarProntuarioMedico(id, request);
        return ResponseEntity.ok().build();
    }

}
