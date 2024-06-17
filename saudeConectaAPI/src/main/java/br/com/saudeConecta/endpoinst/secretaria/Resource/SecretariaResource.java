package br.com.saudeConecta.endpoinst.secretaria.Resource;

import br.com.saudeConecta.endpoinst.endereco.Repository.EnderecoRepository;
import br.com.saudeConecta.endpoinst.secretaria.DTO.DadosSecretariaView;
import br.com.saudeConecta.endpoinst.secretaria.DTO.DadosCadastraSecretaria;
import br.com.saudeConecta.endpoinst.secretaria.Entity.Secretaria;
import br.com.saudeConecta.endpoinst.secretaria.Service.SecretariaService;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RequestMapping(value = "/secretaria")
@RestController
@CrossOrigin(origins = "http://localhost:4200")
public class SecretariaResource {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SecretariaService service;

    @Autowired
    private EnderecoRepository enderecoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping(value = "/buscarId/{id}")
    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @Transactional
    public ResponseEntity<DadosSecretariaView> buscarPorId(@NotNull @Valid @PathVariable("id") Long Id) {
        Optional<Secretaria> medico = service.buscarPacientePorId(Id);

        return ResponseEntity.status(HttpStatus.OK).body(new DadosSecretariaView((medico.get())));
    }




    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @PostMapping("/post")
    @Transactional
    public ResponseEntity<DadosSecretariaView> cadastrarSecretaria(@RequestBody @Valid DadosCadastraSecretaria dados,
                                                                   BindingResult result,
                                                                   UriComponentsBuilder uriBuilder) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().build();
        }

        Long idUsuario = dados.SecreUsuario();

        Optional<Usuario> usuarioOptional = usuarioRepository.findById(idUsuario);

        if (usuarioOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Usuario usuario = usuarioOptional.get();
        Secretaria secretaria = new Secretaria(dados, usuario);

        service.CadastraRegistroPaciente(secretaria);

        URI uri = uriBuilder.path("/Secretaria/secretaria/{id}")
                .buildAndExpand(secretaria.getSecreCodigo())
                .toUri();

        return ResponseEntity.created(uri).body(new DadosSecretariaView(secretaria));
    }



    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePacienteById(@PathVariable("id") Long id) throws Exception {
        service.deletarPorId(id);
        return ResponseEntity.noContent().build();
    }





    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @GetMapping("/pacientepagina")
    public Page<DadosSecretariaView> BuscarPorPaginas(@PageableDefault(size = 12, sort = {"CIDCódigo"}) Pageable paginacao) {
        return service.BuscarPorPaginas(paginacao);
    }




    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @GetMapping(value = "/listatodospaciente")
    public List<Secretaria> buscarTodos() {
        return service.buscarTodosPaciente();
    }



    @GetMapping(value = "/buscarPorCoigoAutorizacao/{codigo}")
    @Transactional
    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    public ResponseEntity<Void> buscarCodigodeAutorizacao(@NotNull @Valid @PathVariable("codigo") String codigo) throws Exception {
        boolean existe = service.BuscarCodigodeAutorizacao(codigo);
        if (existe) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }



//
//    @GetMapping(value = "/buscarIdDeUsusario/{id}")
//    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
//    @Transactional
//    public ResponseEntity<DadosSecretariaView> buscarPorIdDeUsusario(@NotNull @Valid @PathVariable("id") Long Id) {
//        Optional<Secretaria> medico = service.buscarPacientePorIdDeUsusario(Id);
//
//        return ResponseEntity.status(HttpStatus.OK).body(new DadosSecretariaView((medico.get())));
//    }


//    @GetMapping(value = "/buscarPorEmail/{email}")
//    @Transactional
//    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
//    public ResponseEntity<Object> ObeterCodigoParaRecuperacaoDeSenhaPassandoOEmail(@NotNull @Valid @PathVariable("email") String email) throws Exception {
//
//           // Optional<Object> obj = service.buscarPacsientePorEmail(email);
//
//
//        return ResponseEntity.ok().body(obj);
//    }









//    @GetMapping(value = "/InserirCodigo/{codigo}")
//    @Transactional
//    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
//    public ResponseEntity<ResponseEntity<Object>> InserirCodigoDeRecuperacaoDeSenhaValido(@NotNull @Valid @PathVariable("codigo") String codigo) throws Exception {
//
//        Boolean paciente = service.VerificarCodigoValido(codigo);
//        service.deletraCodigoVerificacao(codigo);
//        if (paciente) {
//
//            return ResponseEntity.ok().build();
//        }
//
//        return ResponseEntity.notFound().build();
//    }
//

//
//
//    @PutMapping(value = "/esqueciMinhaSenhaADM")
//    @Transactional
//    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
//    public ResponseEntity<Object> esqueciMinhaSenha(@RequestBody @Valid DadosTrocaDeSenha dados, BindingResult result) {
//        if (result.hasErrors()) {
//            return ResponseEntity.badRequest().build();
//        }
//        String senhaNova = passwordEncoder.encode(dados.senhaNova());
//        service.BuscaPorSenhaAntiga(senhaNova, dados.id());
//        return ResponseEntity.ok().build();
//    }
//
//
//
//
//
//
//    @PutMapping(value = "/TrocaSenhaADM")
//    @Transactional
//    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
//    public boolean TrocaSenha(@RequestBody @Valid DadosTrocaDeSenha dados,
//                              @NotNull BindingResult result) {
//        if (result.hasErrors()) {
//            return false;
//        }
//        String SenhaNova = passwordEncoder.encode(dados.senhaNova());
//        // Não encode a senha antiga aqui, apenas passe-a para o serviço
//        boolean existe = service.EsqueciMinhaSenha(SenhaNova, dados.senhaAntiga(), dados.id(), dados.email());
//        return existe;
//    }
//


}
