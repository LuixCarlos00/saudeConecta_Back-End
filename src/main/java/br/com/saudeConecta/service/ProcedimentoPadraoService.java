package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.prontuario.ProcedimentoPadrao;
import br.com.saudeConecta.infrastructure.persistence.repository.ProcedimentoPadraoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import br.com.saudeConecta.infra.tenant.TenantContext;
import br.com.saudeConecta.presentation.dto.prontuario.ProcedimentoPadraoRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    @Transactional
    public ProcedimentoPadrao criar(Long profissionalId, ProcedimentoPadraoRequest request) {
        Long orgId = TenantContext.getCurrentTenant();
        log.info("Criando procedimento padrão — profissional={}, nome={}", profissionalId, request.getNomeProcedimento());

        Profissional profissional = profissionalRepository.findById(profissionalId)
                .orElseThrow(() -> new IllegalArgumentException("Profissional não encontrado: " + profissionalId));

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
     * Atualiza um procedimento padrão existente.
     *
     * @param id ID do procedimento
     * @param request dados atualizados
     * @return procedimento atualizado
     */
    @Transactional
    public ProcedimentoPadrao atualizar(Long id, ProcedimentoPadraoRequest request) {
        ProcedimentoPadrao procedimento = procedimentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Procedimento não encontrado: " + id));

        procedimento.setNomeProcedimento(request.getNomeProcedimento());
        procedimento.setValorPadrao(request.getValorPadrao());

        return procedimentoRepository.save(procedimento);
    }

    /**
     * Desativa um procedimento (soft delete).
     *
     * @param id ID do procedimento
     */
    @Transactional
    public void desativar(Long id) {
        ProcedimentoPadrao procedimento = procedimentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Procedimento não encontrado: " + id));

        procedimento.setAtivo(false);
        procedimentoRepository.save(procedimento);
        log.info("Procedimento desativado — id={}", id);
    }
}
