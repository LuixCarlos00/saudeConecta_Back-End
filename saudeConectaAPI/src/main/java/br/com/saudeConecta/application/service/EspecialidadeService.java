package br.com.saudeConecta.application.service;

import br.com.saudeConecta.domain.profissional.Especialidade;
import br.com.saudeConecta.domain.profissional.TipoProfissional;
import br.com.saudeConecta.infrastructure.persistence.repository.EspecialidadeRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.TipoProfissionalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EspecialidadeService {
    
    private final EspecialidadeRepository especialidadeRepository;
    private final TipoProfissionalRepository tipoProfissionalRepository;
    
    public List<Especialidade> listarTodas() {
        return especialidadeRepository.findAll();
    }
    
    public List<Especialidade> listarAtivas() {
        return especialidadeRepository.findAll().stream()
            .filter(e -> e.getStatus() != null && e.getStatus() == 1)
            .toList();
    }
    
    public Optional<Especialidade> buscarPorId(Long id) {
        return especialidadeRepository.findById(id);
    }
    
    public List<Especialidade> listarPorTipo(String tipoCodigo) {
        return especialidadeRepository.findAtivasByTipoProfissionalCodigo(tipoCodigo.toUpperCase());
    }
    
    public List<Especialidade> listarPorTipoId(Long tipoId) {
        return especialidadeRepository.findByTipoProfissional_IdAndStatus(tipoId, (byte) 1);
    }
    
    public List<Especialidade> listarMedicas() {
        return listarPorTipo("MEDICO");
    }
    
    public List<Especialidade> listarOdontologicas() {
        return listarPorTipo("DENTISTA");
    }
    
    public List<TipoProfissional> listarTiposProfissional() {
        return tipoProfissionalRepository.findByStatusOrderByNomeAsc((byte) 1);
    }
    
    public List<TipoProfissional> listarTodosTiposProfissional() {
        return tipoProfissionalRepository.findAll();
    }
    
    public Optional<TipoProfissional> buscarTipoPorCodigo(String codigo) {
        return tipoProfissionalRepository.findByCodigo(codigo.toUpperCase());
    }
}
