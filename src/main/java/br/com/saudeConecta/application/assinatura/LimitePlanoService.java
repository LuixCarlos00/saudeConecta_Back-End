package br.com.saudeConecta.application.assinatura;

import br.com.saudeConecta.application.assinatura.dto.LimitesPlanoResponse;
import br.com.saudeConecta.application.exception.LimitePlanoException;
import br.com.saudeConecta.config.tenant.TenantHelper;
import br.com.saudeConecta.domain.planos.AssinaturaTenant;
import br.com.saudeConecta.domain.planos.PlanoAssinatura;
import br.com.saudeConecta.infrastructure.persistence.repository.AdminOrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.AssinaturaTenantRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.SecretariaRepository;
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
    private final TenantHelper tenantHelper;

    public LimitePlanoService(
            AssinaturaTenantRepository assinaturaTenantRepository,
            AdminOrganizacaoRepository adminOrganizacaoRepository,
            ProfissionalRepository profissionalRepository,
            SecretariaRepository secretariaRepository,
            TenantHelper tenantHelper) {
        this.assinaturaTenantRepository = assinaturaTenantRepository;
        this.adminOrganizacaoRepository = adminOrganizacaoRepository;
        this.profissionalRepository = profissionalRepository;
        this.secretariaRepository = secretariaRepository;
        this.tenantHelper = tenantHelper;
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
            negarSemAssinaturaExcetoRoot(organizacaoId);
            return;
        }

        AssinaturaTenant assinatura = assinaturaOpt.get();
        Integer limite = obterLimiteEfetivo(
                assinatura.getLimiteAdminOrgCustom(),
                assinatura.getPlanoAssinatura().getLimiteAdminOrg());

        if (limite == null) {
            return;
        }

        Long usado = adminOrganizacaoRepository.countAtivosByOrganizacaoId(organizacaoId);
        if (usado >= limite) {
            log.warn("assinatura.limite negado motivo=LIMITE_GESTOR organizacaoId={} usado={} limite={}", organizacaoId, usado, limite);
            throw new LimitePlanoException("GESTOR", limite, usado);
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
            negarSemAssinaturaExcetoRoot(organizacaoId);
            return;
        }

        AssinaturaTenant assinatura = assinaturaOpt.get();
        Integer limite = obterLimiteEfetivo(
                assinatura.getLimiteProfissionalCustom(),
                assinatura.getPlanoAssinatura().getLimiteProfissional());

        if (limite == null) {
            return;
        }

        Long usado = profissionalRepository.countAtivosByOrganizacaoId(organizacaoId);
        if (usado >= limite) {
            log.warn("assinatura.limite negado motivo=LIMITE_PROFISSIONAL organizacaoId={} usado={} limite={}", organizacaoId, usado, limite);
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
            negarSemAssinaturaExcetoRoot(organizacaoId);
            return;
        }

        AssinaturaTenant assinatura = assinaturaOpt.get();
        Integer limite = obterLimiteEfetivo(
                assinatura.getLimiteSecretariaCustom(),
                assinatura.getPlanoAssinatura().getLimiteSecretaria());

        if (limite == null) {
            return;
        }

        Long usado = secretariaRepository.countAtivasByOrganizacaoId(organizacaoId);
        if (usado >= limite) {
            log.warn("assinatura.limite negado motivo=LIMITE_SECRETARIA organizacaoId={} usado={} limite={}", organizacaoId, usado, limite);
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

        AssinaturaTenant assinatura = assinaturaOpt.get();
        PlanoAssinatura plano = assinatura.getPlanoAssinatura();

        return LimitesPlanoResponse.of(
                plano.getNome(),
                plano.getTipo().name(),
                obterLimiteEfetivo(assinatura.getLimiteAdminOrgCustom(), plano.getLimiteAdminOrg()),
                obterLimiteEfetivo(assinatura.getLimiteProfissionalCustom(), plano.getLimiteProfissional()),
                obterLimiteEfetivo(assinatura.getLimiteSecretariaCustom(), plano.getLimiteSecretaria()),
                adminOrganizacaoRepository.countAtivosByOrganizacaoId(organizacaoId),
                profissionalRepository.countAtivosByOrganizacaoId(organizacaoId),
                secretariaRepository.countAtivasByOrganizacaoId(organizacaoId)
        );
    }

    /**
     * Sem assinatura com acesso não há plano para medir o limite: antes a criação era liberada,
     * o que permitia cadastrar usuários sem pagar. Só o ROOT (SUPER_ADMIN) segue podendo, para
     * montar a organização antes de associar o plano.
     *
     * @param organizacaoId ID da organização
     * @throws LimitePlanoException quando o usuário atual não é SUPER_ADMIN
     */
    private void negarSemAssinaturaExcetoRoot(Long organizacaoId) {
        if (tenantHelper.isSuperAdmin()) {
            log.warn("assinatura.limite liberado motivo=SUPER_ADMIN_SEM_ASSINATURA organizacaoId={}", organizacaoId);
            return;
        }
        log.warn("assinatura.limite negado motivo=SEM_ASSINATURA_COM_ACESSO organizacaoId={}", organizacaoId);
        throw new LimitePlanoException(
                "Sua organizacao nao possui uma assinatura ativa. Regularize o plano para cadastrar novos usuarios.");
    }

    /**
     * Retorna o limite efetivo: custom se definido, senão o padrão do plano.
     *
     * @param limiteCustom limite personalizado da assinatura (pode ser null)
     * @param limitePlano  limite padrão do plano (pode ser null = ilimitado)
     * @return limite efetivo
     */
    private Integer obterLimiteEfetivo(Integer limiteCustom, Integer limitePlano) {
        return limiteCustom != null ? limiteCustom : limitePlano;
    }

    /**
     * Busca a assinatura ativa (TRIAL, ATIVA ou INADIMPLENTE) da organização.
     */
    private Optional<AssinaturaTenant> buscarAssinaturaAtiva(Long organizacaoId) {
        return assinaturaTenantRepository.findAssinaturaAtivaByOrganizacaoId(organizacaoId);
    }
}
