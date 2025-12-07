package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.administrador.Administrador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdministradorRepository extends JpaRepository<Administrador, Long> {

    Optional<Administrador> findByAdmEmail(String email);

    Optional<Administrador> findByAdmUsuario_Id(Long usuarioId);
    
    boolean existsByAdmUsuario_Id(Long usuarioId);
}
