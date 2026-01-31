package br.com.saudeConecta.application.port.in.consulta;

import br.com.saudeConecta.domain.consulta.Consulta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

public interface ConsultaInputPort {
    
    Optional<Consulta> buscarPorId(Long id);
    
    List<Consulta> buscarTodos();
    
    Page<Consulta> buscarTodos(Pageable pageable);
    
    Optional<Consulta> buscarPorHorarioDataEMedico(String horario, Date data, Long medicoId);
    
    boolean existePorHorarioDataEMedico(String horario, String data, Long medicoId);
    
    List<Consulta> buscarPorMedicoEData(Long medicoId, String data);
    
    List<Consulta> buscarPorMedico(Long medicoId);
    
    List<Consulta> buscarEmIntervaloDatas(String dataInicial, String dataFinal);
    
    List<Consulta> buscarEmIntervaloComEspecialidade(String dataInicial, String dataFinal, String especialidade);
    
    List<Consulta> buscarPorMedicoEmIntervalo(Long medicoId, String dataInicial, String dataFinal);
    
    List<Consulta> buscarPorEspecialidade(String especialidade);
    
    List<Object[]> contarPorStatusEMedico(Long medicoId, String dataInicial, String dataFinal);
    
    List<Consulta> buscarPorPaciente(Long pacienteId);
    
    // Consultas concluídas
    List<Consulta> buscarConcluidasEmIntervalo(String dataInicial, String dataFinal);
    
    List<Consulta> buscarConcluidasEmIntervaloComEspecialidade(String dataInicial, String dataFinal, String especialidade);
    
    List<Consulta> buscarConcluidasPorMedico(Long medicoId);
    
    List<Consulta> buscarConcluidasPorMedicoEmIntervalo(Long medicoId, String dataInicial, String dataFinal);
    
    List<Consulta> buscarConcluidasPorEspecialidade(String especialidade);
    
    List<Consulta> buscarConcluidasPorMedicoEEspecialidade(Long medicoId, String especialidade);
    
    List<Consulta> buscarConcluidasPorMedicoEspecialidadeEmIntervalo(Long medicoId, String especialidade, String dataInicial, String dataFinal);
    
    // Estatísticas
    Long contarPorData(String data);
    
    Long contarPorDataEUsuario(String data, Long usuarioId);
    
    Long contarRealizadasPorData(String data);
    
    Long contarRealizadasPorDataEUsuario(String data, Long usuarioId);
    
    Long contarAgendadasPorData(String data);
    
    Long contarAgendadasPorDataEUsuario(String data, Long usuarioId);
    
    Long contarDaSemana(String dataInicial, String dataFinal);
    
    Long contarDaSemanaPorUsuario(String dataInicial, String dataFinal, Long usuarioId);
    
    Consulta cadastrar(Consulta consulta);
    
    void deletar(Long id) throws Exception;
}
