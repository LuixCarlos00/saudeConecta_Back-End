package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.suporte.ChamadoAnexo;
import br.com.saudeConecta.domain.suporte.ChamadoSuporte;
import br.com.saudeConecta.domain.suporte.PrioridadeChamado;
import br.com.saudeConecta.domain.suporte.StatusChamado;
import br.com.saudeConecta.domain.usuario.TipoUsuarioNovo;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.email.EmailNotificacaoService;
import br.com.saudeConecta.infra.tenant.TenantHelper;
import br.com.saudeConecta.infrastructure.persistence.repository.AdminOrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ChamadoSuporteRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.OrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.SecretariaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import br.com.saudeConecta.presentation.dto.suporte.AtualizarStatusChamadoRequest;
import br.com.saudeConecta.presentation.dto.suporte.ChamadoAnexoRequest;
import br.com.saudeConecta.presentation.dto.suporte.ChamadoSuporteRequest;
import br.com.saudeConecta.presentation.dto.suporte.ChamadoSuporteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service responsavel pelo ciclo de vida dos chamados de suporte.
 * Todas as operacoes sao isoladas por tenant; o ROOT (sem organizacao) possui visao global.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChamadoSuporteService {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String PREFIXO_DATA_URL = "base64,";
    private static final int MAX_TENTATIVAS_ID = 5;

    private final ChamadoSuporteRepository chamadoSuporteRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProfissionalRepository profissionalRepository;
    private final SecretariaRepository secretariaRepository;
    private final AdminOrganizacaoRepository adminOrganizacaoRepository;
    private final EmailNotificacaoService emailNotificacaoService;
    private final TenantHelper tenantHelper;

    /**
     * Cadastra um novo chamado de suporte para a organizacao do usuario autenticado
     * e dispara o email de confirmacao com a previsao de atendimento.
     *
     * @param request dados do chamado
     * @return DTO do chamado criado, com anexos
     */
    @Transactional
    public ChamadoSuporteResponse cadastrar(ChamadoSuporteRequest request) {
        Long organizacaoId = tenantHelper.getCurrentTenantId();
        Long usuarioId = tenantHelper.getCurrentUserId();

        if (usuarioId == null) {
            throw new IllegalStateException("Usuario nao identificado no contexto atual");
        }

        Organizacao organizacao = organizacaoRepository.findById(organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Organizacao nao encontrada: " + organizacaoId));

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado: " + usuarioId));

        DadosCriador criador = resolverDadosCriador(usuario);

        ChamadoSuporte chamado = ChamadoSuporte.builder()
                .id(gerarIdentificadorUnico())
                .organizacao(organizacao)
                .usuarioCriador(usuario)
                .criadorNome(criador.nome())
                .criadorEmail(criador.email())
                .titulo(request.titulo().trim())
                .corpo(request.corpo().trim())
                .categoria(request.categoria())
                .prioridade(request.prioridade())
                .status(StatusChamado.EM_ANALISE)
                .previsaoAtendimento(LocalDate.now().plusDays(request.prioridade().getPrazoPrevistoDias()))
                .build();

        adicionarAnexos(chamado, request.anexos());

        ChamadoSuporte salvo = chamadoSuporteRepository.save(chamado);
        log.info("Chamado de suporte {} criado na organizacao {} pelo usuario {}",
                salvo.getProtocolo(), organizacaoId, usuarioId);

        notificarCriador(salvo, organizacaoId);

        return ChamadoSuporteResponse.fromEntityComAnexos(salvo);
    }

    /**
     * Lista os chamados da organizacao do usuario autenticado.
     * Quando o usuario nao possui organizacao (ROOT), retorna os chamados de todas as organizacoes.
     *
     * @param status     filtro opcional por status
     * @param prioridade filtro opcional por prioridade
     * @param pageable   paginacao
     * @return pagina de chamados sem os anexos
     */
    @Transactional(readOnly = true)
    public Page<ChamadoSuporteResponse> listarPorTenant(StatusChamado status, PrioridadeChamado prioridade,
                                                        Pageable pageable) {
        Long organizacaoId = tenantHelper.getCurrentTenantIdOrNull();

        if (organizacaoId == null) {
            log.info("ROOT: listando chamados de suporte globais");
            return chamadoSuporteRepository.findAllWithFilters(status, prioridade, pageable)
                    .map(ChamadoSuporteResponse::fromEntity);
        }

        return chamadoSuporteRepository.findByOrganizacaoWithFilters(organizacaoId, status, prioridade, pageable)
                .map(ChamadoSuporteResponse::fromEntity);
    }

    /**
     * Busca um chamado especifico com seus anexos, validando o isolamento por tenant.
     *
     * @param chamadoId ID do chamado
     * @return DTO do chamado com anexos
     */
    @Transactional(readOnly = true)
    public ChamadoSuporteResponse buscarPorId(String chamadoId) {
        ChamadoSuporte chamado = buscarValidandoTenant(chamadoId);
        return ChamadoSuporteResponse.fromEntityComAnexos(chamado);
    }

    /**
     * Atualiza o status de um chamado e notifica o autor por email.
     * Operacao exclusiva do ROOT (usuario sem organizacao no contexto).
     *
     * @param chamadoId ID do chamado
     * @param request   novo status e observacao opcional
     * @return DTO do chamado atualizado
     */
    @Transactional
    public ChamadoSuporteResponse atualizarStatus(String chamadoId, AtualizarStatusChamadoRequest request) {
        if (tenantHelper.getCurrentTenantIdOrNull() != null) {
            throw new IllegalStateException("Apenas o suporte pode atualizar o status de um chamado");
        }

        ChamadoSuporte chamado = chamadoSuporteRepository.findByIdWithAnexos(chamadoId)
                .orElseThrow(() -> new IllegalArgumentException("Chamado nao encontrado: " + chamadoId));

        StatusChamado statusAnterior = chamado.getStatus();
        if (statusAnterior == request.status()) {
            log.info("Chamado {} ja esta no status {}. Nenhuma alteracao realizada.", chamadoId, statusAnterior);
            return ChamadoSuporteResponse.fromEntityComAnexos(chamado);
        }

        chamado.setStatus(request.status());
        ChamadoSuporte salvo = chamadoSuporteRepository.save(chamado);
        log.info("Status do chamado {} alterado de {} para {}", chamadoId, statusAnterior, request.status());

        notificarAtualizacaoStatus(salvo, statusAnterior, request.observacao());

        return ChamadoSuporteResponse.fromEntityComAnexos(salvo);
    }

    /**
     * Remove um chamado e seus anexos, validando o isolamento por tenant.
     *
     * @param chamadoId ID do chamado
     */
    @Transactional
    public void deletar(String chamadoId) {
        ChamadoSuporte chamado = buscarValidandoTenant(chamadoId);
        chamadoSuporteRepository.delete(chamado);
        log.info("Chamado de suporte {} excluido", chamadoId);
    }

    /**
     * Recupera o chamado garantindo que pertence ao tenant atual.
     * O ROOT (sem organizacao no contexto) tem acesso a qualquer chamado.
     *
     * @param chamadoId ID do chamado
     * @return entidade do chamado
     */
    private ChamadoSuporte buscarValidandoTenant(String chamadoId) {
        ChamadoSuporte chamado = chamadoSuporteRepository.findByIdWithAnexos(chamadoId)
                .orElseThrow(() -> new IllegalArgumentException("Chamado nao encontrado: " + chamadoId));

        Long organizacaoId = tenantHelper.getCurrentTenantIdOrNull();
        if (organizacaoId != null && !organizacaoId.equals(chamado.getOrganizacaoId())) {
            throw new IllegalArgumentException("Acesso negado ao chamado: " + chamadoId);
        }

        return chamado;
    }

    /**
     * Gera um identificador aleatorio garantindo que nao exista outro chamado com o mesmo valor.
     *
     * @return identificador unico no formato 999999-999999-99999
     */
    private String gerarIdentificadorUnico() {
        for (int tentativa = 0; tentativa < MAX_TENTATIVAS_ID; tentativa++) {
            String identificador = ChamadoSuporte.gerarIdentificador();
            if (!chamadoSuporteRepository.existsById(identificador)) {
                return identificador;
            }
            log.warn("Colisao de identificador de chamado detectada: {}", identificador);
        }
        throw new IllegalStateException("Nao foi possivel gerar um identificador unico para o chamado");
    }

    /**
     * Converte os anexos recebidos no request em entidades vinculadas ao chamado.
     *
     * @param chamado chamado destino
     * @param anexos  anexos recebidos (pode ser nulo)
     */
    private void adicionarAnexos(ChamadoSuporte chamado, List<ChamadoAnexoRequest> anexos) {
        if (anexos == null || anexos.isEmpty()) {
            return;
        }

        anexos.forEach(anexoRequest -> {
            String conteudo = normalizarBase64(anexoRequest.conteudoBase64());
            chamado.adicionarAnexo(ChamadoAnexo.builder()
                    .nomeArquivo(anexoRequest.nomeArquivo())
                    .tipoConteudo(anexoRequest.tipoConteudo())
                    .tamanhoBytes(calcularTamanhoBytes(conteudo))
                    .conteudoBase64(conteudo)
                    .build());
        });
    }

    /**
     * Remove o prefixo data URL do conteudo base64, quando presente.
     *
     * @param conteudoBase64 conteudo original recebido do frontend
     * @return conteudo base64 puro
     */
    private String normalizarBase64(String conteudoBase64) {
        int indice = conteudoBase64.indexOf(PREFIXO_DATA_URL);
        return indice >= 0 ? conteudoBase64.substring(indice + PREFIXO_DATA_URL.length()) : conteudoBase64;
    }

    /**
     * Calcula o tamanho aproximado em bytes de um conteudo base64.
     *
     * @param conteudoBase64 conteudo base64 puro
     * @return tamanho estimado em bytes
     */
    private Long calcularTamanhoBytes(String conteudoBase64) {
        return (long) (conteudoBase64.length() * 3L / 4L);
    }

    /**
     * Dispara o email de confirmacao ao usuario que abriu o chamado.
     * Falhas de envio nao impedem a criacao do chamado.
     *
     * @param chamado       chamado ja persistido
     * @param organizacaoId ID da organizacao
     */
    private void notificarCriador(ChamadoSuporte chamado, Long organizacaoId) {
        if (chamado.getCriadorEmail() == null || chamado.getCriadorEmail().isBlank()) {
            log.warn("Chamado {} sem email do criador. Confirmacao nao enviada.", chamado.getProtocolo());
            return;
        }

        emailNotificacaoService.enviarConfirmacaoChamadoSuporte(
                chamado.getCriadorEmail(),
                chamado.getCriadorNome(),
                chamado.getProtocolo(),
                chamado.getTitulo(),
                chamado.getCorpo(),
                chamado.getCategoria().getDescricao(),
                chamado.getPrioridade().getDescricao(),
                chamado.getStatus().getDescricao(),
                chamado.getPrioridade().getPrazoPrevistoDias(),
                chamado.getPrevisaoAtendimento().format(FORMATO_DATA),
                chamado.getAnexos() != null ? chamado.getAnexos().size() : 0,
                organizacaoId
        );
    }

    /**
     * Dispara o email informando a mudanca de status ao autor do chamado.
     *
     * @param chamado        chamado atualizado
     * @param statusAnterior status antes da alteracao
     * @param observacao     mensagem opcional da equipe de suporte
     */
    private void notificarAtualizacaoStatus(ChamadoSuporte chamado, StatusChamado statusAnterior, String observacao) {
        if (chamado.getCriadorEmail() == null || chamado.getCriadorEmail().isBlank()) {
            log.warn("Chamado {} sem email do criador. Notificacao de status nao enviada.", chamado.getProtocolo());
            return;
        }

        emailNotificacaoService.enviarAtualizacaoStatusChamado(
                chamado.getCriadorEmail(),
                chamado.getCriadorNome(),
                chamado.getProtocolo(),
                chamado.getTitulo(),
                statusAnterior.getDescricao(),
                chamado.getStatus().getDescricao(),
                observacao,
                chamado.getOrganizacaoId()
        );
    }

    /**
     * Resolve nome e email do criador do chamado a partir do perfil do usuario autenticado.
     *
     * @param usuario usuario autenticado
     * @return dados de contato do criador
     */
    private DadosCriador resolverDadosCriador(Usuario usuario) {
        TipoUsuarioNovo tipo = usuario.getTipoUsuarioNovo();

        if (tipo == TipoUsuarioNovo.CLINICO) {
            return profissionalRepository.findByUsuario_Id(usuario.getId())
                    .map(p -> new DadosCriador(p.getNome(), p.getEmail()))
                    .orElseGet(() -> fallbackCriador(usuario));
        }

        if (tipo == TipoUsuarioNovo.ASSISTENTE) {
            return secretariaRepository.findByUsuario_Id(usuario.getId())
                    .map(s -> new DadosCriador(s.getNome(), s.getEmail()))
                    .orElseGet(() -> fallbackCriador(usuario));
        }

        if (tipo == TipoUsuarioNovo.GESTOR) {
            return adminOrganizacaoRepository.findByUsuario_Id(usuario.getId())
                    .map(a -> new DadosCriador(a.getNome(), a.getEmail()))
                    .orElseGet(() -> fallbackCriador(usuario));
        }

        return fallbackCriador(usuario);
    }

    /**
     * Retorna os dados do proprio usuario quando nao ha perfil especifico associado.
     *
     * @param usuario usuario autenticado
     * @return dados de contato baseados na tabela usuarios
     */
    private DadosCriador fallbackCriador(Usuario usuario) {
        return new DadosCriador(usuario.getLogin(), usuario.getEmail());
    }

    /**
     * Estrutura auxiliar com nome e email do criador do chamado.
     *
     * @param nome  nome do criador
     * @param email email do criador
     */
    private record DadosCriador(String nome, String email) {
    }
}
