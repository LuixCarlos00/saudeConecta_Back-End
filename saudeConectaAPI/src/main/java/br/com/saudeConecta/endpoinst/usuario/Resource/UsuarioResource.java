package br.com.saudeConecta.endpoinst.usuario.Resource;


import br.com.saudeConecta.email.EnviarEmail.EnviarEmail;
import br.com.saudeConecta.endpoinst.medico.Entity.Medico;
import br.com.saudeConecta.endpoinst.medico.Repository.MedicoRepository;
import br.com.saudeConecta.endpoinst.paciente.Entity.Paciente;
import br.com.saudeConecta.endpoinst.paciente.Repository.PacienteRepository;
import br.com.saudeConecta.endpoinst.secretaria.Entity.BuscarTodosUsuarios;
import br.com.saudeConecta.endpoinst.usuario.DTO.*;
import br.com.saudeConecta.endpoinst.usuario.Entity.Usuario;
import br.com.saudeConecta.endpoinst.usuario.Service.UsuarioService;


import br.com.saudeConecta.infra.configuracoesseguranca.TokenService;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;


import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RequestMapping("/Home")
@RestController
@CrossOrigin(origins = "http://localhost:4200")
public class UsuarioResource {

    @Autowired
    private UsuarioService userService;

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private PasswordEncoder passwordEncoder;


    @Autowired
    private TokenService tokenService;
    @Autowired
    private PacienteRepository pacienteRepository;
    @Autowired
    private MedicoRepository medicoRepository;

    @Autowired
    private EnviarEmail enviarEmail;
    @Autowired
    private UsuarioService usuarioService;

    @CrossOrigin(origins = "http://localhost:4200  ", allowCredentials = "true")
    @PostMapping("/login")
    public ResponseEntity<DadosTokenJWT> login(@RequestBody @NotNull DadosLoginUsuario dados) {
        var authenticatetoken = new UsernamePasswordAuthenticationToken(dados.login(), dados.senha());
        var authentication = authenticationManager.authenticate(authenticatetoken);

        var TokenJWT = tokenService.gerarToken((Usuario) authentication.getPrincipal());

        return ResponseEntity.ok(new DadosTokenJWT(TokenJWT));
    }


    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @PostMapping("/cadastralogin")
    public ResponseEntity<CadastroResponse> cadastraUser(@RequestBody @NotNull DadosLoginUsuario dados) {

        if (dados.login() == null || dados.senha() == null) {
            return ResponseEntity.badRequest().build();
        }

        String senhaCriptografada = passwordEncoder.encode(dados.senha());

        Usuario usuario = new Usuario(dados, senhaCriptografada);

        Boolean exite = usuarioService.existeLogin(dados.login());

        if (exite) {
            return ResponseEntity.badRequest().build();
        }

        // Cadastra o usuário
        userService.CadastraUsuario(usuario);

        // Autentica o usuário recém-cadastrado para gerar o token JWT
        var authenticateToken = new UsernamePasswordAuthenticationToken(dados.login(), dados.senha());
        var authentication = authenticationManager.authenticate(authenticateToken);
        var tokenJWT = tokenService.gerarToken((Usuario) authentication.getPrincipal());


        CadastroResponse response = new CadastroResponse(new DadosUsuarioView(usuario), tokenJWT);
        return ResponseEntity.ok().body(response);
    }


    @GetMapping(value = "/buscarId/{id}")
    @Transactional
    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    public ResponseEntity<DadosUsuarioView> buscarPorId(@NotNull @Valid @PathVariable("id") Long Id) {
        Optional<Usuario> usuario = userService.buscarUserPorId(Id);

        return ResponseEntity.status(HttpStatus.OK).body(new DadosUsuarioView((usuario.get())));
    }


    @GetMapping(value = "/buscarUsuarioExistente/{login}")
    @Transactional
    @CrossOrigin(origins = "http://localhost:4200  ", allowCredentials = "true")
    public Boolean buscarPorloginSeExiste(@NotNull @Valid @PathVariable("login") String login) {
        Boolean usuario = userService.buscarPorloginSeExiste(login);

        return usuario;
    }


    @PutMapping(value = "/trocaDeSenha/{Id}")
    @Transactional
    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    public ResponseEntity<ResponseEntity<Object>> updatedeSenha(@NotNull @Valid @PathVariable("Id") Long Id,
                                                                @RequestBody DadosLoginUsuario dados,
                                                                @NotNull BindingResult result) {

        if (result.hasErrors()) {
            return ResponseEntity.badRequest().build();
        }

        String senhaCriptografada = passwordEncoder.encode(dados.senha());
        Usuario usuario = new Usuario(dados, senhaCriptografada);

        userService.UpdateDeSenha(usuario, Id);


        return ResponseEntity.ok().build();
    }


    @GetMapping(value = "/recuperaLogin={Id}&dados={tipoUsuario}")
    @Transactional
    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    public ResponseEntity<ResponseEntity<Object>> RecuperaLogin(@NotNull @Valid @PathVariable("Id") Long Id,
                                                                @NotNull @Valid @PathVariable("tipoUsuario") String dados
    ) throws MessagingException {


        Usuario usuario = userService.RecuperaLogin(Id);

        String login = usuario.getLogin();
        Long IdUsuario = usuario.getId();

        Optional<Medico> medico = null;
        Optional<Paciente> paciente = null;
        if (dados.equals("Medico")) {

            medico = medicoRepository.findByUsuario_Id(IdUsuario);


            if (medico == null) {
                return ResponseEntity.notFound().build();
            }
            enviarEmail.enviarLoginDeMedicoRecuperacaoDeLogin(medico, login);
        }



        return ResponseEntity.ok().build();
    }



    @GetMapping(value = "/BuscarTodosUsuarios")
    @Transactional
    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    public BuscarTodosUsuarios BuscarTodosUsuarios() {
        return userService.BuscarTodosUsuarios();
    }






}
