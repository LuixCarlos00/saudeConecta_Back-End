package br.com.saudeConecta.endpoinst.medico.Service;

import br.com.saudeConecta.endpoinst.medico.DTO.DadosMedicoView;
import br.com.saudeConecta.endpoinst.medico.Entity.Medico;
import br.com.saudeConecta.endpoinst.medico.Repository.MedicoRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class MedicoService {

    @Autowired
    private MedicoRepository repository;


    public Optional<Medico> buscarMedicoPorId(Long id) {
        return repository.findById(id);
    }


    public void cadastrar(@NotNull Medico medico) {


        repository.save(medico);

    }

    public Optional<Medico> BuscarPorId(Long id) {
        return repository.findById(id);
    }


    public Page<DadosMedicoView> BuscarPorPaginas(Pageable paginacao) {
        return repository.findAll(paginacao).map(DadosMedicoView::new);
    }


    @Transactional
    public void deletarPorId(Long id) throws Exception {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID inválido");
        }

        if (!repository.existsById(id)) {
            throw new Exception("Registro não encontrado");
        }

        try {
            repository.deleteById(id);
        } catch (Exception e) {
            throw new Exception("Violação de Integridade", e);
        }
    }


    public List<Medico> buscarTodosMedicos() {
        return repository.findAll();

    }


    public void CadastraRegistroMedico(Medico medico) {

        repository.save(medico);
    }


    public Optional<Medico> buscarMedicoPorEmail(String email) {
        return repository.findByMedEmail(email);
    }


    public List<Medico> buscarMedicoPorCRM(String crm) {
        return repository.findByMedCrmContainingIgnoreCase(crm);
    }

    public List<Medico> buscarMedicoPorCidade(String cidade) {
        return repository.findByEndereco_EndMunicipioContainingIgnoreCase(cidade);
    }

    public List<Medico> buscarMedicoPorEspecialidade(String especialidade) {
        return repository.findByMedEspecialidadeContainingIgnoreCase(especialidade);
    }


    public List<Medico> buscarMedicoPorNome(String nome) {
        return repository.findByMedNomeContainingIgnoreCase(nome);
    }

}