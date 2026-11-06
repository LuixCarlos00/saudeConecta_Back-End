package br.com.saudeConecta.application.paciente;

import br.com.saudeConecta.application.cache.CacheEvictionService;
import br.com.saudeConecta.application.historico.HistoricoDadosPessoaisService;
import br.com.saudeConecta.application.historico.SnapshotUtil;
import br.com.saudeConecta.application.paciente.dto.AtualizarPacienteRequest;
import br.com.saudeConecta.application.paciente.dto.CadastrarPacienteCompletoRequest;
import br.com.saudeConecta.config.logging.MascaraLog;
import br.com.saudeConecta.config.tenant.RequiresTenant;
import br.com.saudeConecta.config.tenant.TenantHelper;
import br.com.saudeConecta.domain.endereco.Endereco;
import br.com.saudeConecta.domain.historicodadospessoais.EntidadeTipo;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.infrastructure.persistence.repository.EnderecoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.OrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.PacienteRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PacienteService  {
    private static final String STATUS_ATIVO = "ATIVO";

     private final PacienteRepository pacienteRepository;
    private final EnderecoRepository enderecoRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final TenantHelper tenantHelper;
    private final HistoricoDadosPessoaisService historicoDadosPessoaisService;
    private final CacheEvictionService cacheEvictionService;

    // ========== MÉTODOS COM TENANT ==========

    @Caching(evict = {
        @CacheEvict(value = "pacientes-org", key = "'org-' + @tenantHelper.getCurrentTenantIdOrNull() + '-filtro-ATIVO'"),
        @CacheEvict(value = "pacientes-org", key = "'org-' + @tenantHelper.getCurrentTenantIdOrNull() + '-filtro-ALL'")
    })
    @RequiresTenant
    public Paciente cadastrarPacientebyOrg(CadastrarPacienteCompletoRequest request) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.info("paciente.cadastrar inicio organizacaoId={}", orgId);

        String cpfLimpo = limparCpf(request.cpf());

        if (cpfLimpo != null && !cpfLimpo.isEmpty() && existeCpfNoTenant(cpfLimpo)) {
            throw new IllegalStateException("CPF já cadastrado no sistema");
        }

        Organizacao organizacao = organizacaoRepository.findById(orgId)
            .orElseThrow(() -> new IllegalStateException("organizacao não encontrada"));

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
                .paciStatus(STATUS_ATIVO)
                .build();

        enderecoRepository.save(endereco);
        Paciente salvo = pacienteRepository.save(paciente);
        log.info("paciente.cadastrar ok pacienteId={}", salvo.getPaciCodigo());

        return salvo;
    }

    private String limparCpf(String cpf) {
        return cpf != null ? cpf.replaceAll("\\D", "") : null;
    }

    @RequiresTenant
    public Optional<Paciente> buscarrPacientebyOrg(Long id) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.debug("paciente.buscar pacienteId={} organizacaoId={}", id, orgId);
           return   pacienteRepository.findByPaciCodigoAndOrganizacao_IdWithEndereco(id, orgId);
    }

    @Caching(evict = {
        @CacheEvict(value = "pacientes-org", key = "'org-' + @tenantHelper.getCurrentTenantIdOrNull() + '-filtro-ATIVO'"),
        @CacheEvict(value = "pacientes-org", key = "'org-' + @tenantHelper.getCurrentTenantIdOrNull() + '-filtro-ALL'")
    })
    @Transactional
    public Paciente atualizarPacientebyOrg(Long id, AtualizarPacienteRequest dados) {
        log.info("paciente.atualizar inicio pacienteId={}", id);

        // Busca o paciente existente
        Paciente antes  = buscarrPacientebyOrg(id)
                .orElseThrow(() -> {
                    log.warn("paciente.atualizar negado motivo=NAO_ENCONTRADO pacienteId={}", id);
                    return new IllegalArgumentException("Paciente não encontrado");
                });
        Paciente snapshot = SnapshotUtil.copiarSnapshot(antes);

        // Atualiza o endereço se existir
        Endereco enderecoAtualizado = atualizarEndereco(antes.getEndereco(), dados);

        // Reconstrói o paciente com os dados atualizados
        Paciente pacienteAtualizado = reconstruirPaciente(antes, dados, enderecoAtualizado);

        // Salva o paciente atualizado
        Paciente resultado = pacienteRepository.save(pacienteAtualizado);
        log.info("paciente.atualizar ok pacienteId={}", id);

        historicoDadosPessoaisService.registrarAlteracoesDeObjeto(
                EntidadeTipo.PACIENTE,
                resultado.getPaciCodigo(),
                tenantHelper.getCurrentUserId(),
                snapshot,
                resultado
        );

        return resultado;
    }

    /**
     * Aplica sobre o endereço atual apenas os campos informados e salva o resultado.
     *
     * @param atual endereço vigente do paciente (pode ser nulo)
     * @param dados dados enviados na atualização
     * @return endereço salvo, ou nulo quando o paciente não tem endereço
     */
    private Endereco atualizarEndereco(Endereco atual, AtualizarPacienteRequest dados) {
        if (atual == null) {
            return null;
        }
        Endereco atualizado = Endereco.builder()
                .endCodigo(atual.getEndCodigo())
                .endNacionalidade(valorOuAtual(dados.nacionalidade(), atual.getEndNacionalidade()))
                .endUF(valorOuAtual(dados.uf(), atual.getEndUF()))
                .endMunicipio(valorOuAtual(dados.municipio(), atual.getEndMunicipio()))
                .endBairro(valorOuAtual(dados.bairro(), atual.getEndBairro()))
                .endCep(valorOuAtual(dados.cep(), atual.getEndCep()))
                .endRua(valorOuAtual(dados.rua(), atual.getEndRua()))
                .endNumero(dados.numero() != null ?
                        dados.numero().longValue() : atual.getEndNumero())
                .endComplemento(valorOuAtual(dados.complemento(), atual.getEndComplemento()))
                .build();

        // Salva o endereço atualizado
        return enderecoRepository.save(atualizado);
    }

    private Paciente reconstruirPaciente(Paciente antes, AtualizarPacienteRequest dados, Endereco endereco) {
        return Paciente.builder()
                .paciCodigo(antes.getPaciCodigo())
                .organizacao(antes.getOrganizacao())
                .paciNome(valorOuAtual(dados.nome(), antes.getPaciNome()))
                .paciSexo(valorOuAtual(dados.sexo(), antes.getPaciSexo()))
                .paciDataNacimento(dados.dataNascimento() != null ?
                        Date.valueOf(dados.dataNascimento()) : antes.getPaciDataNacimento())
                .paciCpf(valorOuAtual(limparCpf(dados.cpf()), antes.getPaciCpf()))
                .paciRg(valorOuAtual(dados.rg(), antes.getPaciRg()))
                .paciEmail(valorOuAtual(dados.email(), antes.getPaciEmail()))
                .paciTelefone(valorOuAtual(dados.telefone(), antes.getPaciTelefone()))
                .endereco(endereco)
                .paciStatus(antes.getPaciStatus())
                .build();
    }

    /** Atualização parcial: o valor enviado vence; ausente (nulo) preserva o atual. */
    private <T> T valorOuAtual(T novo, T atual) {
        return novo != null ? novo : atual;
    }

    @Caching(evict = {
        @CacheEvict(value = "pacientes-org", key = "'org-' + @tenantHelper.getCurrentTenantIdOrNull() + '-filtro-ATIVO'"),
        @CacheEvict(value = "pacientes-org", key = "'org-' + @tenantHelper.getCurrentTenantIdOrNull() + '-filtro-ALL'")
    })
    public void deletarPacientebyOrg(Long id) {
        log.info("paciente.excluir inicio pacienteId={}", id);

        if (id == null || id <= 0) {
            log.warn("paciente.excluir negado motivo=ID_INVALIDO pacienteId={}", id);
            throw new IllegalArgumentException("ID inválido");
        }

        var pacienteOpt = this.buscarrPacientebyOrg(id);
        if (pacienteOpt.isEmpty()) {
            log.warn("paciente.excluir negado motivo=NAO_ENCONTRADO pacienteId={}", id);
            throw new EntityNotFoundException("Registro não encontrado");
        }

        Paciente paciente = pacienteOpt.get();
        Endereco endereco = paciente.getEndereco();

        // Violação de FK (paciente com consultas, etc.) sobe como DataIntegrityViolationException → 409 no handler
        // 1. Deletar o paciente primeiro (remove a FK)
        pacienteRepository.deleteById(id);
        log.info("paciente.excluir ok pacienteId={}", id);

        // 2. Deletar o endereço associado
        if (endereco != null) {
            enderecoRepository.deleteById(endereco.getEndCodigo());
            log.debug("paciente.excluir endereco excluido enderecoId={}", endereco.getEndCodigo());
        }
    }

    @Caching(evict = {
        @CacheEvict(value = "pacientes-org", key = "'org-' + @tenantHelper.getCurrentTenantIdOrNull() + '-filtro-ATIVO'"),
        @CacheEvict(value = "pacientes-org", key = "'org-' + @tenantHelper.getCurrentTenantIdOrNull() + '-filtro-ALL'")
    })
    public void bloquearPacientebyOrg(Long id, int status) {
        log.info("paciente.status inicio pacienteId={} status={}", id, status == 0 ? "INATIVO" : STATUS_ATIVO);

        var pacienteOpt = buscarrPacientebyOrg(id);
        if (pacienteOpt.isEmpty()) {
            log.warn("paciente.status negado motivo=NAO_ENCONTRADO pacienteId={}", id);
            throw new IllegalArgumentException("Paciente não encontrado");
        }

        var paciente = pacienteOpt.get();
        paciente.setPaciStatus(status == 0 ? "INATIVO" : STATUS_ATIVO);
        pacienteRepository.save(paciente);

        cacheEvictionService.evictUsuariosAgrupados(paciente.getOrganizacaoId());
        cacheEvictionService.evictUsuariosAgrupadosSuperAdmin();

        log.info("paciente.status ok pacienteId={}", id);
    }

     public boolean existeCpfNoTenant(String cpf) {
         Long orgId = tenantHelper.getCurrentTenantId();
         return pacienteRepository.existsByPaciCpfAndOrganizacao_Id(cpf, orgId);
     }

    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Paciente> buscarListaPacientesPorNomeComFiltro(String pesquisa, String filtro) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.debug("paciente.buscar nome organizacaoId={} filtro={}", orgId, filtro);
        
        if (STATUS_ATIVO.equals(filtro)) {
            return pacienteRepository.findByOrganizacaoIdAndNomeContainingWithFiltro(orgId, pesquisa, filtro, STATUS_ATIVO);
        } else {
            // ALL - usa consulta original
            return pacienteRepository.findByOrganizacaoIdAndNomeContaining(orgId, pesquisa);
        }
    }
    

    
    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Paciente> buscarListaPacientesPorCPFComFiltro(String pesquisa, String filtro) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.debug("paciente.buscar cpf={} organizacaoId={} filtro={}", MascaraLog.cpf(pesquisa), orgId, filtro);
        
        if (STATUS_ATIVO.equals(filtro)) {
            return pacienteRepository.findByOrganizacaoIdAndCpfContainingWithFiltro(orgId, pesquisa, filtro, STATUS_ATIVO);
        } else {
            // ALL - usa consulta original
            return pacienteRepository.findByOrganizacaoIdAndCpfContaining(orgId, pesquisa);
        }
    }
    

    
    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Paciente> buscarListaPacientesPorRGComFiltro(String pesquisa, String filtro) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.debug("paciente.buscar rg organizacaoId={} filtro={}", orgId, filtro);
        
        if (STATUS_ATIVO.equals(filtro)) {
            return pacienteRepository.findByOrganizacaoIdAndRgContainingWithFiltro(orgId, pesquisa, filtro, STATUS_ATIVO);
        } else {
            // ALL - usa consulta original
            return pacienteRepository.findByOrganizacaoIdAndRgContaining(orgId, pesquisa);
        }
    }
    

    
    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Paciente> buscarListaPacientesPorTelefoneComFiltro(String pesquisa, String filtro) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.debug("paciente.buscar telefone organizacaoId={} filtro={}", orgId, filtro);
        
        if (STATUS_ATIVO.equals(filtro)) {
            return pacienteRepository.findByOrganizacaoIdAndTelefoneContainingWithFiltro(orgId, pesquisa, filtro, STATUS_ATIVO);
        } else {
            // ALL - usa consulta original
            return pacienteRepository.findByOrganizacaoIdAndTelefoneContaining(orgId, pesquisa);
        }
    }

    @RequiresTenant
    @Cacheable(value = "pacientes-org", key = "'org-' + @tenantHelper.getCurrentTenantId() + '-filtro-' + #filtro")
    @Transactional(readOnly = true)
    public List<Paciente> buscarTodosPacientesComFiltro(String filtro) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.debug("paciente.buscar todos organizacaoId={} filtro={}", orgId, filtro);
        
        if (STATUS_ATIVO.equals(filtro)) {
            return pacienteRepository.findByOrganizacao_IdWithFiltro(orgId, filtro, STATUS_ATIVO);
        } else {
            // ALL - usa consulta original
            return pacienteRepository.findByOrganizacao_Id(orgId);
        }
    }

}
