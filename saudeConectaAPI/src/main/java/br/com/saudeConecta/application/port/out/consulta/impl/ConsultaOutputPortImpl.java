package br.com.saudeConecta.application.port.out.consulta.impl;

import br.com.saudeConecta.application.port.out.consulta.ConsultaOutputPort;
import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.infrastructure.persistence.repository.ConsultaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ConsultaOutputPortImpl implements ConsultaOutputPort {

    private final ConsultaRepository consultaRepository;

    @Override
    public Optional<Consulta> findById(Long id) {
        return consultaRepository.findById(id);
    }

    @Override
    public List<Consulta> findAll() {
        return consultaRepository.findAll();
    }

    @Override
    public Page<Consulta> findAll(Pageable pageable) {
        return consultaRepository.findAll(pageable);
    }

    @Override
    public Optional<Consulta> findByConHorarioAndConDataAndConMedico_MedCodigo(String conHorario, Date conData, Long medCodigo) {
        return consultaRepository.findByConHorarioAndConDataAndConMedico_MedCodigo(conHorario, conData, medCodigo);
    }

    @Override
    public boolean existsByConHorarioAndConDataAndConMedico_MedCodigo(String conHorario, String conData, Long medCodigo) {
        return consultaRepository.existsByConHorarioAndConDataAndConMedico_MedCodigo(conHorario, conData, medCodigo);
    }

    @Override
    public List<Consulta> findByConMedico_MedCodigoAndConData(Long medCodigo, String conData) {
        return consultaRepository.findByConMedico_MedCodigoAndConData(medCodigo, conData);
    }

    @Override
    public List<Consulta> findByConMedico_MedCodigo(Long medCodigo) {
        return consultaRepository.findByConMedico_MedCodigo(medCodigo);
    }

    @Override
    public List<Consulta> buscarConsultasEmIntervaloDeDatas(String dataInicial, String dataFinal) {
        return consultaRepository.buscarConsultasEmIntervaloDeDatas(dataInicial, dataFinal);
    }

    @Override
    public List<Consulta> buscarConsultasEmIntervaloComEspecialidade(String dataInicial, String dataFinal, String especialidade) {
        return consultaRepository.buscarConsultasEmIntervaloComEspecialidade(dataInicial, dataFinal, especialidade);
    }

    @Override
    public List<Consulta> buscarConsultasPorMedico(Long medicoId) {
        return consultaRepository.buscarConsultasPorMedico(medicoId);
    }

    @Override
    public List<Consulta> buscarConsultasPorMedicoEmIntervalo(String dataInicial, String dataFinal, Long medicoId) {
        return consultaRepository.buscarConsultasPorMedicoEmIntervalo(dataInicial, dataFinal, medicoId);
    }

    @Override
    public List<Consulta> buscarConsultasPorEspecialidade(String especialidade) {
        return consultaRepository.buscarConsultasPorEspecialidade(especialidade);
    }

    @Override
    public List<Object[]> contarConsultasPorStatusEMedico(Long medicoId, String dataInicial, String dataFinal) {
        return consultaRepository.contarConsultasPorStatusEMedico(medicoId, dataInicial, dataFinal);
    }

    @Override
    public List<Consulta> findByConPaciente_PaciCodigo(Long paciCodigo) {
        return consultaRepository.findByConPaciente_PaciCodigo(paciCodigo);
    }

    @Override
    public List<Consulta> buscarConsultasPorIntervaloDeDatas(String dataInicial, String dataFinal) {
        return consultaRepository.buscarConsultasPorIntervaloDeDatas(dataInicial, dataFinal);
    }

    @Override
    public List<Consulta> buscarConsultasConcluidasEmIntervaloDeDatas(String dataInicial, String dataFinal) {
        return consultaRepository.buscarConsultasConcluidasEmIntervaloDeDatas(dataInicial, dataFinal);
    }

    @Override
    public List<Consulta> buscarConsultasConcluidasEmIntervaloComEspecialidade(String dataInicial, String dataFinal, String especialidade) {
        return consultaRepository.buscarConsultasConcluidasEmIntervaloComEspecialidade(dataInicial, dataFinal, especialidade);
    }

    @Override
    public List<Consulta> buscarConsultasConcluidasPorMedico(Long medicoId) {
        return consultaRepository.buscarConsultasConcluidasPorMedico(medicoId);
    }

    @Override
    public List<Consulta> buscarConsultasConcluidasPorMedicoEmIntervalo(String dataInicial, String dataFinal, Long medicoId) {
        return consultaRepository.buscarConsultasConcluidasPorMedicoEmIntervalo(dataInicial, dataFinal, medicoId);
    }

    @Override
    public List<Consulta> buscarConsultasConcluidasPorEspecialidade(String especialidade) {
        return consultaRepository.buscarConsultasConcluidasPorEspecialidade(especialidade);
    }

    @Override
    public List<Consulta> buscarConsultasConcluidasPorMedicoEEspecialidade(Long medicoId, String especialidade) {
        return consultaRepository.buscarConsultasConcluidasPorMedicoEEspecialidade(medicoId, especialidade);
    }

    @Override
    public List<Consulta> buscarConsultasConcluidasPorMedicoEspecialidadeEmIntervalo(Long medicoId, String especialidade, String dataInicial, String dataFinal) {
        return consultaRepository.buscarConsultasConcluidasPorMedicoEspecialidadeEmIntervalo(medicoId, especialidade, dataInicial, dataFinal);
    }

    @Override
    public Long contarConsultasPorData(String data) {
        return consultaRepository.contarConsultasPorData(data);
    }

    @Override
    public Long contarConsultasPorDataEUsuario(String data, Long usuarioId) {
        return consultaRepository.contarConsultasPorDataEUsuario(data, usuarioId);
    }

    @Override
    public Long contarConsultasRealizadasPorData(String data) {
        return consultaRepository.contarConsultasRealizadasPorData(data);
    }

    @Override
    public Long contarConsultasRealizadasPorDataEUsuario(String data, Long usuarioId) {
        return consultaRepository.contarConsultasRealizadasPorDataEUsuario(data, usuarioId);
    }

    @Override
    public Long contarConsultasAgendadasPorData(String data) {
        return consultaRepository.contarConsultasAgendadasPorData(data);
    }

    @Override
    public Long contarConsultasAgendadasPorDataEUsuario(String data, Long usuarioId) {
        return consultaRepository.contarConsultasAgendadasPorDataEUsuario(data, usuarioId);
    }

    @Override
    public Long contarConsultasDaSemana(String dataInicial, String dataFinal) {
        return consultaRepository.contarConsultasDaSemana(dataInicial, dataFinal);
    }

    @Override
    public Long contarConsultasDaSemanaPorUsuario(String dataInicial, String dataFinal, Long usuarioId) {
        return consultaRepository.contarConsultasDaSemanaPorUsuario(dataInicial, dataFinal, usuarioId);
    }

    @Override
    public Consulta save(Consulta consulta) {
        return consultaRepository.save(consulta);
    }

    @Override
    public void deleteById(Long id) {
        consultaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return consultaRepository.existsById(id);
    }
}
