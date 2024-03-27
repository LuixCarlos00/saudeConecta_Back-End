package br.com.saudeConecta.endpoinst.consulta.Resource;

import br.com.saudeConecta.endpoinst.consulta.DTO.DadosCadastraConsulta;
import br.com.saudeConecta.endpoinst.consulta.DTO.DadosConsultaView;
import br.com.saudeConecta.endpoinst.consulta.Entity.Consulta;
import br.com.saudeConecta.endpoinst.consulta.Service.ConsultaService;
import br.com.saudeConecta.endpoinst.endereco.Entity.Endereco;
import br.com.saudeConecta.endpoinst.medico.Entity.Medico;
import br.com.saudeConecta.endpoinst.medico.Repository.MedicoRepository;
import br.com.saudeConecta.endpoinst.paciente.Entity.Paciente;
import br.com.saudeConecta.endpoinst.paciente.Repository.PacienteRepository;
import br.com.saudeConecta.endpoinst.usuario.Entity.Usuario;
import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.apache.tomcat.util.bcel.Const;
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
public class ConsultaResource {

    @Autowired
    private ConsultaService service;

    @Autowired
    private MedicoRepository medicoRepository;

    @Autowired
    private PacienteRepository pacienteRepository;


    @GetMapping(value = "/buscarId/{id}")
    @Transactional
    public ResponseEntity<DadosConsultaView> buscarPorId(@NotNull @Valid @PathVariable("id") Long Id) {
        Optional<Consulta> consulta = service.buscarPacientePorId(Id);

        return ResponseEntity.status(HttpStatus.OK).body(new DadosConsultaView((consulta.get())));
    }

    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @RolesAllowed("ROLE_PROFESSOR")
    @PostMapping("/post")
    @Transactional
    public ResponseEntity<DadosConsultaView> Cadastra(@NotNull @RequestBody @Valid DadosCadastraConsulta dados, @NotNull BindingResult result,
                                                      UriComponentsBuilder uriBuilder) {

        if (result.hasErrors()) {
            return ResponseEntity.badRequest().build();
        }


        Long idMedicos = dados.ConMedico();
        Long idPaciente = dados.ConPaciente();


        Optional<Medico> medicoOptional = medicoRepository.findById(idMedicos);
        Optional<Paciente> pacienteOptional = pacienteRepository.findById(idPaciente);


        if (medicoOptional.isEmpty() || pacienteOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Medico medico = medicoOptional.get();
        Paciente paciente = pacienteOptional.get();

        Consulta consulta = new Consulta(medico, paciente, dados);

        service.CadastraRegistroMedico(consulta);

        URI uri = uriBuilder.path("/plano/{id}").buildAndExpand(consulta.getConCodigoConsulta()).toUri();

        return ResponseEntity.created(uri).body(new DadosConsultaView(consulta));

    }


    @RolesAllowed("ROLE_DIRETOR")
    @GetMapping("/pacientepagina")
    public Page<DadosConsultaView> BuscarPorPaginas(@PageableDefault(size = 12, sort = {"CIDCódigo"}) Pageable paginacao) {
        return service.BuscarPorPaginas(paginacao);
    }


    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @GetMapping(value = "/listatodospaciente")
    public List<Consulta> buscarTodos() {
        return service.buscarTodosMedicos();
    }


    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePacienteById(@PathVariable("id") Long id) throws Exception {
        service.deletarPorId(id);
        return ResponseEntity.noContent().build();
    }

}
