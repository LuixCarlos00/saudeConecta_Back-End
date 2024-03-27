package br.com.saudeConecta.endpoinst.paciente.DTO;

import br.com.saudeConecta.endpoinst.endereco.Entity.Endereco;
import br.com.saudeConecta.endpoinst.paciente.Entity.Paciente;
import br.com.saudeConecta.endpoinst.usuario.Entity.Usuario;
import org.jetbrains.annotations.NotNull;

import java.sql.Date;

public record DadosPacienteView(Long PaciCodigo, String PaciNome, String PaciSexo, Date PaciDataNacimento,
                                String PaciCpf, String PaciRg, Usuario usuario, String PaciTelefone, Endereco endereco

) {
    public DadosPacienteView(@NotNull Paciente registro) {
        this(registro.getPaciCodigo(), registro.getPaciNome(), registro.getPaciSexo(), registro.getPaciDataNacimento(), registro.getPaciCpf(),
                registro.getPaciRg(), registro.getUsuario(), registro.getPaciTelefone(), registro.getEndereco());
    }



}
