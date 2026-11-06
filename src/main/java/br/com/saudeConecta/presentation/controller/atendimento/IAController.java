package br.com.saudeConecta.presentation.controller.atendimento;

import br.com.saudeConecta.application.atendimento.IALocalService;
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
        
        // Usar IA local - gratuito e sem configuração
        String resumo = iaLocalService.gerarResumoHistorico(request.getPacienteId(), request.getTipo());
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
