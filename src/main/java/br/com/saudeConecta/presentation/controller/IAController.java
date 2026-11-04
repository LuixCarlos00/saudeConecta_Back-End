package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.service.IALocalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ia")
@RequiredArgsConstructor
@Slf4j
public class IAController {

    private final IALocalService iaLocalService;

    /**
     * Gera resumo do histórico de consultas do paciente usando IA local
     * 
     * POST /ia/resumir-historico
     * 
     * @param request Dados do paciente e tipo de prontuário
     * @return Resumo gerado pela IA local (gratuito, sem API keys)
     */
    @PostMapping("/resumir-historico")
    public ResponseEntity<String> resumirHistorico(@RequestBody ResumirHistoricoRequest request) {
        log.info("=== Requisição recebida: POST /ia/resumir-historico (IA Local) ===");
        log.debug("Paciente ID: {}, Tipo: {}", request.getPacienteId(), request.getTipo());
        
        // Usar IA local - gratuito e sem configuração
        String resumo = iaLocalService.gerarResumoHistorico(request.getPacienteId(), request.getTipo());
        log.info("Resumo gerado com sucesso usando IA local para paciente ID: {}", request.getPacienteId());
        return ResponseEntity.ok(resumo);
    }

    /**
     * DTO para requisição de resumo de histórico
     */
    public static class ResumirHistoricoRequest {
        private Long pacienteId;
        private String tipo; // "medico" ou "dentista"

        // Getters e Setters
        public Long getPacienteId() { return pacienteId; }
        public void setPacienteId(Long pacienteId) { this.pacienteId = pacienteId; }
        public String getTipo() { return tipo; }
        public void setTipo(String tipo) { this.tipo = tipo; }
    }
}
