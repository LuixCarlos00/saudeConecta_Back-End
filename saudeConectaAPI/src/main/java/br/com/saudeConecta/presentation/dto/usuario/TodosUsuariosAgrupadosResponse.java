package br.com.saudeConecta.presentation.dto.usuario;

import java.util.List;

/**
 * DTO para retornar todos os usuários agrupados por tipo.
 * Utilizado em: GerenciamentoUsuariosComponent
 */
public record TodosUsuariosAgrupadosResponse(
    List<PacienteResponse> paciente,
    List<MedicoComUsuarioResponse> medico,
    List<Object> secretaria,
    List<AdministradorComUsuarioResponse> administrador
) {}
