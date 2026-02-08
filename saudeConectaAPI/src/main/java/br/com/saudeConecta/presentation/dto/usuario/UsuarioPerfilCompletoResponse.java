package br.com.saudeConecta.presentation.dto.usuario;

import br.com.saudeConecta.domain.admin.AdminOrganizacao;
import br.com.saudeConecta.domain.endereco.Endereco;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.usuario.Usuario;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record UsuarioPerfilCompletoResponse(
    Long usuarioId,
    String login,
    String tipoUsuario,
    Long organizacaoId,
    ProfissionalResumo profissional,
    AdminResumo adminOrganizacao,
    EnderecoResumo endereco
) {
    
    public static UsuarioPerfilCompletoResponse fromEntities(
            Usuario usuario,
            Profissional profissional,
            AdminOrganizacao admin) {
        
        return new UsuarioPerfilCompletoResponse(
            usuario.getId(),
            usuario.getLogin(),
            usuario.getTipoUsuarioNovo() != null ? usuario.getTipoUsuarioNovo().name() : null,
            usuario.getOrganizacao() != null ? usuario.getOrganizacao().getId() : null,
            profissional != null ? ProfissionalResumo.fromEntity(profissional) : null,
            admin != null ? AdminResumo.fromEntity(admin) : null,
            profissional != null && profissional.getEndereco() != null 
                ? EnderecoResumo.fromEntity(profissional.getEndereco()) 
                : null
        );
    }
    
    public record ProfissionalResumo(
        Long id,
        String nome,
        String tipoProfissional,
        String conselho,
        String registroConselho,
        String conselhoFormatado,
        String sexo,
        LocalDate dataNascimento,
        String cpf,
        String email,
        String telefone,
        String formacao,
        String instituicao,
        Integer tempoConsultaMinutos,
        String status,
        List<EspecialidadeResumo> especialidades
    ) {
        public static ProfissionalResumo fromEntity(Profissional p) {
            return new ProfissionalResumo(
                p.getId(),
                p.getNome(),
                p.getTipoProfissional() != null ? p.getTipoProfissional().getNome() : null,
                p.getTipoProfissional() != null ? p.getTipoProfissional().getConselho() : null,
                p.getRegistroConselho(),
                p.getRegistroConselho(),
                p.getSexo() != null ? p.getSexo().name() : null,
                p.getDataNascimento(),
                p.getCpf(),
                p.getEmail(),
                p.getTelefone(),
                p.getFormacao(),
                p.getInstituicao(),
                p.getTempoConsultaMinutos(),
                p.getStatus() != null ? p.getStatus().name() : null,
                p.getEspecialidades() != null ? p.getEspecialidades().stream()
                    .map(e -> new EspecialidadeResumo(e.getId(), e.getNome()))
                    .toList() : List.of()
            );
        }
    }
    
    public record EspecialidadeResumo(Long id, String nome) {}
    
    public record AdminResumo(
        Long id,
        String nome,
        String cargo,
        String email,
        Boolean isOwner,
        String status,
        LocalDateTime createdAt
    ) {
        public static AdminResumo fromEntity(AdminOrganizacao a) {
            return new AdminResumo(
                a.getId(),
                a.getNome(),
                a.getCargo(),
                a.getEmail(),
                a.getIsOwner(),
                a.getStatus() != null ? a.getStatus().name() : null,
                a.getCreatedAt()
            );
        }
    }
    
    public record EnderecoResumo(
        Long endCodigo,
        String endRua,
        Long endNumero,
        String endComplemento,
        String endBairro,
        String endCep,
        String endMunicipio,
        String endUF,
        String endNacionalidade
    ) {
        public static EnderecoResumo fromEntity(Endereco e) {
            return new EnderecoResumo(
                e.getEndCodigo(),
                e.getEndRua(),
                e.getEndNumero(),
                e.getEndComplemento(),
                e.getEndBairro(),
                e.getEndCep(),
                e.getEndMunicipio(),
                e.getEndUF(),
                e.getEndNacionalidade()
            );
        }
    }
}
