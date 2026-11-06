package br.com.saudeConecta.application.assinatura;

import br.com.saudeConecta.application.assinatura.dto.AssinaturaPlataformaResponse;
import br.com.saudeConecta.application.assinatura.dto.AssinaturaTenantResponse;
import br.com.saudeConecta.application.assinatura.dto.CustomizarPlanoTenantRequest;
import br.com.saudeConecta.application.exception.BusinessException;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.planos.AssinaturaTenant;
import br.com.saudeConecta.domain.planos.OrigemCancelamento;
import br.com.saudeConecta.domain.planos.PlanoAssinatura;
import br.com.saudeConecta.domain.planos.StatusAssinatura;
import br.com.saudeConecta.domain.planos.TipoPlano;
import br.com.saudeConecta.infrastructure.mercadopago.MercadoPagoClient;
import br.com.saudeConecta.infrastructure.persistence.repository.AssinaturaTenantRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.OrganizacaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AssinaturaTenantService {
    private static final String MSG_ASSINATURA_NAO_ENCONTRADA = "Assinatura não encontrada: ";

    private static final Logger log = LoggerFactory.getLogger(AssinaturaTenantService.class);
    /** Código do 409 ao associar plano a uma clínica com assinatura SUSPENSA vigente. */
    public static final String CODE_ASSINATURA_SUSPENSA = "ASSINATURA_SUSPENSA";
    /** Código do 409 ao suspender uma assinatura CANCELADA. */
    public static final String CODE_ASSINATURA_CANCELADA = "ASSINATURA_CANCELADA";
    private static final int DIAS_TRIAL = 30;
    /** Prazo até o novo vencimento ao remover a cortesia: o mesmo aviso que o trial recebe. */
    private static final int DIAS_PARA_PAGAR_APOS_CORTESIA = 7;

    private final AssinaturaTenantRepository assinaturaTenantRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final PlanoAssinaturaService planoAssinaturaService;
    private final AssinaturaMercadoPagoService assinaturaMercadoPagoService;
    /**
     * Pede o link de pagamento ao {@code LinkPagamentoAssinaturaService} por evento: depender
     * dele direto formaria ciclo (ele depende do webhook, que depende deste serviço).
     */
    private final ApplicationEventPublisher eventos;
    /** Relógio em America/Sao_Paulo: "hoje", vencimentos e o dia âncora seguem o dia de Brasília. */
    private final Clock clock;

    public AssinaturaTenantService(
            AssinaturaTenantRepository assinaturaTenantRepository,
            OrganizacaoRepository organizacaoRepository,
            PlanoAssinaturaService planoAssinaturaService,
            AssinaturaMercadoPagoService assinaturaMercadoPagoService,
            ApplicationEventPublisher eventos,
            Clock clock) {
        this.eventos = eventos;
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

        // SUSPENSA vigente: uma TRIAL nova daria 30 dias grátis por cima da dívida e deixaria duas
        // assinaturas vivas. Para liberar, o SUPER_ADMIN dá cortesia ou cancela antes.
        boolean suspensa = assinaturaTenantRepository.findAssinaturaVigenteByOrganizacaoId(organizacaoId)
                .filter(a -> StatusAssinatura.SUSPENSA.equals(a.getStatus()))
                .isPresent();
        if (suspensa) {
            log.warn("assinatura.criar negado motivo=ASSINATURA_SUSPENSA organizacaoId={} planoId={}", organizacaoId, planoId);
            throw new BusinessException(
                    "A clínica tem uma assinatura suspensa. Dê cortesia ou cancele antes de associar um plano.",
                    HttpStatus.CONFLICT, CODE_ASSINATURA_SUSPENSA);
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
        AssinaturaTenant salva = aplicarTrocaDePlano(organizacaoId, novoPlanoId);
        log.info("assinatura.trocarPlano ok origem=SUPER_ADMIN organizacaoId={} plano={} valorMensal={}",
                organizacaoId, salva.getPlanoAssinatura().getNome(), salva.getValorMensal());

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
        AssinaturaTenant salva = aplicarTrocaDePlano(organizacaoId, novoPlanoId);
        log.info("assinatura.trocarPlano ok organizacaoId={} plano={} valorMensal={}",
                organizacaoId, salva.getPlanoAssinatura().getNome(), salva.getValorMensal());

        return AssinaturaTenantResponse.fromEntity(salva);
    }

    /**
     * Troca o plano mantendo os limites personalizados: os extras são recalculados contra os
     * limites do plano novo e cobrados (preço do plano + extras × valor adicional). O valor
     * novo vale a partir do próximo ciclo e é levado ao Mercado Pago antes do commit; se o MP
     * falhar, a exceção desfaz a troca.
     */
    private AssinaturaTenant aplicarTrocaDePlano(Long organizacaoId, Long novoPlanoId) {
        AssinaturaTenant assinatura = buscarAtivaParaAtualizacao(organizacaoId);
        PlanoAssinatura novoPlano = planoAssinaturaService.buscarEntidadePorId(novoPlanoId);
        BigDecimal valorAnterior = assinatura.getValorMensal();

        assinatura.setPlanoAssinatura(novoPlano);
        aplicarLimitesEValor(assinatura, assinatura.getLimiteAdminOrgCustom(),
                assinatura.getLimiteProfissionalCustom(), assinatura.getLimiteSecretariaCustom());

        return salvarEPropagarValor(assinatura, valorAnterior);
    }

    /**
     * Assinatura ativa da organização, relida com lock de escrita: serializa a troca de valor
     * com a geração de link (que lê o valor para criar o preapproval) e com o webhook.
     */
    private AssinaturaTenant buscarAtivaParaAtualizacao(Long organizacaoId) {
        Long assinaturaId = assinaturaTenantRepository
                .findAssinaturaAtivaByOrganizacaoId(organizacaoId)
                .map(AssinaturaTenant::getId)
                .orElseThrow(() -> new BusinessException(
                        "Nenhuma assinatura ativa encontrada para a organização",
                        HttpStatus.NOT_FOUND));
        return assinaturaTenantRepository.findByIdParaAtualizacao(assinaturaId)
                .orElseThrow(() -> new BusinessException(
                        MSG_ASSINATURA_NAO_ENCONTRADA + assinaturaId, HttpStatus.NOT_FOUND));
    }

    /**
     * Grava e só então chama o MP: um erro de banco aparece antes de mudar o valor lá fora.
     * Se o MP falhar, a exceção desfaz a transação inteira.
     */
    private AssinaturaTenant salvarEPropagarValor(AssinaturaTenant assinatura, BigDecimal valorAnterior) {
        AssinaturaTenant salva = assinaturaTenantRepository.saveAndFlush(assinatura);
        assinaturaMercadoPagoService.propagarNovoValor(salva, valorAnterior);
        return salva;
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
     * Suspensão pelo SUPER_ADMIN: com a linha travada (como a régua), grava SUSPENSA e pausa o
     * preapproval no Mercado Pago na mesma transação. Falha do MP não desfaz a suspensão local
     * (log ERROR; a reconciliação mantém o espelho). Já SUSPENSA: só tenta pausar de novo.
     *
     * @param assinaturaId ID da assinatura
     * @throws BusinessException 404 inexistente; 409 {@code ASSINATURA_CANCELADA}
     */
    @Transactional
    public void suspenderPeloSuperAdmin(Long assinaturaId) {
        log.info("assinatura.suspender inicio origem=SUPER_ADMIN assinaturaId={}", assinaturaId);
        AssinaturaTenant assinatura = buscarParaAtualizacao(assinaturaId);
        if (StatusAssinatura.CANCELADA.equals(assinatura.getStatus())) {
            log.warn("assinatura.suspender negado motivo=ASSINATURA_CANCELADA assinaturaId={}", assinaturaId);
            throw new BusinessException("Assinatura cancelada não pode ser suspensa.",
                    HttpStatus.CONFLICT, CODE_ASSINATURA_CANCELADA);
        }
        if (!StatusAssinatura.SUSPENSA.equals(assinatura.getStatus())) {
            assinatura.setStatus(StatusAssinatura.SUSPENSA);
            assinatura.setDataSuspensao(LocalDateTime.now(clock));
            assinaturaTenantRepository.save(assinatura);
        }
        assinaturaMercadoPagoService.pausar(assinatura);
        log.info("assinatura.suspender ok origem=SUPER_ADMIN assinaturaId={} organizacaoId={} status=SUSPENSA mpStatus={}",
                assinaturaId, assinatura.getOrganizacao().getId(), assinatura.getMpStatus());
    }

    /**
     * Cancelamento pelo SUPER_ADMIN: com a linha travada, cancela o preapproval no Mercado Pago e
     * então a assinatura (CANCELADA, canceladoPor = SUPER_ADMIN). Se o MP falhar, nada muda (502).
     * Já CANCELADA: não faz nada.
     *
     * @param assinaturaId ID da assinatura
     * @throws BusinessException 404 inexistente; 502/503 Mercado Pago indisponível
     */
    @Transactional
    public void cancelarPeloSuperAdmin(Long assinaturaId) {
        log.info("assinatura.cancelar inicio origem=SUPER_ADMIN assinaturaId={}", assinaturaId);
        assinaturaMercadoPagoService.cancelar(buscarParaAtualizacao(assinaturaId), OrigemCancelamento.SUPER_ADMIN);
    }

    private AssinaturaTenant buscarParaAtualizacao(Long assinaturaId) {
        return assinaturaTenantRepository.findByIdParaAtualizacao(assinaturaId)
                .orElseThrow(() -> new BusinessException(
                        MSG_ASSINATURA_NAO_ENCONTRADA + assinaturaId, HttpStatus.NOT_FOUND));
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
     * Tela de assinaturas do SUPER_ADMIN: a assinatura vigente (mais recente) de cada clínica,
     * paginada e com filtros opcionais. Clínica sem nenhuma assinatura não aparece.
     *
     * @param status   status (null = todos)
     * @param plano    tipo do plano base (null = todos)
     * @param cortesia true/false (null = todas)
     * @param busca    trecho do nome da clínica, sem diferenciar maiúsculas (null ou vazio = sem filtro)
     * @param pageable página (0-based), tamanho (máx. {@value #TAMANHO_MAXIMO_PAGINA}) e ordenação pelos
     *                 campos de {@link #ORDENACAO_PERMITIDA}; sem ordenação = nome da clínica
     * @return página de linhas sem dado pessoal
     * @throws IllegalArgumentException campo de ordenação fora da lista (400)
     */
    @Transactional(readOnly = true)
    public Page<AssinaturaPlataformaResponse> listarParaPlataforma(StatusAssinatura status, TipoPlano plano,
                                                                  Boolean cortesia, String busca, Pageable pageable) {
        Sort ordem = Sort.unsorted();
        for (Sort.Order o : pageable.getSort()) {
            String campo = ORDENACAO_PERMITIDA.get(o.getProperty());
            if (campo == null) {
                throw new IllegalArgumentException("Ordenação não permitida: " + o.getProperty()
                        + ". Use um de " + ORDENACAO_PERMITIDA.keySet());
            }
            ordem = ordem.and(Sort.by(o.getDirection(), campo));
        }
        if (ordem.isUnsorted()) {
            ordem = Sort.by("organizacao.nome");
        }
        // Desempate estável entre páginas
        ordem = ordem.and(Sort.by("id"));
        Pageable pagina = PageRequest.of(pageable.getPageNumber(),
                Math.min(Math.max(pageable.getPageSize(), 1), TAMANHO_MAXIMO_PAGINA), ordem);

        return assinaturaTenantRepository
                .listarVigentesParaPlataforma(status, plano, cortesia, padraoDeBusca(busca), pagina)
                .map(AssinaturaPlataformaResponse::fromEntity);
    }

    /** Nome do campo na API -> caminho na entidade (lista branca: campo livre viraria 500 do Hibernate). */
    private static final Map<String, String> ORDENACAO_PERMITIDA = Map.of(
            "organizacaoNome", "organizacao.nome",
            "status", "status",
            "plano", "planoAssinatura.tipo",
            "valorMensal", "valorMensal",
            "dataVencimento", "dataVencimento",
            "criadoEm", "criadoEm");
    private static final int TAMANHO_MAXIMO_PAGINA = 100;

    /** LIKE em minúsculas com % e _ do usuário escapados por '!' (o mesmo ESCAPE da query). */
    private static String padraoDeBusca(String busca) {
        if (busca == null || busca.isBlank()) {
            return null;
        }
        String escapada = busca.trim().toLowerCase(Locale.ROOT)
                .replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return "%" + escapada + "%";
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
        AssinaturaTenant assinatura = buscarAtivaParaAtualizacao(organizacaoId);
        BigDecimal valorAnterior = assinatura.getValorMensal();

        aplicarLimitesEValor(assinatura, request.limiteAdminOrgCustom(),
                request.limiteProfissionalCustom(), request.limiteSecretariaCustom());

        AssinaturaTenant salva = salvarEPropagarValor(assinatura, valorAnterior);
        log.info("assinatura.customizar ok organizacaoId={} extraAdmin={} extraProf={} extraSec={} valorAdicional={} valorTotal={}",
                organizacaoId, salva.getQtdAdminExtra(), salva.getQtdProfissionalExtra(), salva.getQtdSecretariaExtra(),
                salva.getValorAdicionalPerfis(), salva.getValorMensal());

        return AssinaturaTenantResponse.fromEntity(salva);
    }

    /**
     * Grava os limites personalizados e recalcula extras e valor mensal contra o plano atual
     * da assinatura: valor = preço do plano + extras × valor adicional de cada perfil.
     */
    private void aplicarLimitesEValor(AssinaturaTenant assinatura, Integer limiteAdmin,
                                      Integer limiteProfissional, Integer limiteSecretaria) {
        PlanoAssinatura plano = assinatura.getPlanoAssinatura();

        int extraAdmin = calcularExtras(limiteAdmin, plano.getLimiteAdminOrg());
        int extraProf = calcularExtras(limiteProfissional, plano.getLimiteProfissional());
        int extraSec = calcularExtras(limiteSecretaria, plano.getLimiteSecretaria());

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

        assinatura.setLimiteAdminOrgCustom(limiteAdmin);
        assinatura.setLimiteProfissionalCustom(limiteProfissional);
        assinatura.setLimiteSecretariaCustom(limiteSecretaria);
        assinatura.setQtdAdminExtra(extraAdmin);
        assinatura.setQtdProfissionalExtra(extraProf);
        assinatura.setQtdSecretariaExtra(extraSec);
        assinatura.setValorAdicionalPerfis(valorAdicional);
        assinatura.setValorMensal(plano.getValorMensal().add(valorAdicional));
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
        // Lock: serializa com a geração de link, que relê a cortesia com a linha travada
        AssinaturaTenant assinatura = assinaturaTenantRepository.findByIdParaAtualizacao(assinaturaId)
                .orElseThrow(() -> new BusinessException(
                        MSG_ASSINATURA_NAO_ENCONTRADA + assinaturaId, HttpStatus.NOT_FOUND));
        boolean cancelada = StatusAssinatura.CANCELADA.equals(assinatura.getStatus());
        boolean removendoCortesia = !cortesia && assinatura.isCortesia() && !cancelada;
        assinatura.setCortesia(cortesia);
        if (cortesia && !cancelada) {
            liberarComoCortesia(assinatura);
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
     * Cortesia libera o acesso (ATIVA, inclusive vinda de SUSPENSA ou INADIMPLENTE) e para a
     * cobrança: o preapproval é desvinculado e cancelado no MP depois do commit.
     */
    private void liberarComoCortesia(AssinaturaTenant assinatura) {
        StatusAssinatura status = assinatura.getStatus();
        if (StatusAssinatura.TRIAL.equals(status) || StatusAssinatura.SUSPENSA.equals(status)
                || StatusAssinatura.INADIMPLENTE.equals(status)) {
            assinatura.setStatus(StatusAssinatura.ATIVA);
            assinatura.setDataSuspensao(null);
        }
        if (assinatura.temPreapproval()) {
            assinaturaMercadoPagoService.desvincularECancelarAposCommit(assinatura, "CORTESIA");
        }
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
        assinaturaMercadoPagoService.executarAposCommit("assinatura.cortesia.link", assinaturaId,
                () -> eventos.publishEvent(new LinkPagamentoSolicitadoEvent(assinaturaId)));
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
                        MSG_ASSINATURA_NAO_ENCONTRADA + id,
                        HttpStatus.NOT_FOUND));
    }
}
