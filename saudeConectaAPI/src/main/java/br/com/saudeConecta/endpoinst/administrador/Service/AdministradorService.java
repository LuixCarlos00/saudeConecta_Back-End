package br.com.saudeConecta.endpoinst.administrador.Service;

import br.com.saudeConecta.email.EnviarEmail.EnviarEmail;
import br.com.saudeConecta.endpoinst.administrador.DTO.DadosAdiministradorView;
import br.com.saudeConecta.endpoinst.administrador.Entity.Administrador;

import br.com.saudeConecta.endpoinst.administrador.Repository.AdministradorRepository;
import br.com.saudeConecta.endpoinst.codigoVerificacao.Repository.CodigoVerificacaoRepository;
import br.com.saudeConecta.infra.exceptions.ResourceNotFoundException;
import br.com.saudeConecta.util.RecuperaSenha;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AdministradorService {


    private AdministradorRepository repository;

    @Autowired
    private CodigoVerificacaoRepository codigoVerificacaoRepository;

    @Autowired
    private EnviarEmail enviarEmail;

    @Autowired
    private RecuperaSenha recuperaSenha;



    public Optional<Administrador> buscarPacientePorId(Long id) {
        return repository.findById(id);
    }



    public Page<DadosAdiministradorView> BuscarPorPaginas(Pageable paginacao) {
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


    public List<Administrador> buscarTodosPaciente() {
        return repository.findAll();

    }


    public void CadastraRegistroPaciente(Administrador paciente) throws ResourceNotFoundException {
        repository.save(paciente);
    }


    public Optional<Administrador> buscarPacsientePorEmail(String email) throws MessagingException {

        Optional<Administrador> paciente = repository.findByAdmEmail(email);


            enviarEmail.enviarEmailDestinatarioAdministrador(paciente, recuperaSenha.gerarCodigoVerificacaoTabelaUsuarios());


        return paciente ;
    }



    public boolean VerificarCodigoValido(String codigo) {
        return codigoVerificacaoRepository.existsByCodVerificacaoCodigo(codigo);

    }


    public void deletraCodigoVerificacao(String codigo) {

          codigoVerificacaoRepository.deleteByCodVerificacaoCodigo(codigo);
    }

    public Optional<Administrador> buscarPacientePorIdDeUsusario(Long id) {
        return repository.findByAdmUsuario_Id(id);
    }

    public boolean BuscarCodigodeAutorização(String codigo) {


            return repository.existsByAdmCodigoAtorizacao(codigo);

    }
}