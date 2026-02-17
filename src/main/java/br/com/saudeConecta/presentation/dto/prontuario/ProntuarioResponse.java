package br.com.saudeConecta.presentation.dto.prontuario;

import br.com.saudeConecta.domain.prontuario.Prontuario;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;

/**
 * DTO de resposta para Prontuário Médico
 * Evita problemas de LazyInitializationException ao serializar entidades relacionadas
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProntuarioResponse {
    
    private Long prontCodigoProntuario;
    private String prontPeso;
    private String prontAltura;
    private String prontTemperatura;
    private String prontDataNacimento;
    private String prontSexo;
    private String prontSaturacao;
    private String prontHemoglobina;
    private String prontPressao;
    private String prontFrequenciaRespiratoria;
    private String prontFrequenciaArterialSistolica;
    private String prontFrequenciaArterialDiastolica;
    private String prontObservacao;
    private String prontCondulta;
    private String prontAnamnese;
    private String prontQueixaPricipal;
    private String prontDiagnostico;
    private String prontModeloPrescricao;
    private String prontTituloPrescricao;
    private String prontDataPrescricao;
    private String prontPrescricao;
    private Date prontDataFinalizado;
    private String prontModeloExame;
    private String prontTituloExame;
    private String prontDataExame;
    private String prontExame;
    private String prontTempoDuracao;
    
    // IDs dos relacionamentos (sem carregar as entidades completas)
    private Long profissionalId;
    private Long consultaId;
    
    /**
     * Converte entidade Prontuario para DTO de resposta
     * @param prontuario Entidade do prontuário
     * @return DTO de resposta
     */
    public static ProntuarioResponse fromEntity(Prontuario prontuario) {
        ProntuarioResponse response = new ProntuarioResponse();
        
        response.setProntCodigoProntuario(prontuario.getProntCodigoProntuario());
        response.setProntPeso(prontuario.getProntPeso());
        response.setProntAltura(prontuario.getProntAltura());
        response.setProntTemperatura(prontuario.getProntTemperatura());
        response.setProntDataNacimento(prontuario.getProntDataNacimento());
        response.setProntSexo(prontuario.getProntSexo());
        response.setProntSaturacao(prontuario.getProntSaturacao());
        response.setProntHemoglobina(prontuario.getProntHemoglobina());
        response.setProntPressao(prontuario.getProntPressao());
        response.setProntFrequenciaRespiratoria(prontuario.getProntFrequenciaRespiratoria());
        response.setProntFrequenciaArterialSistolica(prontuario.getProntFrequenciaArterialSistolica());
        response.setProntFrequenciaArterialDiastolica(prontuario.getProntFrequenciaArterialDiastolica());
        response.setProntObservacao(prontuario.getProntObservacao());
        response.setProntCondulta(prontuario.getProntCondulta());
        response.setProntAnamnese(prontuario.getProntAnamnese());
        response.setProntQueixaPricipal(prontuario.getProntQueixaPricipal());
        response.setProntDiagnostico(prontuario.getProntDiagnostico());
        response.setProntModeloPrescricao(prontuario.getProntModeloPrescricao());
        response.setProntTituloPrescricao(prontuario.getProntTituloPrescricao());
        response.setProntDataPrescricao(prontuario.getProntDataPrescricao());
        response.setProntPrescricao(prontuario.getProntPrescricao());
        response.setProntDataFinalizado(prontuario.getProntDataFinalizado());
        response.setProntModeloExame(prontuario.getProntModeloExame());
        response.setProntTituloExame(prontuario.getProntTituloExame());
        response.setProntDataExame(prontuario.getProntDataExame());
        response.setProntExame(prontuario.getProntExame());
        response.setProntTempoDuracao(prontuario.getProntTempoDuracao());
        
        // Apenas IDs, sem carregar entidades relacionadas
        if (prontuario.getProfissional() != null) {
            response.setProfissionalId(prontuario.getProfissional().getId());
        }
        if (prontuario.getConsulta() != null) {
            response.setConsultaId(prontuario.getConsulta().getId());
        }
        
        return response;
    }
}
