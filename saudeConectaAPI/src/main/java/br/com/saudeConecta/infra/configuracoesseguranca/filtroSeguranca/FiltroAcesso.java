package br.com.saudeConecta.infra.configuracoesseguranca.filtroSeguranca;



import br.com.saudeConecta.endpoinst.usuario.Repository.UsuarioRepository;
import br.com.saudeConecta.infra.configuracoesseguranca.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jdk.jfr.Name;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Component
public class FiltroAcesso extends OncePerRequestFilter {


    @Autowired
    private TokenService tokenService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Name("A classe FiltroAceso e responsável por fazer a filtração do token valido do usuário ")


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, @NotNull FilterChain filterChain) throws ServletException, IOException {


        var tokenJwt = recuperarToken(request);


      if (tokenJwt!=null){

          var subjet  =  tokenService.getSubjet(tokenJwt);
          var usuario = usuarioRepository.findBylogin(subjet);
          var authetication = new UsernamePasswordAuthenticationToken(usuario,null, usuario.getAuthorities());

          SecurityContextHolder.getContext().setAuthentication(authetication);

      }


        filterChain.doFilter(request,response);
    }




    private @Nullable String recuperarToken(@NotNull HttpServletRequest request) {

        var authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null) {
            return authorizationHeader.replace("Bearer ", "");
        }
        return null;
    }


}