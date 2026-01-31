package br.com.saudeConecta.application.port.out.paciente.impl;

import br.com.saudeConecta.application.port.out.paciente.PacienteOutputPort;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.infrastructure.persistence.repository.PacienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PacienteOutputPortImpl implements PacienteOutputPort {

    private final PacienteRepository pacienteRepository;

    @Override
    public Optional<Paciente> findById(Long id) {
        return pacienteRepository.findById(id);
    }

    @Override
    public Optional<Paciente> findByPaciEmail(String email) {
        return pacienteRepository.findByPaciEmail(email);
    }

    @Override
    public List<Paciente> findAll() {
        return pacienteRepository.findAll();
    }

    @Override
    public Page<Paciente> findAll(Pageable pageable) {
        return pacienteRepository.findAll(pageable);
    }

    @Override
    public List<Paciente> findByPaciCpfContainingIgnoreCase(String cpf) {
        return pacienteRepository.findByPaciCpfContainingIgnoreCase(cpf);
    }

    @Override
    public List<Paciente> findByPaciRgContainingIgnoreCase(String rg) {
        return pacienteRepository.findByPaciRgContainingIgnoreCase(rg);
    }

    @Override
    public List<Paciente> findByPaciTelefoneContainingIgnoreCase(String telefone) {
        return pacienteRepository.findByPaciTelefoneContainingIgnoreCase(telefone);
    }

    @Override
    public List<Paciente> findByPaciNomeContainingIgnoreCase(String nome) {
        return pacienteRepository.findByPaciNomeContainingIgnoreCase(nome);
    }

    @Override
    public Paciente save(Paciente paciente) {
        return pacienteRepository.save(paciente);
    }

    @Override
    public void deleteById(Long id) {
        pacienteRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return pacienteRepository.existsById(id);
    }
}
