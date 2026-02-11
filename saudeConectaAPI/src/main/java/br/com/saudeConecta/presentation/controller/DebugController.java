package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import br.com.saudeConecta.domain.usuario.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/debug")
@RequiredArgsConstructor
@Slf4j
public class DebugController {

    private final UsuarioRepository usuarioRepository;

    @GetMapping("/usuarios")
    public ResponseEntity<List<Usuario>> listarTodosUsuarios() {
        List<Usuario> usuarios = usuarioRepository.findAll();
        log.info("Total de usuários encontrados: {}", usuarios.size());
        return ResponseEntity.ok(usuarios);
    }

    @GetMapping("/usuario/{login}")
    public ResponseEntity<Map<String, Object>> buscarUsuario(@PathVariable String login) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Buscar como Usuario
            Usuario usuario = usuarioRepository.findUsuarioByLogin(login);
            response.put("usuarioEntity", usuario != null ? Map.of(
                "id", usuario.getId(),
                "login", usuario.getLogin(),
                "status", usuario.getStatus(),
                "enabled", usuario.isEnabled(),
                "authorities", usuario.getAuthorities(),
                "organizacaoId", usuario.getOrganizacaoId()
            ) : null);
            
            // Buscar como UserDetails
            UserDetails userDetails = usuarioRepository.findByLogin(login);
            response.put("userDetails", userDetails != null ? Map.of(
                "username", userDetails.getUsername(),
                "enabled", userDetails.isEnabled(),
                "authorities", userDetails.getAuthorities()
            ) : null);
            
            // Verificar se existe
            boolean exists = usuarioRepository.existsByLogin(login);
            response.put("exists", exists);
            
            log.info("Debug - Usuário {}: exists={}, usuarioEntity={}, userDetails={}", 
                    login, exists, usuario != null, userDetails != null);
            
        } catch (Exception e) {
            log.error("Erro ao buscar usuário: " + login, e);
            response.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
}
