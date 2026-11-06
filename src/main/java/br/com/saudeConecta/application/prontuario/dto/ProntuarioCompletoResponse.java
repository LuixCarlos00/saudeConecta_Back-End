package br.com.saudeConecta.application.prontuario.dto;

import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.prontuario.PlanejamentoTerapeutico;
import br.com.saudeConecta.domain.prontuario.Prontuario;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO completo para resposta do prontuário com todos os campos
 * 
 * @author Sistema SaúdeConecta
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProntuarioCompletoResponse {
    
    // ID do prontuário
    private Long codigoProntuario;
    
    // Dados vitais e antropométricos
    private String peso;
    private String altura;
    private String temperatura;
    private String saturacao;
    private String pressao;
    private String frequenciaRespiratoria;
    private String frequenciaArterialSistolica;
    private String frequenciaArterialDiastolica;
    private String hemoglobina;
    
    // Dados demográficos
    private String dataNascimento;
    
    // Anamnese e avaliação
    private String queixaPrincipal;
    private String anamnese;
    private String observacao;
    private String diagnostico;
    
    // Prescrição médica
    private String modeloPrescricao;
    private String tituloPrescricao;
    private String dataPrescricao;
    private String prescricao;
    
    // Exames
    private String tempoDuracao;
    
    // Dados de controle
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date dataFinalizado;

    // ── Identificação do Paciente ─────────────────────────────────────────────
    private String responsavel;

    // ── Sinais Vitais (campo adicional) ───────────────────────────────────────
    private String pulso;


    // ── Exame Físico ────────────────────────────────────────────────────────────
    private String exameOutros;

    // ── Diagnóstico e Tratamento ──────────────────────────────────────────────
    private String orientacoes;

    // ── TUSS e CID ────────────────────────────────────────────────────────────
    private String tussTexto;
    private String cidTexto;
    private String solicitacaoExameTexto;
    
    // Relacionamentos (DTOs para evitar lazy loading)
    private ProfissionalResponse profissional;
    private ConsultaResponse consulta;

    // Planejamentos terapêuticos
    private List<PlanejamentoTerapeuticoResponse> planejamentos;

    
    /**
     * DTO para informações do Planejamento Terapêutico
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanejamentoTerapeuticoResponse {
        private Long id;
        private String dataProcedimento;
        private String procedimentoRealizado;
        private Double valor;
        private String statusAssinatura;
        private String assinaturaBase64;
        private String dataAssinatura;
        private String ipOrigem;
    }

    
    /**
     * Converte entidade Prontuario para DTO de forma segura
     * 
     * @param prontuario Entidade Prontuario
     * @return DTO ProntuarioCompletoResponse
     */
    public static ProntuarioCompletoResponse fromEntity(Prontuario prontuario) {
        if (prontuario == null) {
            return null;
        }
        
        return ProntuarioCompletoResponse.builder()
                .codigoProntuario(prontuario.getProntCodigoProntuario())
                .peso(prontuario.getProntPeso())
                .altura(prontuario.getProntAltura())
                .temperatura(prontuario.getProntTemperatura())
                .saturacao(prontuario.getProntSaturacao())
                .pressao(prontuario.getProntPressao())
                .frequenciaRespiratoria(prontuario.getProntFrequenciaRespiratoria())
                .frequenciaArterialSistolica(prontuario.getProntFrequenciaArterialSistolica())
                .frequenciaArterialDiastolica(prontuario.getProntFrequenciaArterialDiastolica())
                .hemoglobina(prontuario.getProntHemoglobina())
                .queixaPrincipal(prontuario.getProntQueixaPricipal())
                .anamnese(prontuario.getProntAnamnese())
                .observacao(prontuario.getProntObservacao())
                .diagnostico(prontuario.getProntDiagnostico())
                 .tituloPrescricao(prontuario.getProntTituloPrescricao())
                 .prescricao(prontuario.getProntPrescricao())
                .tempoDuracao(prontuario.getProntTempoDuracao())
                .dataFinalizado(prontuario.getProntDataFinalizado())
                // Novos campos
                .responsavel(prontuario.getProntResponsavel())
                .pulso(prontuario.getProntPulso())
                 .orientacoes(prontuario.getProntOrientacoes())
                .tussTexto(prontuario.getProntTussTexto())
                .cidTexto(prontuario.getProntCidTexto())
                .solicitacaoExameTexto(prontuario.getProntSolicitacaoExameTexto())
                .profissional(converterProfissional(prontuario.getProfissional()))
                .consulta(converterConsulta(prontuario.getConsulta()))
                .planejamentos(converterPlanejamentos(prontuario.getPlanejamentos()))
                .build();
    }

    private static ProfissionalResponse converterProfissional(Profissional profissional) {
        if (profissional == null) {
            return null;
        }
        return ProfissionalResponse.builder()
                .id(profissional.getId())
                .nome(profissional.getNome())
                .conselho(profissional.getRegistroConselho())
                .email(profissional.getEmail())
                .telefone(profissional.getTelefone())
                .build();
    }

    private static PacienteResponse converterPaciente(Paciente paciente) {
        if (paciente == null) {
            return null;
        }
        return PacienteResponse.builder()
                .id(paciente.getPaciCodigo())
                .nome(paciente.getPaciNome())
                .cpf(paciente.getPaciCpf())
                .email(paciente.getPaciEmail())
                .telefone(paciente.getPaciTelefone())
                .sexo(paciente.getPaciSexo())
                .dataNascimento(textoOuNulo(paciente.getPaciDataNacimento()))
                .build();
    }

    private static ConsultaResponse converterConsulta(Consulta consulta) {
        if (consulta == null) {
            return null;
        }
        Paciente paciente = consulta.getPaciente();
        return ConsultaResponse.builder()
                .id(consulta.getId())
                .pacienteNome(paciente != null ? paciente.getPaciNome() : null)
                .pacienteCpf(paciente != null ? paciente.getPaciCpf() : null)
                .dataHora(textoOuNulo(consulta.getDataHora()))
                .status(consulta.getStatus() != null ? consulta.getStatus().name() : null)
                .observacoes(consulta.getObservacoes())
                .valor(doubleOuNulo(consulta.getValor()))
                .formaPagamentoNome(consulta.getFormaPagamento() != null ?
                        consulta.getFormaPagamento().getNome() : null)
                .paciente(converterPaciente(paciente))
                .build();
    }

    private static List<PlanejamentoTerapeuticoResponse> converterPlanejamentos(List<PlanejamentoTerapeutico> planejamentos) {
        if (planejamentos == null) {
            return null;
        }
        return planejamentos.stream()
                .map(ProntuarioCompletoResponse::converterPlanejamento)
                .collect(Collectors.toList());
    }

    private static PlanejamentoTerapeuticoResponse converterPlanejamento(PlanejamentoTerapeutico p) {
        return PlanejamentoTerapeuticoResponse.builder()
                .id(p.getId())
                .dataProcedimento(textoOuNulo(p.getDataProcedimento()))
                .procedimentoRealizado(p.getProcedimentoRealizado())
                .valor(doubleOuNulo(p.getValor()))
                .statusAssinatura(p.getStatusAssinatura())
                .assinaturaBase64(p.getAssinaturaBase64())
                .dataAssinatura(textoOuNulo(p.getDataAssinatura()))
                .ipOrigem(p.getIpOrigem())
                .build();
    }

    private static String textoOuNulo(Object valor) {
        return valor != null ? valor.toString() : null;
    }

    private static Double doubleOuNulo(Number valor) {
        return valor != null ? valor.doubleValue() : null;
    }
    
    /**
     * DTO para informações do Profissional
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfissionalResponse {
        private Long id;
        private String nome;
        private String conselho;
        private String email;
        private String telefone;
    }
    
    /**
     * DTO para informações do Paciente
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PacienteResponse {
        private Long id;
        private String nome;
        private String cpf;
        private String email;
        private String telefone;
        private String sexo;
        private String dataNascimento;
    }
    
    /**
     * DTO para informações da Consulta
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConsultaResponse {
        private Long id;
        private String pacienteNome;
        private String pacienteCpf;
        private String dataHora;
        private String status;
        private String observacoes;
        private Double valor;
        private String formaPagamentoNome;
        private PacienteResponse paciente;
    }
}
