package br.com.saudeConecta.endpoinst.medico.DTO;

import br.com.saudeConecta.endpoinst.endereco.Entity.Endereco;
import br.com.saudeConecta.endpoinst.medico.Entity.Medico;
import br.com.saudeConecta.endpoinst.usuario.Entity.Usuario;
import org.jetbrains.annotations.NotNull;

import java.sql.Date;

public record DadosMedicoView(
        Long MedCodigo,
        String MedNome,
        String MedSexo,
        Date MedDataNacimento,
        String MedCrm,
        String MedCpf,
        String MedRg,

        String MedEmail,
        Usuario usuario,
        String MedTelefone,
        Endereco endereco

) {
    public DadosMedicoView(@NotNull Medico registro) {
        this(registro.getMedCodigo(), registro.getMedNome(), registro.getMedSexo(), registro.getMedDataNacimento(), registro.getMedCrm(),
                registro.getMedCpf(), registro.getMedRg(), registro.getMedEmail(),registro.getUsuario(), registro.getMedTelefone(), registro.getEndereco());
    }



}
