package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.planos.AssinaturaTenant;
import br.com.saudeConecta.domain.planos.PlanoAssinatura;
import br.com.saudeConecta.infra.exceptions.LimitePlanoException;
import br.com.saudeConecta.infrastructure.persistence.repository.AdminOrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.AssinaturaTenantRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.SecretariaRepository;
import br.com.saudeConecta.presentation.dto.planos.LimitesPlanoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class LimitePlanoService {

    private static final Logger log = LoggerFactory.getLogger(LimitePlanoService.class);

    private final AssinaturaTenantRepository assinaturaTenantRepository;
    private final AdminOrganizacaoRepository adminOrganizacaoRepository;
    private final ProfissionalRepository profissionalRepository;
    private final SecretariaRepository secretariaRepository;

    public LimitePlanoService(
            AssinaturaTenantRepository assinaturaTenantRepository,
            AdminOrganizacaoRepository adminOrganizacaoRepository,
            ProfissionalRepository profissionalRepository,
            SecretariaRepository secretariaRepository) {
        this.assinaturaTenantRepository = assinaturaTenantRepository;
        this.adminOrganizacaoRepository = adminOrganizacaoRepository;
        this.profissionalRepository = profissionalRepository;
        this.secretariaRepository = secretariaRepository;
    }

    /**
     * Valida se a organização pode adicionar mais um Admin Org.
     * Lança LimitePlanoException se o limite foi atingido.
     *
     * @param organizacaoId ID da organização
     */
    @Transactional(readOnly = true)
    public void validarLimiteAdminOrg(Long organizacaoId) {
        Optional<AssinaturaTenant> assinaturaOpt = buscarAssinaturaAtiva(organizacaoId);
        if (assinaturaOpt.isEmpty()) {
            log.warn("Organização {} sem assinatura ativa. Permitindo criação (sem plano).", organizacaoId);
            return;
        }

        PlanoAssinatura plano = assinaturaOpt.get().getPlanoAssinatura();
        Integer limite = plano.getLimiteAdminOrg();

        if (limite == null) {
            return;
        }

        Long usado = adminOrganizacaoRepository.countAtivosByOrganizacaoId(organizacaoId);
        if (usado >= limite) {
            log.info("Limite de ADMIN_ORG atingido para org {}: {}/{}", organizacaoId, usado, limite);
            throw new LimitePlanoException("ADMIN_ORG", limite, usado);
        }
    }

    /**
     * Valida se a organização pode adicionar mais um Profissional.
     * Lança LimitePlanoException se o limite foi atingido.
     *
     * @param organizacaoId ID da organização
     */
    @Transactional(readOnly = true)
    public void validarLimiteProfissional(Long organizacaoId) {
        Optional<AssinaturaTenant> assinaturaOpt = buscarAssinaturaAtiva(organizacaoId);
        if (assinaturaOpt.isEmpty()) {
            log.warn("Organização {} sem assinatura ativa. Permitindo criação (sem plano).", organizacaoId);
            return;
        }

        PlanoAssinatura plano = assinaturaOpt.get().getPlanoAssinatura();
        Integer limite = plano.getLimiteProfissional();

        if (limite == null) {
            return;
        }

        Long usado = profissionalRepository.countAtivosByOrganizacaoId(organizacaoId);
        if (usado >= limite) {
            log.info("Limite de PROFISSIONAL atingido para org {}: {}/{}", organizacaoId, usado, limite);
            throw new LimitePlanoException("PROFISSIONAL", limite, usado);
        }
    }

    /**
     * Valida se a organização pode adicionar mais uma Secretária.
     * Lança LimitePlanoException se o limite foi atingido.
     *
     * @param organizacaoId ID da organização
     */
    @Transactional(readOnly = true)
    public void validarLimiteSecretaria(Long organizacaoId) {
        Optional<AssinaturaTenant> assinaturaOpt = buscarAssinaturaAtiva(organizacaoId);
        if (assinaturaOpt.isEmpty()) {
            log.warn("Organização {} sem assinatura ativa. Permitindo criação (sem plano).", organizacaoId);
            return;
        }

        PlanoAssinatura plano = assinaturaOpt.get().getPlanoAssinatura();
        Integer limite = plano.getLimiteSecretaria();

        if (limite == null) {
            return;
        }

        Long usado = secretariaRepository.countAtivasByOrganizacaoId(organizacaoId);
        if (usado >= limite) {
            log.info("Limite de SECRETARIA atingido para org {}: {}/{}", organizacaoId, usado, limite);
            throw new LimitePlanoException("SECRETARIA", limite, usado);
        }
    }

    /**
     * Retorna o uso atual vs. limites do plano da organização.
     *
     * @param organizacaoId ID da organização
     * @return LimitesPlanoResponse com uso atual e limites
     */
    @Transactional(readOnly = true)
    public LimitesPlanoResponse obterLimitesPlano(Long organizacaoId) {
        Optional<AssinaturaTenant> assinaturaOpt = buscarAssinaturaAtiva(organizacaoId);

        if (assinaturaOpt.isEmpty()) {
            return LimitesPlanoResponse.of(
                    "Sem plano", null,
                    null, null, null,
                    adminOrganizacaoRepository.countAtivosByOrganizacaoId(organizacaoId),
                    profissionalRepository.countAtivosByOrganizacaoId(organizacaoId),
                    secretariaRepository.countAtivasByOrganizacaoId(organizacaoId)
            );
        }

        PlanoAssinatura plano = assinaturaOpt.get().getPlanoAssinatura();

        return LimitesPlanoResponse.of(
                plano.getNome(),
                plano.getTipo().name(),
                plano.getLimiteAdminOrg(),
                plano.getLimiteProfissional(),
                plano.getLimiteSecretaria(),
                adminOrganizacaoRepository.countAtivosByOrganizacaoId(organizacaoId),
                profissionalRepository.countAtivosByOrganizacaoId(organizacaoId),
                secretariaRepository.countAtivasByOrganizacaoId(organizacaoId)
        );
    }

    /**
     * Busca a assinatura ativa (TRIAL, ATIVA ou INADIMPLENTE) da organização.
     */
    private Optional<AssinaturaTenant> buscarAssinaturaAtiva(Long organizacaoId) {
        return assinaturaTenantRepository.findAssinaturaAtivaByOrganizacaoId(organizacaoId);
    }
}
