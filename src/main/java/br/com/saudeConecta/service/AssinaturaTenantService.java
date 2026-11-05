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
import br.com.saudeConecta.service.mercadopago.AssinaturaMercadoPagoService;
import br.com.saudeConecta.service.mercadopago.MercadoPagoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AssinaturaTenantService {

    private static final Logger log = LoggerFactory.getLogger(AssinaturaTenantService.class);
    private static final int DIAS_TRIAL = 30;
    /** Prazo até o novo vencimento ao remover a cortesia: o mesmo aviso que o trial recebe. */
    private static final int DIAS_PARA_PAGAR_APOS_CORTESIA = 7;

    private final AssinaturaTenantRepository assinaturaTenantRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final PlanoAssinaturaService planoAssinaturaService;
    private final AssinaturaMercadoPagoService assinaturaMercadoPagoService;
    /** Relógio em America/Sao_Paulo: "hoje", vencimentos e o dia âncora seguem o dia de Brasília. */
    private final Clock clock;

    public AssinaturaTenantService(
            AssinaturaTenantRepository assinaturaTenantRepository,
            OrganizacaoRepository organizacaoRepository,
            PlanoAssinaturaService planoAssinaturaService,
            AssinaturaMercadoPagoService assinaturaMercadoPagoService,
            Clock clock) {
        this.clock = clock;
        this.assinaturaTenantRepository = assinaturaTenantRepository;
        this.organizacaoRepository = organizacaoRepository;
        this.planoAssinaturaService = planoAssinaturaService;
        this.assinaturaMercadoPagoService = assinaturaMercadoPagoService;
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
        return assinar(organizacaoId, planoId, false);
    }

    /**
     * Cria uma assinatura para a organização.
     * Assinaturas de cortesia nascem ATIVAS e ficam fora da rotina de cobrança;
     * as demais nascem em TRIAL de 30 dias, sem exigir cartão.
     *
     * @param organizacaoId ID da organização
     * @param planoId       ID do plano escolhido
     * @param cortesia      true para liberar o acesso sem cobrança
     * @return AssinaturaTenantResponse
     */
    @Transactional
    public AssinaturaTenantResponse assinar(Long organizacaoId, Long planoId, boolean cortesia) {
        log.info("assinatura.criar inicio organizacaoId={} planoId={} cortesia={}", organizacaoId, planoId, cortesia);
        // Verifica se já existe assinatura ativa
        var assinaturaExistente = assinaturaTenantRepository.findAssinaturaAtivaByOrganizacaoId(organizacaoId);
        
        if (assinaturaExistente.isPresent()) {
            // Se já existe, troca o plano em vez de criar nova assinatura
            log.info("assinatura.criar ignorado motivo=JA_POSSUI_ASSINATURA organizacaoId={} planoId={} acao=TROCAR_PLANO",
                    organizacaoId, planoId);
            return trocarPlanoSuperAdmin(organizacaoId, planoId);
        }

        Organizacao organizacao = organizacaoRepository.findById(organizacaoId)
                .orElseThrow(() -> new BusinessException(
                        "Organização não encontrada: " + organizacaoId,
                        HttpStatus.NOT_FOUND));

        PlanoAssinatura plano = planoAssinaturaService.buscarEntidadePorId(planoId);

        LocalDate hoje = LocalDate.now(clock);
        AssinaturaTenant assinatura = AssinaturaTenant.builder()
                .organizacao(organizacao)
                .planoAssinatura(plano)
                .status(cortesia ? StatusAssinatura.ATIVA : StatusAssinatura.TRIAL)
                .cortesia(cortesia)
                .dataInicio(hoje)
                .dataVencimento(hoje.plusDays(DIAS_TRIAL))
                .dataProximaCobranca(cortesia ? null : hoje.plusDays(DIAS_TRIAL))
                .valorMensal(plano.getValorMensal())
                .qtdAdminExtra(0)
                .qtdProfissionalExtra(0)
                .qtdSecretariaExtra(0)
                .valorAdicionalPerfis(BigDecimal.ZERO)
                .build();

        AssinaturaTenant salva = assinaturaTenantRepository.save(assinatura);
        log.info("assinatura.criar ok assinaturaId={} organizacaoId={} plano={} status={} vencimento={}",
                salva.getId(), organizacaoId, plano.getNome(), salva.getStatus(), salva.getDataVencimento());

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
        log.info("assinatura.trocarPlano ok origem=SUPER_ADMIN organizacaoId={} plano={} valorMensal={}",
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
        log.info("assinatura.trocarPlano inicio organizacaoId={} novoPlanoId={}", organizacaoId, novoPlanoId);
        AssinaturaTenant assinatura = assinaturaTenantRepository
                .findAssinaturaAtivaByOrganizacaoId(organizacaoId)
                .orElseThrow(() -> new BusinessException(
                        "Nenhuma assinatura ativa encontrada para a organização",
                        HttpStatus.NOT_FOUND));

        PlanoAssinatura novoPlano = planoAssinaturaService.buscarEntidadePorId(novoPlanoId);

        assinatura.setPlanoAssinatura(novoPlano);
        assinatura.setValorMensal(novoPlano.getValorMensal());

        AssinaturaTenant salva = assinaturaTenantRepository.save(assinatura);
        log.info("assinatura.trocarPlano ok organizacaoId={} plano={} valorMensal={}",
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
        log.info("assinatura.ativar inicio organizacaoId={}", organizacaoId);
        AssinaturaTenant assinatura = assinaturaTenantRepository
                .findAssinaturaAtivaByOrganizacaoId(organizacaoId)
                .orElseThrow(() -> new BusinessException(
                        "Nenhuma assinatura encontrada para a organização",
                        HttpStatus.NOT_FOUND));

        assinatura.setStatus(StatusAssinatura.ATIVA);
        assinatura.setDataVencimento(LocalDate.now(clock).plusMonths(1));
        assinatura.setDataProximaCobranca(LocalDate.now(clock).plusMonths(1));

        AssinaturaTenant salva = assinaturaTenantRepository.save(assinatura);
        log.info("assinatura.ativar ok organizacaoId={} vencimento={}",
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
        log.info("assinatura.inadimplente ok assinaturaId={} organizacaoId={} status=INADIMPLENTE",
                assinaturaId, assinatura.getOrganizacao().getId());
    }

    /**
     * Suspende a assinatura (bloqueia login) e registra a data da suspensão,
     * que conta o prazo até o cancelamento automático.
     *
     * @param assinaturaId ID da assinatura
     */
    @Transactional
    public void suspender(Long assinaturaId) {
        log.info("assinatura.suspender inicio assinaturaId={}", assinaturaId);
        AssinaturaTenant assinatura = buscarPorId(assinaturaId);
        assinatura.setStatus(StatusAssinatura.SUSPENSA);
        assinatura.setDataSuspensao(LocalDateTime.now(clock));
        assinaturaTenantRepository.save(assinatura);
        log.info("assinatura.suspender ok assinaturaId={} organizacaoId={} status=SUSPENSA",
                assinaturaId, assinatura.getOrganizacao().getId());
    }

    /**
     * Cancela a assinatura definitivamente.
     *
     * @param assinaturaId ID da assinatura
     */
    @Transactional
    public void cancelar(Long assinaturaId) {
        log.info("assinatura.cancelar inicio assinaturaId={}", assinaturaId);
        AssinaturaTenant assinatura = buscarPorId(assinaturaId);
        assinatura.setStatus(StatusAssinatura.CANCELADA);
        assinaturaTenantRepository.save(assinatura);
        log.info("assinatura.cancelar ok assinaturaId={} organizacaoId={} status=CANCELADA",
                assinaturaId, assinatura.getOrganizacao().getId());
    }

    /**
     * Busca a assinatura vigente da organização (inclui a SUSPENSA, que precisa ver a
     * própria assinatura para regularizar o pagamento).
     *
     * @param organizacaoId ID da organização
     * @return AssinaturaTenantResponse
     */
    @Transactional(readOnly = true)
    public AssinaturaTenantResponse buscarAssinaturaVigente(Long organizacaoId) {
        AssinaturaTenant assinatura = assinaturaTenantRepository
                .findAssinaturaVigenteByOrganizacaoId(organizacaoId)
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
     * Renova o vencimento da assinatura após pagamento confirmado,
     * mantendo o dia âncora do ciclo de cobrança.
     *
     * @param assinaturaId ID da assinatura
     */
    @Transactional
    public void renovarAposPagamento(Long assinaturaId) {
        AssinaturaTenant assinatura = buscarPorId(assinaturaId);
        assinatura.setStatus(StatusAssinatura.ATIVA);
        assinatura.setDataSuspensao(null);
        // Âncora: data do processamento no fuso de Brasília. O DTO da fatura do MP não traz
        // debit_date/date_created, então não há data "oficial" melhor para ancorar.
        assinatura.renovarCiclo(LocalDate.now(clock));
        assinaturaTenantRepository.save(assinatura);
        log.info("assinatura.renovar ok assinaturaId={} vencimento={}",
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
        log.info("assinatura.customizar inicio organizacaoId={} limAdmin={} limProf={} limSec={}",
                organizacaoId, request.limiteAdminOrgCustom(),
                request.limiteProfissionalCustom(), request.limiteSecretariaCustom());
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
        log.info("assinatura.customizar ok organizacaoId={} extraAdmin={} extraProf={} extraSec={} valorAdicional={} valorTotal={}",
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
     * Marca ou remove a condição de cortesia da assinatura (SUPER_ADMIN).
     * Assinaturas de cortesia ficam fora de toda a rotina de cobrança.
     *
     * Ao remover a cortesia (assinatura não cancelada):
     * <ul>
     *   <li>com cartão já autorizado no Mercado Pago, o vencimento passa a ser a próxima
     *       cobrança agendada no MP (next_payment_date) e o status é mantido — o próprio MP
     *       cobra nessa data; se o MP não responder, usa hoje + 7 e mantém o status;</li>
     *   <li>sem cartão autorizado, a assinatura volta para TRIAL com vencimento hoje + 7.
     *       O link de pagamento é gerado logo após o commit (fora desta transação) e, se o
     *       MP falhar, a rotina diária de links de trial ({@code findTrialsSemLinkVencendoAte},
     *       que também pega TRIAL com preapproval não autorizado) gera depois. Um preapproval
     *       antigo não pendente é cancelado no MP antes de o novo ser criado.</li>
     * </ul>
     *
     * A única chamada ao MP dentro desta transação é o GET do next_payment_date (cartão
     * autorizado): é leitura, não trava a linha (o UPDATE só sai no commit, protegido por
     * {@code @Version}) e é limitada pelo read timeout do RestTemplate.
     *
     * @param assinaturaId ID da assinatura
     * @param cortesia     true para isentar de cobrança
     * @return AssinaturaTenantResponse atualizada
     */
    @Transactional
    public AssinaturaTenantResponse definirCortesia(Long assinaturaId, boolean cortesia) {
        log.info("assinatura.cortesia inicio assinaturaId={} cortesia={}", assinaturaId, cortesia);
        AssinaturaTenant assinatura = buscarPorId(assinaturaId);
        boolean removendoCortesia = !cortesia && assinatura.isCortesia()
                && !StatusAssinatura.CANCELADA.equals(assinatura.getStatus());
        assinatura.setCortesia(cortesia);
        if (cortesia && StatusAssinatura.TRIAL.equals(assinatura.getStatus())) {
            assinatura.setStatus(StatusAssinatura.ATIVA);
        }
        boolean precisaDeLink = removendoCortesia && !aplicarVencimentoAposCortesia(assinatura);

        AssinaturaTenant salva = assinaturaTenantRepository.save(assinatura);
        log.info("assinatura.cortesia ok assinaturaId={} cortesia={} organizacaoId={} status={} vencimento={}",
                assinaturaId, cortesia, salva.getOrganizacao().getId(), salva.getStatus(), salva.getDataVencimento());

        if (precisaDeLink) {
            enviarLinkDePagamentoAposCommit(assinaturaId);
        }

        return AssinaturaTenantResponse.fromEntity(salva);
    }

    /**
     * Define vencimento e status de quem perde a cortesia.
     *
     * @return true quando a assinatura já tem cartão autorizado no MP (não precisa de link)
     */
    private boolean aplicarVencimentoAposCortesia(AssinaturaTenant assinatura) {
        LocalDate prazoParaPagar = LocalDate.now(clock).plusDays(DIAS_PARA_PAGAR_APOS_CORTESIA);

        if (assinatura.temPreapproval() && MercadoPagoClient.STATUS_AUTHORIZED.equals(assinatura.getMpStatus())) {
            try {
                LocalDate proximaCobranca = assinaturaMercadoPagoService.buscarProximaCobrancaAutorizada(assinatura);
                if (proximaCobranca != null) {
                    definirVencimento(assinatura, proximaCobranca);
                    return true;
                }
                // MP respondeu que o preapproval não está mais autorizado: segue como sem cartão
            } catch (BusinessException e) {
                log.warn("assinatura.cortesia negado motivo=MP_INDISPONIVEL assinaturaId={} causa={} vencimento={}",
                        assinatura.getId(), e.getClass().getSimpleName(), prazoParaPagar);
                definirVencimento(assinatura, prazoParaPagar);
                return true;
            }
        }

        assinatura.setStatus(StatusAssinatura.TRIAL);
        assinatura.setDataSuspensao(null);
        definirVencimento(assinatura, prazoParaPagar);
        return false;
    }

    private static void definirVencimento(AssinaturaTenant assinatura, LocalDate vencimento) {
        assinatura.setDataVencimento(vencimento);
        assinatura.setDataProximaCobranca(vencimento);
    }

    /**
     * O link é gerado depois do commit: a chamada HTTP ao Mercado Pago fica fora da
     * transação, a nova transação já enxerga cortesia=false e uma falha no MP não
     * desfaz a remoção da cortesia (a rotina diária de links de trial tenta de novo).
     */
    private void enviarLinkDePagamentoAposCommit(Long assinaturaId) {
        Runnable envio = () -> {
            try {
                assinaturaMercadoPagoService.gerarLinkENotificar(assinaturaId);
            } catch (Exception e) {
                log.error("assinatura.cortesia.link falha assinaturaId={}", assinaturaId, e);
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    envio.run();
                }
            });
        } else {
            envio.run();
        }
    }

    /**
     * Busca a entidade AssinaturaTenant por ID.
     *
     * @param id ID da assinatura
     * @return entidade encontrada
     */
    @Transactional(readOnly = true)
    public AssinaturaTenant buscarEntidadePorId(Long id) {
        return buscarPorId(id);
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
