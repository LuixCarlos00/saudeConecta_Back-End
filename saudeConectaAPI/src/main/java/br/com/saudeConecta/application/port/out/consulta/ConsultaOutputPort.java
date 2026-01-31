package br.com.saudeConecta.application.port.out.consulta;

import br.com.saudeConecta.domain.consulta.Consulta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

public interface ConsultaOutputPort {
    
    Optional<Consulta> findById(Long id);
    
    List<Consulta> findAll();
    
    Page<Consulta> findAll(Pageable pageable);
    
    Optional<Consulta> findByConHorarioAndConDataAndConMedico_MedCodigo(String conHorario, Date conData, Long medCodigo);
    
    boolean existsByConHorarioAndConDataAndConMedico_MedCodigo(String conHorario, String conData, Long medCodigo);
    
    List<Consulta> findByConMedico_MedCodigoAndConData(Long medCodigo, String conData);
    
    List<Consulta> findByConMedico_MedCodigo(Long medCodigo);
    
    List<Consulta> buscarConsultasEmIntervaloDeDatas(String dataInicial, String dataFinal);
    
    List<Consulta> buscarConsultasEmIntervaloComEspecialidade(String dataInicial, String dataFinal, String especialidade);
    
    List<Consulta> buscarConsultasPorMedico(Long medicoId);
    
    List<Consulta> buscarConsultasPorMedicoEmIntervalo(String dataInicial, String dataFinal, Long medicoId);
    
    List<Consulta> buscarConsultasPorEspecialidade(String especialidade);
    
    List<Object[]> contarConsultasPorStatusEMedico(Long medicoId, String dataInicial, String dataFinal);
    
    List<Consulta> findByConPaciente_PaciCodigo(Long paciCodigo);
    
    List<Consulta> buscarConsultasPorIntervaloDeDatas(String dataInicial, String dataFinal);
    
    // Consultas concluídas
    List<Consulta> buscarConsultasConcluidasEmIntervaloDeDatas(String dataInicial, String dataFinal);
    
    List<Consulta> buscarConsultasConcluidasEmIntervaloComEspecialidade(String dataInicial, String dataFinal, String especialidade);
    
    List<Consulta> buscarConsultasConcluidasPorMedico(Long medicoId);
    
    List<Consulta> buscarConsultasConcluidasPorMedicoEmIntervalo(String dataInicial, String dataFinal, Long medicoId);
    
    List<Consulta> buscarConsultasConcluidasPorEspecialidade(String especialidade);
    
    List<Consulta> buscarConsultasConcluidasPorMedicoEEspecialidade(Long medicoId, String especialidade);
    
    List<Consulta> buscarConsultasConcluidasPorMedicoEspecialidadeEmIntervalo(Long medicoId, String especialidade, String dataInicial, String dataFinal);
    
    // Estatísticas
    Long contarConsultasPorData(String data);
    
    Long contarConsultasPorDataEUsuario(String data, Long usuarioId);
    
    Long contarConsultasRealizadasPorData(String data);
    
    Long contarConsultasRealizadasPorDataEUsuario(String data, Long usuarioId);
    
    Long contarConsultasAgendadasPorData(String data);
    
    Long contarConsultasAgendadasPorDataEUsuario(String data, Long usuarioId);
    
    Long contarConsultasDaSemana(String dataInicial, String dataFinal);
    
    Long contarConsultasDaSemanaPorUsuario(String dataInicial, String dataFinal, Long usuarioId);
    
    Consulta save(Consulta consulta);
    
    void deleteById(Long id);
    
    boolean existsById(Long id);
}
