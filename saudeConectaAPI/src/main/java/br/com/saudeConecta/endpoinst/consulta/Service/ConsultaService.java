package br.com.saudeConecta.endpoinst.consulta.Service;

import br.com.saudeConecta.email.EnviarEmail.EnviarEmail;
import br.com.saudeConecta.endpoinst.consulta.DTO.DadosConsultaView;
import br.com.saudeConecta.endpoinst.consulta.Entity.Consulta;
import br.com.saudeConecta.endpoinst.consulta.Repository.ConsultaRepository;
import br.com.saudeConecta.endpoinst.medico.Entity.Medico;
import br.com.saudeConecta.endpoinst.medico.Repository.MedicoRepository;
import br.com.saudeConecta.endpoinst.paciente.Entity.Paciente;
import br.com.saudeConecta.endpoinst.paciente.Repository.PacienteRepository;
import br.com.saudeConecta.endpoinst.usuario.Entity.Usuario;
import br.com.saudeConecta.endpoinst.usuario.Repository.UsuarioRepository;
import br.com.saudeConecta.infra.exceptions.ResourceNotFoundException;
import jakarta.mail.MessagingException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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

    @Value("${twilio.accountSid}")
    private String accountSid;

    @Value("${twilio.authToken}")
    private String authToken;

    @Value("${twilio.phoneNumber}")
    private String phoneNumber;


    public Optional<Consulta> buscarPacientePorId(Long id) {
        return repository.findById(id);
    }


    public List<Consulta> BuscarConsultaPorPaginas() {
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
            throw new IllegalArgumentException("ID inválido");
        }

        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Registro não encontrado");
        }

        try {
            repository.deleteById(id);
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException("Violação de Integridade");
        }
    }


    public List<Consulta> buscarTodasConsulta() {
        return repository.findAll();

    }


//    public List<Consulta> BuscatodasAsConsultasPorDataSelecionada(String data) {
//        return repository.BuscarConsultasComFiltroData(data);
//    }


    public List<Consulta> BuscandoTodasConsultasEmIntervaloDeDatas(String dataInicial, String dataFinal) {
        return repository.BuscandoTodasConsultasEmIntervaloDeDatas(dataInicial, dataFinal);
    }


    public List<Consulta> BuscandoTodasConsultasEmIntervaloDeDatasComEspecialidade(String dataInicial, String dataFinal, String especialidade) {
        return repository.BuscandoTodasConsultasEmIntervaloDeDatasComEspecialidade(dataInicial, dataFinal, especialidade);
    }


    public List<Consulta> BuscandoTodasConsultasPorMedico(Long medicoID) {
        return repository.BuscandoTodasConsultasPorMedico(medicoID);
    }

    public List<Consulta> BuscandoTodasConsultasPorMedicoEmIntervaloDeDatas(Long medicoID, String dataInicio, String dataFim) {
        return repository.BuscandoTodasConsultasPorMedicoEmIntervaloDeDatas(dataInicio, dataFim, medicoID);
    }


    public List<Consulta> BuscandoTodasConsultasPorEspecialidade(String especialidades) {
    return  repository.BuscandoTodasConsultasPorEspecialidade(especialidades);
    }

    public void CadastraRegistroConsulta(Consulta consulta) throws ResourceNotFoundException {
        repository.save(consulta);
    }


    public Boolean VericarSeExetemConsultasMarcadas(String data, String horario, Long medico) {

        Optional<Medico> Medico = medicoRepository.findById(medico);

        if (Medico.isEmpty()) {
            return false;
        }
        Date data2 = Date.valueOf(data);

        return repository.existsByConHorarioAndConDataAndConMedico_MedCodigo(horario, data, medico);

    }

 


    public DadosConsultaView EditarConsulta(Consulta consulta, Long id) {

        Consulta consulta1 = repository.getReferenceById(id);
        consulta1.update(consulta);
        repository.save(consulta1);
        return new DadosConsultaView(consulta1);

    }


    public DadosConsultaView ConcluirConsulta(Long id) {
        Optional<Consulta> optionalConsulta = repository.findById(id);

        if (optionalConsulta.isPresent()) {
            Consulta consulta = optionalConsulta.get();
            consulta.setConStatus((byte) 1); // Usando (byte) para garantir o tipo correto
            repository.save(consulta);
            return new DadosConsultaView(consulta);
        } else {
            throw new EntityNotFoundException("Consulta não encontrada com o id: " + id);
        }
    }


    public List<String> VerificarHorariosDisponiveisReferentesAoMedicoEData(Long medico, String data) {

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


    public Optional<Object> buscarPacientePorEmail(String email, String messagem) throws MessagingException {

        Optional<Paciente> paciente = pacienteRepository.findByPaciEmail(email);

        Optional<Medico> medico = medicoRepository.findByMedEmail(email);

        if (paciente.isPresent()) {
            enviarEmail.enviarLembreteDeAlertaParaPaciente(paciente, messagem);
            return Optional.of(paciente);
        }
        if (medico.isPresent()) {
            enviarEmail.enviarLembreteDeAlertaParaMedico(medico, messagem);
            return Optional.of(medico);
        }

        return Optional.of("Email invalido");

    }


    public Optional<Object> buscarPacientePorTelefone(String medicoTelefone, String messagem) {
        Twilio.init(accountSid, authToken);

        Message.creator(
                new PhoneNumber(medicoTelefone),
                new PhoneNumber(phoneNumber),
                messagem
        ).create();
        return Optional.empty();
    }







    // ##################################################
    // ##################################################
    // ##################################################
    // ##################################################
    // ###############- MEDICOS -########################
    // ##################################################
    // ##################################################
    // ##################################################





    public List<Consulta> BuscarTodaAgendaDeMedicoDoDia(Long IdUsuarioMedico ) {
        Optional<Usuario> user = usuarioRepository.findById(IdUsuarioMedico);
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
