package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.profissional.Especialidade;
import br.com.saudeConecta.domain.profissional.TipoProfissional;
import br.com.saudeConecta.infrastructure.persistence.repository.EspecialidadeRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.TipoProfissionalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
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

    @Cacheable(value = "especialidades", key = "'todas-ativas'")
    public List<Especialidade> carregarEspecialidades() {
        return especialidadeRepository.findAllAtivasWithTipoProfissional();
    }

    public Optional<Especialidade> buscarPorId(Long id) {
        return especialidadeRepository.findById(id);
    }

    @Cacheable(value = "especialidades", key = "'tipo-' + #tipoCodigo.toUpperCase()")
    public List<Especialidade> listarPorTipo(String tipoCodigo) {
        return especialidadeRepository.findAtivasByTipoProfissionalCodigo(tipoCodigo.toUpperCase());
    }

    @Cacheable(value = "especialidades", key = "'tipo-id-' + #tipoId")
    public List<Especialidade> listarPorTipoId(Long tipoId) {
        return especialidadeRepository.findByTipoProfissional_IdAndStatus(tipoId, (byte) 1);
    }

    public List<Especialidade> listarMedicas() {
        return listarPorTipo("MEDICO");
    }

    public List<Especialidade> listarOdontologicas() {
        return listarPorTipo("DENTISTA");
    }

    @Cacheable(value = "especialidades", key = "'tipos-profissional'")
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
