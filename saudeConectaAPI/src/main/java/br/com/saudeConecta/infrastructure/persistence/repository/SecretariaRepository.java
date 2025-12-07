package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.secretaria.Secretaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SecretariaRepository extends JpaRepository<Secretaria,Long> {
  
boolean existsBySecreUsuario_Login(String login);

   Optional <Secretaria>    findBySecreUsuario_Id(Long id);
}
