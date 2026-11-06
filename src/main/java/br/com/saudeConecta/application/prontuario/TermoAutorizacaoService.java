package br.com.saudeConecta.application.prontuario;

import br.com.saudeConecta.domain.FusoHorario;
import br.com.saudeConecta.application.cache.CacheEvictionService;
import br.com.saudeConecta.application.prontuario.dto.QuestionarioSaudeRequest;
import br.com.saudeConecta.config.logging.MascaraLog;
import br.com.saudeConecta.config.tenant.TenantHelper;
import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.domain.prontuario.TermoAutorizacao;
import br.com.saudeConecta.infrastructure.persistence.repository.ConsultaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.TermoAutorizacaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TermoAutorizacaoService {

    private final TermoAutorizacaoRepository termoRepository;
    private final ConsultaRepository consultaRepository;
    private final TenantHelper tenantHelper;
    private final CacheEvictionService cacheEvictionService;

    private static final int HORAS_EXPIRACAO = 24;

    // Chave de cache sempre prefixada pela organização do token (mesmo formato usado na evicção).
    private static final String CHAVE_CACHE_CONSULTA_TENANT =
            "T(br.com.saudeConecta.application.cache.CacheEvictionService).chaveConsultaPorTenant("
                    + "@tenantHelper.getCurrentTenantIdOrNull(), #consultaId)";

    /**
     * Gera um link único para o questionário de saúde vinculado a uma consulta.
     *
     * @param consultaId ID da consulta
     * @return token gerado
     */
    @Transactional
    public String gerarLinkQuestionario(Long consultaId) {
        log.info("questionario.gerarLink inicio consultaId={}", consultaId);
        Long orgId = tenantHelper.getCurrentTenantId();

        // Consulta de outra clínica responde igual a inexistente.
        Consulta consulta = consultaRepository.findByIdAndOrganizacao_Id(consultaId, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Consulta nao encontrada: " + consultaId));

        // Usa lock pessimista para evitar race condition em requests simultâneos
        var termoExistente = termoRepository.findByConsultaIdWithLock(consultaId);
        if (termoExistente.isPresent()) {
            TermoAutorizacao existente = termoExistente.get();
            // Se existe registro com status PENDENTE, deleta e cria novo
            if (existente.isPendente()) {
                log.info("questionario.gerarLink termo pendente removido consultaId={}", consultaId);
                termoRepository.delete(existente);
                termoRepository.flush(); // Flush explícito para garantir remoção antes do insert
            } else {
                // Se já foi assinado, não permite recriar
                throw new IllegalStateException("Questionário já foi respondido para esta consulta.");
            }
        }

        Paciente paciente = consulta.getPaciente();
        Organizacao organizacao = consulta.getOrganizacao();

        String token = UUID.randomUUID().toString();

        TermoAutorizacao termo = TermoAutorizacao.builder()
                .consulta(consulta)
                .paciente(paciente)
                .organizacao(organizacao)
                .token(token)
                .dataExpiracao(LocalDateTime.now(FusoHorario.ZONA).plusHours(HORAS_EXPIRACAO))
                .status("PENDENTE")
                .build();

        termoRepository.save(termo);
        cacheEvictionService.evictQuestionarioSaude(orgId, consultaId);
        log.info("questionario.gerarLink ok token={} consultaId={} expiraEmHoras={}",
                MascaraLog.token(token), consultaId, HORAS_EXPIRACAO);

        return token;
    }

    /**
     * Busca o termo pelo token (rota pública, sem autenticação).
     *
     * @param token token único
     * @return dados básicos do termo para exibir o questionário
     */
    @Transactional(readOnly = true)
    public TermoAutorizacao buscarPorToken(String token) {
        TermoAutorizacao termo = termoRepository.findByTokenComRelacionamentos(token)
                .orElseThrow(() -> new IllegalArgumentException("Link inválido ou nao encontrado."));

        if (termo.isExpirado()) {
            throw new IllegalStateException("Este link expirou. Solicite um novo ao profissional.");
        }

        if (termo.isAssinado()) {
            throw new IllegalStateException("Este questionario já foi respondido e assinado.");
        }

        return termo;
    }

    /**
     * Salva as respostas do questionário e a assinatura digital (rota pública).
     *
     * @param request dados do questionário + assinatura
     * @param ipOrigem IP do paciente
     */
    @Transactional
    public void responderQuestionario(QuestionarioSaudeRequest request, String ipOrigem) {
        log.info("questionario.responder inicio token={} ip={}", MascaraLog.token(request.getToken()), ipOrigem);

        TermoAutorizacao termo = termoRepository.findByToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Token invalido."));

        if (termo.isExpirado()) {
            throw new IllegalStateException("Este link expirou.");
        }

        if (termo.isAssinado()) {
            throw new IllegalStateException("Este questionario já foi respondido.");
        }

        // As checagens acima só dão a mensagem certa; quem garante o uso único é o UPDATE
        // condicional (status PENDENTE e não expirado), atômico mesmo com envios simultâneos.
        int atualizados = termoRepository.responderSePendente(
                request.getToken(),
                request.getRespostasQuestionario(),
                request.getAssinaturaBase64(),
                ipOrigem,
                LocalDateTime.now(FusoHorario.ZONA));
        if (atualizados == 0) {
            throw new IllegalStateException("Este questionario já foi respondido.");
        }

        Long consultaId = termo.getConsulta().getId();
        log.info("questionario.responder ok consultaId={}", consultaId);
        // Rota pública não tem tenant no contexto: a chave usa a organização dona do termo.
        // Evicção via bean porque @CacheEvict em método da própria classe não passa pelo proxy.
        cacheEvictionService.evictQuestionarioSaude(termo.getOrganizacao().getId(), consultaId);
    }

    /**
     * Busca o questionário respondido por consultaId (para exibir na aba do prontuário).
     *
     * @param consultaId ID da consulta
     * @return termo com respostas ou null se nao existir
     */
    @Cacheable(value = "questionario-saude", key = CHAVE_CACHE_CONSULTA_TENANT, unless = "#result == null")
    @Transactional(readOnly = true)
    public TermoAutorizacao buscarPorConsultaId(Long consultaId) {
        Long orgId = tenantHelper.getCurrentTenantId();
        return termoRepository.findByConsultaIdAndOrganizacaoId(consultaId, orgId).orElse(null);
    }
}
