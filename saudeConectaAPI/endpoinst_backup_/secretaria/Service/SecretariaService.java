package br.com.saudeConecta.endpoinst.secretaria.Service;

import br.com.saudeConecta.domain.secretaria.Secretaria;
import br.com.saudeConecta.email.EnviarEmail.EnviarEmail;
import br.com.saudeConecta.endpoinst.secretaria.DTO.DadosSecretariaView;
import br.com.saudeConecta.infra.exceptions.ResourceNotFoundException;
import br.com.saudeConecta.infrastructure.persistence.repository.*;
import br.com.saudeConecta.util.RecuperaSenha;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SecretariaService {

    @Autowired
    private AdministradorRepository administradorRepository;

    @Autowired
    private SecretariaRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PacienteRepository pacienteRepository;
    @Autowired
    private MedicoRepository medicoRepository;

    @Autowired
    private CodigoVerificacaoRepository codigoVerificacaoRepository;

    @Autowired
    private EnviarEmail enviarEmail;

    @Autowired
    private RecuperaSenha recuperaSenha;
    @Autowired
    private UsuarioRepository usuarioRepository;


    public Optional<Secretaria> buscarPacientePorId(Long id) {
        return repository.findById(id);
    }


    public Page<DadosSecretariaView> BuscarPorPaginas(Pageable paginacao) {
        return repository.findAll(paginacao).map(DadosSecretariaView::new);
    }


    @Transactional
    public void deletarPorId(Long id) throws ResourceNotFoundException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID invalido");
        }

        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Registro nao encontrado");
        }

        try {
            repository.deleteById(id);
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException("Violacao de Integridade");
        }
    }


    public List<Secretaria> buscarTodosPaciente() {
        return repository.findAll();

    }


    public void CadastraRegistroPaciente(Secretaria paciente) throws ResourceNotFoundException {
        repository.save(paciente);
    }



 
}