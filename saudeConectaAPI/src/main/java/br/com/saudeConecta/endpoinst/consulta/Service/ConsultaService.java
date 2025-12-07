package br.com.saudeConecta.endpoinst.consulta.Service;

import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.medico.Medico;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.email.EnviarEmail.EnviarEmail;
import br.com.saudeConecta.endpoinst.consulta.DTO.DadosConsultaView;
import br.com.saudeConecta.infra.exceptions.ResourceNotFoundException;
import br.com.saudeConecta.infrastructure.persistence.repository.ConsultaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.MedicoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.PacienteRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import jakarta.mail.MessagingException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Date;
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

    public void cadastrarConsulta(Consulta consulta) throws ResourceNotFoundException {
        log.info("Cadastrando nova consulta para paciente: {}", consulta.getConPaciente().getPaciNome());
        repository.save(consulta);
        log.info("Consulta cadastrada com sucesso ID: {}", consulta.getConCodigoConsulta());
    }


    public Boolean verificarDisponibilidadeHorario(String data, String horario, Long medico) {

        Optional<Medico> Medico = medicoRepository.findById(medico);

        if (Medico.isEmpty()) {
            return false;
        }
        Date data2 = Date.valueOf(data);

        return repository.existsByConHorarioAndConDataAndConMedico_MedCodigo(horario, data, medico);

    }

 


    public DadosConsultaView editarConsulta(Consulta consulta, Long id) {

        Consulta consulta1 = repository.getReferenceById(id);
        consulta1.update(consulta);
        repository.save(consulta1);
        return new DadosConsultaView(consulta1);

    }


    public DadosConsultaView concluirConsulta(Long id) {
        Optional<Consulta> optionalConsulta = repository.findById(id);

        if (optionalConsulta.isPresent()) {
            Consulta consulta = optionalConsulta.get();
            consulta.setConStatus((byte) 1);
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
}
