package br.com.saudeConecta.endpoinst.prontuario.DTO;

import br.com.saudeConecta.domain.consultastatus.ConsultaStatus;
import br.com.saudeConecta.domain.medico.Medico;
import br.com.saudeConecta.domain.prontuario.Prontuario;
import org.jetbrains.annotations.NotNull;

import java.sql.Date;

public record DadosProntuarioView(

        Long prontCodigoProntuario,
        String prontPeso,
        String prontAltura,
        String prontTemperatura,
        String prontDataNacimento,
        String prontSexo,
        String prontSaturacao,
        String prontHemoglobina,
        String prontPressao,
        String prontFrequenciaRespiratoria,
        String prontFrequenciaArterialSistolica,
        String prontFrequenciaArterialDiastolica,
        String prontObservacao,
        String prontCondulta,
        String prontAnamnese,
        String prontQueixaPricipal,
        String prontDiagnostico,
        String prontModeloPrescricao,
        String prontTituloPrescricao,
        String prontDataPrescricao,
        String prontPrescricao,
        Date prontDataFinalizado,
        Medico prontCodigoMedico,
        ConsultaStatus prontCodigoConsulta,
        String prontTituloExame,
        String prontExame,
        String prontDataExame,
        String prontModeloExame


) {
    public DadosProntuarioView(@NotNull Prontuario registro) {
        this(registro.getProntCodigoProntuario(), registro.getProntPeso(), registro.getProntAltura(), registro.getProntTemperatura(), registro.getProntDataNacimento(),
                registro.getProntSexo(),registro.getProntSaturacao(),   registro.getProntHemoglobina(), registro.getProntPressao(),registro.getProntFrequenciaRespiratoria(),
                registro.getProntFrequenciaArterialSistolica(), registro.getProntFrequenciaArterialDiastolica(), registro.getProntObservacao(), registro.getProntCondulta(),
                registro.getProntAnamnese(),registro.getProntQueixaPricipal() , registro.getProntDiagnostico(), registro.getProntModeloPrescricao(), registro.getProntTituloPrescricao(), registro.getProntDataPrescricao(),
                registro.getProntPrescricao(), registro.getProntDataFinalizado(), registro.getProntCodigoMedico(), registro.getProntCodigoConsulta(),registro.getProntTituloExame(), registro.getProntExame(), registro.getProntDataExame(),
                registro.getProntModeloExame());
    }



}
