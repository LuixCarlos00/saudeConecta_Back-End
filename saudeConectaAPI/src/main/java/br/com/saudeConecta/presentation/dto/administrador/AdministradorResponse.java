package br.com.saudeConecta.presentation.dto.administrador;

import br.com.saudeConecta.domain.administrador.Administrador;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public record AdministradorResponse(
        Long admCodigo,
        String admNome,
        Byte admStatus,
        @JsonFormat(pattern = "dd/MM/yyyy")
        LocalDate admDataCriacao,
        String admEmail,
        String admCodigoAtorizacao,
        Long admUsuarioId
) {
    public AdministradorResponse(Administrador administrador) {
        this(
                administrador.getAdmCodigo(),
                administrador.getAdmNome(),
                administrador.getAdmStatus(),
                administrador.getAdmDataCriacao() != null ? 
                    administrador.getAdmDataCriacao().toLocalDate() : null,
                administrador.getAdmEmail(),
                administrador.getAdmCodigoAtorizacao(),
                administrador.getAdmUsuario() != null ? 
                    administrador.getAdmUsuario().getId() : null
        );
    }
}
