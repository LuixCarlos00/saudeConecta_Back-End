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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @GetMapping(value = "/buscarId/{id}")
    @Transactional
    public ResponseEntity<DadosConsultaView> buscarPorId(@NotNull @Valid @PathVariable("id") Long Id) {
        Optional<Consulta> consulta = service.buscarPacientePorId(Id);

        return ResponseEntity.status(HttpStatus.OK).body(new DadosConsultaView((consulta.get())));
    }


    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @GetMapping(value = "/consultaData={data}&horario={horario}&medico={medico}")
    @Transactional
    public Boolean VericarSeExetemConsultasMarcadas(@NotNull @Valid @PathVariable("data") String data,
                                                    @PathVariable("horario") String horario,
                                                    @PathVariable("medico") Long medico) {
        System.out.println(data + horario + medico);
        Boolean consulta = service.VericarSeExetemConsultasMarcadas(data, horario, medico);
        System.out.println(consulta);
        return consulta;
    }


    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @PostMapping("/post")
    @Transactional
    public ResponseEntity<DadosConsultaView> Cadastra(@NotNull @RequestBody @Valid DadosCadastraConsulta dados, @NotNull BindingResult result,
                                                      UriComponentsBuilder uriBuilder) {

        if (result.hasErrors()) {
            return ResponseEntity.badRequest().build();
        }

        System.out.println(dados.toString());

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

        service.CadastraRegistroConsulta(consulta);

        URI uri = uriBuilder.path("/plano/{id}").buildAndExpand(consulta.getConCodigoConsulta()).toUri();

        return ResponseEntity.created(uri).body(new DadosConsultaView(consulta));

    }


    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @GetMapping(value = "/Consultapagina")
    public ResponseEntity<List<DadosConsultaView>> BuscarConsultaPorPaginas() {
        List <Consulta> list = service.BuscarConsultaPorPaginas();
        List <DadosConsultaView> Dados = list.stream().map(DadosConsultaView::new).toList();
        return ResponseEntity.ok().body(Dados);
     }

 

    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @GetMapping(value = "/listatodasConsulta")
    public List<Consulta> buscarTodasConsulta() {
        return service.buscarTodasConsulta();
    }


 


    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @GetMapping(value = "/BuscandoTodasConsultasEmIntervaloDeDatas/dataInicial={dataInicial}&dataFinal={dataFinal}")
    public List<Consulta> BuscandoTodasConsultasEmIntervaloDeDatas(@NotNull @PathVariable("dataInicial") String dataInicial,
                                                                   @NotNull @PathVariable("dataFinal") String dataFinal) {
        return service.BuscandoTodasConsultasEmIntervaloDeDatas(dataInicial, dataFinal);
    }


    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @GetMapping(value = "/BuscandoTodasConsultasEmIntervaloDeDatasComEspecialidade/dataInicial={dataInicial}&dataFinal={dataFinal}&especialidades={especialidade}")
    public List<Consulta> BuscandoTodasConsultasEmIntervaloDeDatasComEspecialidade(@NotNull @PathVariable("dataInicial") String dataInicial,
                                                                                   @NotNull @PathVariable("dataFinal") String dataFinal,
                                                                                   @NotNull @PathVariable("especialidade") String especialidade) {
        return service.BuscandoTodasConsultasEmIntervaloDeDatasComEspecialidade(dataInicial, dataFinal, especialidade);
    }


    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @GetMapping(value = "/BuscandoTodasConsultasPorMedico/{medicoID}")
    public List<Consulta> BuscandoTodasConsultasPorMedico(@NotNull @PathVariable("medicoID") Long medicoID) {
        return service.BuscandoTodasConsultasPorMedico(medicoID);
    }


    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @GetMapping(value = "/BuscandoTodasConsultasPorMedicoEmIntervaloDeDatas/medico={medCodigo}&dataInicial={DataInicioFormatada}&dataFinal={DataFimFormatada}")
    public List<Consulta> BuscandoTodasConsultasPorMedicoEmIntervaloDeDatas(@NotNull @PathVariable("medCodigo") Long medicoID,
                                                                            @NotNull @PathVariable("DataInicioFormatada") String dataInicio,
                                                                            @NotNull @PathVariable("DataFimFormatada") String dataFim) {
        return service.BuscandoTodasConsultasPorMedicoEmIntervaloDeDatas(medicoID, dataInicio, dataFim);
    }

    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @GetMapping(value = "/BuscandoTodasConsultasPorEspecialidade/especialidades={especialidades}")
    public List<Consulta> BuscandoTodasConsultasPorEspecialidade(@NotNull @PathVariable("especialidades") String especialidades) {
        return service.BuscandoTodasConsultasPorEspecialidade(especialidades);
    }


    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConsultaById(@PathVariable("id") Long id) throws Exception {
        service.deletarPorId(id);
        return ResponseEntity.noContent().build();
    }


    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @PutMapping(value = "/editar/{id}")
    @Transactional
    public ResponseEntity<DadosConsultaView> EditarConsulta(@NotNull @RequestBody @Valid DadosCadastraConsulta dados,
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

        service.EditarConsulta(consulta, id);

        URI uri = uriBuilder.path("/plano/{id}").buildAndExpand(consulta.getConCodigoConsulta()).toUri();

        return ResponseEntity.created(uri).body(new DadosConsultaView(consulta));

    }


    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @PutMapping(value = "/concluido/{id}")
    @Transactional
    public ResponseEntity<DadosConsultaView> FazerConclusaoConsulta(@NotNull @PathVariable("id") Long id) {
        DadosConsultaView consulta = service.ConcluirConsulta(id);
        return ResponseEntity.ok().body(new DadosConsultaView(consulta));
    }


    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @GetMapping(value = "/VerificarHorariosDisponiveisReferentesAoMedicoEData/medico={medico}&data={data}")
    public List<String> VerificarHorariosDisponiveisReferentesAoMedicoEData(@Valid @NotNull @PathVariable("medico") Long medico,
                                                                            @Valid @NotNull @PathVariable("data") String data) {

        List<String> list = service.VerificarHorariosDisponiveisReferentesAoMedicoEData(medico, data);
        return list;
    }


    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @PostMapping("/EnviarMensagem")
    @Transactional
    public ResponseEntity<Object> EnviarMensagem(@NotNull @RequestBody @Valid DadosSeendToNewMenssage dados, @NotNull BindingResult result,
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
            obj = service.buscarPacientePorEmail(medicoEmail, dados.mensagem());
        } else if (!medicoTelefone.isEmpty()) {
            obj = service.buscarPacientePorTelefone(medicoTelefone, dados.mensagem());
        } else if (!pacienteEmail.isEmpty()) {
            obj = service.buscarPacientePorEmail(pacienteEmail, dados.mensagem());
        } else if (!pacienteTelefone.isEmpty()) {
            obj = service.buscarPacientePorTelefone(pacienteTelefone, dados.mensagem());
        }
        return ResponseEntity.ok().body(obj);

    }

    // ##################################################
    // ##################################################
    // ##################################################
    // ##################################################
    // ###############- MEDICOS -########################
    // ##################################################
    // ##################################################
    // ##################################################


    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @GetMapping(value = "/BuscarTodaAgendaDeMedico/{IdMedico}")
    public List<Consulta> BuscarTodaAgendaDeMedicoDoDia(@NotNull @PathVariable("IdMedico") Long IdUsuarioMedico) {
        return service.BuscarTodaAgendaDeMedicoDoDia(IdUsuarioMedico);

    }


}
