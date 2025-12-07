package br.com.saudeConecta.endpoinst.administrador.Service;

import br.com.saudeConecta.email.EnviarEmail.EnviarEmail;
import br.com.saudeConecta.endpoinst.administrador.DTO.DadosAdiministradorView;
import br.com.saudeConecta.endpoinst.administrador.Entity.Administrador;
import br.com.saudeConecta.endpoinst.administrador.Repository.AdministradorRepository;
import br.com.saudeConecta.endpoinst.codigoVerificacao.Repository.CodigoVerificacaoRepository;
import br.com.saudeConecta.endpoinst.medico.Entity.Medico;
import br.com.saudeConecta.endpoinst.medico.Repository.MedicoRepository;
import br.com.saudeConecta.endpoinst.paciente.Entity.Paciente;
import br.com.saudeConecta.endpoinst.paciente.Repository.PacienteRepository;
import br.com.saudeConecta.endpoinst.usuario.Repository.UsuarioRepository;
import br.com.saudeConecta.infra.exceptions.ResourceNotFoundException;
import br.com.saudeConecta.util.RecuperaSenha;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AdministradorService {

    @Autowired
    private AdministradorRepository repository;

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

    public Optional<Administrador> buscarAdministradorPorId(Long id) {
        return repository.findById(id);
    }








    public Page<DadosAdiministradorView> buscarPorPaginas(Pageable paginacao) {
        return repository.findAll(paginacao).map(DadosAdiministradorView::new);
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









    public List<Administrador> buscarTodosAdministradores() {
        return repository.findAll();

    }





    public void cadastrarAdministrador(Administrador administrador) throws ResourceNotFoundException {
        repository.save(administrador);
    }







    public Optional<Object> buscarPorEmailEnviarCodigo(String email) throws MessagingException {

        Optional<Paciente> paciente = pacienteRepository.findByPaciEmail(email);

        Optional<Medico> medico = medicoRepository.findByMedEmail(email);

        Optional<Administrador> adm = repository.findByAdmEmail(email);

        if (paciente.isPresent()) {
            enviarEmail.enviarEmailDestinatarioPacienteVerificacaoDuasEtapas(paciente, recuperaSenha.gerarCodigoVerificacaoTabelaUsuarios());
            return Optional.of(paciente);
        }
        if (medico.isPresent()) {
            enviarEmail.enviarEmailDestinatarioMedicoVerificacaoDuasEtapas(medico, recuperaSenha.gerarCodigoVerificacaoTabelaUsuarios());
            return Optional.of(medico);
        }
        if (adm.isPresent()) {
            enviarEmail.enviarEmailDestinatarioAdministradorVerificacaoDuasEtapas(adm, recuperaSenha.gerarCodigoVerificacaoTabelaUsuarios());
            return Optional.of(adm);
        }
        return Optional.of("Email invalido");
    }






    public boolean verificarCodigoValido(String codigo) {
        return codigoVerificacaoRepository.existsByCodVerificacaoCodigo(codigo);

    }






    public void deletarCodigoVerificacao(String codigo) {
        codigoVerificacaoRepository.deleteByCodVerificacaoCodigo(codigo);
    }




    public Optional<Administrador> buscarAdministradorPorIdUsuario(Long id) {
        return repository.findByAdmUsuario_Id(id);
    }







}