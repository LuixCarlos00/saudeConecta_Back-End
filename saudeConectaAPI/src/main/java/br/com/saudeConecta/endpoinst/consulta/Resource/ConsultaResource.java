package br.com.saudeConecta.endpoinst.consulta.Resource;

import br.com.saudeConecta.endpoinst.administrador.Entity.Administrador;
import br.com.saudeConecta.endpoinst.administrador.Repository.AdministradorRepository;
import br.com.saudeConecta.endpoinst.consulta.DTO.DadosCadastraConsulta;
import br.com.saudeConecta.endpoinst.consulta.DTO.DadosConsultaView;
import br.com.saudeConecta.endpoinst.consulta.Entity.Consulta;
import br.com.saudeConecta.endpoinst.consulta.Service.ConsultaService;
import br.com.saudeConecta.endpoinst.medico.Entity.Medico;
import br.com.saudeConecta.endpoinst.medico.Repository.MedicoRepository;
import br.com.saudeConecta.endpoinst.paciente.Entity.Paciente;
import br.com.saudeConecta.endpoinst.paciente.Repository.PacienteRepository;
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
                                                                              @PathVariable("medico") Long medico ) {
        System.out.println( data + horario + medico);
        Boolean consulta = service.VericarSeExetemConsultasMarcadas(data,horario,medico);
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

        Long idMedicos = dados.ConMedico();
        Long idPaciente = dados.ConPaciente();
        Long idAdm = dados.ConAdm();


        Optional<Medico> medicoOptional = medicoRepository.findById(idMedicos);
        Optional<Paciente> pacienteOptional = pacienteRepository.findById(idPaciente);
        Optional<Administrador> admOptional = administradorRepository.findById(idAdm);


        if (medicoOptional.isEmpty() || pacienteOptional.isEmpty() || admOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }


        Administrador adm = admOptional.get();
        Medico medico = medicoOptional.get();
        Paciente paciente = pacienteOptional.get();

        Consulta consulta = new Consulta(medico, paciente, adm,dados);

        service.CadastraRegistroConsulta(consulta);

        URI uri = uriBuilder.path("/plano/{id}").buildAndExpand(consulta.getConCodigoConsulta()).toUri();

        return ResponseEntity.created(uri).body(new DadosConsultaView(consulta));

    }



    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @GetMapping(value ="/Consultapagina")
    public Page<DadosConsultaView> BuscarConsultaPorPaginas(@PageableDefault(sort = {"conMedico"}) Pageable paginacao) {
        return service.BuscarConsultaPorPaginas(paginacao);
    }

    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @GetMapping(value ="/BuscarRegistrosDeConsulta/{busca}")
    public Page<DadosConsultaView> BuscarRegistrosDeConsulta( @NotNull @PathVariable("busca") String busca) {

        return service.BuscarRegistrosDeConsulta(busca);
    }


    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @GetMapping(value = "/listatodasConsulta")
    public List<Consulta> buscarTodasConsulta() {

        return service.buscarTodasConsulta();
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



        Long idMedicos = dados.ConMedico();
        Long idPaciente = dados.ConPaciente();
        Long idAdm = dados.ConAdm();


        Optional<Medico> medicoOptional = medicoRepository.findById(idMedicos);
        Optional<Paciente> pacienteOptional = pacienteRepository.findById(idPaciente);
        Optional<Administrador> admOptional = administradorRepository.findById(idAdm);


        if (medicoOptional.isEmpty() || pacienteOptional.isEmpty() || admOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }


        Administrador adm = admOptional.get();
        Medico medico = medicoOptional.get();
        Paciente paciente = pacienteOptional.get();

        Consulta consulta = new Consulta(medico, paciente, adm,dados);

        service.EditarConsulta(consulta, id);

        URI uri = uriBuilder.path("/plano/{id}").buildAndExpand(consulta.getConCodigoConsulta()).toUri();

        return ResponseEntity.created(uri).body(new DadosConsultaView(consulta));

    }



    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @PutMapping(value = "/concluido/{id}")
    @Transactional
    public ResponseEntity<DadosConsultaView> FazerConclusaoConsulta( @NotNull @PathVariable("id") Long id) {
        DadosConsultaView consulta =  service.ConcluirConsulta( id);
        return ResponseEntity.ok().body(new DadosConsultaView(consulta));
    }









}
