package br.com.saudeConecta.application.service;

import br.com.saudeConecta.application.port.in.consulta.ConsultaInputPort;
import br.com.saudeConecta.application.port.out.consulta.ConsultaOutputPort;
import br.com.saudeConecta.domain.consulta.Consulta;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsultaService implements ConsultaInputPort {

    private final ConsultaOutputPort consultaOutputPort;

    @Override
    public Optional<Consulta> buscarPorId(Long id) {
        log.debug("Buscando consulta por ID: {}", id);
        return consultaOutputPort.findById(id);
    }

    @Override
    public List<Consulta> buscarTodos() {
        log.debug("Buscando todas as consultas");
        return consultaOutputPort.findAll();
    }

    @Override
    public Page<Consulta> buscarTodos(Pageable pageable) {
        log.debug("Buscando todas as consultas com paginação");
        return consultaOutputPort.findAll(pageable);
    }

    @Override
    public Optional<Consulta> buscarPorHorarioDataEMedico(String horario, Date data, Long medicoId) {
        log.debug("Buscando consulta por horário, data e médico: {} {} {}", horario, data, medicoId);
        return consultaOutputPort.findByConHorarioAndConDataAndConMedico_MedCodigo(horario, data, medicoId);
    }

    @Override
    public boolean existePorHorarioDataEMedico(String horario, String data, Long medicoId) {
        log.debug("Verificando existência de consulta por horário, data e médico: {} {} {}", horario, data, medicoId);
        return consultaOutputPort.existsByConHorarioAndConDataAndConMedico_MedCodigo(horario, data, medicoId);
    }

    @Override
    public List<Consulta> buscarPorMedicoEData(Long medicoId, String data) {
        log.debug("Buscando consultas por médico e data: {} {}", medicoId, data);
        return consultaOutputPort.findByConMedico_MedCodigoAndConData(medicoId, data);
    }

    @Override
    public List<Consulta> buscarPorMedico(Long medicoId) {
        log.debug("Buscando consultas por médico: {}", medicoId);
        return consultaOutputPort.findByConMedico_MedCodigo(medicoId);
    }

    @Override
    public List<Consulta> buscarEmIntervaloDatas(String dataInicial, String dataFinal) {
        log.debug("Buscando consultas em intervalo de datas: {} a {}", dataInicial, dataFinal);
        return consultaOutputPort.buscarConsultasEmIntervaloDeDatas(dataInicial, dataFinal);
    }

    @Override
    public List<Consulta> buscarEmIntervaloComEspecialidade(String dataInicial, String dataFinal, String especialidade) {
        log.debug("Buscando consultas em intervalo com especialidade: {} a {} - {}", dataInicial, dataFinal, especialidade);
        return consultaOutputPort.buscarConsultasEmIntervaloComEspecialidade(dataInicial, dataFinal, especialidade);
    }

    @Override
    public List<Consulta> buscarPorMedicoEmIntervalo(Long medicoId, String dataInicial, String dataFinal) {
        log.debug("Buscando consultas por médico em intervalo: {} {} a {}", medicoId, dataInicial, dataFinal);
        return consultaOutputPort.buscarConsultasPorMedicoEmIntervalo(dataInicial, dataFinal, medicoId);
    }

    @Override
    public List<Consulta> buscarPorEspecialidade(String especialidade) {
        log.debug("Buscando consultas por especialidade: {}", especialidade);
        return consultaOutputPort.buscarConsultasPorEspecialidade(especialidade);
    }

    @Override
    public List<Object[]> contarPorStatusEMedico(Long medicoId, String dataInicial, String dataFinal) {
        log.debug("Contando consultas por status e médico: {} {} a {}", medicoId, dataInicial, dataFinal);
        return consultaOutputPort.contarConsultasPorStatusEMedico(medicoId, dataInicial, dataFinal);
    }

    @Override
    public List<Consulta> buscarPorPaciente(Long pacienteId) {
        log.debug("Buscando consultas por paciente: {}", pacienteId);
        return consultaOutputPort.findByConPaciente_PaciCodigo(pacienteId);
    }

    // Consultas concluídas
    @Override
    public List<Consulta> buscarConcluidasEmIntervalo(String dataInicial, String dataFinal) {
        log.debug("Buscando consultas concluídas em intervalo: {} a {}", dataInicial, dataFinal);
        return consultaOutputPort.buscarConsultasConcluidasEmIntervaloDeDatas(dataInicial, dataFinal);
    }

    @Override
    public List<Consulta> buscarConcluidasEmIntervaloComEspecialidade(String dataInicial, String dataFinal, String especialidade) {
        log.debug("Buscando consultas concluídas em intervalo com especialidade: {} a {} - {}", dataInicial, dataFinal, especialidade);
        return consultaOutputPort.buscarConsultasConcluidasEmIntervaloComEspecialidade(dataInicial, dataFinal, especialidade);
    }

    @Override
    public List<Consulta> buscarConcluidasPorMedico(Long medicoId) {
        log.debug("Buscando consultas concluídas por médico: {}", medicoId);
        return consultaOutputPort.buscarConsultasConcluidasPorMedico(medicoId);
    }

    @Override
    public List<Consulta> buscarConcluidasPorMedicoEmIntervalo(Long medicoId, String dataInicial, String dataFinal) {
        log.debug("Buscando consultas concluídas por médico em intervalo: {} {} a {}", medicoId, dataInicial, dataFinal);
        return consultaOutputPort.buscarConsultasConcluidasPorMedicoEmIntervalo(dataInicial, dataFinal, medicoId);
    }

    @Override
    public List<Consulta> buscarConcluidasPorEspecialidade(String especialidade) {
        log.debug("Buscando consultas concluídas por especialidade: {}", especialidade);
        return consultaOutputPort.buscarConsultasConcluidasPorEspecialidade(especialidade);
    }

    @Override
    public List<Consulta> buscarConcluidasPorMedicoEEspecialidade(Long medicoId, String especialidade) {
        log.debug("Buscando consultas concluídas por médico e especialidade: {} - {}", medicoId, especialidade);
        return consultaOutputPort.buscarConsultasConcluidasPorMedicoEEspecialidade(medicoId, especialidade);
    }

    @Override
    public List<Consulta> buscarConcluidasPorMedicoEspecialidadeEmIntervalo(Long medicoId, String especialidade, String dataInicial, String dataFinal) {
        log.debug("Buscando consultas concluídas por médico e especialidade em intervalo: {} - {} {} a {}", medicoId, especialidade, dataInicial, dataFinal);
        return consultaOutputPort.buscarConsultasConcluidasPorMedicoEspecialidadeEmIntervalo(medicoId, especialidade, dataInicial, dataFinal);
    }

    // Estatísticas
    @Override
    public Long contarPorData(String data) {
        log.debug("Contando consultas por data: {}", data);
        return consultaOutputPort.contarConsultasPorData(data);
    }

    @Override
    public Long contarPorDataEUsuario(String data, Long usuarioId) {
        log.debug("Contando consultas por data e usuário: {} {}", data, usuarioId);
        return consultaOutputPort.contarConsultasPorDataEUsuario(data, usuarioId);
    }

    @Override
    public Long contarRealizadasPorData(String data) {
        log.debug("Contando consultas realizadas por data: {}", data);
        return consultaOutputPort.contarConsultasRealizadasPorData(data);
    }

    @Override
    public Long contarRealizadasPorDataEUsuario(String data, Long usuarioId) {
        log.debug("Contando consultas realizadas por data e usuário: {} {}", data, usuarioId);
        return consultaOutputPort.contarConsultasRealizadasPorDataEUsuario(data, usuarioId);
    }

    @Override
    public Long contarAgendadasPorData(String data) {
        log.debug("Contando consultas agendadas por data: {}", data);
        return consultaOutputPort.contarConsultasAgendadasPorData(data);
    }

    @Override
    public Long contarAgendadasPorDataEUsuario(String data, Long usuarioId) {
        log.debug("Contando consultas agendadas por data e usuário: {} {}", data, usuarioId);
        return consultaOutputPort.contarConsultasAgendadasPorDataEUsuario(data, usuarioId);
    }

    @Override
    public Long contarDaSemana(String dataInicial, String dataFinal) {
        log.debug("Contando consultas da semana: {} a {}", dataInicial, dataFinal);
        return consultaOutputPort.contarConsultasDaSemana(dataInicial, dataFinal);
    }

    @Override
    public Long contarDaSemanaPorUsuario(String dataInicial, String dataFinal, Long usuarioId) {
        log.debug("Contando consultas da semana por usuário: {} a {} - {}", dataInicial, dataFinal, usuarioId);
        return consultaOutputPort.contarConsultasDaSemanaPorUsuario(dataInicial, dataFinal, usuarioId);
    }

    @Override
    public Consulta cadastrar(Consulta consulta) {
        log.info("Cadastrando nova consulta: {}", consulta.getConCodigoConsulta());
        Consulta consultaSalva = consultaOutputPort.save(consulta);
        log.info("Consulta cadastrada com sucesso. ID: {}", consultaSalva.getConCodigoConsulta());
        return consultaSalva;
    }

    @Override
    public void deletar(Long id) throws Exception {
        log.info("Iniciando exclusão da consulta ID: {}", id);

        if (id == null || id <= 0) {
            log.warn("Tentativa de exclusão com ID inválido: {}", id);
            throw new IllegalArgumentException("ID inválido");
        }

        if (!consultaOutputPort.existsById(id)) {
            log.warn("Consulta não encontrada para exclusão ID: {}", id);
            throw new Exception("Registro não encontrado");
        }

        try {
            consultaOutputPort.deleteById(id);
            log.info("Consulta ID: {} excluída com sucesso", id);
        } catch (Exception e) {
            log.error("Erro ao excluir consulta ID: {}", id, e);
            throw new Exception("Violação de Integridade", e);
        }
    }

    // ==========================================
    // MÉTODOS DE ESTATÍSTICAS
    // ==========================================

    public Long contarConsultasAgendadasHoje() {
        String dataHoje = java.time.LocalDate.now().toString();
        return consultaOutputPort.contarConsultasAgendadasPorData(dataHoje);
    }

    public Long contarConsultasHoje() {
        String dataHoje = java.time.LocalDate.now().toString();
        return consultaOutputPort.contarConsultasPorData(dataHoje);
    }

    public Long contarConsultasRealizadasHoje() {
        String dataHoje = java.time.LocalDate.now().toString();
        return consultaOutputPort.contarConsultasRealizadasPorData(dataHoje);
    }

    public Long contarConsultasDaSemanaAtual() {
        java.time.LocalDate hoje = java.time.LocalDate.now();
        java.time.LocalDate inicioSemana = hoje.minusDays(hoje.getDayOfWeek().getValue() - 1);
        String dataInicial = inicioSemana.toString();
        String dataFinal = hoje.toString();
        return consultaOutputPort.contarConsultasPorIntervalo(dataInicial, dataFinal);
    }

    public Long contarMedicosAtivos() {
        return consultaOutputPort.contarMedicosAtivos();
    }

    // ==========================================
    // MÉTODOS DE BUSCA POR DATAS
    // ==========================================

    public List<Consulta> buscarConsultasPorIntervalo(String dataInicial, String dataFinal) {
        return consultaOutputPort.buscarConsultasEmIntervaloDeDatas(dataInicial, dataFinal);
    }

    public List<Consulta> buscarConsultasConcluidasPorIntervalo(String dataInicial, String dataFinal) {
        return consultaOutputPort.buscarConsultasConcluidasEmIntervaloDeDatas(dataInicial, dataFinal);
    }

    // ==========================================
    // MÉTODOS ADICIONAIS PARA ENDPOINTS FALTANTES
    // ==========================================

    @Override
    public Boolean verificarDisponibilidadeHorario(String data, String horario, Long medicoId) {
        return !consultaOutputPort.existsByConHorarioAndConDataAndConMedico_MedCodigo(horario, data, medicoId);
    }

    @Override
    public List<Consulta> buscarConsultasDoDiaAtual() {
        String dataHoje = java.time.LocalDate.now().toString();
        return consultaOutputPort.buscarConsultasEmIntervaloDeDatas(dataHoje, dataHoje);
    }

    @Override
    public List<Consulta> buscarConsultasDaSemanaAtual() {
        java.time.LocalDate hoje = java.time.LocalDate.now();
        java.time.LocalDate inicioSemana = hoje.minusDays(hoje.getDayOfWeek().getValue() - 1);
        String dataInicial = inicioSemana.toString();
        String dataFinal = hoje.toString();
        return consultaOutputPort.buscarConsultasEmIntervaloDeDatas(dataInicial, dataFinal);
    }



    @Override
    public List<Consulta> buscarConsultasDoMesAtual() {
        java.time.LocalDate hoje = java.time.LocalDate.now();
        java.time.LocalDate inicioMes = hoje.withDayOfMonth(1);
        java.time.LocalDate fimMes = hoje.withDayOfMonth(hoje.lengthOfMonth());

        String dataInicialMes = inicioMes.toString();
        String dataFinalMes = fimMes.toString();

        return consultaOutputPort.buscarConsultasPorIntervaloDeDatas(dataInicialMes, dataFinalMes);
    }

    @Override
    public List<Consulta> buscarConsultasDoAnoAtual() {
        java.time.LocalDate hoje = java.time.LocalDate.now();
        java.time.LocalDate inicioAno = hoje.withDayOfYear(1);
        String dataInicial = inicioAno.toString();
        String dataFinal = hoje.toString();
        return consultaOutputPort.buscarConsultasEmIntervaloDeDatas(dataInicial, dataFinal);
    }

    @Override
    public Consulta concluirConsulta(Long id) {
        Optional<Consulta> consultaOptional = consultaOutputPort.findById(id);
        if (consultaOptional.isEmpty()) {
            throw new RuntimeException("Consulta não encontrada com ID: " + id);
        }
        
        Consulta consulta = consultaOptional.get();
        consulta.setConStatus("REALIZADA");
        return consultaOutputPort.save(consulta);
    }

    @Override
    public Consulta atualizarConsulta(Long id, Consulta consulta) {
        Optional<Consulta> consultaExistente = consultaOutputPort.findById(id);
        if (consultaExistente.isEmpty()) {
            throw new RuntimeException("Consulta não encontrada com ID: " + id);
        }
        
        Consulta consultaAtualizada = consultaExistente.get();
        // Atualizar campos necessários
        consultaAtualizada.setConData(consulta.getConData());
        consultaAtualizada.setConHorario(consulta.getConHorario());
        consultaAtualizada.setConStatus(consulta.getConStatus());
        
        return consultaOutputPort.save(consultaAtualizada);
    }

    @Override
    public List<String> buscarHorariosOcupados(Long medicoId, String data) {
        List<Consulta> consultas = consultaOutputPort.findByConMedico_MedCodigoAndConData(medicoId, data);
        return consultas.stream()
                .map(Consulta::getConHorario)
                .toList();
    }

    @Override
    public List<Consulta> buscarAgendaMedico(Long idUsuarioMedico) {
        // Buscar consultas futuras do médico
        String dataHoje = java.time.LocalDate.now().toString();
        return consultaOutputPort.findByConMedico_MedCodigo(idUsuarioMedico).stream()
                .filter(consulta -> consulta.getConData().compareTo(dataHoje) >= 0)
                .toList();
    }

    @Override
    public List<Consulta> buscarHistoricoAgendaMedico(Long idUsuarioMedico) {
        // Buscar todas as consultas do médico (histórico completo)
        return consultaOutputPort.findByConMedico_MedCodigo(idUsuarioMedico);
    }
}
