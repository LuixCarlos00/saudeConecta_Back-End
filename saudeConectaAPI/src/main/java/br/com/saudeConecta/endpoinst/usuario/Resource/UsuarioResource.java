package br.com.saudeConecta.endpoinst.usuario.Resource;


import br.com.saudeConecta.endpoinst.consulta.DTO.DadosConsultaView;
import br.com.saudeConecta.endpoinst.usuario.DTO.DadosCadastraUsuario;
import br.com.saudeConecta.endpoinst.usuario.DTO.DadosLoginUsuario;
import br.com.saudeConecta.endpoinst.usuario.DTO.DadosTokenJWT;
import br.com.saudeConecta.endpoinst.usuario.DTO.DadosUsuarioView;
import br.com.saudeConecta.endpoinst.usuario.Entity.Usuario;
import br.com.saudeConecta.endpoinst.usuario.Service.UsuarioService;


import br.com.saudeConecta.infra.configuracoesseguranca.TokenService;
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
import org.springframework.web.util.UriComponentsBuilder;

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
    private   PasswordEncoder passwordEncoder;



    @Autowired
    private TokenService tokenService;

    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @PostMapping("/login")
    public ResponseEntity<DadosTokenJWT> login(@RequestBody @NotNull   DadosLoginUsuario dados) {
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

        // Cadastra o usuário
        userService.CadastraUsuario(usuario);

        // Autentica o usuário recém-cadastrado para gerar o token JWT
        var authenticateToken = new UsernamePasswordAuthenticationToken(dados.login(), dados.senha());
        var authentication = authenticationManager.authenticate(authenticateToken);
        var tokenJWT = tokenService.gerarToken((Usuario) authentication.getPrincipal());

        // Retorna o token JWT e os dados do usuário cadastrado encapsulados na classe CadastroResponse
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


}
