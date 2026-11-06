package br.com.saudeConecta.application.especialidade;

import br.com.saudeConecta.domain.profissional.Especialidade;
import br.com.saudeConecta.infrastructure.persistence.repository.EspecialidadeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EspecialidadeService {

    private final EspecialidadeRepository especialidadeRepository;

    public List<Especialidade> listarTodas() {
        return especialidadeRepository.findAll();
    }

    @Cacheable(value = "especialidades", key = "'todas-ativas'")
    public List<Especialidade> carregarEspecialidades() {
        return especialidadeRepository.findAllAtivasWithTipoProfissional();
    }

}
