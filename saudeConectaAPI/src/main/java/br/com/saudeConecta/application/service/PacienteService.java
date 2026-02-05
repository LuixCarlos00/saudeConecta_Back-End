package br.com.saudeConecta.application.service;

import br.com.saudeConecta.application.port.in.paciente.PacienteInputPort;
import br.com.saudeConecta.application.port.out.paciente.PacienteOutputPort;
import br.com.saudeConecta.domain.endereco.Endereco;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.infra.tenant.RequiresTenant;
import br.com.saudeConecta.infra.tenant.TenantHelper;
import br.com.saudeConecta.infrastructure.persistence.repository.EnderecoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.OrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.PacienteRepository;
import br.com.saudeConecta.presentation.dto.paciente.AtualizarPacienteRequest;
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
    private final PacienteRepository pacienteRepository;
    private final EnderecoRepository enderecoRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final TenantHelper tenantHelper;

    // ========== MÉTODOS COM TENANT ==========

    @RequiresTenant
    public Paciente cadastrarPacientebyOrg(CadastrarPacienteCompletoRequest request) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.info("Cadastrando paciente: {} na organização: {}", request.paciNome(), orgId);

        String cpfLimpo = limparCpf(request.paciCpf());

        if (cpfLimpo != null && !cpfLimpo.isEmpty() && existeCpfNoTenant(cpfLimpo)) {
            throw new IllegalStateException("CPF já cadastrado no sistema");
        }

        Organizacao organizacao = organizacaoRepository.findById(orgId)
            .orElseThrow(() -> new IllegalStateException("Organização não encontrada"));

        Endereco endereco = new Endereco();
        endereco.setEndNacionalidade(request.endNacionalidade());
        endereco.setEndUF(request.endUF());
        endereco.setEndMunicipio(request.endMunicipio());
        endereco.setEndBairro(request.endBairro());
        endereco.setEndCep(request.endCep());
        endereco.setEndRua(request.endRua());
        endereco.setEndNumero(request.endNumero() != null ? request.endNumero().longValue() : null);
        endereco.setEndComplemento(request.endComplemento());



        Paciente paciente = new Paciente();
        paciente.setOrganizacao(organizacao);
        paciente.setPaciNome(request.paciNome());
        paciente.setPaciSexo(request.paciSexo());
        paciente.setPaciDataNacimento(request.paciDataNacimento() != null ? 
            Date.valueOf(request.paciDataNacimento()) : null);
        paciente.setPaciCpf(cpfLimpo);
        paciente.setPaciRg(request.paciRg());
        paciente.setPaciEmail(request.paciEmail());
        paciente.setPaciTelefone(request.paciTelefone());
        paciente.setEndereco(endereco);
        paciente.setPaciStatus("ATIVO");

        enderecoRepository.save(endereco);
        Paciente salvo = pacienteRepository.save(paciente);
        log.info("Paciente cadastrado com sucesso. ID: {}", salvo.getPaciCodigo());

        return salvo;
    }

    private String limparCpf(String cpf) {
        return cpf != null ? cpf.replaceAll("[^0-9]", "") : null;
    }

    @RequiresTenant
    public List<Paciente> buscarTodosPorTenant() {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.debug("Buscando pacientes da organização: {}", orgId);
        return pacienteRepository.findByOrganizacao_Id(orgId);
    }
    
    @RequiresTenant
    public Page<Paciente> buscarTodosPorTenant(Pageable pageable) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.debug("Buscando pacientes paginados da organização: {}", orgId);
        return pacienteRepository.findByOrganizacao_Id(orgId, pageable);
    }
    
    @RequiresTenant
    public Optional<Paciente> buscarPorIdTenant(Long id) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.debug("Buscando paciente ID: {} da organização: {}", id, orgId);
        return pacienteRepository.findByPaciCodigoAndOrganizacao_Id(id, orgId);
    }
    
    @RequiresTenant
    public List<Paciente> buscarPorNomeTenant(String nome) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.debug("Buscando pacientes por nome '{}' na organização: {}", nome, orgId);
        return pacienteRepository.findByOrganizacaoIdAndNomeContaining(orgId, nome);
    }
    
    @RequiresTenant
    public Long contarAtivosTenant() {
        Long orgId = tenantHelper.getCurrentTenantId();
        return pacienteRepository.countByOrganizacaoIdAndStatus(orgId, "ATIVO");
    }
    
    @RequiresTenant
    public boolean existeCpfNoTenant(String cpf) {
        Long orgId = tenantHelper.getCurrentTenantId();
        return pacienteRepository.existsByPaciCpfAndOrganizacao_Id(cpf, orgId);
    }

    // ========== MÉTODOS LEGADOS (sem tenant) ==========

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
        // Se houver tenant, filtra por tenant
        if (tenantHelper.hasTenant()) {
            return buscarTodosPorTenant();
        }
        return pacienteOutputPort.findAll();
    }

    @Override
    public Page<Paciente> buscarTodos(Pageable pageable) {
        log.debug("Buscando todos os pacientes com paginação");
        if (tenantHelper.hasTenant()) {
            return buscarTodosPorTenant(pageable);
        }
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
        if (tenantHelper.hasTenant()) {
            return buscarPorNomeTenant(nome);
        }
        return pacienteOutputPort.findByPaciNomeContainingIgnoreCase(nome);
    }

    @Override
    public Paciente cadastrar(Paciente paciente) {
        log.info("Cadastrando novo paciente: {}", paciente.getPaciNome());
        
        // Se houver tenant e paciente não tem organização, define automaticamente
        if (tenantHelper.hasTenant() && paciente.getOrganizacao() == null) {
            Long orgId = tenantHelper.getCurrentTenantId();
            Organizacao org = organizacaoRepository.findById(orgId)
                .orElseThrow(() -> new IllegalStateException("Organização não encontrada"));
            paciente.setOrganizacao(org);
        }
        
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

        var pacienteOpt = pacienteOutputPort.findById(id);
        if (pacienteOpt.isEmpty()) {
            log.warn("Paciente não encontrado para exclusão ID: {}", id);
            throw new Exception("Registro não encontrado");
        }

        Paciente paciente = pacienteOpt.get();
        Endereco endereco = paciente.getEndereco();

        try {
            // 1. Deletar o paciente primeiro (remove a FK)
            pacienteOutputPort.deleteById(id);
            log.info("Paciente ID: {} excluído com sucesso", id);

            // 2. Deletar o endereço associado
            if (endereco != null) {
                enderecoRepository.deleteById(endereco.getEndCodigo());
                log.info("Endereço ID: {} associado ao paciente excluído com sucesso", endereco.getEndCodigo());
            }
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

    /**
     * Atualiza os dados de um paciente existente.
     * @param id ID do paciente
     * @param dados DTO com dados atualizados
     * @return Paciente atualizado
     * @throws IllegalArgumentException se paciente não for encontrado
     */
    public Paciente atualizar(Long id, AtualizarPacienteRequest dados) {
        log.info("Atualizando paciente ID: {}", id);
        
        var pacienteOpt = buscarPorId(id);
        if (pacienteOpt.isEmpty()) {
            log.warn("Paciente não encontrado para atualização: {}", id);
            throw new IllegalArgumentException("Paciente não encontrado");
        }
        
        Paciente paciente = pacienteOpt.get();
        
        // Atualiza dados do paciente
        if (dados.paciNome() != null) paciente.setPaciNome(dados.paciNome());
        if (dados.paciSexo() != null) paciente.setPaciSexo(dados.paciSexo());
        if (dados.paciDataNacimento() != null) paciente.setPaciDataNacimento(Date.valueOf(dados.paciDataNacimento()));
        if (dados.paciCpf() != null) paciente.setPaciCpf(dados.paciCpf());
        if (dados.paciRg() != null) paciente.setPaciRg(dados.paciRg());
        if (dados.paciEmail() != null) paciente.setPaciEmail(dados.paciEmail());
        if (dados.paciTelefone() != null) paciente.setPaciTelefone(dados.paciTelefone());
        
        // Atualiza endereço se existir
        if (paciente.getEndereco() != null) {
            Endereco endereco = paciente.getEndereco();
            if (dados.endNacionalidade() != null) endereco.setEndNacionalidade(dados.endNacionalidade());
            if (dados.endUF() != null) endereco.setEndUF(dados.endUF());
            if (dados.endMunicipio() != null) endereco.setEndMunicipio(dados.endMunicipio());
            if (dados.endBairro() != null) endereco.setEndBairro(dados.endBairro());
            if (dados.endCep() != null) endereco.setEndCep(dados.endCep());
            if (dados.endRua() != null) endereco.setEndRua(dados.endRua());
            if (dados.endNumero() != null) endereco.setEndNumero(dados.endNumero().longValue());
            if (dados.endComplemento() != null) endereco.setEndComplemento(dados.endComplemento());
            enderecoRepository.save(endereco);
        }
        
        Paciente pacienteAtualizado = pacienteOutputPort.save(paciente);
        log.info("Paciente ID: {} atualizado com sucesso", id);
        
        return pacienteAtualizado;
    }
}
