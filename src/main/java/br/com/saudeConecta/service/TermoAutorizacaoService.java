package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.domain.prontuario.TermoAutorizacao;
import br.com.saudeConecta.infrastructure.persistence.repository.ConsultaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.TermoAutorizacaoRepository;
import br.com.saudeConecta.presentation.dto.prontuario.QuestionarioSaudeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
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

    private static final int HORAS_EXPIRACAO = 24;

    /**
     * Gera um link único para o questionário de saúde vinculado a uma consulta.
     *
     * @param consultaId ID da consulta
     * @return token gerado
     */
    @CacheEvict(value = "questionario-saude", key = "#consultaId")
    @Transactional
    public String gerarLinkQuestionario(Long consultaId) {
        log.info("Gerando link do questionário para consulta={}", consultaId);

        Consulta consulta = consultaRepository.findById(consultaId)
                .orElseThrow(() -> new IllegalArgumentException("Consulta nao encontrada: " + consultaId));

        // Se já existe um termo para esta consulta, retorna o token existente (se nao expirado)
        var termoExistente = termoRepository.findByConsultaId(consultaId);
        if (termoExistente.isPresent()) {
            TermoAutorizacao existente = termoExistente.get();
            if (!existente.isExpirado() && existente.isPendente()) {
                log.info("Token existente reutilizado para consulta={}", consultaId);
                return existente.getToken();
            }
            // Se expirado ou já assinado, remove e cria novo
            termoRepository.delete(existente);
        }

        Paciente paciente = consulta.getPaciente();
        Organizacao organizacao = consulta.getOrganizacao();

        String token = UUID.randomUUID().toString();

        TermoAutorizacao termo = TermoAutorizacao.builder()
                .consulta(consulta)
                .paciente(paciente)
                .organizacao(organizacao)
                .token(token)
                .dataExpiracao(LocalDateTime.now().plusHours(HORAS_EXPIRACAO))
                .status("PENDENTE")
                .build();

        termoRepository.save(termo);
        log.info("Token gerado={} para consulta={}, expira em {}h", token, consultaId, HORAS_EXPIRACAO);

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
        // @CacheEvict aplicado no final do método para garantir que o termo já foi salvo
        log.info("Recebendo respostas do questionário — token={}", request.getToken());

        TermoAutorizacao termo = termoRepository.findByToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Token invalido."));

        if (termo.isExpirado()) {
            throw new IllegalStateException("Este link expirou.");
        }

        if (termo.isAssinado()) {
            throw new IllegalStateException("Este questionario já foi respondido.");
        }

        termo.setRespostasQuestionario(request.getRespostasQuestionario());
        termo.setAssinaturaBase64(request.getAssinaturaBase64());
        termo.setIpOrigem(ipOrigem);
        termo.setDataAssinatura(LocalDateTime.now());
        termo.setStatus("ASSINADO");

        termoRepository.save(termo);
        log.info("Questionário respondido e assinado — consulta={}", termo.getConsulta().getId());
        evictQuestionarioCache(termo.getConsulta().getId());
    }

    /**
     * Invalida o cache do questionário de saúde para a consulta informada.
     * Separado para garantir execução após o flush da transação.
     *
     * @param consultaId ID da consulta cujo cache deve ser invalidado
     */
    @CacheEvict(value = "questionario-saude", key = "#consultaId")
    public void evictQuestionarioCache(Long consultaId) {
        log.debug("Cache questionario-saude invalidado para consulta={}", consultaId);
    }

    /**
     * Busca o questionário respondido por consultaId (para exibir na aba do prontuário).
     *
     * @param consultaId ID da consulta
     * @return termo com respostas ou null se nao existir
     */
    @Cacheable(value = "questionario-saude", key = "#consultaId", unless = "#result == null")
    @Transactional(readOnly = true)
    public TermoAutorizacao buscarPorConsultaId(Long consultaId) {
        return termoRepository.findByConsultaId(consultaId).orElse(null);
    }
}
