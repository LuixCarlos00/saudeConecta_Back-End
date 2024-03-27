package br.com.saudeConecta.endpoinst.usuario.Resource;


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
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RequestMapping("/Home")
@RestController
public class UsuarioResource {

    @Autowired
    private UsuarioService service;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private TokenService tokenService;


    @PostMapping("/login")
    public ResponseEntity<DadosTokenJWT> login(@RequestBody @NotNull @org.jetbrains.annotations.NotNull DadosLoginUsuario dados) {
        var authenticatetoken = new UsernamePasswordAuthenticationToken(dados.login(), dados.senha());
        var authentication = authenticationManager.authenticate(authenticatetoken);

        var TokenJWT = tokenService.gerarToken((Usuario) authentication.getPrincipal());

        return ResponseEntity.ok(new DadosTokenJWT(TokenJWT));
    }





    @GetMapping(value = "/buscarId/{id}")
    @Transactional
    public ResponseEntity<DadosUsuarioView> buscarPorId(@NotNull @Valid @PathVariable("id") Long Id) {
        Optional<Usuario> usuario = service.buscarPacientePorId(Id);

        return ResponseEntity.status(HttpStatus.OK).body(new DadosUsuarioView((usuario.get())));
    }


}
