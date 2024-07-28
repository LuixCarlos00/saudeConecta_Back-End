package br.com.saudeConecta.endpoinst.secretaria.Service;

import br.com.saudeConecta.email.EnviarEmail.EnviarEmail;
import br.com.saudeConecta.endpoinst.administrador.Repository.AdministradorRepository;
import br.com.saudeConecta.endpoinst.codigoVerificacao.Repository.CodigoVerificacaoRepository;
import br.com.saudeConecta.endpoinst.medico.Repository.MedicoRepository;
import br.com.saudeConecta.endpoinst.paciente.Repository.PacienteRepository;
import br.com.saudeConecta.endpoinst.secretaria.DTO.DadosSecretariaView;
import br.com.saudeConecta.endpoinst.secretaria.Entity.Secretaria;
import br.com.saudeConecta.endpoinst.secretaria.Repository.SecretariaRepository;
import br.com.saudeConecta.endpoinst.usuario.Entity.Usuario;
import br.com.saudeConecta.endpoinst.usuario.Repository.UsuarioRepository;
import br.com.saudeConecta.infra.exceptions.ResourceNotFoundException;
import br.com.saudeConecta.util.RecuperaSenha;
import jakarta.persistence.EntityNotFoundException;
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


    public List<Secretaria> buscarTodosPaciente() {
        return repository.findAll();

    }


    public void CadastraRegistroPaciente(Secretaria paciente) throws ResourceNotFoundException {
        repository.save(paciente);
    }




//
//
//    public boolean VerificarCodigoValido(String codigo) {
//        return codigoVerificacaoRepository.existsByCodVerificacaoCodigo(codigo);
//
//    }
//
//
//    public void deletraCodigoVerificacao(String codigo) {
//
//        codigoVerificacaoRepository.deleteByCodVerificacaoCodigo(codigo);
//    }
//
//    public Optional<Secretaria> buscarPacientePorIdDeUsusario(Long id) {
//        return repository.findByAdmUsuario_Id(id);
//    }
//
//

//
//
//    public void BuscaPorSenhaAntiga(String SenhaNova, Long id) {
//        Secretaria adm = repository.getReferenceById(id);
//        usuarioRepository.getReferenceById(adm.getSecreUsuario().getId()).setSenha(SenhaNova);
//    }
//
//    public boolean EsqueciMinhaSenha(String SenhaNova, String SenhaAntiga, Long id, String email) {
//        try {
//            Optional<Secretaria> adm = repository.findById(id);
//            if (!adm.isPresent()) {
//                return false;
//            }
//
//            Long codigoUser = adm.get().getSecreUsuario().getId();
//            Optional<Usuario> usuario = usuarioRepository.findById(codigoUser);
//            if (!usuario.isPresent()) {
//                return false;
//            }
//
//            if (!adm.get().getSecreEmail().equals(email)) {
//                return false;
//            }
//
//            Usuario user = usuario.get();
//
//            if (passwordEncoder.matches(SenhaAntiga, user.getSenha())) {
//                user.setSenha(SenhaNova);
//                usuarioRepository.save(user);
//                return true;
//            } else {
//                return false;
//            }
//        } catch (EntityNotFoundException e) {
//            return false;
//        }
//    }


}