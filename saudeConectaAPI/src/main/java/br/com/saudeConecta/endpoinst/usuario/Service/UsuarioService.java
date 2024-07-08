package br.com.saudeConecta.endpoinst.usuario.Service;

import br.com.saudeConecta.endpoinst.administrador.Entity.Administrador;
import br.com.saudeConecta.endpoinst.administrador.Repository.AdministradorRepository;
import br.com.saudeConecta.endpoinst.medico.Entity.Medico;
import br.com.saudeConecta.endpoinst.medico.Repository.MedicoRepository;
import br.com.saudeConecta.endpoinst.paciente.Entity.Paciente;
import br.com.saudeConecta.endpoinst.paciente.Repository.PacienteRepository;
import br.com.saudeConecta.endpoinst.secretaria.Entity.BuscarTodosUsuarios;
import br.com.saudeConecta.endpoinst.secretaria.Entity.Secretaria;
import br.com.saudeConecta.endpoinst.secretaria.Repository.SecretariaRepository;
import br.com.saudeConecta.endpoinst.usuario.Entity.Usuario;

import br.com.saudeConecta.endpoinst.usuario.Repository.UsuarioRepository;
import br.com.saudeConecta.infra.exceptions.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
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


    @Autowired
    private MedicoRepository medicoRepository;
    @Autowired
    private PacienteRepository pacienteRepository;
    @Autowired
    private AdministradorRepository administradorRepository;

    @Autowired
    private SecretariaRepository secretariaRepository;


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
        Usuario usuario = repository.getReferenceById(id);


        if (usuario != null) {

            usuario.update(dados);
            repository.save(usuario);
        } else ResponseEntity.notFound().build();

    }

    public Usuario RecuperaLogin(Long id) {
        return repository.getReferenceById(id);
    }

    public Boolean existeLogin(String login) {
        return repository.existsByLogin(login);
    }


    public boolean buscarPorloginSeExiste(String login) {
        Usuario user = repository.findByLogin(login);
        if (user == null) {
            return false;
        }
        //boolean extistePaciente = pacienteRepository.existsByUsuario_Id(user.getId());
        boolean extisteMedico = medicoRepository.existsByUsuario_Id(user.getId());
        boolean extisteAdministrador = administradorRepository.existsByAdmUsuario_Id(user.getId());
        boolean extisteSecretaria = secretariaRepository.existsBySecreUsuario_Login(user.getLogin());
        //  if (extistePaciente){
        //      return true;
        // }
        if (extisteSecretaria) {
            return true;
        }
        if (extisteMedico) {
            return true;
        }
        if (extisteAdministrador) {
            return true;
        }

        return false;
    }

    public BuscarTodosUsuarios BuscarTodosUsuarios() {
        List<Paciente> listaPacientes = pacienteRepository.findAll();
        List<Medico> listaMedicos = medicoRepository.findAll();
        List<Secretaria> listaSecretarias = secretariaRepository.findAll();
        List<Administrador> listaAdministradores = administradorRepository.findAll();

        BuscarTodosUsuarios buscarTodosUsuarios = new BuscarTodosUsuarios();
        buscarTodosUsuarios.setPaciente(listaPacientes);
        buscarTodosUsuarios.setMedico(listaMedicos);
        buscarTodosUsuarios.setSecretaria(listaSecretarias);
        buscarTodosUsuarios.setAdministrador(listaAdministradores);

        return buscarTodosUsuarios;
    }
}