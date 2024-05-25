package br.com.saudeConecta.endpoinst.paciente.Repository;

import br.com.saudeConecta.endpoinst.paciente.Entity.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface PacienteRepository extends JpaRepository<Paciente,Long> {
    Optional<Paciente> findByPaciEmail(String PaciEmail);

    Optional<Paciente> findByUsuario_Id(Long id);

    List<Paciente> findByPaciNomeContainingIgnoreCase(String PaciNome);

    List<Paciente> findByPaciTelefoneContainingIgnoreCase(String PaciTelefone);

    @Query("SELECT p FROM Paciente p WHERE REPLACE(REPLACE(REPLACE(REPLACE(p.paciRg, '.', ''), '-', ''), '/', ''), ' ', '') LIKE %:rg%")
    List<Paciente> findByRgIgnoringFormatting(@Param("rg") String rg);

    @Query("SELECT p FROM Paciente p WHERE REPLACE(REPLACE(REPLACE(p.paciCpf, '.', ''), '-', ''), '/', '') LIKE %:cpf%")
    List<Paciente> findByCpfIgnoringFormatting(@Param("cpf") String cpf);
}
