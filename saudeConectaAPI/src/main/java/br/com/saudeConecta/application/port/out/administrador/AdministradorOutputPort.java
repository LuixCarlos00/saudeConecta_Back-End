package br.com.saudeConecta.application.port.out.administrador;

import br.com.saudeConecta.domain.administrador.Administrador;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface AdministradorOutputPort {
    
    Optional<Administrador> findById(Long id);
    
    Optional<Administrador> findByAdmUsuario_Id(Long usuarioId);
    
    List<Administrador> findAll();
    
    Page<Administrador> findAll(Pageable pageable);
    
    Administrador save(Administrador administrador);
    
    void deleteById(Long id);
    
    boolean existsById(Long id);
}
