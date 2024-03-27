package br.com.saudeConecta.endpoinst.endereco.Service;

import br.com.saudeConecta.endpoinst.endereco.DTO.DadosEnderecoView;
import br.com.saudeConecta.endpoinst.endereco.Entity.Endereco;
import br.com.saudeConecta.endpoinst.endereco.Repository.EnderecoRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EnderecoService {


    @Autowired
    private EnderecoRepository repository;


    public Optional<Endereco> buscarMedicoPorId(Long id) {
        return repository.findById(id);
    }



    public void cadastrar(@NotNull Endereco medico)   {



        repository.save(medico);

    }

    public Optional<Endereco> BuscarPorId(Long id)  {
        return repository.findById(id);
    }

    public Page<DadosEnderecoView> BuscarPorPaginas(Pageable paginacao) {
        return repository.findAll(paginacao).map(DadosEnderecoView::new);
    }



    @Transactional
    public void deletarPorId(Long id)  throws  Exception {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID inválido");
        }

        if (!repository.existsById(id)) {
            throw new Exception("Registro não encontrado");
        }

        try {
            repository.deleteById(id);
        } catch (Exception e) {
            throw new Exception("Violação de Integridade",e);
        }
    }



    public List<Endereco> buscarTodosEndereco() {
        return repository.findAll();

    }


    public void CadastraRegistroEndereco(Endereco medico) {

        repository.save(medico);
    }



}
