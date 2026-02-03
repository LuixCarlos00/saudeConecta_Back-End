package br.com.saudeConecta.endpoinst.prontuario.DTO;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;

import java.sql.Date;

public record DadosCadastraProntuario(

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


        Long prontCodigoMedico,


        Long prontCodigoConsulta,




        String prontTituloExame,


        String prontExame,


        String prontDataExame,


        String prontModeloExame,

        String prontTempoDuracao







) {

}
