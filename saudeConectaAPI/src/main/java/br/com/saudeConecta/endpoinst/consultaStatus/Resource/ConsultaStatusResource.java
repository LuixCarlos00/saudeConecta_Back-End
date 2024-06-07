package br.com.saudeConecta.endpoinst.consultaStatus.Resource;

import br.com.saudeConecta.endpoinst.consultaStatus.DTO.DadosCadastraConsultaStatus;
import br.com.saudeConecta.endpoinst.consultaStatus.DTO.DadosConsultaStatusView;
import br.com.saudeConecta.endpoinst.consultaStatus.Entity.ConsultaStatus;
import br.com.saudeConecta.endpoinst.consultaStatus.Service.ConsultaStatusService;
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

@RequestMapping(value = "/consultaStatus")
@RestController
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class ConsultaStatusResource {

    @Autowired
    private ConsultaStatusService service;

    @Autowired
    private MedicoRepository medicoRepository;

    @Autowired
    private PacienteRepository pacienteRepository;

    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @GetMapping(value = "/buscarId/{id}")
    @Transactional
    public ResponseEntity<DadosConsultaStatusView> buscarPorId(@NotNull @Valid @PathVariable("id") Long Id) {
        Optional<ConsultaStatus> consulta = service.buscarPacientePorId(Id);

        return ResponseEntity.status(HttpStatus.OK).body(new DadosConsultaStatusView((consulta.get())));
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
    public ResponseEntity<DadosConsultaStatusView> Cadastra(@NotNull @RequestBody @Valid DadosCadastraConsultaStatus dados, @NotNull BindingResult result,
                                                            UriComponentsBuilder uriBuilder) {

        if (result.hasErrors()) {
            return ResponseEntity.badRequest().build();
        }

            System.out.println(dados.toString());

        Long idMedicos = dados.ConSttMedico();
        Long idPaciente = dados.ConSttPaciente();


        Optional<Medico> medicoOptional = medicoRepository.findById(idMedicos);
        Optional<Paciente> pacienteOptional = pacienteRepository.findById(idPaciente);


        if (medicoOptional.isEmpty() || pacienteOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Medico medico = medicoOptional.get();
        Paciente paciente = pacienteOptional.get();

        ConsultaStatus consulta = new ConsultaStatus(medico, paciente, dados);

        service.CadastraRegistroConsulta(consulta);

        URI uri = uriBuilder.path("/plano/{id}").buildAndExpand(consulta.getConSttCodigoConsulata()).toUri();

        return ResponseEntity.created(uri).body(new DadosConsultaStatusView(consulta));

    }



    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @GetMapping(value ="/Consultapagina")
    public Page<DadosConsultaStatusView> BuscarConsultaPorPaginas(@PageableDefault(sort = {"conSttMedico"}) Pageable paginacao) {
        return service.BuscarConsultaPorPaginas(paginacao);
    }

    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @GetMapping(value ="/BuscarRegistrosDeConsulta/{busca}")
    public Page<DadosConsultaStatusView> BuscarRegistrosDeConsulta(@NotNull @PathVariable("busca") String busca) {

        return service.BuscarRegistrosDeConsulta(busca);
    }


    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @GetMapping(value = "/listatodasConsulta")
    public List<ConsultaStatus> buscarTodasConsulta() {

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
    public ResponseEntity<DadosConsultaStatusView> EditarConsulta(@NotNull @RequestBody @Valid DadosCadastraConsultaStatus dados,
                                                                  @NotNull @PathVariable("id") Long id,
                                                                  @NotNull BindingResult result,
                                                                  UriComponentsBuilder uriBuilder) {

        if (result.hasErrors()) {
            return ResponseEntity.badRequest().build();
        }



        Long idMedicos = dados.ConSttMedico();
        Long idPaciente = dados.ConSttPaciente();


        Optional<Medico> medicoOptional = medicoRepository.findById(idMedicos);
        Optional<Paciente> pacienteOptional = pacienteRepository.findById(idPaciente);


        if (medicoOptional.isEmpty() || pacienteOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }



        Medico medico = medicoOptional.get();
        Paciente paciente = pacienteOptional.get();

        ConsultaStatus consulta = new ConsultaStatus(medico, paciente, dados);

        service.EditarConsulta(consulta, id);

        URI uri = uriBuilder.path("/plano/{id}").buildAndExpand(consulta.getConSttCodigoConsulata()).toUri();

        return ResponseEntity.created(uri).body(new DadosConsultaStatusView(consulta));

    }



    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @PutMapping(value = "/concluido/{id}")
    @Transactional
    public ResponseEntity<DadosConsultaStatusView> FazerConclusaoConsulta(@NotNull @PathVariable("id") Long id) {
        DadosConsultaStatusView consulta =  service.ConcluirConsulta( id);
        return ResponseEntity.ok().body(new DadosConsultaStatusView(consulta));
    }









}
