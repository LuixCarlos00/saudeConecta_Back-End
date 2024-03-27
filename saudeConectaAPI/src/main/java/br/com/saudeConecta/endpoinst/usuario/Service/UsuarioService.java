package br.com.saudeConecta.endpoinst.usuario.Service;

import br.com.saudeConecta.endpoinst.usuario.DTO.DadosUsuarioView;
import br.com.saudeConecta.endpoinst.usuario.Entity.Usuario;

import br.com.saudeConecta.endpoinst.usuario.Repository.UsuarioRepository;
import br.com.saudeConecta.infra.exceptions.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository repository;


    public Optional<Usuario> buscarPacientePorId(Long id) {
        return repository.findById(id);
    }



    public Page<DadosUsuarioView> BuscarPorPaginas(Pageable paginacao) {
        return repository.findAll(paginacao).map(DadosUsuarioView::new);
    }


    @Transactional
    public void deletarPorId(Long id) throws ResourceNotFoundException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID inválido");
        }

        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Registro não encontrado");
        }

        try {
            repository.deleteById(id);
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException("Violação de Integridade");
        }
    }


    public List<Usuario> buscarTodosMedicos() {
        return repository.findAll();

    }


    public void CadastraRegistroMedico(Usuario usuario) throws ResourceNotFoundException {
         repository.save(usuario);
    }


}