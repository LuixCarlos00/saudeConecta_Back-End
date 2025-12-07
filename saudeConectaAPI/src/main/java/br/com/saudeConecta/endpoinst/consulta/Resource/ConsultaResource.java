package br.com.saudeConecta.endpoinst.consulta.Resource;

import br.com.saudeConecta.endpoinst.administrador.Entity.Administrador;
import br.com.saudeConecta.endpoinst.administrador.Repository.AdministradorRepository;
import br.com.saudeConecta.endpoinst.consulta.DTO.DadosCadastraConsulta;
import br.com.saudeConecta.endpoinst.consulta.DTO.DadosConsultaView;
import br.com.saudeConecta.endpoinst.consulta.DTO.DadosSeendToNewMenssage;
import br.com.saudeConecta.endpoinst.consulta.Entity.Consulta;
import br.com.saudeConecta.endpoinst.consulta.Service.ConsultaService;
import br.com.saudeConecta.endpoinst.medico.Entity.Medico;
import br.com.saudeConecta.endpoinst.medico.Repository.MedicoRepository;
import br.com.saudeConecta.endpoinst.paciente.Entity.Paciente;
import br.com.saudeConecta.endpoinst.paciente.Repository.PacienteRepository;
import br.com.saudeConecta.endpoinst.usuario.Repository.UsuarioRepository;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RequestMapping(value = "/consulta")
@RestController
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class ConsultaResource {

    @Autowired
    private ConsultaService service;

    @Autowired
    private MedicoRepository medicoRepository;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private AdministradorRepository administradorRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping(value = "/buscarId/{id}")
    @Transactional
    public ResponseEntity<DadosConsultaView> buscarPorId(@NotNull @Valid @PathVariable("id") Long Id) {
        Optional<Consulta> consulta = service.buscarConsultaPorId(Id);

        return ResponseEntity.status(HttpStatus.OK).body(new DadosConsultaView((consulta.get())));
    }


    @GetMapping(value = "/consultaData={data}&horario={horario}&medico={medico}")
    @Transactional
    public Boolean verificarSeExistemConsultasMarcadas(@NotNull @Valid @PathVariable("data") String data,
                                                       @PathVariable("horario") String horario,
                                                       @PathVariable("medico") Long medico) {
        return service.verificarDisponibilidadeHorario(data, horario, medico);
    }


    @PostMapping("/post")
    @Transactional
    public ResponseEntity<DadosConsultaView> cadastrar(@NotNull @RequestBody @Valid DadosCadastraConsulta dados, @NotNull BindingResult result,
                                                       UriComponentsBuilder uriBuilder) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().build();
        }

        Long idMedicos = dados.conMedico();
        Long idPaciente = dados.conPaciente();
        Long idAdm = dados.conAdm();


        Optional<Medico> medicoOptional = medicoRepository.findById(idMedicos);
        Optional<Paciente> pacienteOptional = pacienteRepository.findById(idPaciente);
        Optional<Administrador> admOptional = administradorRepository.findByAdmUsuario_Id(idAdm);


        if (medicoOptional.isEmpty() || pacienteOptional.isEmpty() || admOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }


        Administrador adm = admOptional.get();
        Medico medico = medicoOptional.get();
        Paciente paciente = pacienteOptional.get();

        Consulta consulta = new Consulta(medico, paciente, adm, dados);

        service.cadastrarConsulta(consulta);

        URI uri = uriBuilder.path("/plano/{id}").buildAndExpand(consulta.getConCodigoConsulta()).toUri();

        return ResponseEntity.created(uri).body(new DadosConsultaView(consulta));

    }


    @GetMapping(value = "/Consultapagina")
    public ResponseEntity<List<DadosConsultaView>> listarConsultas() {
        List<Consulta> list = service.listarTodasConsultas();
        List <DadosConsultaView> Dados = list.stream().map(DadosConsultaView::new).toList();
        return ResponseEntity.ok().body(Dados);
     }

 

    @GetMapping(value = "/listatodasConsulta")
    public List<Consulta> listarTodasConsultas() {
        return service.buscarTodasConsultas();
    }


 



    @GetMapping(value = "/BuscandoTodasConsultasEmIntervaloDeDatas/dataInicial={dataInicial}&dataFinal={dataFinal}")
    public List<Consulta> buscarConsultasPorIntervalo(@NotNull @PathVariable("dataInicial") String dataInicial,
                                                                   @NotNull @PathVariable("dataFinal") String dataFinal) {
        return service.buscarConsultasPorIntervalo(dataInicial, dataFinal);
    }


    @GetMapping(value = "/BuscandoTodasConsultasEmIntervaloDeDatasComEspecialidade/dataInicial={dataInicial}&dataFinal={dataFinal}&especialidades={especialidade}")
    public List<Consulta> buscarConsultasPorIntervaloEEspecialidade(@NotNull @PathVariable("dataInicial") String dataInicial,
                                                                                   @NotNull @PathVariable("dataFinal") String dataFinal,
                                                                                   @NotNull @PathVariable("especialidade") String especialidade) {
        return service.buscarConsultasPorIntervaloEEspecialidade(dataInicial, dataFinal, especialidade);
    }


    @GetMapping(value = "/BuscandoTodasConsultasPorMedico/{medicoID}")
    public List<Consulta> buscarConsultasPorMedico(@NotNull @PathVariable("medicoID") Long medicoID) {
        return service.buscarConsultasPorMedico(medicoID);
    }


    @GetMapping(value = "/BuscandoTodasConsultasPorMedicoEmIntervaloDeDatas/medico={medCodigo}&dataInicial={DataInicioFormatada}&dataFinal={DataFimFormatada}")
    public List<Consulta> buscarConsultasPorMedicoEIntervalo(@NotNull @PathVariable("medCodigo") Long medicoID,
                                                                            @NotNull @PathVariable("DataInicioFormatada") String dataInicio,
                                                                            @NotNull @PathVariable("DataFimFormatada") String dataFim) {
        return service.buscarConsultasPorMedicoEIntervalo(medicoID, dataInicio, dataFim);
    }

    @GetMapping(value = "/BuscandoTodasConsultasPorEspecialidade/especialidades={especialidades}")
    public List<Consulta> buscarConsultasPorEspecialidade(@NotNull @PathVariable("especialidades") String especialidades) {
        return service.buscarConsultasPorEspecialidade(especialidades);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConsultaById(@PathVariable("id") Long id) throws Exception {
        service.deletarPorId(id);
        return ResponseEntity.noContent().build();
    }


    @PutMapping(value = "/editar/{id}")
    @Transactional
    public ResponseEntity<DadosConsultaView> editarConsulta(@NotNull @RequestBody @Valid DadosCadastraConsulta dados,
                                                            @NotNull @PathVariable("id") Long id,
                                                            @NotNull BindingResult result,
                                                            UriComponentsBuilder uriBuilder) {

        if (result.hasErrors()) {
            return ResponseEntity.badRequest().build();
        }


        Long idMedicos = dados.conMedico();
        Long idPaciente = dados.conPaciente();
        Long idUsusario = dados.conAdm();


        Optional<Medico> medicoOptional = medicoRepository.findById(idMedicos);
        Optional<Paciente> pacienteOptional = pacienteRepository.findById(idPaciente);
        Optional<Administrador> administradorOptional = administradorRepository.findByAdmUsuario_Id(idUsusario);

        if (medicoOptional.isEmpty() || pacienteOptional.isEmpty() || administradorOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }


        Administrador ususario = administradorOptional.get();
        Medico medico = medicoOptional.get();
        Paciente paciente = pacienteOptional.get();

        Consulta consulta = new Consulta(medico, paciente, ususario, dados);

        service.editarConsulta(consulta, id);

        URI uri = uriBuilder.path("/plano/{id}").buildAndExpand(consulta.getConCodigoConsulta()).toUri();

        return ResponseEntity.created(uri).body(new DadosConsultaView(consulta));

    }


    @PutMapping(value = "/concluido/{id}")
    @Transactional
    public ResponseEntity<DadosConsultaView> concluirConsulta(@NotNull @PathVariable("id") Long id) {
        DadosConsultaView consulta = service.concluirConsulta(id);
        return ResponseEntity.ok().body(new DadosConsultaView(consulta));
    }


    @GetMapping(value = "/VerificarHorariosDisponiveisReferentesAoMedicoEData/medico={medico}&data={data}")
    public List<String> buscarHorariosOcupados(@Valid @NotNull @PathVariable("medico") Long medico,
                                                                            @Valid @NotNull @PathVariable("data") String data) {

        List<String> list = service.buscarHorariosOcupados(medico, data);
        return list;
    }


    @PostMapping("/EnviarMensagem")
    @Transactional
    public ResponseEntity<Object> enviarMensagem(@NotNull @RequestBody @Valid DadosSeendToNewMenssage dados, @NotNull BindingResult result,
                                                 UriComponentsBuilder uriBuilder) throws MessagingException {

        if (result.hasErrors()) {
            return ResponseEntity.badRequest().build();
        }

        Optional<Object> obj = Optional.empty();

        String medicoEmail = dados.medEmail();
        String medicoTelefone = dados.medTelefone();
        String pacienteEmail = dados.paciEmail();
        String pacienteTelefone = dados.paciTelefone();

        if (medicoEmail.isEmpty() && medicoTelefone.isEmpty() && pacienteEmail.isEmpty() && pacienteTelefone.isEmpty()) {
            return ResponseEntity.badRequest().build();
        } else if (!medicoEmail.isEmpty()) {
            obj = service.enviarNotificacaoPorEmail(medicoEmail, dados.mensagem());
        } else if (!pacienteEmail.isEmpty()) {
            obj = service.enviarNotificacaoPorEmail(pacienteEmail, dados.mensagem());
        }
        return ResponseEntity.ok().body(obj);

    }

    @GetMapping(value = "/BuscarTodaAgendaDeMedico/{IdMedico}")
    public List<Consulta> buscarAgendaMedico(@NotNull @PathVariable("IdMedico") Long idUsuarioMedico) {
        return service.buscarAgendaMedico(idUsuarioMedico);

    }


}
