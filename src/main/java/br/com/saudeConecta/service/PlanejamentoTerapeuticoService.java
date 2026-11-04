package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.prontuario.PlanejamentoTerapeutico;
import br.com.saudeConecta.domain.prontuario.ProntuarioDentista;
import br.com.saudeConecta.infrastructure.persistence.repository.*;
import br.com.saudeConecta.infra.tenant.TenantHelper;
import br.com.saudeConecta.presentation.dto.prontuario.PlanejamentoTerapeuticoRequest;
import br.com.saudeConecta.util.MascaraLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanejamentoTerapeuticoService {

    private final PlanejamentoTerapeuticoRepository planejamentoRepository;
    private final ProntuarioDentistaRepository prontuarioRepository;
    private final ConsultaRepository consultaRepository;
    private final ProfissionalRepository profissionalRepository;
    private final PacienteRepository pacienteRepository;
    private final TenantHelper tenantHelper;

    /** Validade do link público de assinatura do planejamento. */
    private static final int DIAS_EXPIRACAO_LINK = 7;

    /**
     * Lista planejamentos de um prontuário (dentista ou médico).
     * Tenta buscar primeiro por prontuário dentista, depois por prontuário médico.
     *
     * @param prontuarioId ID do prontuário
     * @return lista de planejamentos
     */
    @Transactional(readOnly = true)
    public List<PlanejamentoTerapeutico> listarPorProntuario(Long prontuarioId) {
        Long orgId = tenantHelper.getCurrentTenantId();
        List<PlanejamentoTerapeutico> lista = planejamentoRepository.findByProntuarioId(prontuarioId, orgId);
        if (lista.isEmpty()) {
            lista = planejamentoRepository.findByProntuarioMedicoId(prontuarioId, orgId);
        }
        return lista;
    }

    /**
     * Adiciona um item ao planejamento terapêutico.
     *
     * @param profissionalId ID do profissional logado
     * @param request dados do planejamento
     * @return planejamento criado
     */
    @Transactional
    public PlanejamentoTerapeutico adicionar(Long profissionalId, PlanejamentoTerapeuticoRequest request) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.info("Adicionando planejamento — prontuario={}, procedimento={}",
                request.getProntuarioDentistaId(), request.getProcedimentoRealizado());

        // Prontuário, profissional e paciente precisam ser da clínica do token; de outra = "não encontrado".
        ProntuarioDentista prontuario = prontuarioRepository
                .findByCodigoAndConsulta_Organizacao_Id(request.getProntuarioDentistaId(), orgId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "prontuario nao encontrado: " + request.getProntuarioDentistaId()));

        Profissional profissional = profissionalRepository.findByIdAndOrganizacao_Id(profissionalId, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Profissional nao encontrado: " + profissionalId));

        Paciente paciente = pacienteRepository
                .findByPaciCodigoAndOrganizacao_IdWithEndereco(request.getPacienteId(), orgId)
                .orElseThrow(() -> new IllegalArgumentException("Paciente nao encontrado: " + request.getPacienteId()));

        Organizacao organizacao = new Organizacao();
        organizacao.setId(orgId);

        Consulta consulta = null;
        if (request.getConsultaId() != null) {
            consulta = consultaRepository.findByIdAndOrganizacao_Id(request.getConsultaId(), orgId).orElse(null);
        }

        PlanejamentoTerapeutico planejamento = PlanejamentoTerapeutico.builder()
                .prontuarioDentista(prontuario)
                .consulta(consulta)
                .paciente(paciente)
                .profissional(profissional)
                .organizacao(organizacao)
                .dataProcedimento(parseData(request.getDataProcedimento()))
                .procedimentoRealizado(request.getProcedimentoRealizado())
                .valor(request.getValor())
                .statusAssinatura("PENDENTE")
                .build();

        PlanejamentoTerapeutico salvo = planejamentoRepository.save(planejamento);
        log.info("Planejamento criado — id={}", salvo.getId());
        return salvo;
    }

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
        log.info("Planejamento removido — id={}", id);
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
        log.info("Gerando link de assinatura para prontuario={}, tipo={}", prontuarioId, tipo);
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
        LocalDateTime expiracao = LocalDateTime.now().plusDays(DIAS_EXPIRACAO_LINK);

        for (PlanejamentoTerapeutico p : planejamentos) {
            if (p.isPendente()) {
                p.setTokenAssinatura(token);
                p.setTokenExpiracao(expiracao);
                planejamentoRepository.save(p);
            }
        }

        log.info("Token de assinatura gerado={} para {} itens", MascaraLog.token(token), planejamentos.size());
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
        LocalDateTime agora = LocalDateTime.now();
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
        log.info("Assinando planejamento — token={}, ip={}", MascaraLog.token(token), ipOrigem);

        int atualizados = planejamentoRepository.assinarPorToken(token, assinaturaBase64, ipOrigem, LocalDateTime.now());
        if (atualizados == 0) {
            throw new IllegalArgumentException("Nenhum planejamento encontrado para o token informado.");
        }

        log.info("Planejamento assinado — {} itens atualizados", atualizados);
    }

    private LocalDate parseData(String data) {
        if (data == null || data.isBlank()) return LocalDate.now();
        try {
            return LocalDate.parse(data);
        } catch (Exception e) {
            log.warn("Data inválida '{}', usando data atual.", data);
            return LocalDate.now();
        }
    }
}
