package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.planos.AssinaturaTenant;
import br.com.saudeConecta.domain.planos.PlanoAssinatura;
import br.com.saudeConecta.domain.planos.StatusAssinatura;
import br.com.saudeConecta.infra.exceptions.BusinessException;
import br.com.saudeConecta.infrastructure.persistence.repository.AssinaturaTenantRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.OrganizacaoRepository;
import br.com.saudeConecta.presentation.dto.planos.AssinaturaTenantResponse;
import br.com.saudeConecta.presentation.dto.planos.CustomizarPlanoTenantRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class AssinaturaTenantService {

    private static final Logger log = LoggerFactory.getLogger(AssinaturaTenantService.class);
    private static final int DIAS_TRIAL = 30;

    private final AssinaturaTenantRepository assinaturaTenantRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final PlanoAssinaturaService planoAssinaturaService;

    public AssinaturaTenantService(
            AssinaturaTenantRepository assinaturaTenantRepository,
            OrganizacaoRepository organizacaoRepository,
            PlanoAssinaturaService planoAssinaturaService) {
        this.assinaturaTenantRepository = assinaturaTenantRepository;
        this.organizacaoRepository = organizacaoRepository;
        this.planoAssinaturaService = planoAssinaturaService;
    }

    /**
     * Cria uma assinatura TRIAL para a organização (30 dias grátis).
     *
     * @param organizacaoId ID da organização
     * @param planoId       ID do plano escolhido
     * @return AssinaturaTenantResponse
     */
    @Transactional
    public AssinaturaTenantResponse assinar(Long organizacaoId, Long planoId) {
        // Verifica se já existe assinatura ativa
        var assinaturaExistente = assinaturaTenantRepository.findAssinaturaAtivaByOrganizacaoId(organizacaoId);
        
        if (assinaturaExistente.isPresent()) {
            // Se já existe, troca o plano em vez de criar nova assinatura
            log.info("Organização {} já possui assinatura. Trocando plano para {}", organizacaoId, planoId);
            return trocarPlanoSuperAdmin(organizacaoId, planoId);
        }

        Organizacao organizacao = organizacaoRepository.findById(organizacaoId)
                .orElseThrow(() -> new BusinessException(
                        "Organização não encontrada: " + organizacaoId,
                        HttpStatus.NOT_FOUND));

        PlanoAssinatura plano = planoAssinaturaService.buscarEntidadePorId(planoId);

        LocalDate hoje = LocalDate.now();
        AssinaturaTenant assinatura = AssinaturaTenant.builder()
                .organizacao(organizacao)
                .planoAssinatura(plano)
                .status(StatusAssinatura.TRIAL)
                .dataInicio(hoje)
                .dataVencimento(hoje.plusDays(DIAS_TRIAL))
                .dataProximaCobranca(hoje.plusDays(DIAS_TRIAL))
                .valorMensal(plano.getValorMensal())
                .build();

        AssinaturaTenant salva = assinaturaTenantRepository.save(assinatura);
        log.info("Assinatura TRIAL criada para org {} no plano {} (vence em {})",
                organizacaoId, plano.getNome(), salva.getDataVencimento());

        return AssinaturaTenantResponse.fromEntity(salva);
    }

    /**
     * Troca o plano da assinatura (usado internamente pelo SUPER_ADMIN).
     * Método auxiliar chamado quando organização já possui assinatura.
     *
     * @param organizacaoId ID da organização
     * @param novoPlanoId   ID do novo plano
     * @return AssinaturaTenantResponse
     */
    private AssinaturaTenantResponse trocarPlanoSuperAdmin(Long organizacaoId, Long novoPlanoId) {
        AssinaturaTenant assinatura = assinaturaTenantRepository
                .findAssinaturaAtivaByOrganizacaoId(organizacaoId)
                .orElseThrow(() -> new BusinessException(
                        "Nenhuma assinatura ativa encontrada para a organização",
                        HttpStatus.NOT_FOUND));

        PlanoAssinatura novoPlano = planoAssinaturaService.buscarEntidadePorId(novoPlanoId);

        assinatura.setPlanoAssinatura(novoPlano);
        assinatura.setValorMensal(novoPlano.getValorMensal());

        AssinaturaTenant salva = assinaturaTenantRepository.save(assinatura);
        log.info("SUPER_ADMIN alterou plano da org {} para {} (R$ {})",
                organizacaoId, novoPlano.getNome(), novoPlano.getValorMensal());

        return AssinaturaTenantResponse.fromEntity(salva);
    }

    /**
     * Troca o plano da assinatura ativa da organização.
     *
     * @param organizacaoId ID da organização
     * @param novoPlanoId   ID do novo plano
     * @return AssinaturaTenantResponse
     */
    @Transactional
    public AssinaturaTenantResponse trocarPlano(Long organizacaoId, Long novoPlanoId) {
        AssinaturaTenant assinatura = assinaturaTenantRepository
                .findAssinaturaAtivaByOrganizacaoId(organizacaoId)
                .orElseThrow(() -> new BusinessException(
                        "Nenhuma assinatura ativa encontrada para a organização",
                        HttpStatus.NOT_FOUND));

        PlanoAssinatura novoPlano = planoAssinaturaService.buscarEntidadePorId(novoPlanoId);

        assinatura.setPlanoAssinatura(novoPlano);
        assinatura.setValorMensal(novoPlano.getValorMensal());

        AssinaturaTenant salva = assinaturaTenantRepository.save(assinatura);
        log.info("Plano da org {} alterado para {} (R$ {})",
                organizacaoId, novoPlano.getNome(), novoPlano.getValorMensal());

        return AssinaturaTenantResponse.fromEntity(salva);
    }

    /**
     * Ativa a assinatura após primeiro pagamento (TRIAL → ATIVA).
     *
     * @param organizacaoId ID da organização
     * @return AssinaturaTenantResponse
     */
    @Transactional
    public AssinaturaTenantResponse ativar(Long organizacaoId) {
        AssinaturaTenant assinatura = assinaturaTenantRepository
                .findAssinaturaAtivaByOrganizacaoId(organizacaoId)
                .orElseThrow(() -> new BusinessException(
                        "Nenhuma assinatura encontrada para a organização",
                        HttpStatus.NOT_FOUND));

        assinatura.setStatus(StatusAssinatura.ATIVA);
        assinatura.setDataVencimento(LocalDate.now().plusMonths(1));
        assinatura.setDataProximaCobranca(LocalDate.now().plusMonths(1));

        AssinaturaTenant salva = assinaturaTenantRepository.save(assinatura);
        log.info("Assinatura da org {} ativada. Próximo vencimento: {}",
                organizacaoId, salva.getDataVencimento());

        return AssinaturaTenantResponse.fromEntity(salva);
    }

    /**
     * Marca a assinatura como INADIMPLENTE (acesso com aviso).
     *
     * @param assinaturaId ID da assinatura
     */
    @Transactional
    public void marcarInadimplente(Long assinaturaId) {
        AssinaturaTenant assinatura = buscarPorId(assinaturaId);
        assinatura.setStatus(StatusAssinatura.INADIMPLENTE);
        assinaturaTenantRepository.save(assinatura);
        log.warn("Assinatura {} marcada como INADIMPLENTE (org: {})",
                assinaturaId, assinatura.getOrganizacao().getId());
    }

    /**
     * Suspende a assinatura (bloqueia login).
     *
     * @param assinaturaId ID da assinatura
     */
    @Transactional
    public void suspender(Long assinaturaId) {
        AssinaturaTenant assinatura = buscarPorId(assinaturaId);
        assinatura.setStatus(StatusAssinatura.SUSPENSA);
        assinaturaTenantRepository.save(assinatura);
        log.warn("Assinatura {} SUSPENSA (org: {})",
                assinaturaId, assinatura.getOrganizacao().getId());
    }

    /**
     * Cancela a assinatura definitivamente.
     *
     * @param assinaturaId ID da assinatura
     */
    @Transactional
    public void cancelar(Long assinaturaId) {
        AssinaturaTenant assinatura = buscarPorId(assinaturaId);
        assinatura.setStatus(StatusAssinatura.CANCELADA);
        assinaturaTenantRepository.save(assinatura);
        log.warn("Assinatura {} CANCELADA (org: {})",
                assinaturaId, assinatura.getOrganizacao().getId());
    }

    /**
     * Busca a assinatura ativa da organização.
     *
     * @param organizacaoId ID da organização
     * @return AssinaturaTenantResponse
     */
    @Transactional(readOnly = true)
    public AssinaturaTenantResponse buscarAssinaturaAtiva(Long organizacaoId) {
        AssinaturaTenant assinatura = assinaturaTenantRepository
                .findAssinaturaAtivaByOrganizacaoId(organizacaoId)
                .orElseThrow(() -> new BusinessException(
                        "Nenhuma assinatura ativa encontrada para a organização",
                        HttpStatus.NOT_FOUND));
        return AssinaturaTenantResponse.fromEntity(assinatura);
    }

    /**
     * Lista todas as assinaturas (para SUPER_ADMIN).
     *
     * @return lista de AssinaturaTenantResponse
     */
    @Transactional(readOnly = true)
    public List<AssinaturaTenantResponse> listarTodas() {
        return assinaturaTenantRepository.findAllWithRelations()
                .stream()
                .map(AssinaturaTenantResponse::fromEntity)
                .toList();
    }

    /**
     * Lista assinaturas de uma organização (histórico).
     *
     * @param organizacaoId ID da organização
     * @return lista de AssinaturaTenantResponse
     */
    @Transactional(readOnly = true)
    public List<AssinaturaTenantResponse> listarPorOrganizacao(Long organizacaoId) {
        return assinaturaTenantRepository.findByOrganizacaoId(organizacaoId)
                .stream()
                .map(AssinaturaTenantResponse::fromEntity)
                .toList();
    }

    /**
     * Renova o vencimento da assinatura após pagamento confirmado.
     *
     * @param assinaturaId ID da assinatura
     */
    @Transactional
    public void renovarAposPagamento(Long assinaturaId) {
        AssinaturaTenant assinatura = buscarPorId(assinaturaId);
        assinatura.setStatus(StatusAssinatura.ATIVA);
        assinatura.setDataVencimento(LocalDate.now().plusMonths(1));
        assinatura.setDataProximaCobranca(LocalDate.now().plusMonths(1));
        assinaturaTenantRepository.save(assinatura);
        log.info("Assinatura {} renovada. Próximo vencimento: {}",
                assinaturaId, assinatura.getDataVencimento());
    }

    /**
     * Customiza os limites do plano para um tenant específico (SUPER_ADMIN).
     * Calcula automaticamente o valor adicional baseado nos perfis extras.
     *
     * @param organizacaoId ID da organização
     * @param request       limites customizados desejados
     * @return AssinaturaTenantResponse atualizada
     */
    @Transactional
    public AssinaturaTenantResponse customizarPlano(Long organizacaoId, CustomizarPlanoTenantRequest request) {
        AssinaturaTenant assinatura = assinaturaTenantRepository
                .findAssinaturaAtivaByOrganizacaoId(organizacaoId)
                .orElseThrow(() -> new BusinessException(
                        "Nenhuma assinatura ativa encontrada para a organização",
                        HttpStatus.NOT_FOUND));

        PlanoAssinatura plano = assinatura.getPlanoAssinatura();

        // Calcular extras para cada perfil
        int extraAdmin = calcularExtras(request.limiteAdminOrgCustom(), plano.getLimiteAdminOrg());
        int extraProf = calcularExtras(request.limiteProfissionalCustom(), plano.getLimiteProfissional());
        int extraSec = calcularExtras(request.limiteSecretariaCustom(), plano.getLimiteSecretaria());

        // Calcular valor adicional
        BigDecimal valorAdicional = BigDecimal.ZERO;
        if (extraAdmin > 0 && plano.getValorAdicionalAdmin() != null) {
            valorAdicional = valorAdicional.add(plano.getValorAdicionalAdmin().multiply(BigDecimal.valueOf(extraAdmin)));
        }
        if (extraProf > 0 && plano.getValorAdicionalProfissional() != null) {
            valorAdicional = valorAdicional.add(plano.getValorAdicionalProfissional().multiply(BigDecimal.valueOf(extraProf)));
        }
        if (extraSec > 0 && plano.getValorAdicionalSecretaria() != null) {
            valorAdicional = valorAdicional.add(plano.getValorAdicionalSecretaria().multiply(BigDecimal.valueOf(extraSec)));
        }

        // Atualizar assinatura
        assinatura.setLimiteAdminOrgCustom(request.limiteAdminOrgCustom());
        assinatura.setLimiteProfissionalCustom(request.limiteProfissionalCustom());
        assinatura.setLimiteSecretariaCustom(request.limiteSecretariaCustom());
        assinatura.setQtdAdminExtra(extraAdmin);
        assinatura.setQtdProfissionalExtra(extraProf);
        assinatura.setQtdSecretariaExtra(extraSec);
        assinatura.setValorAdicionalPerfis(valorAdicional);
        assinatura.setValorMensal(plano.getValorMensal().add(valorAdicional));

        AssinaturaTenant salva = assinaturaTenantRepository.save(assinatura);
        log.info("Plano customizado para org {}: +{} admin, +{} prof, +{} sec | Adicional: R${} | Total: R${}",
                organizacaoId, extraAdmin, extraProf, extraSec, valorAdicional, salva.getValorMensal());

        return AssinaturaTenantResponse.fromEntity(salva);
    }

    /**
     * Calcula a quantidade de perfis extras com base no limite custom vs. limite do plano.
     *
     * @param limiteCustom limite personalizado (pode ser null = sem customização)
     * @param limitePlano  limite padrão do plano (pode ser null = ilimitado)
     * @return quantidade de extras (>= 0)
     */
    private int calcularExtras(Integer limiteCustom, Integer limitePlano) {
        if (limiteCustom == null || limitePlano == null) {
            return 0;
        }
        return Math.max(0, limiteCustom - limitePlano);
    }

    /**
     * Busca entidade AssinaturaTenant por ID (uso interno).
     */
    private AssinaturaTenant buscarPorId(Long id) {
        return assinaturaTenantRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "Assinatura não encontrada: " + id,
                        HttpStatus.NOT_FOUND));
    }
}
