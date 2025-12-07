package br.com.saudeConecta.endpoinst.administrador.Resource;

import br.com.saudeConecta.domain.administrador.Administrador;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.endpoinst.administrador.DTO.DadosAdiministradorView;
import br.com.saudeConecta.endpoinst.administrador.DTO.DadosCadastraAdministrador;
import br.com.saudeConecta.endpoinst.administrador.Service.AdministradorService;
import br.com.saudeConecta.infrastructure.persistence.repository.EnderecoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
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

@RequestMapping(value = "/administrador")
@RestController
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class AdministradorResource {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AdministradorService service;

    @Autowired
    private EnderecoRepository enderecoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping(value = "/buscarId/{id}")
    @Transactional
    public ResponseEntity<DadosAdiministradorView> buscarPorId(@NotNull @Valid @PathVariable("id") Long Id) {
        Optional<Administrador> administrador = service.buscarAdministradorPorId(Id);

        return ResponseEntity.status(HttpStatus.OK).body(new DadosAdiministradorView((administrador.get())));
    }


    @GetMapping(value = "/buscarIdDeUsusario/{id}")
    @Transactional
    public ResponseEntity<DadosAdiministradorView> buscarPorIdUsuario(@NotNull @Valid @PathVariable("id") Long Id) {
        Optional<Administrador> administrador = service.buscarAdministradorPorIdUsuario(Id);

        return ResponseEntity.status(HttpStatus.OK).body(new DadosAdiministradorView((administrador.get())));
    }


    @GetMapping(value = "/buscarPorEmail/{email}")
    @Transactional
    public ResponseEntity<Object> buscarPorEmailEnviarCodigo(@NotNull @Valid @PathVariable("email") String email) throws Exception {

            Optional<Object> obj = service.buscarPorEmailEnviarCodigo(email);


        return ResponseEntity.ok().body(obj);
    }








    @GetMapping(value = "/InserirCodigo/{codigo}")
    @Transactional
    public ResponseEntity<ResponseEntity<Object>> verificarCodigoRecuperacao(@NotNull @Valid @PathVariable("codigo") String codigo) throws Exception {

        Boolean valido = service.verificarCodigoValido(codigo);
        service.deletarCodigoVerificacao(codigo);
        if (valido) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }







    @PostMapping("/post")
    @Transactional
    public ResponseEntity<DadosAdiministradorView> cadastrarAdministrador(@RequestBody @Valid DadosCadastraAdministrador dados,
                                                                          BindingResult result,
                                                                          UriComponentsBuilder uriBuilder) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().build();
        }

        Long idUsuario = dados.AdmUsuario();

        Optional<Usuario> usuarioOptional = usuarioRepository.findById(idUsuario);

        if (usuarioOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Usuario usuario = usuarioOptional.get();
        Administrador administrador = new Administrador(dados, usuario);

        service.cadastrarAdministrador(administrador);

        URI uri = uriBuilder.path("/administrador/administrador/{id}")
                .buildAndExpand(administrador.getAdmCodigo())
                .toUri();

        return ResponseEntity.created(uri).body(new DadosAdiministradorView(administrador));
    }







    @GetMapping("/pacientepagina")
    public Page<DadosAdiministradorView> buscarPorPaginas(@PageableDefault(size = 12, sort = {"admCodigo"}) Pageable paginacao) {
        return service.buscarPorPaginas(paginacao);
    }








    @GetMapping(value = "/listatodospaciente")
    public List<Administrador> buscarTodos() {
        return service.buscarTodosAdministradores();
    }








    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePacienteById(@PathVariable("id") Long id) throws Exception {
        service.deletarPorId(id);
        return ResponseEntity.noContent().build();
    }









}
