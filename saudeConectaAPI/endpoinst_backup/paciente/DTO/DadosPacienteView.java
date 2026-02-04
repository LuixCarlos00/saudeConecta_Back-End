package br.com.saudeConecta.endpoinst.paciente.DTO;

import br.com.saudeConecta.domain.endereco.Endereco;
import br.com.saudeConecta.domain.paciente.Paciente;
import org.jetbrains.annotations.NotNull;

import java.sql.Date;

public record DadosPacienteView(Long PaciCodigo, String PaciNome, String PaciSexo, Date PaciDataNacimento,
                                String PaciCpf, String PaciRg,String PaciEmail,   String PaciTelefone, Endereco endereco,String PaciStatus

) {
    public DadosPacienteView(@NotNull Paciente registro) {
        this(registro.getPaciCodigo(), registro.getPaciNome(), registro.getPaciSexo(), registro.getPaciDataNacimento(), registro.getPaciCpf(),
                registro.getPaciRg(),registro.getPaciEmail(),   registro.getPaciTelefone(), registro.getEndereco(),registro.getPaciStatus());
    }



}
