package br.com.saudeConecta.service;

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
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
  import org.springframework.stereotype.Service;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PacienteService  {

     private final PacienteRepository pacienteRepository;
    private final EnderecoRepository enderecoRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final TenantHelper tenantHelper;

    // ========== MÉTODOS COM TENANT ==========

    @RequiresTenant
    public Paciente cadastrarPacientebyOrg(CadastrarPacienteCompletoRequest request) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.info("Cadastrando paciente: {} na organização: {}", request.nome(), orgId);

        String cpfLimpo = limparCpf(request.cpf());

        if (cpfLimpo != null && !cpfLimpo.isEmpty() && existeCpfNoTenant(cpfLimpo)) {
            throw new IllegalStateException("CPF já cadastrado no sistema");
        }

        Organizacao organizacao = organizacaoRepository.findById(orgId)
            .orElseThrow(() -> new IllegalStateException("Organização não encontrada"));

        Endereco endereco = Endereco.builder()
                .endNacionalidade(request.nacionalidade())
                .endUF(request.uf())
                .endMunicipio(request.municipio())
                .endBairro(request.bairro())
                .endCep(request.cep())
                .endRua(request.rua())
                .endNumero(request.numero() != null ? request.numero().longValue() : null)
                .endComplemento(request.complemento())
                .build();



        Paciente paciente = Paciente.builder()
                .organizacao(organizacao)
                .paciNome(request.nome())
                .paciSexo(request.sexo())
                .paciDataNacimento(request.dataNacimento() != null ?
                    Date.valueOf(request.dataNacimento()) : null)
                .paciCpf(cpfLimpo)
                .paciRg(request.rg())
                .paciEmail(request.email())
                .paciTelefone(request.telefone())
                .endereco(endereco)
                .paciStatus("ATIVO")
                .build();

        enderecoRepository.save(endereco);
        Paciente salvo = pacienteRepository.save(paciente);
        log.info("Paciente cadastrado com sucesso. ID: {}", salvo.getPaciCodigo());

        return salvo;
    }

    private String limparCpf(String cpf) {
        return cpf != null ? cpf.replaceAll("[^0-9]", "") : null;
    }


    @RequiresTenant
    public Optional<Paciente> buscarrPacientebyOrg(Long id) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.debug("Buscando paciente ID: {} da organização: {}", id, orgId);
           return   pacienteRepository.findByPaciCodigoAndOrganizacao_IdWithEndereco(id, orgId);
    }



    @Transactional
    public Paciente atualizarPacientebyOrg(Long id, AtualizarPacienteRequest dados) {
        log.info("Atualizando paciente ID: {}", id);

        // Busca o paciente existente
        Paciente pacienteExistente = buscarrPacientebyOrg(id)
                .orElseThrow(() -> {
                    log.warn("Paciente não encontrado para atualização: {}", id);
                    return new IllegalArgumentException("Paciente não encontrado");
                });

        // Atualiza o endereço se existir
        Endereco enderecoAtualizado = null;
        if (pacienteExistente.getEndereco() != null) {
            enderecoAtualizado = Endereco.builder()
                    .endCodigo(pacienteExistente.getEndereco().getEndCodigo())
                    .endNacionalidade(dados.nacionalidade() != null ?
                            dados.nacionalidade() : pacienteExistente.getEndereco().getEndNacionalidade())
                    .endUF(dados.uf() != null ?
                            dados.uf() : pacienteExistente.getEndereco().getEndUF())
                    .endMunicipio(dados.municipio() != null ?
                            dados.municipio() : pacienteExistente.getEndereco().getEndMunicipio())
                    .endBairro(dados.bairro() != null ?
                            dados.bairro() : pacienteExistente.getEndereco().getEndBairro())
                    .endCep(dados.cep() != null ?
                            dados.cep() : pacienteExistente.getEndereco().getEndCep())
                    .endRua(dados.rua() != null ?
                            dados.rua() : pacienteExistente.getEndereco().getEndRua())
                    .endNumero(dados.numero() != null ?
                            dados.numero().longValue() : pacienteExistente.getEndereco().getEndNumero())
                    .endComplemento(dados.complemento() != null ?
                            dados.complemento() : pacienteExistente.getEndereco().getEndComplemento())
                    .build();

            // Salva o endereço atualizado
            enderecoAtualizado = enderecoRepository.save(enderecoAtualizado);
        }

        // Reconstrói o paciente com os dados atualizados
        Paciente pacienteAtualizado = Paciente.builder()
                .paciCodigo(pacienteExistente.getPaciCodigo())
                .organizacao(pacienteExistente.getOrganizacao())
                .paciNome(dados.nome() != null ? dados.nome() : pacienteExistente.getPaciNome())
                .paciSexo(dados.sexo() != null ? dados.sexo() : pacienteExistente.getPaciSexo())
                .paciDataNacimento(dados.dataNacimento() != null ?
                        Date.valueOf(dados.dataNacimento()) : pacienteExistente.getPaciDataNacimento())
                .paciCpf(dados.cpf() != null ? dados.cpf() : pacienteExistente.getPaciCpf())
                .paciRg(dados.rg() != null ? dados.rg() : pacienteExistente.getPaciRg())
                .paciEmail(dados.email() != null ? dados.email() : pacienteExistente.getPaciEmail())
                .paciTelefone(dados.telefone() != null ? dados.telefone() : pacienteExistente.getPaciTelefone())
                .endereco(enderecoAtualizado)
                .paciStatus(pacienteExistente.getPaciStatus())
                .build();

        // Salva o paciente atualizado
        Paciente resultado = pacienteRepository.save(pacienteAtualizado);
        log.info("Paciente ID: {} atualizado com sucesso", id);

        return resultado;
    }






    public void deletarPacientebyOrg(Long id) throws Exception {
        log.info("Iniciando exclusão do paciente ID: {}", id);

        if (id == null || id <= 0) {
            log.warn("Tentativa de exclusão com ID inválido: {}", id);
            throw new IllegalArgumentException("ID inválido");
        }

        var pacienteOpt = this.buscarrPacientebyOrg(id);
        if (pacienteOpt.isEmpty()) {
            log.warn("Paciente não encontrado para exclusão ID: {}", id);
            throw new Exception("Registro não encontrado");
        }

        Paciente paciente = pacienteOpt.get();
        Endereco endereco = paciente.getEndereco();

        try {
            // 1. Deletar o paciente primeiro (remove a FK)
            pacienteRepository.deleteById(id);
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




    public void bloquearPacientebyOrg(Long id, int status) {
        log.info("Alterando status do paciente ID: {} para {}", id, status == 0 ? "INATIVO" : "ATIVO");

        var pacienteOpt = buscarrPacientebyOrg(id);
        if (pacienteOpt.isEmpty()) {
            log.warn("Paciente não encontrado para alteração de status: {}", id);
            throw new IllegalArgumentException("Paciente não encontrado");
        }

        var paciente = pacienteOpt.get();
        paciente.setPaciStatus(status == 0 ? "INATIVO" : "ATIVO");
        pacienteRepository.save(paciente);
        log.info("Status do paciente ID: {} alterado com sucesso", id);
    }


     public boolean existeCpfNoTenant(String cpf) {
         Long orgId = tenantHelper.getCurrentTenantId();
         return pacienteRepository.existsByPaciCpfAndOrganizacao_Id(cpf, orgId);
     }





















//
//    @RequiresTenant
//    public List<Paciente> buscarTodosPorTenant() {
//        Long orgId = tenantHelper.getCurrentTenantId();
//        log.debug("Buscando pacientes da organização: {}", orgId);
//        return pacienteRepository.findByOrganizacao_Id(orgId);
//    }
//
//    @RequiresTenant
//    public Page<Paciente> buscarTodosPorTenant(Pageable pageable) {
//        Long orgId = tenantHelper.getCurrentTenantId();
//        log.debug("Buscando pacientes paginados da organização: {}", orgId);
//        return pacienteRepository.findByOrganizacao_Id(orgId, pageable);
//    }
//
//
//
//    @RequiresTenant
//    public List<Paciente> buscarPorNomeTenant(String nome) {
//        Long orgId = tenantHelper.getCurrentTenantId();
//        log.debug("Buscando pacientes por nome '{}' na organização: {}", nome, orgId);
//        return pacienteRepository.findByOrganizacaoIdAndNomeContaining(orgId, nome);
//    }
//
//    @RequiresTenant
//    public Long contarAtivosTenant() {
//        Long orgId = tenantHelper.getCurrentTenantId();
//        return pacienteRepository.countByOrganizacaoIdAndStatus(orgId, "ATIVO");
//    }
//
//

//
//    // ========== MÉTODOS LEGADOS (sem tenant) ==========
//
//
//    public Optional<Paciente> buscarPorId(Long id) {
//        log.debug("Buscando paciente por ID: {}", id);
//        return pacienteOutputPort.findById(id);
//    }
//
//
//    public Optional<Paciente> buscarPorEmail(String email) {
//        log.debug("Buscando paciente por email: {}", email);
//        return pacienteOutputPort.findByPaciEmail(email);
//    }
//
//
//
//
//    public Page<Paciente> buscarTodos(Pageable pageable) {
//        log.debug("Buscando todos os pacientes com paginação");
//        if (tenantHelper.hasTenant()) {
//            return buscarTodosPorTenant(pageable);
//        }
//        return pacienteOutputPort.findAll(pageable);
//    }
//
//
//    public List<Paciente> buscarPorCpf(String cpf) {
//        log.debug("Buscando pacientes por CPF: {}", cpf);
//        return pacienteOutputPort.findByPaciCpfContainingIgnoreCase(cpf);
//    }
//
//
//    public List<Paciente> buscarPorRg(String rg) {
//        log.debug("Buscando pacientes por RG: {}", rg);
//        return pacienteOutputPort.findByPaciRgContainingIgnoreCase(rg);
//    }
//
//
//    public List<Paciente> buscarPorTelefone(String telefone) {
//        log.debug("Buscando pacientes por telefone: {}", telefone);
//        return pacienteOutputPort.findByPaciTelefoneContainingIgnoreCase(telefone);
//    }
//
//
//    public List<Paciente> buscarPorNome(String nome) {
//        log.debug("Buscando pacientes por nome: {}", nome);
//        if (tenantHelper.hasTenant()) {
//            return buscarPorNomeTenant(nome);
//        }
//        return pacienteOutputPort.findByPaciNomeContainingIgnoreCase(nome);
//    }
//
//
//    public Paciente cadastrar(Paciente paciente) {
//        log.info("Cadastrando novo paciente: {}", paciente.getPaciNome());
//
//        // Se houver tenant e paciente não tem organização, define automaticamente
//        if (tenantHelper.hasTenant() && paciente.getOrganizacao() == null) {
//            Long orgId = tenantHelper.getCurrentTenantId();
//            Organizacao org = organizacaoRepository.findById(orgId)
//                .orElseThrow(() -> new IllegalStateException("Organização não encontrada"));
//            paciente.setOrganizacao(org);
//        }
//
//        Paciente pacienteSalvo = pacienteOutputPort.save(paciente);
//        log.info("Paciente cadastrado com sucesso. ID: {}", pacienteSalvo.getPaciCodigo());
//        return pacienteSalvo;
//    }
//
//
//    /**
//     * Cadastra paciente com validação de endereço.
//     * @param dados DTO com dados do paciente
//     * @return Paciente cadastrado
//     * @throws IllegalArgumentException se endereço não for encontrado
//     */
//
//
//    /**
//     * Bloqueia ou desbloqueia paciente.
//     * @param id ID do paciente
//     * @param status 0 para inativo, 1 para ativo
//     * @throws IllegalArgumentException se paciente não for encontrado
//     */

    // ========== MÉTODOS DE BUSCA PARA AUTOCOMPLETE ==========
    
    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Paciente> buscarListaPacientesPorNome(String pesquisa) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.debug("Buscando pacientes por nome: {} na organização: {}", pesquisa, orgId);
        return pacienteRepository.findByOrganizacaoIdAndNomeContaining(orgId, pesquisa);
    }
    
    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Paciente> buscarListaPacientesPorCPF(String pesquisa) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.debug("Buscando pacientes por CPF: {} na organização: {}", pesquisa, orgId);
        return pacienteRepository.findByOrganizacaoIdAndCpfContaining(orgId, pesquisa);
    }
    
    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Paciente> buscarListaPacientesPorRG(String pesquisa) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.debug("Buscando pacientes por RG: {} na organização: {}", pesquisa, orgId);
        return pacienteRepository.findByOrganizacaoIdAndRgContaining(orgId, pesquisa);
    }
    
    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Paciente> buscarListaPacientesPorTelefone(String pesquisa) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.debug("Buscando pacientes por telefone: {} na organização: {}", pesquisa, orgId);
        return pacienteRepository.findByOrganizacaoIdAndTelefoneContaining(orgId, pesquisa);
    }
    
    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Paciente> buscarTodosPacientes() {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.debug("Buscando todos os pacientes na organização: {}", orgId);
        return pacienteRepository.findByOrganizacao_Id(orgId);
    }





}
