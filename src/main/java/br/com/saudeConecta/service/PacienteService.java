package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.endereco.Endereco;
import br.com.saudeConecta.domain.historicodadospessoais.EntidadeTipo;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.infra.tenant.RequiresTenant;
import br.com.saudeConecta.infra.tenant.TenantHelper;
import br.com.saudeConecta.infrastructure.persistence.repository.EnderecoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.OrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.PacienteRepository;
import br.com.saudeConecta.presentation.dto.paciente.AtualizarPacienteRequest;
import br.com.saudeConecta.presentation.dto.paciente.CadastrarPacienteCompletoRequest;
import br.com.saudeConecta.util.SnapshotUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final HistoricoDadosPessoaisService historicoDadosPessoaisService;

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
        Paciente antes  = buscarrPacientebyOrg(id)
                .orElseThrow(() -> {
                    log.warn("Paciente não encontrado para atualização: {}", id);
                    return new IllegalArgumentException("Paciente não encontrado");
                });
        Paciente snapshot = SnapshotUtil.copiarSnapshot(antes);


        // Atualiza o endereço se existir
        Endereco enderecoAtualizado = null;
        if (antes .getEndereco() != null) {
            enderecoAtualizado = Endereco.builder()
                    .endCodigo(antes .getEndereco().getEndCodigo())
                    .endNacionalidade(dados.nacionalidade() != null ?
                            dados.nacionalidade() : antes .getEndereco().getEndNacionalidade())
                    .endUF(dados.uf() != null ?
                            dados.uf() : antes .getEndereco().getEndUF())
                    .endMunicipio(dados.municipio() != null ?
                            dados.municipio() : antes .getEndereco().getEndMunicipio())
                    .endBairro(dados.bairro() != null ?
                            dados.bairro() : antes .getEndereco().getEndBairro())
                    .endCep(dados.cep() != null ?
                            dados.cep() : antes .getEndereco().getEndCep())
                    .endRua(dados.rua() != null ?
                            dados.rua() : antes .getEndereco().getEndRua())
                    .endNumero(dados.numero() != null ?
                            dados.numero().longValue() : antes .getEndereco().getEndNumero())
                    .endComplemento(dados.complemento() != null ?
                            dados.complemento() : antes .getEndereco().getEndComplemento())
                    .build();

            // Salva o endereço atualizado
            enderecoAtualizado = enderecoRepository.save(enderecoAtualizado);
        }

        // Reconstrói o paciente com os dados atualizados
        Paciente pacienteAtualizado = Paciente.builder()
                .paciCodigo(antes .getPaciCodigo())
                .organizacao(antes .getOrganizacao())
                .paciNome(dados.nome() != null ? dados.nome() : antes .getPaciNome())
                .paciSexo(dados.sexo() != null ? dados.sexo() : antes .getPaciSexo())
                .paciDataNacimento(dados.dataNascimento () != null ?
                        Date.valueOf(dados.dataNascimento ()) : antes .getPaciDataNacimento())
                .paciCpf(dados.cpf() != null ? dados.cpf() : antes .getPaciCpf())
                .paciRg(dados.rg() != null ? dados.rg() : antes .getPaciRg())
                .paciEmail(dados.email() != null ? dados.email() : antes .getPaciEmail())
                .paciTelefone(dados.telefone() != null ? dados.telefone() : antes .getPaciTelefone())
                .endereco(enderecoAtualizado)
                .paciStatus(antes .getPaciStatus())
                .build();

        // Salva o paciente atualizado
        Paciente resultado = pacienteRepository.save(pacienteAtualizado);
        log.info("Paciente ID: {} atualizado com sucesso", id);

        historicoDadosPessoaisService.registrarAlteracoesDeObjeto(
                EntidadeTipo.PACIENTE,
                resultado.getPaciCodigo(),
                tenantHelper.getCurrentUserId(),
                snapshot,
                resultado
        );

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






    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Paciente> buscarListaPacientesPorNomeComFiltro(String pesquisa, String filtro) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.debug("Buscando pacientes por nome: {} na organização: {} com filtro: {}", pesquisa, orgId, filtro);
        
        if ("ATIVO".equals(filtro)) {
            return pacienteRepository.findByOrganizacaoIdAndNomeContainingWithFiltro(orgId, pesquisa, filtro, "ATIVO");
        } else {
            // ALL - usa consulta original
            return pacienteRepository.findByOrganizacaoIdAndNomeContaining(orgId, pesquisa);
        }
    }
    

    
    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Paciente> buscarListaPacientesPorCPFComFiltro(String pesquisa, String filtro) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.debug("Buscando pacientes por CPF: {} na organização: {} com filtro: {}", pesquisa, orgId, filtro);
        
        if ("ATIVO".equals(filtro)) {
            return pacienteRepository.findByOrganizacaoIdAndCpfContainingWithFiltro(orgId, pesquisa, filtro, "ATIVO");
        } else {
            // ALL - usa consulta original
            return pacienteRepository.findByOrganizacaoIdAndCpfContaining(orgId, pesquisa);
        }
    }
    

    
    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Paciente> buscarListaPacientesPorRGComFiltro(String pesquisa, String filtro) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.debug("Buscando pacientes por RG: {} na organização: {} com filtro: {}", pesquisa, orgId, filtro);
        
        if ("ATIVO".equals(filtro)) {
            return pacienteRepository.findByOrganizacaoIdAndRgContainingWithFiltro(orgId, pesquisa, filtro, "ATIVO");
        } else {
            // ALL - usa consulta original
            return pacienteRepository.findByOrganizacaoIdAndRgContaining(orgId, pesquisa);
        }
    }
    

    
    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Paciente> buscarListaPacientesPorTelefoneComFiltro(String pesquisa, String filtro) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.debug("Buscando pacientes por telefone: {} na organização: {} com filtro: {}", pesquisa, orgId, filtro);
        
        if ("ATIVO".equals(filtro)) {
            return pacienteRepository.findByOrganizacaoIdAndTelefoneContainingWithFiltro(orgId, pesquisa, filtro, "ATIVO");
        } else {
            // ALL - usa consulta original
            return pacienteRepository.findByOrganizacaoIdAndTelefoneContaining(orgId, pesquisa);
        }
    }

    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Paciente> buscarTodosPacientesComFiltro(String filtro) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.debug("Buscando todos os pacientes na organização: {} com filtro: {}", orgId, filtro);
        
        if ("ATIVO".equals(filtro)) {
            return pacienteRepository.findByOrganizacao_IdWithFiltro(orgId, filtro, "ATIVO");
        } else {
            // ALL - usa consulta original
            return pacienteRepository.findByOrganizacao_Id(orgId);
        }
    }





}
