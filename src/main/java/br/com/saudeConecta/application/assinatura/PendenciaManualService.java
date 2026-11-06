package br.com.saudeConecta.application.assinatura;

import br.com.saudeConecta.application.assinatura.dto.PendenciaManualResponse;
import br.com.saudeConecta.application.exception.BusinessException;
import br.com.saudeConecta.application.exception.ResourceNotFoundException;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.planos.MpWebhookEvento;
import br.com.saudeConecta.infrastructure.persistence.repository.MpWebhookEventoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.OrganizacaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Pendências manuais do Mercado Pago na tela do SUPER_ADMIN: listar (abertas ou resolvidas) e marcar
 * como resolvida. Resolver só registra a decisão: nada é alterado no Mercado Pago nem na assinatura.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PendenciaManualService {

    /** Código do 409 quando a pendência já foi resolvida. */
    public static final String CODE_PENDENCIA_JA_RESOLVIDA = "PENDENCIA_JA_RESOLVIDA";

    private final MpWebhookEventoRepository eventoRepository;
    private final OrganizacaoRepository organizacaoRepository;
    /** Relógio em America/Sao_Paulo (FusoHorarioConfig). */
    private final Clock clock;

    /**
     * @param resolvidas false = abertas (requer_atencao), da mais recente; true = resolvidas, da
     *                   resolução mais recente
     * @return pendências com a clínica, quando conhecida
     */
    @Transactional(readOnly = true)
    public List<PendenciaManualResponse> listar(boolean resolvidas) {
        List<MpWebhookEvento> eventos = resolvidas
                ? eventoRepository.findByResolvidoEmIsNotNullOrderByResolvidoEmDesc()
                : eventoRepository.findByRequerAtencaoTrueOrderByRecebidoEmDesc();
        // Nomes das clínicas numa query só (sem N+1)
        Map<Long, String> nomes = organizacaoRepository.findAllById(eventos.stream()
                        .map(MpWebhookEvento::getOrganizacaoId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(Organizacao::getId, Organizacao::getNome, (a, b) -> a));
        return eventos.stream()
                .map(e -> PendenciaManualResponse.fromEntity(e,
                        e.getOrganizacaoId() != null ? nomes.get(e.getOrganizacaoId()) : null))
                .toList();
    }

    /**
     * Marca a pendência como resolvida (requer_atencao = false) guardando quem, quando e a observação.
     *
     * @param eventoId   ID do evento em mp_webhook_evento
     * @param usuarioId  usuário do token (SUPER_ADMIN)
     * @param observacao o que foi feito (já validada no controller)
     * @return a pendência resolvida
     * @throws ResourceNotFoundException evento inexistente ou que nunca foi pendência (404)
     * @throws BusinessException         409 {@code PENDENCIA_JA_RESOLVIDA}
     */
    @Transactional
    // O dashboard da plataforma guarda a contagem de pendências por 5 min: sem evict o badge não baixaria
    @CacheEvict(value = "plataforma-dashboard", allEntries = true)
    public PendenciaManualResponse resolver(Long eventoId, Long usuarioId, String observacao) {
        log.info("assinatura.pendencia.resolver inicio eventoId={}", eventoId);
        MpWebhookEvento evento = eventoRepository.findByIdParaAtualizacao(eventoId)
                .filter(e -> Boolean.TRUE.equals(e.getRequerAtencao()) || e.getResolvidoEm() != null)
                .orElseThrow(() -> {
                    log.warn("assinatura.pendencia.resolver negado motivo=NAO_ENCONTRADA eventoId={}", eventoId);
                    return new ResourceNotFoundException("Pendência não encontrada: " + eventoId);
                });
        if (evento.getResolvidoEm() != null) {
            log.warn("assinatura.pendencia.resolver negado motivo=PENDENCIA_JA_RESOLVIDA eventoId={} resolvidoEm={}",
                    eventoId, evento.getResolvidoEm());
            throw new BusinessException("Esta pendência já foi resolvida.", HttpStatus.CONFLICT,
                    CODE_PENDENCIA_JA_RESOLVIDA);
        }

        evento.resolver(usuarioId, observacao.trim(), LocalDateTime.now(clock));
        eventoRepository.save(evento);
        log.info("assinatura.pendencia.resolver ok eventoId={} motivo={} organizacaoId={} resolvidoPorUsuarioId={}",
                eventoId, evento.getMotivoCodigo(), evento.getOrganizacaoId(), usuarioId);

        String nome = evento.getOrganizacaoId() == null ? null : organizacaoRepository.findById(evento.getOrganizacaoId())
                .map(Organizacao::getNome).orElse(null);
        return PendenciaManualResponse.fromEntity(evento, nome);
    }
}
