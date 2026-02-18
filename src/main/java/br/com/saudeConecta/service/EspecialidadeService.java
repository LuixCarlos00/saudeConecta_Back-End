package br.com.saudeConecta.service;

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
    
    public List<Especialidade> carregarEspecialidades() {
        return especialidadeRepository.findAllAtivasWithTipoProfissional();
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
    
    public Especialidade criar(Long tipoProfissionalId, String nome, String codigo) {
        TipoProfissional tipoProfissional = tipoProfissionalRepository.findById(tipoProfissionalId)
            .orElseThrow(() -> new IllegalArgumentException("Tipo profissional não encontrado"));
        
        if (especialidadeRepository.existsByTipoProfissional_IdAndNome(tipoProfissionalId, nome)) {
            throw new IllegalArgumentException("Já existe uma especialidade com este nome para este tipo profissional");
        }
        
        Especialidade especialidade = Especialidade.builder()
            .tipoProfissional(tipoProfissional)
            .nome(nome)
            .codigo(codigo)
            .status((byte) 1)
            .build();
        
        return especialidadeRepository.save(especialidade);
    }
    
    public Especialidade atualizar(Long id, String nome, String codigo, Byte status) {
        Especialidade especialidade = especialidadeRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Especialidade não encontrada"));
        
        Optional<Especialidade> especialidadeExistente = especialidadeRepository
            .findByTipoProfissional_IdAndNome(especialidade.getTipoProfissional().getId(), nome);
        
        if (especialidadeExistente.isPresent() && !especialidadeExistente.get().getId().equals(id)) {
            throw new IllegalArgumentException("Já existe uma especialidade com este nome para este tipo profissional");
        }
        
        especialidade.setNome(nome);
        especialidade.setCodigo(codigo);
        if (status != null) {
            especialidade.setStatus(status);
        }
        
        return especialidadeRepository.save(especialidade);
    }
    
    public void deletar(Long id) {
        Especialidade especialidade = especialidadeRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Especialidade não encontrada"));
        
        especialidade.setStatus((byte) 0);
        especialidadeRepository.save(especialidade);
    }
    
    public void deletarPermanente(Long id) {
        if (!especialidadeRepository.existsById(id)) {
            throw new IllegalArgumentException("Especialidade não encontrada");
        }
        especialidadeRepository.deleteById(id);
    }
}
