package br.com.saudeConecta.application.historico;

import br.com.saudeConecta.application.consulta.dto.HistoricoConsultaPacienteResponse;
import br.com.saudeConecta.config.tenant.RequiresTenant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Use Case responsável por buscar o histórico completo (médico + odontológico)
 * de um paciente para o contexto do Administrador da organização.
 * O Administrador tem visão total: não é aplicado filtro por profissional,
 * e o resultado combina os prontuários médicos e odontológicos, ordenados
 * por data da consulta (mais recente primeiro).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BuscarHistoricoCompletoPacienteAdminUseCase {

    private final BuscarHistoricoCompletoPacienteMedicoUseCase buscarHistoricoCompletoPacienteMedicoUseCase;
    private final BuscarHistoricoCompletoPacienteDentistaUseCase buscarHistoricoCompletoPacienteDentistaUseCase;

    /**
     * Executa a busca do histórico completo (médico + dentista) do paciente,
     * sem restrição de profissional.
     *
     * @param pacienteId ID do paciente
     * @return Lista combinada de DTOs com histórico médico e odontológico
     */
    @RequiresTenant
    @Transactional(readOnly = true)
    public List<HistoricoConsultaPacienteResponse> executar(Long pacienteId) {
        log.debug("historico.paciente buscar perfil=ADMIN pacienteId={}", pacienteId);

        List<HistoricoConsultaPacienteResponse> historicoMedico =
                buscarHistoricoCompletoPacienteMedicoUseCase.executar(pacienteId, null);
        List<HistoricoConsultaPacienteResponse> historicoDentista =
                buscarHistoricoCompletoPacienteDentistaUseCase.executar(pacienteId, null);

        List<HistoricoConsultaPacienteResponse> historicoCompleto = new ArrayList<>();
        historicoCompleto.addAll(historicoMedico);
        historicoCompleto.addAll(historicoDentista);

        historicoCompleto.sort(
                Comparator.comparing(HistoricoConsultaPacienteResponse::getDataHora,
                        Comparator.nullsLast(Comparator.reverseOrder()))
        );

        log.debug("historico.paciente combinado perfil=ADMIN registros={}", historicoCompleto.size());
        return historicoCompleto;
    }
}
