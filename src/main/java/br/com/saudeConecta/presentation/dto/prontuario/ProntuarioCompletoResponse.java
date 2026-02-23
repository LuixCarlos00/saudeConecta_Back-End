package br.com.saudeConecta.presentation.dto.prontuario;

import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.prontuario.Prontuario;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.util.Date;

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
    private String sexo;
    
    // Anamnese e avaliação
    private String queixaPrincipal;
    private String anamnese;
    private String conduta;
    private String observacao;
    private String diagnostico;
    
    // Prescrição médica
    private String modeloPrescricao;
    private String tituloPrescricao;
    private String dataPrescricao;
    private String prescricao;
    
    // Exames
    private String modeloExame;
    private String tituloExame;
    private String dataExame;
    private String exame;
    private String tempoDuracao;
    
    // Dados de controle
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date dataFinalizado;
    
    // Relacionamentos (DTOs para evitar lazy loading)
    private ProfissionalResponse profissional;
    private ConsultaResponse consulta;
    private PacienteResponse paciente;

    
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
        
        // Converter profissional
        ProfissionalResponse profissionalResponse = null;
        if (prontuario.getProfissional() != null) {
            profissionalResponse = ProfissionalResponse.builder()
                    .id(prontuario.getProfissional().getId())
                    .nome(prontuario.getProfissional().getNome())
                    .conselho(prontuario.getProfissional().getRegistroConselho())
                    .email(prontuario.getProfissional().getEmail())
                    .telefone(prontuario.getProfissional().getTelefone())
                    .build();
        }
        
        // Converter consulta
        ConsultaResponse consultaResponse = null;
        if (prontuario.getConsulta() != null) {
            Consulta consulta = prontuario.getConsulta();
            
            // Converter paciente
            PacienteResponse pacienteResponse = null;
            if (consulta.getPaciente() != null) {
                pacienteResponse = PacienteResponse.builder()
                        .id(consulta.getPaciente().getPaciCodigo())
                        .nome(consulta.getPaciente().getPaciNome())
                        .cpf(consulta.getPaciente().getPaciCpf())
                        .email(consulta.getPaciente().getPaciEmail())
                        .telefone(consulta.getPaciente().getPaciTelefone())
                        .sexo(consulta.getPaciente().getPaciSexo())
                        .dataNascimento(consulta.getPaciente().getPaciDataNacimento() != null ? 
                        consulta.getPaciente().getPaciDataNacimento().toString() : null)
                        .build();
            }
            
            consultaResponse = ConsultaResponse.builder()
                    .id(consulta.getId())
                    .pacienteNome(consulta.getPaciente() != null ? consulta.getPaciente().getPaciNome() : null)
                    .pacienteCpf(consulta.getPaciente() != null ? consulta.getPaciente().getPaciCpf() : null)
                    .dataHora(consulta.getDataHora() != null ? consulta.getDataHora().toString() : null)
                    .status(consulta.getStatus() != null ? consulta.getStatus().name() : null)
                    .observacoes(consulta.getObservacoes())
                    .valor(consulta.getValor() != null ? consulta.getValor().doubleValue() : null)
                    .formaPagamentoNome(consulta.getFormaPagamento() != null ? 
                            consulta.getFormaPagamento().getNome() : null)
                    .paciente(pacienteResponse)
                    .build();
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
                 .sexo(prontuario.getProntSexo())
                .queixaPrincipal(prontuario.getProntQueixaPricipal())
                .anamnese(prontuario.getProntAnamnese())
                .conduta(prontuario.getProntCondulta())
                .observacao(prontuario.getProntObservacao())
                .diagnostico(prontuario.getProntDiagnostico())
                .modeloPrescricao(prontuario.getProntModeloPrescricao())
                .tituloPrescricao(prontuario.getProntTituloPrescricao())
                .dataPrescricao(prontuario.getProntDataPrescricao())
                .prescricao(prontuario.getProntPrescricao())
                .modeloExame(prontuario.getProntModeloExame())
                .tituloExame(prontuario.getProntTituloExame())
                .dataExame(prontuario.getProntDataExame())
                .exame(prontuario.getProntExame())
                .tempoDuracao(prontuario.getProntTempoDuracao())
                .dataFinalizado(prontuario.getProntDataFinalizado())
                .profissional(profissionalResponse)
                .consulta(consultaResponse)
                .build();
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
