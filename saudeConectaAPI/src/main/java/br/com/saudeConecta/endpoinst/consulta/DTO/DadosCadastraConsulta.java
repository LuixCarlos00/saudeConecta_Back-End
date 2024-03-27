package br.com.saudeConecta.endpoinst.consulta.DTO;

import br.com.saudeConecta.endpoinst.consulta.Entity.Consulta;

import br.com.saudeConecta.endpoinst.medico.Entity.Medico;
import br.com.saudeConecta.endpoinst.paciente.Entity.Paciente;
import lombok.extern.java.Log;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.sql.Date;
import java.sql.Time;

public record DadosCadastraConsulta(

        Long ConMedico,
        Long ConPaciente,
        String ConDia_semana,
        Time ConHorario,
        Date ConData,
        String ConObservacoes

) {
//    @Contract(value = " -> new", pure = true)
//    public @NotNull Consulta toEntity() {
//        return new Consulta(ConMedico, ConPaciente, ConDia_semana, ConHorario, ConData, ConObservacoes);
//    }
}
