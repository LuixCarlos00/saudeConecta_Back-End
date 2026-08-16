package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.planos.PlanoAssinatura;
import br.com.saudeConecta.domain.planos.TipoPlano;
import br.com.saudeConecta.infra.exceptions.BusinessException;
import br.com.saudeConecta.infrastructure.persistence.repository.PlanoAssinaturaRepository;
import br.com.saudeConecta.presentation.dto.planos.PlanoAssinaturaRequest;
import br.com.saudeConecta.presentation.dto.planos.PlanoAssinaturaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlanoAssinaturaService {

    private static final Logger log = LoggerFactory.getLogger(PlanoAssinaturaService.class);

    private final PlanoAssinaturaRepository planoAssinaturaRepository;

    public PlanoAssinaturaService(PlanoAssinaturaRepository planoAssinaturaRepository) {
        this.planoAssinaturaRepository = planoAssinaturaRepository;
    }

    /**
     * Lista todos os planos ativos.
     *
     * @return lista de PlanoAssinaturaResponse
     */
    @Cacheable(value = "planos", key = "'ativos'")
    @Transactional(readOnly = true)
    public List<PlanoAssinaturaResponse> listarPlanosAtivos() {
        return planoAssinaturaRepository.findByAtivoTrue()
                .stream()
                .map(PlanoAssinaturaResponse::fromEntity)
                .toList();
    }

    /**
     * Busca um plano ativo por ID.
     *
     * @param id ID do plano
     * @return PlanoAssinaturaResponse
     */
    @Cacheable(value = "planos", key = "'id-' + #id")
    @Transactional(readOnly = true)
    public PlanoAssinaturaResponse buscarPorId(Long id) {
        PlanoAssinatura plano = planoAssinaturaRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new BusinessException(
                        "Plano nao encontrado ou inativo: " + id,
                        HttpStatus.NOT_FOUND));
        return PlanoAssinaturaResponse.fromEntity(plano);
    }

    /**
     * Busca um plano por tipo.
     *
     * @param tipo TipoPlano
     * @return PlanoAssinaturaResponse
     */
    @Cacheable(value = "planos", key = "'tipo-' + #tipo.name()")
    @Transactional(readOnly = true)
    public PlanoAssinaturaResponse buscarPorTipo(TipoPlano tipo) {
        PlanoAssinatura plano = planoAssinaturaRepository.findByTipo(tipo)
                .orElseThrow(() -> new BusinessException(
                        "Plano nao encontrado para o tipo: " + tipo,
                        HttpStatus.NOT_FOUND));
        return PlanoAssinaturaResponse.fromEntity(plano);
    }


    /**
     * Atualiza um plano existente.
     *
     * @param id      ID do plano
     * @param request dados atualizados
     * @return PlanoAssinaturaResponse do plano atualizado
     */
    @CacheEvict(value = "planos", allEntries = true)
    @Transactional
    public PlanoAssinaturaResponse atualizarPlano(Long id, PlanoAssinaturaRequest request) {
        PlanoAssinatura plano = planoAssinaturaRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "Plano nao encontrado: " + id,
                        HttpStatus.NOT_FOUND));

        plano.setNome(request.nome());
        plano.setDescricao(request.getDescricaoFormatada());
        plano.setValorMensal(request.valorMensal());
        plano.setLimiteAdminOrg(request.limiteAdminOrg());
        plano.setLimiteProfissional(request.limiteProfissional());
        plano.setLimiteSecretaria(request.limiteSecretaria());
        plano.setValorAdicionalAdmin(request.valorAdicionalAdmin());
        plano.setValorAdicionalProfissional(request.valorAdicionalProfissional());
        plano.setValorAdicionalSecretaria(request.valorAdicionalSecretaria());

        PlanoAssinatura salvo = planoAssinaturaRepository.save(plano);
        log.info("Plano atualizado: {} ({})", salvo.getNome(), salvo.getTipo());
        return PlanoAssinaturaResponse.fromEntity(salvo);
    }

    /**
     * Desativa um plano (soft delete).
     *
     * @param id ID do plano
     */
    @CacheEvict(value = "planos", allEntries = true)
    @Transactional
    public void desativarPlano(Long id) {
        PlanoAssinatura plano = planoAssinaturaRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "Plano nao encontrado: " + id,
                        HttpStatus.NOT_FOUND));

        plano.setAtivo(false);
        planoAssinaturaRepository.save(plano);
        log.info("Plano desativado: {} ({})", plano.getNome(), plano.getTipo());
    }

    /**
     * Busca a entidade PlanoAssinatura por ID (uso interno por outros services).
     *
     * @param id ID do plano
     * @return PlanoAssinatura
     */
    @Transactional(readOnly = true)
    public PlanoAssinatura buscarEntidadePorId(Long id) {
        return planoAssinaturaRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new BusinessException(
                        "Plano nao encontrado ou inativo: " + id,
                        HttpStatus.NOT_FOUND));
    }
}
