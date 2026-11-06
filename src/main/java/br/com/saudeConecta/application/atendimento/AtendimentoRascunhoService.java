package br.com.saudeConecta.application.atendimento;

import br.com.saudeConecta.application.atendimento.dto.RascunhoAtendimentoResponse;
import br.com.saudeConecta.application.atendimento.dto.RascunhoAtendimentoResumoResponse;
import br.com.saudeConecta.application.atendimento.dto.SalvarRascunhoAtendimentoRequest;
import br.com.saudeConecta.application.exception.BusinessException;
import br.com.saudeConecta.application.exception.ResourceNotFoundException;
import br.com.saudeConecta.config.tenant.TenantHelper;
import br.com.saudeConecta.domain.atendimento.AtendimentoRascunho;
import br.com.saudeConecta.domain.atendimento.TipoAtendimentoRascunho;
import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.consulta.StatusConsulta;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.infrastructure.persistence.repository.AtendimentoRascunhoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ConsultaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Rascunho do atendimento (contrato em CONTRATO-RASCUNHO-ATENDIMENTO.md).
 *
 * <p>Só o profissional dono da consulta ({@code consulta.profissional.usuario} = usuário do token), na
 * clínica do token, lê, grava ou apaga. Consulta de outra clínica ou de outro profissional responde 404,
 * como se não existisse.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AtendimentoRascunhoService {

    /** Limite de {@code dados} serializado em UTF-8: 2 MB (o planejamento pode ter assinatura em base64). */
    static final int LIMITE_DADOS_BYTES = 2 * 1024 * 1024;

    static final String CODE_DESATUALIZADO = "RASCUNHO_DESATUALIZADO";
    static final String CODE_CONSULTA_ENCERRADA = "CONSULTA_ENCERRADA";
    static final String CODE_INVALIDO = "RASCUNHO_INVALIDO";
    static final String CODE_MUITO_GRANDE = "RASCUNHO_MUITO_GRANDE";

    private static final Set<StatusConsulta> STATUS_ENCERRADOS =
            EnumSet.of(StatusConsulta.REALIZADA, StatusConsulta.PAGO, StatusConsulta.CANCELADA);

    private final AtendimentoRascunhoRepository rascunhoRepository;
    private final ConsultaRepository consultaRepository;
    private final TenantHelper tenantHelper;
    private final ObjectMapper objectMapper;

    /**
     * Rascunho da consulta do profissional logado.
     *
     * @param consultaId ID da consulta
     * @return rascunho, ou vazio quando não há (o controller responde 204)
     * @throws ResourceNotFoundException consulta inexistente, de outra clínica ou de outro profissional
     */
    @Transactional(readOnly = true)
    public Optional<RascunhoAtendimentoResponse> buscar(Long consultaId) {
        Long orgId = tenantHelper.getCurrentTenantId();
        Consulta consulta = consultaDoUsuario(consultaRepository.findByIdAndOrganizacao_Id(consultaId, orgId),
                "rascunho.buscar", consultaId);
        return rascunhoRepository.findByConsulta_IdAndOrganizacao_Id(consulta.getId(), orgId)
                .map(this::paraResponse);
    }

    /**
     * Cria ou atualiza o rascunho da consulta (lock otimista pela {@code versao}).
     *
     * <p>A linha da consulta fica travada até o fim da transação: duas gravações da mesma consulta, ou
     * uma gravação e a finalização do prontuário, nunca se cruzam. Se ainda assim duas primeiras gravações
     * colidirem no UNIQUE de {@code consulta_id}, ou uma atualização perder a corrida da versão, a perdedora
     * recebe 409 {@code RASCUNHO_DESATUALIZADO} (nunca 500).</p>
     *
     * @param consultaId ID da consulta
     * @param request    tipo, dados, cronômetro e versão lida pelo front
     * @return rascunho gravado, com a versão nova
     * @throws ResourceNotFoundException consulta inexistente, de outra clínica ou de outro profissional (404)
     * @throws BusinessException         409 {@code CONSULTA_ENCERRADA}, 409 {@code RASCUNHO_DESATUALIZADO},
     *                                   422 {@code RASCUNHO_INVALIDO} ou 422 {@code RASCUNHO_MUITO_GRANDE}
     */
    @Transactional
    public RascunhoAtendimentoResponse salvar(Long consultaId, SalvarRascunhoAtendimentoRequest request) {
        log.info("rascunho.salvar inicio consultaId={} versao={}", consultaId, request.versao());
        Long orgId = tenantHelper.getCurrentTenantId();
        Consulta consulta = consultaDoUsuario(consultaRepository.buscarComLock(consultaId, orgId),
                "rascunho.salvar", consultaId);

        if (STATUS_ENCERRADOS.contains(consulta.getStatus())) {
            log.warn("rascunho.salvar negado motivo=CONSULTA_ENCERRADA consultaId={} status={}",
                    consultaId, consulta.getStatus());
            throw new BusinessException("A consulta ja foi encerrada e nao aceita rascunho",
                    HttpStatus.CONFLICT, CODE_CONSULTA_ENCERRADA);
        }

        TipoAtendimentoRascunho tipo = validarTipo(request.tipo(), consultaId);
        String dados = serializarDados(request.dados(), consultaId);
        int tempo = validarTempo(request.tempoAcumuladoSegundos(), consultaId);

        Optional<AtendimentoRascunho> atual = rascunhoRepository.findByConsulta_IdAndOrganizacao_Id(consultaId, orgId);
        Long versaoAtual = atual.map(AtendimentoRascunho::getVersao).orElse(null);
        if (!Objects.equals(versaoAtual, request.versao())) {
            throw desatualizado(consultaId, request.versao(), versaoAtual);
        }

        AtendimentoRascunho rascunho = atual.orElseGet(() -> AtendimentoRascunho.builder()
                .consulta(consulta)
                .organizacao(consulta.getOrganizacao())
                .profissional(consulta.getProfissional())
                .build());
        rascunho.setTipo(tipo);
        rascunho.setDados(dados);
        rascunho.setTempoAcumuladoSegundos(tempo);

        AtendimentoRascunho salvo;
        try {
            salvo = rascunhoRepository.saveAndFlush(rascunho);
        } catch (DataIntegrityViolationException | OptimisticLockingFailureException e) {
            // UNIQUE de consulta_id (duas primeiras gravações) ou UPDATE ... WHERE versao=? sem linha afetada.
            log.warn("rascunho.salvar negado motivo=CORRIDA consultaId={} causa={}", consultaId, e.getClass().getSimpleName());
            throw new BusinessException("O rascunho foi salvo em outra aba ou computador. Recarregue para continuar.",
                    HttpStatus.CONFLICT, CODE_DESATUALIZADO);
        }

        log.info("rascunho.salvar ok consultaId={} rascunhoId={} versao={} tamanhoBytes={}",
                consultaId, salvo.getId(), salvo.getVersao(), dados.getBytes(StandardCharsets.UTF_8).length);
        return paraResponse(salvo);
    }

    /**
     * Descarta o rascunho da consulta. Idempotente: não ter rascunho não é erro.
     *
     * @param consultaId ID da consulta
     * @throws ResourceNotFoundException consulta inexistente, de outra clínica ou de outro profissional
     */
    @Transactional
    public void descartar(Long consultaId) {
        log.info("rascunho.descartar inicio consultaId={}", consultaId);
        Long orgId = tenantHelper.getCurrentTenantId();
        // Lock na consulta (mesma ordem do salvar): um PUT que chega depois do DELETE só roda após o commit dele.
        Consulta consulta = consultaDoUsuario(consultaRepository.buscarComLock(consultaId, orgId),
                "rascunho.descartar", consultaId);
        int removidos = rascunhoRepository.apagarPorConsulta(consulta.getId());
        log.info("rascunho.descartar ok consultaId={} removidos={}", consultaId, removidos);
    }

    /**
     * Rascunhos do profissional logado na clínica do token, para a agenda mostrar "Retomar atendimento".
     *
     * @return resumos (vazio se o usuário não for profissional)
     */
    @Transactional(readOnly = true)
    public List<RascunhoAtendimentoResumoResponse> listarMeus() {
        Long orgId = tenantHelper.getCurrentTenantId();
        Long usuarioId = tenantHelper.getCurrentUserId();
        if (usuarioId == null) {
            return List.of();
        }
        return rascunhoRepository.listarDoUsuario(orgId, usuarioId).stream()
                .map(RascunhoAtendimentoResumoResponse::de)
                .toList();
    }

    /**
     * Mantém a consulta só se o profissional dela for o usuário do token. Outro profissional da mesma
     * clínica recebe o mesmo 404 de consulta inexistente (não revela que a consulta existe).
     */
    private Consulta consultaDoUsuario(Optional<Consulta> consulta, String evento, Long consultaId) {
        Long usuarioId = tenantHelper.getCurrentUserId();
        return consulta
                .filter(c -> usuarioId != null && usuarioId.equals(usuarioDoProfissional(c.getProfissional())))
                .orElseThrow(() -> {
                    log.warn("{} negado motivo=NAO_ENCONTRADO consultaId={}", evento, consultaId);
                    return new ResourceNotFoundException("Consulta nao encontrada");
                });
    }

    private static Long usuarioDoProfissional(Profissional profissional) {
        if (profissional == null || profissional.getUsuario() == null) {
            return null;
        }
        return profissional.getUsuario().getId();
    }

    private static TipoAtendimentoRascunho validarTipo(String tipo, Long consultaId) {
        if (tipo != null) {
            for (TipoAtendimentoRascunho t : TipoAtendimentoRascunho.values()) {
                if (t.name().equals(tipo)) {
                    return t;
                }
            }
        }
        log.warn("rascunho.salvar negado motivo=TIPO_INVALIDO consultaId={}", consultaId);
        throw new BusinessException("Tipo do rascunho invalido: use MEDICO ou DENTISTA",
                HttpStatus.UNPROCESSABLE_ENTITY, CODE_INVALIDO);
    }

    private String serializarDados(JsonNode dados, Long consultaId) {
        if (dados == null || dados.isNull() || dados.isMissingNode()) {
            log.warn("rascunho.salvar negado motivo=DADOS_AUSENTES consultaId={}", consultaId);
            throw new BusinessException("Dados do rascunho sao obrigatorios",
                    HttpStatus.UNPROCESSABLE_ENTITY, CODE_INVALIDO);
        }
        if (!dados.isObject()) {
            log.warn("rascunho.salvar negado motivo=DADOS_NAO_OBJETO consultaId={} tipoJson={}",
                    consultaId, dados.getNodeType());
            throw new BusinessException("Dados do rascunho devem ser um objeto JSON",
                    HttpStatus.UNPROCESSABLE_ENTITY, CODE_INVALIDO);
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(dados);
        } catch (JsonProcessingException e) {
            // JsonNode que acabou de ser lido sempre serializa; chegar aqui é defeito (500), não entrada do cliente.
            throw new UncheckedIOException("Falha ao serializar o rascunho", e);
        }
        int tamanho = json.getBytes(StandardCharsets.UTF_8).length;
        if (tamanho > LIMITE_DADOS_BYTES) {
            log.warn("rascunho.salvar negado motivo=MUITO_GRANDE consultaId={} tamanhoBytes={} limiteBytes={}",
                    consultaId, tamanho, LIMITE_DADOS_BYTES);
            throw new BusinessException("Rascunho acima do limite de 2 MB",
                    HttpStatus.UNPROCESSABLE_ENTITY, CODE_MUITO_GRANDE);
        }
        return json;
    }

    private static int validarTempo(Integer tempo, Long consultaId) {
        if (tempo == null) {
            return 0;
        }
        if (tempo < 0) {
            log.warn("rascunho.salvar negado motivo=TEMPO_INVALIDO consultaId={}", consultaId);
            throw new BusinessException("Tempo acumulado nao pode ser negativo",
                    HttpStatus.UNPROCESSABLE_ENTITY, CODE_INVALIDO);
        }
        return tempo;
    }

    private static BusinessException desatualizado(Long consultaId, Long versaoEnviada, Long versaoAtual) {
        log.warn("rascunho.salvar negado motivo=VERSAO_DESATUALIZADA consultaId={} versaoEnviada={} versaoAtual={}",
                consultaId, versaoEnviada, versaoAtual);
        return new BusinessException("O rascunho foi salvo em outra aba ou computador. Recarregue para continuar.",
                HttpStatus.CONFLICT, CODE_DESATUALIZADO);
    }

    private RascunhoAtendimentoResponse paraResponse(AtendimentoRascunho r) {
        JsonNode dados;
        try {
            dados = objectMapper.readTree(r.getDados());
        } catch (JsonProcessingException e) {
            // Só gravamos JSON serializado por nós; texto inválido aqui é dado corrompido no banco.
            throw new UncheckedIOException("Rascunho com JSON invalido no banco", e);
        }
        return new RascunhoAtendimentoResponse(r.getConsulta().getId(), r.getTipo().name(), dados,
                r.getTempoAcumuladoSegundos(), r.getVersao(), r.getAtualizadoEm());
    }
}
