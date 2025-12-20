package br.com.saudeConecta.endpoinst.consulta.Service;

import br.com.saudeConecta.domain.administrador.Administrador;
import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.medico.Medico;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.email.EnviarEmail.EnviarEmail;
import br.com.saudeConecta.endpoinst.consulta.DTO.DadosCadastraConsulta;
import br.com.saudeConecta.endpoinst.consulta.DTO.DadosConsultaView;
import br.com.saudeConecta.infra.exceptions.ResourceNotFoundException;
import br.com.saudeConecta.infrastructure.persistence.repository.*;
import jakarta.mail.MessagingException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ConsultaService {

    @Autowired
    private ConsultaRepository repository;

    @Autowired
    private MedicoRepository medicoRepository;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AdministradorRepository administradorRepository;

    @Autowired
    private EnviarEmail enviarEmail;



    public Optional<Consulta> buscarConsultaPorId(Long id) {
        log.debug("Buscando consulta por ID: {}", id);
        return repository.findById(id);
    }


    public List<Consulta> listarTodasConsultas() {
        List <Consulta> consultas = repository.findAll();
        if (consultas.isEmpty()) {
            return Collections.emptyList();
        } else {
            return consultas;
        }

    }


    @Transactional
    public void deletarPorId(Long id) throws ResourceNotFoundException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID invalido");
        }

        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Registro nao encontrado");
        }

        try {
            repository.deleteById(id);
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException("Violacao de Integridade");
        }
    }


    public List<Consulta> buscarTodasConsultas() {
        return repository.findAll();

    }



    public List<Consulta> buscarConsultasPorIntervalo(String dataInicial, String dataFinal) {
        return repository.buscarConsultasEmIntervaloDeDatas(dataInicial, dataFinal);
    }


    public List<Consulta> buscarConsultasPorIntervaloEEspecialidade(String dataInicial, String dataFinal, String especialidade) {
        return repository.buscarConsultasEmIntervaloComEspecialidade(dataInicial, dataFinal, especialidade);
    }


    public List<Consulta> buscarConsultasPorMedico(Long medicoID) {
        return repository.buscarConsultasPorMedico(medicoID);
    }

    public List<Consulta> buscarConsultasPorMedicoEIntervalo(Long medicoID, String dataInicio, String dataFim) {
        return repository.buscarConsultasPorMedicoEmIntervalo(dataInicio, dataFim, medicoID);
    }


    public List<Consulta> buscarConsultasPorEspecialidade(String especialidades) {
    return repository.buscarConsultasPorEspecialidade(especialidades);
    }

    public List<Consulta> buscarConsultasDoDiaAtual() {
        String dataHoje = java.time.LocalDate.now().toString();
        return repository.buscarConsultasPorIntervaloDeDatas(dataHoje, dataHoje);
    }

    public List<Consulta> buscarConsultasDaSemanaAtual() {
        java.time.LocalDate hoje = java.time.LocalDate.now();
        java.time.LocalDate inicioSemana = hoje.with(java.time.DayOfWeek.MONDAY);
        java.time.LocalDate fimSemana = hoje.with(java.time.DayOfWeek.SUNDAY);
        
        String dataInicialSemana = inicioSemana.toString();
        String dataFinalSemana = fimSemana.toString();
        
        return repository.buscarConsultasPorIntervaloDeDatas(dataInicialSemana, dataFinalSemana);
    }

    public List<Consulta> buscarConsultasDoMesAtual() {
        java.time.LocalDate hoje = java.time.LocalDate.now();
        java.time.LocalDate inicioMes = hoje.withDayOfMonth(1);
        java.time.LocalDate fimMes = hoje.withDayOfMonth(hoje.lengthOfMonth());
        
        String dataInicialMes = inicioMes.toString();
        String dataFinalMes = fimMes.toString();
        
        return repository.buscarConsultasPorIntervaloDeDatas(dataInicialMes, dataFinalMes);
    }

    public List<Consulta> buscarConsultasDoAnoAtual() {
        java.time.LocalDate hoje = java.time.LocalDate.now();
        java.time.LocalDate inicioAno = hoje.withDayOfYear(1);
        java.time.LocalDate fimAno = hoje.withDayOfYear(hoje.lengthOfYear());
        
        String dataInicialAno = inicioAno.toString();
        String dataFinal = fimAno.toString();
        
        return repository.buscarConsultasPorIntervaloDeDatas(dataInicialAno, dataFinal);
    }

    @Transactional
    public DadosConsultaView cadastrarConsulta(DadosCadastraConsulta dados) {
        log.info("Iniciando cadastro de nova consulta");

        Medico medico = medicoRepository.findById(dados.conMedico())
                .orElseThrow(() -> new EntityNotFoundException("Medico nao encontrado com ID: " + dados.conMedico()));

        Paciente paciente = pacienteRepository.findById(dados.conPaciente())
                .orElseThrow(() -> new EntityNotFoundException("Paciente nao encontrado com ID: " + dados.conPaciente()));

        Administrador administrador = administradorRepository.findByAdmUsuario_Id(dados.conAdm())
                .orElseThrow(() -> new EntityNotFoundException("Administrador nao encontrado com ID de usuario: " + dados.conAdm()));

        Consulta consulta = new Consulta(medico, paciente, administrador, dados);
        repository.save(consulta);

        log.info("Consulta cadastrada com sucesso - ID: {}, Paciente: {}", consulta.getConCodigoConsulta(), paciente.getPaciNome());
        return new DadosConsultaView(consulta);
    }       

    @Transactional
    public DadosConsultaView atualizarConsulta(Long id, DadosCadastraConsulta dados) {
        log.info("Iniciando atualizacao da consulta ID: {}", id);

        Consulta consultaExistente = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Consulta nao encontrada com ID: " + id));

        Medico medico = medicoRepository.findById(dados.conMedico())
                .orElseThrow(() -> new EntityNotFoundException("Medico nao encontrado com ID: " + dados.conMedico()));

        Paciente paciente = pacienteRepository.findById(dados.conPaciente())
                .orElseThrow(() -> new EntityNotFoundException("Paciente nao encontrado com ID: " + dados.conPaciente()));

        Administrador administrador = administradorRepository.findByAdmUsuario_Id(dados.conAdm())
                .orElseThrow(() -> new EntityNotFoundException("Administrador nao encontrado com ID de usuario: " + dados.conAdm()));

        Consulta consultaAtualizada = new Consulta(medico, paciente, administrador, dados);
        consultaExistente.update(consultaAtualizada);
        repository.save(consultaExistente);

        log.info("Consulta atualizada com sucesso - ID: {}", id);
        return new DadosConsultaView(consultaExistente);
    }


    public Boolean verificarDisponibilidadeHorario(String data, String horario, Long medicoId) {
        if (!medicoRepository.existsById(medicoId)) {
            return false;
        }
        return repository.existsByConHorarioAndConDataAndConMedico_MedCodigo(horario, data, medicoId);
    }

    @Transactional
    public DadosConsultaView concluirConsulta(Long id) {
        Optional<Consulta> optionalConsulta = repository.findById(id);

        if (optionalConsulta.isPresent()) {
            Consulta consulta = optionalConsulta.get();
            consulta.setConStatus("REALIZADA");
            repository.save(consulta);
            return new DadosConsultaView(consulta);
        } else {
            throw new EntityNotFoundException("Consulta nao encontrada com o id: " + id);
        }
    }


    public List<String> buscarHorariosOcupados(Long medico, String data) {

        List<Consulta> consultas = repository.findByConMedico_MedCodigoAndConData(medico, data);

        List<String> HorariosDisponiveis = new ArrayList<>();

        for (int i = 0; i < consultas.size(); i++) {
            HorariosDisponiveis.add(consultas.get(i).getConHorario());
        }

        if (HorariosDisponiveis.size() > 0) {
            return HorariosDisponiveis;
        } else {
            return null;
        }
    }


    public Optional<Object> enviarNotificacaoPorEmail(String email, String mensagem) throws MessagingException {
        log.info("Enviando notificacao por e-mail para: {}", email);
        Optional<Paciente> paciente = pacienteRepository.findByPaciEmail(email);
        Optional<Medico> medico = medicoRepository.findByMedEmail(email);

        if (paciente.isPresent()) {
            enviarEmail.enviarLembreteDeAlertaParaPaciente(paciente, mensagem);
            return Optional.of(paciente);
        }
        if (medico.isPresent()) {
            enviarEmail.enviarLembreteDeAlertaParaMedico(medico, mensagem);
            return Optional.of(medico);
        }

        return Optional.of("Email invalido");

    }







    public List<Consulta> buscarAgendaMedico(Long idUsuarioMedico) {
        Optional<Usuario> user = usuarioRepository.findById(idUsuarioMedico);
        if (user.isPresent()) {
            Optional<Medico> medico = medicoRepository.findByUsuario_Id(user.get().getId());
            if (medico.isPresent()) {
                return repository.findByConMedico_MedCodigo(medico.get().getMedCodigo());
            } else {
                return null;
            }
        } else {
            return null;
        }

    }

    public List<Object[]> contarConsultasPorStatusEMedico(Long medicoId, String dataInicial, String dataFinal) {
        return repository.contarConsultasPorStatusEMedico(medicoId, dataInicial, dataFinal);
    }

    public List<Consulta> buscarHistoricoAgendaMedico(Long idUsuarioMedico) {
        Optional<Usuario> user = usuarioRepository.findById(idUsuarioMedico);
        if (user.isPresent()) {
            Optional<Medico> medico = medicoRepository.findByUsuario_Id(user.get().getId());
            if (medico.isPresent()) {
                return repository.findByConMedico_MedCodigo(medico.get().getMedCodigo());
            }
        }
        return Collections.emptyList();
    }

    // ==========================================
    // CONSULTAS CONCLUÍDAS (FINALIZADAS)
    // ==========================================

    public List<Consulta> buscarConsultasConcluidasPorIntervalo(String dataInicial, String dataFinal) {
        return repository.buscarConsultasConcluidasEmIntervaloDeDatas(dataInicial, dataFinal);
    }

    public List<Consulta> buscarConsultasConcluidasPorIntervaloEEspecialidade(String dataInicial, String dataFinal, String especialidade) {
        return repository.buscarConsultasConcluidasEmIntervaloComEspecialidade(dataInicial, dataFinal, especialidade);
    }

    public List<Consulta> buscarConsultasConcluidasPorMedico(Long medicoId) {
        return repository.buscarConsultasConcluidasPorMedico(medicoId);
    }

    public List<Consulta> buscarConsultasConcluidasPorMedicoEIntervalo(Long medicoId, String dataInicio, String dataFim) {
        return repository.buscarConsultasConcluidasPorMedicoEmIntervalo(dataInicio, dataFim, medicoId);
    }

    public List<Consulta> buscarConsultasConcluidasPorEspecialidade(String especialidade) {
        return repository.buscarConsultasConcluidasPorEspecialidade(especialidade);
    }

    public List<Consulta> buscarConsultasConcluidasPorMedicoEEspecialidade(Long medicoId, String especialidade) {
        return repository.buscarConsultasConcluidasPorMedicoEEspecialidade(medicoId, especialidade);
    }

    public List<Consulta> buscarConsultasConcluidasPorMedicoEspecialidadeEmIntervalo(Long medicoId, String especialidade, String dataInicial, String dataFinal) {
        return repository.buscarConsultasConcluidasPorMedicoEspecialidadeEmIntervalo(medicoId, especialidade, dataInicial, dataFinal);
    }

    // ==========================================
    // ESTATÍSTICAS DO DASHBOARD
    // ==========================================

    public Long contarConsultasHoje() {
        String dataHoje = java.time.LocalDate.now().toString();
        return repository.contarConsultasPorData(dataHoje);
    }

    public Long contarConsultasHojePorUsuario(Long usuarioId) {
        String dataHoje = java.time.LocalDate.now().toString();
        return repository.contarConsultasPorDataEUsuario(dataHoje, usuarioId);
    }

    public Long contarConsultasRealizadasHoje() {
        String dataHoje = java.time.LocalDate.now().toString();
        return repository.contarConsultasRealizadasPorData(dataHoje);
    }

    public Long contarConsultasRealizadasHojePorUsuario(Long usuarioId) {
        String dataHoje = java.time.LocalDate.now().toString();
        return repository.contarConsultasRealizadasPorDataEUsuario(dataHoje, usuarioId);
    }

    public Long contarConsultasAgendadasHoje() {
        String dataHoje = java.time.LocalDate.now().toString();
        return repository.contarConsultasAgendadasPorData(dataHoje);
    }

    public Long contarConsultasAgendadasHojePorUsuario(Long usuarioId) {
        String dataHoje = java.time.LocalDate.now().toString();
        return repository.contarConsultasAgendadasPorDataEUsuario(dataHoje, usuarioId);
    }

    public Long contarConsultasDaSemanaAtual() {
        java.time.LocalDate hoje = java.time.LocalDate.now();
        java.time.LocalDate inicioSemana = hoje.with(java.time.DayOfWeek.MONDAY);
        java.time.LocalDate fimSemana = hoje.with(java.time.DayOfWeek.SUNDAY);
        return repository.contarConsultasDaSemana(inicioSemana.toString(), fimSemana.toString());
    }

    public Long contarConsultasDaSemanaAtualPorUsuario(Long usuarioId) {
        java.time.LocalDate hoje = java.time.LocalDate.now();
        java.time.LocalDate inicioSemana = hoje.with(java.time.DayOfWeek.MONDAY);
        java.time.LocalDate fimSemana = hoje.with(java.time.DayOfWeek.SUNDAY);
        return repository.contarConsultasDaSemanaPorUsuario(inicioSemana.toString(), fimSemana.toString(), usuarioId);
    }

    public Long contarMedicosAtivos() {
        return medicoRepository.contarMedicosAtivos();
    }
}
