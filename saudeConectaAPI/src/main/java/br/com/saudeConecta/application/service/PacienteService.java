package br.com.saudeConecta.application.service;

import br.com.saudeConecta.application.port.in.paciente.PacienteInputPort;
import br.com.saudeConecta.application.port.out.paciente.PacienteOutputPort;
import br.com.saudeConecta.domain.paciente.Paciente;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PacienteService implements PacienteInputPort {

    private final PacienteOutputPort pacienteOutputPort;

    @Override
    public Optional<Paciente> buscarPorId(Long id) {
        log.debug("Buscando paciente por ID: {}", id);
        return pacienteOutputPort.findById(id);
    }

    @Override
    public Optional<Paciente> buscarPorEmail(String email) {
        log.debug("Buscando paciente por email: {}", email);
        return pacienteOutputPort.findByPaciEmail(email);
    }

    @Override
    public List<Paciente> buscarTodos() {
        log.debug("Buscando todos os pacientes");
        return pacienteOutputPort.findAll();
    }

    @Override
    public Page<Paciente> buscarTodos(Pageable pageable) {
        log.debug("Buscando todos os pacientes com paginação");
        return pacienteOutputPort.findAll(pageable);
    }

    @Override
    public List<Paciente> buscarPorCpf(String cpf) {
        log.debug("Buscando pacientes por CPF: {}", cpf);
        return pacienteOutputPort.findByPaciCpfContainingIgnoreCase(cpf);
    }

    @Override
    public List<Paciente> buscarPorRg(String rg) {
        log.debug("Buscando pacientes por RG: {}", rg);
        return pacienteOutputPort.findByPaciRgContainingIgnoreCase(rg);
    }

    @Override
    public List<Paciente> buscarPorTelefone(String telefone) {
        log.debug("Buscando pacientes por telefone: {}", telefone);
        return pacienteOutputPort.findByPaciTelefoneContainingIgnoreCase(telefone);
    }

    @Override
    public List<Paciente> buscarPorNome(String nome) {
        log.debug("Buscando pacientes por nome: {}", nome);
        return pacienteOutputPort.findByPaciNomeContainingIgnoreCase(nome);
    }

    @Override
    public Paciente cadastrar(Paciente paciente) {
        log.info("Cadastrando novo paciente: {}", paciente.getPaciNome());
        Paciente pacienteSalvo = pacienteOutputPort.save(paciente);
        log.info("Paciente cadastrado com sucesso. ID: {}", pacienteSalvo.getPaciCodigo());
        return pacienteSalvo;
    }

    @Override
    public void deletar(Long id) throws Exception {
        log.info("Iniciando exclusão do paciente ID: {}", id);

        if (id == null || id <= 0) {
            log.warn("Tentativa de exclusão com ID inválido: {}", id);
            throw new IllegalArgumentException("ID inválido");
        }

        if (!pacienteOutputPort.existsById(id)) {
            log.warn("Paciente não encontrado para exclusão ID: {}", id);
            throw new Exception("Registro não encontrado");
        }

        try {
            pacienteOutputPort.deleteById(id);
            log.info("Paciente ID: {} excluído com sucesso", id);
        } catch (Exception e) {
            log.error("Erro ao excluir paciente ID: {}", id, e);
            throw new Exception("Violação de Integridade", e);
        }
    }
}
