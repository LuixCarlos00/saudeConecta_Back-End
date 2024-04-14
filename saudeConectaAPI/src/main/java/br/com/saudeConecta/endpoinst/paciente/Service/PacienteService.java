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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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


    public Optional<Paciente> BuscarPorId(Long id) {
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


    public void CadastraRegistroPaciente(Paciente medico) throws ResourceNotFoundException {
        repository.save(medico);
    }


    public ResponseEntity<Object> buscarPacientePorEmail(String email) throws MessagingException {

        Optional<Paciente> paciente = repository.findByPaciEmail(email);

        if (!paciente.isPresent()) {
            return ResponseEntity.notFound().build();
        } else


            enviarEmail.enviarEmailDestinatarioPaciente(paciente, recuperaSenha.gerarCodigoVerificacaoTabelaUsuarios());


        return ResponseEntity.status(HttpStatus.OK).build();
    }

    public boolean VerificarCodigoValido(String codigo) {
    return codigoVerificacaoRepository.existsByCodVerificacaoCodigo(codigo);

    }
}