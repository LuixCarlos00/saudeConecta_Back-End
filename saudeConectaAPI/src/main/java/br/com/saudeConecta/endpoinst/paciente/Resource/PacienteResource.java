package br.com.saudeConecta.endpoinst.paciente.Resource;

import br.com.saudeConecta.endpoinst.endereco.Entity.Endereco;
import br.com.saudeConecta.endpoinst.endereco.Repository.EnderecoRepository;
import br.com.saudeConecta.endpoinst.medico.Entity.Medico;
import br.com.saudeConecta.endpoinst.paciente.Service.PacienteService;
import br.com.saudeConecta.endpoinst.paciente.DTO.DadosCadastraPaciente;
import br.com.saudeConecta.endpoinst.paciente.DTO.DadosPacienteView;
import br.com.saudeConecta.endpoinst.paciente.Entity.Paciente;
import br.com.saudeConecta.endpoinst.usuario.Entity.Usuario;
import br.com.saudeConecta.endpoinst.usuario.Repository.UsuarioRepository;
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
@CrossOrigin(origins = "http://localhost:4200")
public class PacienteResource {

    @Autowired
    private PacienteService service;


    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EnderecoRepository enderecoRepository;


    @GetMapping(value = "/buscarId/{id}")
    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @Transactional
    public ResponseEntity<DadosPacienteView> buscarPorId(@NotNull @Valid @PathVariable("id") Long Id) {
        Optional<Paciente> paciente = service.buscarPacientePorId(Id);

        return ResponseEntity.status(HttpStatus.OK).body(new DadosPacienteView((paciente.get())));
    }




//
//    @GetMapping(value = "/buscarIdDeUsusario/{id}")
//    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
//    @Transactional
//    public ResponseEntity<DadosPacienteView> buscarPorIdDeUsusario(@NotNull @Valid @PathVariable("id") Long Id) {
//        Optional<Paciente> medico = service.buscarPacientePorIdDeUsusario(Id);
//
//        return ResponseEntity.status(HttpStatus.OK).body(new DadosPacienteView((medico.get())));
//    }






    @GetMapping(value = "/buscarPorEmail/{email}")
    @Transactional
    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    public ResponseEntity<Optional<Paciente>> ObeterCodigoParaRecuperacaoDeSenha(@NotNull @Valid @PathVariable("email") String email) throws Exception {

        Optional<Paciente> paciente = service.buscarPacsientePorEmail(email);


        return  ResponseEntity.ok().body(paciente);
    }



    @GetMapping(value = "/InserirCodigo/{codigo}")
    @Transactional
    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    public ResponseEntity<ResponseEntity<Object>> InserirCodigoDeRecuperacaoDeSenhaValido(@NotNull @Valid @PathVariable("codigo") String codigo) throws Exception {

        Boolean paciente = service.VerificarCodigoValido(codigo);
        service.deletraCodigoVerificacao(codigo);
        if (paciente){

            return ResponseEntity.ok().build();
        }

        return  ResponseEntity.notFound().build();
    }









    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @PostMapping("/post")
    @Transactional
    public ResponseEntity<DadosPacienteView> cadastrarPaciente(@RequestBody @Valid DadosCadastraPaciente dados,
                                                               BindingResult result,
                                                               UriComponentsBuilder uriBuilder) {

        if (result.hasErrors()) { return ResponseEntity.badRequest().build(); }



      //  Long idUsuario = dados.usuario();
        Long idEndereco = dados.endereco();


       // Optional<Usuario> usuarioOptional = usuarioRepository.findById(idUsuario);
        Optional<Endereco> enderecoOptional = enderecoRepository.findById(idEndereco);

        if (  enderecoOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }


       // Usuario usuario = usuarioOptional.get();
        Endereco endereco = enderecoOptional.get();


        Paciente paciente = new Paciente(dados,  endereco);


        service.CadastraRegistroPaciente(paciente );


        URI uri = uriBuilder.path("/api/pacientes/{id}").buildAndExpand(paciente.getPaciCodigo()).toUri();


        return ResponseEntity.created(uri).body(new DadosPacienteView(paciente));
    }






    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
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




    @GetMapping(value = "/buscarPorNome/{nome}")
    @Transactional
    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    public List<Paciente> buscarMedicoPorNome(@NotNull @Valid @PathVariable("nome") String nome) {
        return service.buscarMedicoPorNome(nome);
    }

    @GetMapping(value = "/buscarPorCPF/{CPF}")
    @Transactional
    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    public List<Paciente> buscarMedicoPorCPF(@NotNull @Valid @PathVariable("CPF") String cpf) {
        return service.buscarMedicoPorCPF(cpf);
    }


    @GetMapping(value = "/buscarPorRG/{rg}")
    @Transactional
    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    public List<Paciente> buscarMedicoPorRG(@NotNull @Valid @PathVariable("rg") String rg) {
        return service.buscarMedicoPorRG(rg);
    }

    @GetMapping(value = "/buscarPorTelefone/{telefone}")
    @Transactional
    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    public List<Paciente>buscarMedicoPorMedTelefone(@NotNull @Valid @PathVariable("telefone") String telefone) {
        return   service.buscarMedicoPorTelefone(telefone);
    }



}
