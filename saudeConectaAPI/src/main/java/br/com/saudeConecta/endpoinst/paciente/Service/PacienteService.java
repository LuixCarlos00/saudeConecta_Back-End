package br.com.saudeConecta.endpoinst.paciente.Service;

import br.com.saudeConecta.email.EnviarEmail.EnviarEmail;
import br.com.saudeConecta.endpoinst.codigoVerificacao.Repository.CodigoVerificacaoRepository;
import br.com.saudeConecta.endpoinst.paciente.DTO.DadosPacienteView;
import br.com.saudeConecta.endpoinst.paciente.Entity.Paciente;
import br.com.saudeConecta.endpoinst.paciente.Repository.PacienteRepository;
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
public class PacienteService {

    @Autowired
    private PacienteRepository repository;

    @Autowired
    private CodigoVerificacaoRepository codigoVerificacaoRepository;

    @Autowired
    private EnviarEmail enviarEmail;

    @Autowired
    private RecuperaSenha recuperaSenha;



    public Optional<Paciente> buscarPacientePorId(Long id) {
        return repository.findById(id);
    }



    public Page<DadosPacienteView> BuscarPorPaginas(Pageable paginacao) {
        return repository.findAll(paginacao).map(DadosPacienteView::new);
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


    public List<Paciente> buscarTodosPaciente() {
        return repository.findAll();

    }


    public void CadastraRegistroPaciente(Paciente paciente) throws ResourceNotFoundException {
        repository.save(paciente);
    }


    public Optional<Paciente> buscarPacsientePorEmail(String email) throws MessagingException {

        Optional<Paciente> paciente = repository.findByPaciEmail(email);


            enviarEmail.enviarEmailDestinatarioPacienteVerificacaoDuasEtapas(paciente, recuperaSenha.gerarCodigoVerificacaoTabelaUsuarios());
        return paciente ;
    }



    public boolean VerificarCodigoValido(String codigo) {
        return codigoVerificacaoRepository.existsByCodVerificacaoCodigo(codigo);

    }


    public void deletraCodigoVerificacao(String codigo) {

          codigoVerificacaoRepository.deleteByCodVerificacaoCodigo(codigo);
    }

 


    public List<Paciente> buscarPacientePorCPF(String cpf) {
        return repository.findByCpfIgnoringFormatting( cpf);
    }

    public List<Paciente> buscarPacientePorRG(String rg) {
        return repository.findByRgIgnoringFormatting(rg);
    }

    public List<Paciente> buscarPacientePorTelefone(String telefone) {
        return repository.findByPaciTelefoneContainingIgnoreCase(telefone);
    }


    public List<Paciente> buscarPacientePorNome(String nome) {
        return repository.findByPaciNomeContainingIgnoreCase(nome);
    }

}