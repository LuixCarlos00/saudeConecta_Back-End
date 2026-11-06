package br.com.saudeConecta.application.prontuario;

import br.com.saudeConecta.application.prontuario.dto.ProcedimentoPadraoRequest;
import br.com.saudeConecta.config.tenant.TenantContext;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.prontuario.ProcedimentoPadrao;
import br.com.saudeConecta.infrastructure.persistence.repository.ProcedimentoPadraoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcedimentoPadraoService {

    private final ProcedimentoPadraoRepository procedimentoRepository;
    private final ProfissionalRepository profissionalRepository;

    /**
     * Lista procedimentos ativos de um profissional na organização.
     *
     * @param profissionalId ID do profissional
     * @return lista de procedimentos padrão ativos
     */
    @Cacheable(value = "procedimentos-padrao", key = "'org-' + @tenantHelper.getCurrentTenantIdOrNull() + '-ativos-' + #profissionalId")
    @Transactional(readOnly = true)
    public List<ProcedimentoPadrao> listarPorProfissional(Long profissionalId) {
        Long orgId = TenantContext.getCurrentTenant();
        return procedimentoRepository.findAtivosByProfissionalAndOrg(profissionalId, orgId);
    }

    /**
     * Cria um novo procedimento padrão vinculado ao profissional logado.
     *
     * @param profissionalId ID do profissional
     * @param request dados do procedimento
     * @return procedimento criado
     */
    @CacheEvict(value = "procedimentos-padrao", allEntries = true)
    @Transactional
    public ProcedimentoPadrao criar(Long profissionalId, ProcedimentoPadraoRequest request) {
        Long orgId = TenantContext.getCurrentTenantOrThrow();
        log.info("procedimento.criar inicio profissionalId={}", profissionalId);

        Profissional profissional = profissionalRepository.findByIdAndOrganizacao_Id(profissionalId, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Profissional nao encontrado: " + profissionalId));

        Organizacao organizacao = new Organizacao();
        organizacao.setId(orgId);

        ProcedimentoPadrao procedimento = ProcedimentoPadrao.builder()
                .profissional(profissional)
                .organizacao(organizacao)
                .nomeProcedimento(request.getNomeProcedimento())
                .valorPadrao(request.getValorPadrao())
                .ativo(true)
                .build();

        return procedimentoRepository.save(procedimento);
    }

    /**
     * Lista todos os procedimentos (ativos e inativos) de um profissional na organização.
     * Usado na tela de configurações para gerenciamento completo.
     *
     * @param profissionalId ID do profissional
     * @return lista de todos os procedimentos padrão
     */
    @Cacheable(value = "procedimentos-padrao", key = "'org-' + @tenantHelper.getCurrentTenantIdOrNull() + '-todos-' + #profissionalId")
    @Transactional(readOnly = true)
    public List<ProcedimentoPadrao> listarTodosPorProfissional(Long profissionalId) {
        Long orgId = TenantContext.getCurrentTenant();
        return procedimentoRepository.findAllByProfissionalAndOrg(profissionalId, orgId);
    }

    /**
     * Alterna o status ativo/inativo de um procedimento padrão.
     *
     * @param id ID do procedimento
     * @return procedimento com status atualizado
     */
    @CacheEvict(value = "procedimentos-padrao", allEntries = true)
    @Transactional
    public ProcedimentoPadrao toggleAtivo(Long id) {
        ProcedimentoPadrao procedimento = buscarDaOrganizacao(id);

        procedimento.setAtivo(!procedimento.getAtivo());
        ProcedimentoPadrao salvo = procedimentoRepository.save(procedimento);
        log.info("procedimento.toggle ok procedimentoId={} status={}", id, salvo.getAtivo() ? "ATIVO" : "BLOQUEADO");
        return salvo;
    }

    /**
     * Atualiza um procedimento padrão existente.
     *
     * @param id ID do procedimento
     * @param request dados atualizados
     * @return procedimento atualizado
     */
    @CacheEvict(value = "procedimentos-padrao", allEntries = true)
    @Transactional
    public ProcedimentoPadrao atualizar(Long id, ProcedimentoPadraoRequest request) {
        ProcedimentoPadrao procedimento = buscarDaOrganizacao(id);

        procedimento.setNomeProcedimento(request.getNomeProcedimento());
        procedimento.setValorPadrao(request.getValorPadrao());

        return procedimentoRepository.save(procedimento);
    }

    /**
     * Busca o procedimento restrito à organização do token; de outra clínica responde "não encontrado".
     *
     * @param id ID do procedimento
     * @return procedimento da organização atual
     */
    private ProcedimentoPadrao buscarDaOrganizacao(Long id) {
        return procedimentoRepository.findByIdAndOrganizacao_Id(id, TenantContext.getCurrentTenantOrThrow())
                .orElseThrow(() -> new IllegalArgumentException("Procedimento nao encontrado: " + id));
    }
}
