package br.com.saudeConecta.endpoinst.paciente.Resource;

import br.com.saudeConecta.endpoinst.endereco.Entity.Endereco;
import br.com.saudeConecta.endpoinst.endereco.Repository.EnderecoRepository;
import br.com.saudeConecta.endpoinst.paciente.Service.PacienteService;
import br.com.saudeConecta.endpoinst.paciente.DTO.DadosCadastraPaciente;
import br.com.saudeConecta.endpoinst.paciente.DTO.DadosPacienteView;
import br.com.saudeConecta.endpoinst.paciente.Entity.Paciente;
import br.com.saudeConecta.endpoinst.usuario.Entity.Usuario;
import br.com.saudeConecta.endpoinst.usuario.Repository.UsuarioRepository;
import jakarta.annotation.security.RolesAllowed;
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

@RequestMapping(value = "/paciente")
@RestController
public class PacienteResource {

    @Autowired
    private PacienteService service;


    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EnderecoRepository enderecoRepository;

    @GetMapping(value = "/buscarId/{id}")
    @Transactional
    public ResponseEntity<DadosPacienteView> buscarPorId(@NotNull @Valid @PathVariable("id") Long Id) {
        Optional<Paciente> medico = service.buscarPacientePorId(Id);

        return ResponseEntity.status(HttpStatus.OK).body(new DadosPacienteView((medico.get())));
    }


    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @PostMapping("/post")
    @Transactional
    public ResponseEntity<DadosPacienteView> cadastrarPaciente(@RequestBody @Valid DadosCadastraPaciente dados,
                                                               BindingResult result,
                                                               UriComponentsBuilder uriBuilder) {

        if (result.hasErrors()) { return ResponseEntity.badRequest().build(); }



        Long idUsuario = dados.usuario();
        Long idEndereco = dados.endereco();


        Optional<Usuario> usuarioOptional = usuarioRepository.findById(idUsuario);
        Optional<Endereco> enderecoOptional = enderecoRepository.findById(idEndereco);

        if (usuarioOptional.isEmpty() || enderecoOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }


        Usuario usuario = usuarioOptional.get();
        Endereco endereco = enderecoOptional.get();


        Paciente paciente = new Paciente(dados, usuario, endereco);


        service.CadastraRegistroPaciente(paciente );


        URI uri = uriBuilder.path("/api/pacientes/{id}").buildAndExpand(paciente.getPaciCodigo()).toUri();


        return ResponseEntity.created(uri).body(new DadosPacienteView(paciente));
    }


    @RolesAllowed("ROLE_DIRETOR")
    @GetMapping("/pacientepagina")
    public Page<DadosPacienteView> BuscarPorPaginas(@PageableDefault(size = 12, sort = {"CIDCódigo"}) Pageable paginacao) {
        return service.BuscarPorPaginas(paginacao);
    }


    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @GetMapping(value = "/listatodospaciente")
    public List<Paciente> buscarTodos() {
        return service.buscarTodosPaciente();
    }


    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePacienteById(@PathVariable("id") Long id) throws Exception {
        service.deletarPorId(id);
        return ResponseEntity.noContent().build();
    }

}
