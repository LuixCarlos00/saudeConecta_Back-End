package br.com.saudeConecta.application.port.out.medico.impl;

import br.com.saudeConecta.application.port.out.medico.MedicoOutputPort;
import br.com.saudeConecta.domain.medico.Medico;
import br.com.saudeConecta.infrastructure.persistence.repository.MedicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MedicoOutputPortImpl implements MedicoOutputPort {

    private final MedicoRepository medicoRepository;

    @Override
    public Optional<Medico> findById(Long id) {
        return medicoRepository.findById(id);
    }

    @Override
    public Optional<Medico> findByUsuario_Id(Long usuarioId) {
        return medicoRepository.findByUsuario_Id(usuarioId);
    }

    @Override
    public Optional<Medico> buscarMedicoPorIdUsuario(Long usuarioId) {
        return medicoRepository.buscarMedicoPorIdUsuario(usuarioId);
    }

    @Override
    public Optional<Medico> findByMedEmail(String email) {
        return medicoRepository.findByMedEmail(email);
    }

    @Override
    public List<Medico> findAll() {
        return medicoRepository.findAll();
    }

    @Override
    public Page<Medico> findAll(Pageable pageable) {
        return medicoRepository.findAll(pageable);
    }

    @Override
    public List<Medico> findByMedCrmContainingIgnoreCase(String crm) {
        return medicoRepository.findByMedCrmContainingIgnoreCase(crm);
    }

    @Override
    public List<Medico> findByMedNomeContainingIgnoreCase(String nome) {
        return medicoRepository.findByMedNomeContainingIgnoreCase(nome);
    }

    @Override
    public List<Medico> findByMedEspecialidadeContainingIgnoreCase(String especialidade) {
        return medicoRepository.findByMedEspecialidadeContainingIgnoreCase(especialidade);
    }

    @Override
    public List<Medico> findByEndereco_EndMunicipioContainingIgnoreCase(String municipio) {
        return medicoRepository.findByEndereco_EndMunicipioContainingIgnoreCase(municipio);
    }

    @Override
    public Long contarMedicosAtivos() {
        return medicoRepository.contarMedicosAtivos();
    }

    @Override
    public Medico save(Medico medico) {
        return medicoRepository.save(medico);
    }

    @Override
    public void deleteById(Long id) {
        medicoRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return medicoRepository.existsById(id);
    }

    @Override
    public boolean existsByUsuario_Id(Long usuarioId) {
        return medicoRepository.existsByUsuario_Id(usuarioId);
    }

    @Override
    public List<Medico> findAllWithUsuario() {
        return medicoRepository.findAllWithUsuario();
    }
}
