package br.com.saudeConecta.application.service;

import br.com.saudeConecta.application.port.in.paciente.PacienteInputPort;
import br.com.saudeConecta.application.port.out.paciente.PacienteOutputPort;
import br.com.saudeConecta.domain.endereco.Endereco;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.infrastructure.persistence.repository.EnderecoRepository;
import br.com.saudeConecta.presentation.dto.paciente.CadastrarPacienteCompletoRequest;
import br.com.saudeConecta.presentation.dto.paciente.CadastrarPacienteRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PacienteService implements PacienteInputPort {

    private final PacienteOutputPort pacienteOutputPort;
    private final EnderecoRepository enderecoRepository;

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

    /**
     * Cadastra paciente com validação de endereço.
     * @param dados DTO com dados do paciente
     * @return Paciente cadastrado
     * @throws IllegalArgumentException se endereço não for encontrado
     */
    public Paciente cadastrarComEndereco(CadastrarPacienteRequest dados) {
        log.info("Cadastrando paciente com endereço: {}", dados.paciNome());
        
        var enderecoOptional = enderecoRepository.findById(dados.endereco());
        if (enderecoOptional.isEmpty()) {
            log.warn("Endereço não encontrado para cadastro de paciente: {}", dados.endereco());
            throw new IllegalArgumentException("Endereço não encontrado");
        }

        Endereco endereco = enderecoOptional.get();
        Paciente paciente = new Paciente(dados, endereco);
        
        return cadastrar(paciente);
    }

    /**
     * Bloqueia ou desbloqueia paciente.
     * @param id ID do paciente
     * @param status 0 para inativo, 1 para ativo
     * @throws IllegalArgumentException se paciente não for encontrado
     */
    public void bloquear(Long id, int status) {
        log.info("Alterando status do paciente ID: {} para {}", id, status == 0 ? "INATIVO" : "ATIVO");
        
        var pacienteOpt = buscarPorId(id);
        if (pacienteOpt.isEmpty()) {
            log.warn("Paciente não encontrado para alteração de status: {}", id);
            throw new IllegalArgumentException("Paciente não encontrado");
        }
        
        var paciente = pacienteOpt.get();
        paciente.setPaciStatus(status == 0 ? "INATIVO" : "ATIVO");
        cadastrar(paciente);
        log.info("Status do paciente ID: {} alterado com sucesso", id);
    }

    /**
     * Cadastra paciente completo com endereço em uma única operação.
     * Paciente não faz login no sistema, apenas seus dados são cadastrados.
     */
    public Paciente cadastrarCompleto(CadastrarPacienteCompletoRequest dados) {
        log.info("Cadastrando paciente completo: {}", dados.paciNome());
        
        // Criar e salvar endereço
        Endereco endereco = new Endereco();
        endereco.setEndNacionalidade(dados.endNacionalidade());
        endereco.setEndUF(dados.endUF());
        endereco.setEndMunicipio(dados.endMunicipio());
        endereco.setEndBairro(dados.endBairro());
        endereco.setEndCep(dados.endCep());
        endereco.setEndRua(dados.endRua());
        endereco.setEndNumero(dados.endNumero() != null ? dados.endNumero().longValue() : null);
        endereco.setEndComplemento(dados.endComplemento() != null ? dados.endComplemento() : "");
        Endereco enderecoSalvo = enderecoRepository.save(endereco);
        log.info("Endereço criado para paciente. ID: {}", enderecoSalvo.getEndCodigo());
        
        // Criar e salvar paciente
        Paciente paciente = new Paciente();
        paciente.setPaciNome(dados.paciNome());
        paciente.setPaciSexo(dados.paciSexo());
        paciente.setPaciDataNacimento(dados.paciDataNacimento() != null ? Date.valueOf(dados.paciDataNacimento()) : null);
        paciente.setPaciCpf(dados.paciCpf());
        paciente.setPaciRg(dados.paciRg());
        paciente.setPaciEmail(dados.paciEmail());
        paciente.setPaciTelefone(dados.paciTelefone());
        paciente.setEndereco(enderecoSalvo);
        paciente.setPaciStatus("ATIVO");
        
        Paciente pacienteSalvo = pacienteOutputPort.save(paciente);
        log.info("Paciente cadastrado com sucesso. ID: {}", pacienteSalvo.getPaciCodigo());
        
        return pacienteSalvo;
    }
}
