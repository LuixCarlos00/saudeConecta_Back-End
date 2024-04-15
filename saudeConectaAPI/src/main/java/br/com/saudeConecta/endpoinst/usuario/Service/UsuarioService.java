package br.com.saudeConecta.endpoinst.usuario.Service;

import br.com.saudeConecta.endpoinst.endereco.Entity.Endereco;
import br.com.saudeConecta.endpoinst.paciente.DTO.DadosCadastraPaciente;
import br.com.saudeConecta.endpoinst.paciente.Entity.Paciente;
import br.com.saudeConecta.endpoinst.usuario.DTO.DadosLoginUsuario;
import br.com.saudeConecta.endpoinst.usuario.DTO.DadosUsuarioView;
import br.com.saudeConecta.endpoinst.usuario.Entity.Usuario;

import br.com.saudeConecta.endpoinst.usuario.Repository.UsuarioRepository;
import br.com.saudeConecta.infra.exceptions.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository repository;


    public Optional<Usuario> buscarUserPorId(Long id) {
        return repository.findById(id);
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


    public void CadastraUsuario(Usuario dados) throws ResourceNotFoundException {
         repository.save(dados);
    }


    public void UpdateDeSenha(Usuario dados, Long id) {
     Usuario usuario =  repository.getReferenceById(id);


        if (usuario!=null){

            usuario.update(dados);
            repository.save(usuario);
        }else   ResponseEntity.notFound().build();

    }
}