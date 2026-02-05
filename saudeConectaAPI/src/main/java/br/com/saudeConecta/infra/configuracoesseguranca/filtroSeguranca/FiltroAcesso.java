package br.com.saudeConecta.infra.configuracoesseguranca.filtroSeguranca;


import br.com.saudeConecta.infra.configuracoesseguranca.TokenService;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class FiltroAcesso extends OncePerRequestFilter {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, @NotNull FilterChain filterChain) throws ServletException, IOException {

        var tokenJwt = recuperarToken(request);


        if (tokenJwt != null) {
            var subject = tokenService.getSubject(tokenJwt);
            var usuario = usuarioRepository.findByLogin(subject);
            var authentication = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }


        filterChain.doFilter(request,response);
    }




    private @Nullable String recuperarToken(@NotNull HttpServletRequest request) {

        var authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.replace("Bearer ", "");
        }

       // a requisisao ta vindo so o bearer  validar se tem token e nao o bearer sozinho   fazer isso no metodo de login front tbm  se e necessario colocar o bearer
        return null;
    }


}