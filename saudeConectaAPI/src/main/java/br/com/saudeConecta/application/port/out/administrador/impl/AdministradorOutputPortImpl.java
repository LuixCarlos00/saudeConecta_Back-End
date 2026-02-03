package br.com.saudeConecta.application.port.out.administrador.impl;

import br.com.saudeConecta.application.port.out.administrador.AdministradorOutputPort;
import br.com.saudeConecta.domain.administrador.Administrador;
import br.com.saudeConecta.infrastructure.persistence.repository.AdministradorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AdministradorOutputPortImpl implements AdministradorOutputPort {

    private final AdministradorRepository administradorRepository;

    @Override
    public Optional<Administrador> findById(Long id) {
        return administradorRepository.findById(id);
    }

    @Override
    public Optional<Administrador> findByAdmUsuario_Id(Long usuarioId) {
        return administradorRepository.findByAdmUsuario_Id(usuarioId);
    }

    @Override
    public List<Administrador> findAll() {
        return administradorRepository.findAll();
    }

    @Override
    public Page<Administrador> findAll(Pageable pageable) {
        return administradorRepository.findAll(pageable);
    }

    @Override
    public Administrador save(Administrador administrador) {
        return administradorRepository.save(administrador);
    }

    @Override
    public void deleteById(Long id) {
        administradorRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return administradorRepository.existsById(id);
    }

    @Override
    public List<Administrador> findAllWithUsuario() {
        return administradorRepository.findAllWithUsuario();
    }
}
