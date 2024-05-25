package br.com.saudeConecta.endpoinst.administrador.Resource;

import br.com.saudeConecta.endpoinst.administrador.DTO.DadosCadastraAdministrador;
import br.com.saudeConecta.endpoinst.administrador.DTO.DadosAdiministradorView;
import br.com.saudeConecta.endpoinst.administrador.Entity.Administrador;
import br.com.saudeConecta.endpoinst.administrador.Service.AdministradorService;
import br.com.saudeConecta.endpoinst.endereco.Repository.EnderecoRepository;
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

@RequestMapping(value = "/amd")
@RestController
@CrossOrigin(origins = "http://localhost:4200")
public class AdministradorResource {

    @Autowired
    private AdministradorService service;


    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EnderecoRepository enderecoRepository;


    @GetMapping(value = "/buscarId/{id}")
    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @Transactional
    public ResponseEntity<DadosAdiministradorView> buscarPorId(@NotNull @Valid @PathVariable("id") Long Id) {
        Optional<Administrador> medico = service.buscarPacientePorId(Id);

        return ResponseEntity.status(HttpStatus.OK).body(new DadosAdiministradorView((medico.get())));
    }





    @GetMapping(value = "/buscarIdDeUsusario/{id}")
    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @Transactional
    public ResponseEntity<DadosAdiministradorView> buscarPorIdDeUsusario(@NotNull @Valid @PathVariable("id") Long Id) {
        Optional<Administrador> medico = service.buscarPacientePorIdDeUsusario(Id);

        return ResponseEntity.status(HttpStatus.OK).body(new DadosAdiministradorView((medico.get())));
    }






    @GetMapping(value = "/buscarPorEmail/{email}")
    @Transactional
    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    public ResponseEntity<Optional<Administrador>> ObeterCodigoParaRecuperacaoDeSenha(@NotNull @Valid @PathVariable("email") String email) throws Exception {

        Optional<Administrador> paciente = service.buscarPacsientePorEmail(email);


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
    public ResponseEntity<DadosAdiministradorView> cadastrarAdministrador(@RequestBody @Valid DadosCadastraAdministrador dados,
                                                                     BindingResult result,
                                                                     UriComponentsBuilder uriBuilder) {

        if (result.hasErrors()) { return ResponseEntity.badRequest().build(); }



        Long idUsuario = dados.AdmUsuario();



        Optional<Usuario> usuarioOptional = usuarioRepository.findById(idUsuario);


        if (usuarioOptional.isEmpty()  ) {
            return ResponseEntity.notFound().build();
        }


        Usuario usuario = usuarioOptional.get();



        Administrador paciente = new Administrador(dados, usuario );


        service.CadastraRegistroPaciente(paciente );


        URI uri = uriBuilder.path("/api/pacientes/{id}").buildAndExpand(paciente.getAdmCodigo()).toUri();


        return ResponseEntity.created(uri).body(new DadosAdiministradorView(paciente));
    }






    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @GetMapping("/pacientepagina")
    public Page<DadosAdiministradorView> BuscarPorPaginas(@PageableDefault(size = 12, sort = {"CIDCódigo"}) Pageable paginacao) {
        return service.BuscarPorPaginas(paginacao);
    }






    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @GetMapping(value = "/listatodospaciente")
    public List<Administrador> buscarTodos() {
        return service.buscarTodosPaciente();
    }






    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePacienteById(@PathVariable("id") Long id) throws Exception {
        service.deletarPorId(id);
        return ResponseEntity.noContent().build();
    }

}
