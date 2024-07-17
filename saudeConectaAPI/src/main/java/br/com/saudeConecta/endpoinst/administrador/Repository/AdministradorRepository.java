package br.com.saudeConecta.endpoinst.administrador.Repository;

import br.com.saudeConecta.endpoinst.administrador.Entity.Administrador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdministradorRepository extends JpaRepository<Administrador,Long> {
    Optional<Administrador> findByAdmEmail(String PaciEmail);

    Optional<Administrador> findByAdmUsuario_Id(Long id);




    boolean existsByAdmCodigoAtorizacao(String admCodigoAtorizacao);



    boolean existsByAdmUsuario_Id(Long id);
}
