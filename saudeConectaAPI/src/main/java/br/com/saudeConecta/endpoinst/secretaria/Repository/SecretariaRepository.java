package br.com.saudeConecta.endpoinst.secretaria.Repository;

import br.com.saudeConecta.endpoinst.secretaria.Entity.Secretaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SecretariaRepository extends JpaRepository<Secretaria,Long> {
//    Optional<Secretaria> findByAdmEmail(String PaciEmail);
//
//    Optional<Secretaria> findByAdmUsuario_Id(Long id);
//
//
//
//
//    boolean existsByAdmUsuario_Id(Long id);
boolean existsBySecreUsuario_Login(String login);

   Optional <Secretaria>    findBySecreUsuario_Id(Long id);
}
