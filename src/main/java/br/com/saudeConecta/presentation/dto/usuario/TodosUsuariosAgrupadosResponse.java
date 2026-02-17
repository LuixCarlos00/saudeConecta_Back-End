package br.com.saudeConecta.presentation.dto.usuario;

import br.com.saudeConecta.domain.admin.AdminOrganizacao;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.secretaria.Secretaria;
import br.com.saudeConecta.domain.usuario.Usuario;

import java.util.List;

public record TodosUsuariosAgrupadosResponse(
    List<PacienteResumo> paciente,
    List<ProfissionalResumo> clinico,
    List<SecretariaResumo> secretaria,
    List<AdminResumo> administrador
) {
    
    public record PacienteResumo(
        Long id,
        String nome,
        String cpf,
        String email,
        String telefone,
        String status
    ) {
        public static PacienteResumo fromEntity(Paciente p) {
            return new PacienteResumo(
                p.getPaciCodigo(),
                p.getPaciNome(),
                p.getPaciCpf(),
                p.getPaciEmail(),
                p.getPaciTelefone(),
                p.getPaciStatus()
            );
        }
    }
    
    public record ProfissionalResumo(
        Long id,
        String nome,
        String cpf,
        String email,
        String telefone,
        String tipoProfissional,
        String registroConselho,
        String status,
        Long usuarioId,
        String usuarioLogin
    ) {
        public static ProfissionalResumo fromEntity(Profissional p) {
            return new ProfissionalResumo(
                p.getId(),
                p.getNome(),
                p.getCpf(),
                p.getEmail(),
                p.getTelefone(),
                p.getTipoProfissional() != null ? p.getTipoProfissional().getNome() : null,
                p.getRegistroConselho(),
                p.getStatus() != null ? p.getStatus().name() : null,
                p.getUsuario() != null ? p.getUsuario().getId() : null,
                p.getUsuario() != null ? p.getUsuario().getLogin() : null
            );
        }
    }
    
    public record SecretariaResumo(
        Long id,
        String nome,
        String cpf,
        String email,
        String telefone,
        String status,
        Long usuarioId,
        String usuarioLogin
    ) {
        public static SecretariaResumo fromEntity(Secretaria s) {
            return new SecretariaResumo(
                s.getId(),
                s.getNome(),
                s.getCpf(),
                s.getEmail(),
                s.getTelefone(),
                s.getStatus() != null ? s.getStatus().name() : null,
                s.getUsuario() != null ? s.getUsuario().getId() : null,
                s.getUsuario() != null ? s.getUsuario().getLogin() : null
            );
        }
    }
    
    public record AdminResumo(
        Long id,
        String nome,
        String cargo,
        String email,
        Boolean isOwner,
        String status,
        Long usuarioId,
        String usuarioLogin
    ) {
        public static AdminResumo fromEntity(AdminOrganizacao a) {
            return new AdminResumo(
                a.getId(),
                a.getNome(),
                a.getCargo(),
                a.getEmail(),
                a.getIsOwner(),
                a.getStatus() != null ? a.getStatus().name() : null,
                a.getUsuario() != null ? a.getUsuario().getId() : null,
                a.getUsuario() != null ? a.getUsuario().getLogin() : null
            );
        }
    }
}
