package br.com.saudeConecta.endpoinst.usuario.Service;

import br.com.saudeConecta.domain.administrador.Administrador;
import br.com.saudeConecta.domain.medico.Medico;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.domain.secretaria.Secretaria;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.endpoinst.usuario.DTO.DadosTodosUsuariosView;
import br.com.saudeConecta.endpoinst.usuario.DTO.DadosTrocaDeSenha;
import br.com.saudeConecta.infra.exceptions.ResourceNotFoundException;
import br.com.saudeConecta.infrastructure.persistence.repository.*;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository repository;


    public Optional<Usuario> buscarUsuarioPorId(Long id) {
        log.debug("Buscando usuario por ID: {}", id);
        return repository.findById(id);
    }
    @Autowired
    private PasswordEncoder passwordEncoder;

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
        log.info("Iniciando exclusao de usuario ID: {}", id);
        if (id == null || id <= 0) {
            log.warn("Tentativa de exclusao com ID invalido: {}", id);
            throw new IllegalArgumentException("ID invalido");
        }

        Optional<Paciente> paciente = pacienteRepository.findById(id);
        if (paciente.isPresent()) {
            pacienteRepository.deleteById(paciente.orElseThrow().getPaciCodigo());
        } else if (!repository.existsById(id)) {
            log.warn("Usuario nao encontrado para exclusao ID: {}", id);
            throw new ResourceNotFoundException("Registro nao encontrado");
        }

        try {
            Optional<Administrador> adm = administradorRepository.findByAdmUsuario_Id(id);
            if (adm.isPresent()) {
                administradorRepository.deleteById(adm.get().getAdmCodigo());
            }
            Optional<Medico> medico = medicoRepository.findByUsuario_Id(id);
            if (medico.isPresent()) {
                medicoRepository.deleteById(medico.get().getMedCodigo());
            }

            Optional<Secretaria> secretaria = secretariaRepository.findBySecreUsuario_Id(id);
            if (secretaria.isPresent()) {
                secretariaRepository.deleteById(secretaria.orElseThrow().getSecreCodigo());
            }


            repository.deleteById(id);
            log.info("Usuario ID: {} excluido com sucesso", id);
        } catch (ResourceNotFoundException e) {
            log.error("Erro ao excluir usuario ID: {} - Violacao de integridade", id);
            throw new ResourceNotFoundException("Violacao de Integridade");
        }
    }


    public List<Usuario> buscarTodosUsuarios() {
        return repository.findAll();

    }


    public void cadastrarUsuario(Usuario dados) throws ResourceNotFoundException {
        log.info("Cadastrando novo usuario: {}", dados.getLogin());
        repository.save(dados);
        log.info("Usuario cadastrado com sucesso: {}", dados.getLogin());
    }


    public void atualizarSenha(Usuario dados, Long id) {
        Usuario usuario = repository.getReferenceById(id);


        if (usuario != null) {

            usuario.update(dados);
            repository.save(usuario);
        } else ResponseEntity.notFound().build();

    }



    public ResponseEntity<?> trocarSenha(String senhaNova, DadosTrocaDeSenha dados) {
        log.info("Iniciando troca de senha para usuario ID: {}", dados.id());
        Optional<Usuario> usuario = repository.findById(dados.id());

        if (!usuario.isPresent()) {
            log.warn("Usuario nao encontrado para troca de senha ID: {}", dados.id());
            return ResponseEntity.notFound().build();
        }

        if (!passwordEncoder.matches(dados.senhaAntiga(), usuario.get().getSenha())) {
            log.warn("Senha antiga incorreta para usuario ID: {}", dados.id());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Senha antiga incorreta");
        }

        Optional<Secretaria> secretaria = secretariaRepository.findBySecreUsuario_Id(dados.id());
        Optional<Administrador> administrador = administradorRepository.findByAdmUsuario_Id(dados.id());
        Optional<Medico> medico = medicoRepository.findByUsuario_Id(dados.id());

        if (secretaria.isPresent() && secretaria.get().getSecreEmail().equals(dados.email())) {
            usuario.get().setSenha(senhaNova);
            repository.save(usuario.get());
            log.info("Senha do usuario ID: {} alterada com sucesso", dados.id());
            return ResponseEntity.ok().build();
        }

        if (administrador.isPresent() && administrador.get().getAdmEmail().equals(dados.email())) {
            usuario.get().setSenha(senhaNova);
            repository.save(usuario.get());
            log.info("Senha do usuario ID: {} alterada com sucesso", dados.id());
            return ResponseEntity.ok().build();
        }

        if (medico.isPresent() && medico.get().getMedEmail().equals(dados.email())) {
            usuario.get().setSenha(senhaNova);
            repository.save(usuario.get());
            log.info("Senha do usuario ID: {} alterada com sucesso", dados.id());
            return ResponseEntity.ok().build();
        }

        log.warn("Nao foi possivel alterar a senha do usuario ID: {}", dados.id());
        return ResponseEntity.notFound().build();
    }




    public void trocarSenhaDoUsuario(String senhaNova, Usuario user) {
        user.setSenha(senhaNova);
        repository.save(user);
    }



    public ResponseEntity<?> recuperarSenha(String senhaNova, DadosTrocaDeSenha dados) {
        Optional<Usuario> usuario = repository.findById(dados.id()) ;
        if (usuario != null) {
            usuario.get().setSenha(senhaNova);
            repository.save(usuario.get());
            log.info("Senha do usuario ID: {} recuperada com sucesso", dados.id());
            return ResponseEntity.ok().build();
        } else {
            log.warn("Usuario nao encontrado para recuperacao de senha ID: {}", dados.id());
            return ResponseEntity.notFound().build();
        }
    }
    
    
    

    public Usuario recuperarLogin(Long id) {
        return repository.getReferenceById(id);
    }



    public Boolean existeLogin(String login) {
        log.debug("Verificando existencia de login: {}", login);
        return repository.existsByLogin(login);
    }



    /**
     * Verifica se o login está disponível para uso.
     * @param login Login a ser verificado
     * @return true se o login está DISPONÍVEL (pode usar), false se já existe
     */
    public boolean verificarLoginExistente(String login) {
        Usuario user = repository.findUsuarioByLogin(login);
        
        // Se não encontrou usuário, login está disponível
        if (user == null) {
            return true;
        }
        
        // Verifica se existe em alguma entidade
        boolean existeMedico = medicoRepository.existsByUsuario_Id(user.getId());
        boolean existeAdministrador = administradorRepository.existsByAdmUsuario_Id(user.getId());
        boolean existeSecretaria = secretariaRepository.existsBySecreUsuario_Login(user.getLogin());
       
        // Se existe em qualquer entidade, login NÃO está disponível
        if (existeSecretaria || existeMedico || existeAdministrador) {
            return false;
        }

        // Login existe mas não está vinculado a nenhuma entidade - não disponível
        return false;
    }



    @Transactional
    public DadosTodosUsuariosView listarTodosUsuariosPorTipo() {
        List<Paciente> listaPacientes = pacienteRepository.findAll();
        List<Medico> listaMedicos = medicoRepository.findAll();
        List<Secretaria> listaSecretarias = secretariaRepository.findAll();
        List<Administrador> listaAdministradores = administradorRepository.findAll();

        DadosTodosUsuariosView buscarTodosUsuarios = new DadosTodosUsuariosView();
        buscarTodosUsuarios.setPaciente(listaPacientes);
        buscarTodosUsuarios.setMedico(listaMedicos);
        buscarTodosUsuarios.setSecretaria(listaSecretarias);
        buscarTodosUsuarios.setAdministrador(listaAdministradores);

        return buscarTodosUsuarios;
    }


    public void bloquearUsuario(@NotNull Usuario user, Byte status) {
        log.info("Alterando status do usuario ID: {} para: {}", user.getId(), status);
        user.setStatus(status);
        repository.save(user);
    }


    public void bloquearPaciente(@NotNull Paciente user, String status) {
        user.setPaciStatus(status);
        pacienteRepository.save(user);
    }


}