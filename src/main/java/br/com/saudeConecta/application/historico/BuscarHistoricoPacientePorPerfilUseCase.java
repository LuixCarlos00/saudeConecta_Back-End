package br.com.saudeConecta.application.historico;

import br.com.saudeConecta.application.consulta.dto.HistoricoConsultaPacienteResponse;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Escolhe qual histórico do paciente o usuário logado pode ver, conforme o perfil:
 * <ul>
 *   <li>gestor/ROOT (não clínico): histórico completo, médico e odontológico;</li>
 *   <li>clínico dentista: só os prontuários odontológicos de que ele é autor;</li>
 *   <li>clínico médico (ou outro tipo): só os prontuários médicos de que ele é autor.</li>
 * </ul>
 *
 * <p>Saiu do {@code ConsultaController} na fase B6 (o controller lia o {@code ProfissionalRepository}
 * direto). Não abre transação própria, como antes: cada caso de uso chamado abre a sua.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BuscarHistoricoPacientePorPerfilUseCase {

    private final BuscarHistoricoCompletoPacienteDentistaUseCase buscarHistoricoCompletoPacienteDentistaUseCase;
    private final BuscarHistoricoCompletoPacienteMedicoUseCase buscarHistoricoCompletoPacienteMedicoUseCase;
    private final BuscarHistoricoCompletoPacienteAdminUseCase buscarHistoricoCompletoPacienteAdminUseCase;
    private final ProfissionalRepository profissionalRepository;

    /**
     * Busca o histórico do paciente visível para o usuário logado.
     *
     * @param pacienteId    ID do paciente
     * @param usuarioLogado usuário do token
     * @return o histórico; vazio quando o usuário é clínico mas não tem cadastro de profissional,
     *         ou quando o caso de uso devolve lista nula (o controller responde 404)
     */
    public Optional<List<HistoricoConsultaPacienteResponse>> executar(Long pacienteId, Usuario usuarioLogado) {
        if (!usuarioLogado.isClinico()) {
            return Optional.ofNullable(buscarHistoricoCompletoPacienteAdminUseCase.executar(pacienteId));
        }

        Optional<Profissional> profissionalLogado =
                profissionalRepository.findByUsuarioIdWithRelations(usuarioLogado.getId());
        if (profissionalLogado.isEmpty()) {
            log.warn("consulta.historico negado motivo=PROFISSIONAL_NAO_ENCONTRADO usuarioId={}", usuarioLogado.getId());
            return Optional.empty();
        }

        Profissional profissional = profissionalLogado.get();
        Long profissionalId = profissional.getId();
        if (profissional.getTipoProfissional() != null && profissional.getTipoProfissional().isDentista()) {
            return Optional.ofNullable(buscarHistoricoCompletoPacienteDentistaUseCase.executar(pacienteId, profissionalId));
        }
        return Optional.ofNullable(buscarHistoricoCompletoPacienteMedicoUseCase.executar(pacienteId, profissionalId));
    }
}
