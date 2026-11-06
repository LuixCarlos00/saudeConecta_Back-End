package br.com.saudeConecta.application.prontuario;

import br.com.saudeConecta.domain.FusoHorario;
import br.com.saudeConecta.config.logging.MascaraLog;
import br.com.saudeConecta.config.tenant.TenantHelper;
import br.com.saudeConecta.domain.prontuario.PlanejamentoTerapeutico;
import br.com.saudeConecta.infrastructure.persistence.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanejamentoTerapeuticoService {

    private final PlanejamentoTerapeuticoRepository planejamentoRepository;
    private final TenantHelper tenantHelper;

    /** Validade do link público de assinatura do planejamento. */
    private static final int DIAS_EXPIRACAO_LINK = 7;

    /**
     * Remove um item do planejamento.
     *
     * @param id ID do planejamento
     */
    @Transactional
    public void remover(Long id) {
        PlanejamentoTerapeutico planejamento = planejamentoRepository
                .findByIdAndOrganizacao_Id(id, tenantHelper.getCurrentTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Planejamento nao encontrado: " + id));
        planejamentoRepository.delete(planejamento);
        log.info("planejamento.remover ok planejamentoId={}", id);
    }

    /**
     * Gera um token de assinatura para todos os itens do planejamento de um prontuário.
     * Busca diretamente na coluna correta conforme o tipo do profissional.
     *
     * @param prontuarioId ID do prontuário (dentista ou médico)
     * @param tipo "DENTISTA" ou "MEDICO"
     * @return token gerado
     */
    @Transactional
    public String gerarLinkAssinatura(Long prontuarioId, String tipo) {
        log.info("planejamento.gerarLink inicio prontuarioId={} tipo={}", prontuarioId, tipo);
        Long orgId = tenantHelper.getCurrentTenantId();

        List<PlanejamentoTerapeutico> planejamentos;

        if ("MEDICO".equalsIgnoreCase(tipo)) {
            planejamentos = planejamentoRepository.findByProntuarioMedicoId(prontuarioId, orgId);
        } else {
            planejamentos = planejamentoRepository.findByProntuarioId(prontuarioId, orgId);
        }

        if (planejamentos.isEmpty()) {
            throw new IllegalStateException("Nenhum planejamento encontrado para este prontuario.");
        }

        // Gera um único token para todos os itens pendentes
        String token = UUID.randomUUID().toString();
        LocalDateTime expiracao = LocalDateTime.now(FusoHorario.ZONA).plusDays(DIAS_EXPIRACAO_LINK);

        for (PlanejamentoTerapeutico p : planejamentos) {
            if (p.isPendente()) {
                p.setTokenAssinatura(token);
                p.setTokenExpiracao(expiracao);
                planejamentoRepository.save(p);
            }
        }

        log.info("planejamento.gerarLink ok token={} itens={}", MascaraLog.token(token), planejamentos.size());
        return token;
    }

    /**
     * Busca planejamentos por token de assinatura (rota pública).
     *
     * @param token token de assinatura
     * @return lista de planejamentos vinculados ao token
     */
    @Transactional(readOnly = true)
    public List<PlanejamentoTerapeutico> buscarPorToken(String token) {
        List<PlanejamentoTerapeutico> lista = planejamentoRepository.findByTokenAssinatura(token);
        if (lista.isEmpty()) {
            throw new IllegalArgumentException("Link invalido ou nao encontrado.");
        }

        // Expiração nula = link legado gerado antes da coluna existir (sem expiração).
        LocalDateTime agora = LocalDateTime.now(FusoHorario.ZONA);
        // A expiração só vale para itens ainda PENDENTES: um planejamento já assinado continua consultável.
        boolean expirado = lista.stream()
                .anyMatch(p -> !p.isAssinado() && p.getTokenExpiracao() != null && !p.getTokenExpiracao().isAfter(agora));
        if (expirado) {
            // 404 (EntityNotFoundException): o front mostra "Planejamento não encontrado ou link expirado."
            throw new EntityNotFoundException("Link expirado. Solicite um novo link à clínica.");
        }

        return lista;
    }

    /**
     * Assina todos os itens do planejamento vinculados ao token (rota pública).
     *
     * @param token token de assinatura
     * @param assinaturaBase64 assinatura digital em base64
     * @param ipOrigem IP de origem da assinatura
     */
    @Transactional
    public void assinarPorToken(String token, String assinaturaBase64, String ipOrigem) {
        log.info("planejamento.assinar inicio token={} ip={}", MascaraLog.token(token), ipOrigem);

        int atualizados = planejamentoRepository.assinarPorToken(token, assinaturaBase64, ipOrigem, LocalDateTime.now(FusoHorario.ZONA));
        if (atualizados == 0) {
            throw new IllegalArgumentException("Nenhum planejamento encontrado para o token informado.");
        }

        log.info("planejamento.assinar ok itens={}", atualizados);
    }

}
