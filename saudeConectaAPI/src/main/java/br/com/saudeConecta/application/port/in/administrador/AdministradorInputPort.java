package br.com.saudeConecta.application.port.in.administrador;

import br.com.saudeConecta.domain.administrador.Administrador;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface AdministradorInputPort {
    
    Optional<Administrador> buscarPorId(Long id);
    
    Optional<Administrador> buscarPorIdUsuario(Long usuarioId);
    
    List<Administrador> buscarTodos();
    
    Page<Administrador> buscarTodos(Pageable pageable);
    
    Administrador cadastrar(Administrador administrador);
    
    void deletar(Long id) throws Exception;
}
