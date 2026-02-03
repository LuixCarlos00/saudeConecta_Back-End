package br.com.saudeConecta.endpoinst.paciente.Resource;

import br.com.saudeConecta.domain.endereco.Endereco;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.endpoinst.paciente.DTO.DadosCadastraPaciente;
import br.com.saudeConecta.endpoinst.paciente.DTO.DadosPacienteView;
import br.com.saudeConecta.endpoinst.paciente.Service.PacienteService;
import br.com.saudeConecta.infrastructure.persistence.repository.EnderecoRepository;
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
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class PacienteResource {

    @Autowired
    private PacienteService service;




    @Autowired
    private EnderecoRepository enderecoRepository;


    @GetMapping(value = "/buscarId/{id}")
    @Transactional
    public ResponseEntity<DadosPacienteView> buscarPorId(@NotNull @Valid @PathVariable("id") Long Id) {
        Optional<Paciente> paciente = service.buscarPacientePorId(Id);

        return ResponseEntity.status(HttpStatus.OK).body(new DadosPacienteView((paciente.get())));
    }




 




    @GetMapping(value = "/buscarPorEmail/{email}")
    @Transactional
    public ResponseEntity<Optional<Paciente>> buscarPorEmail(@NotNull @Valid @PathVariable("email") String email) throws Exception {

        Optional<Paciente> paciente = service.buscarPacientePorEmail(email);


        return  ResponseEntity.ok().body(paciente);
    }



    @GetMapping(value = "/InserirCodigo/{codigo}")
    @Transactional
    public ResponseEntity<ResponseEntity<Object>> verificarCodigoRecuperacao(@NotNull @Valid @PathVariable("codigo") String codigo) throws Exception {

        Boolean paciente = service.verificarCodigoValido(codigo);
        service.deletarCodigoVerificacao(codigo);
        if (paciente){

            return ResponseEntity.ok().build();
        }

        return  ResponseEntity.notFound().build();
    }









    @PostMapping("/post")
    @Transactional
    public ResponseEntity<DadosPacienteView> cadastrarPaciente(@RequestBody @Valid DadosCadastraPaciente dados,
                                                               BindingResult result,
                                                               UriComponentsBuilder uriBuilder) {

        if (result.hasErrors()) { return ResponseEntity.badRequest().build(); }



        Long idEndereco = dados.endereco();
        Optional<Endereco> enderecoOptional = enderecoRepository.findById(idEndereco);

        if (enderecoOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Endereco endereco = enderecoOptional.get();


        Paciente paciente = new Paciente(dados,  endereco);


        service.cadastrarPaciente(paciente);


        URI uri = uriBuilder.path("/api/pacientes/{id}").buildAndExpand(paciente.getPaciCodigo()).toUri();


        return ResponseEntity.created(uri).body(new DadosPacienteView(paciente));
    }






    @GetMapping("/pacientepagina")
    public Page<DadosPacienteView> buscarPorPaginas(@PageableDefault(size = 12, sort = {"paciCodigo"}) Pageable paginacao) {
        return service.buscarPorPaginas(paginacao);
    }






    @GetMapping(value = "/listatodospaciente")
    public List<Paciente> buscarTodos() {
        return service.buscarTodosPaciente();
    }






    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePacienteById(@PathVariable("id") Long id) throws Exception {
        service.deletarPorId(id);
        return ResponseEntity.noContent().build();
    }




    @GetMapping(value = "/buscarPorNome/{nome}")
    @Transactional
    public List<Paciente> buscarPacientePorNome(@NotNull @Valid @PathVariable("nome") String nome) {
        return service.buscarPacientePorNome(nome);
    }

    @GetMapping(value = "/buscarPorCPF/{CPF}")
    @Transactional
    public List<Paciente> buscarPacientePorCPF(@NotNull @Valid @PathVariable("CPF") String cpf) {
        return service.buscarPacientePorCPF(cpf);
    }


    @GetMapping(value = "/buscarPorRG/{rg}")
    @Transactional
    public List<Paciente> buscarPacientePorRG(@NotNull @Valid @PathVariable("rg") String rg) {
        return service.buscarPacientePorRG(rg);
    }

    @GetMapping(value = "/buscarPorTelefone/{telefone}")
    @Transactional
    public List<Paciente>buscarPacientePorTelefone(@NotNull @Valid @PathVariable("telefone") String telefone) {
        return   service.buscarPacientePorTelefone(telefone);
    }



}
