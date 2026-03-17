package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.prontuario.PlanejamentoTerapeutico;
import br.com.saudeConecta.domain.prontuario.ProntuarioDentista;
import br.com.saudeConecta.infrastructure.persistence.repository.*;
import br.com.saudeConecta.infra.tenant.TenantContext;
import br.com.saudeConecta.presentation.dto.prontuario.PlanejamentoTerapeuticoRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Lista planejamentos de um prontuario odontológico.
     *
     * @param prontuarioId ID do prontuario dentista
     * @return lista de planejamentos
     */
    @Transactional(readOnly = true)
    public List<PlanejamentoTerapeutico> listarPorProntuario(Long prontuarioId) {
        return planejamentoRepository.findByProntuarioId(prontuarioId);
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
        Long orgId = TenantContext.getCurrentTenant();
        log.info("Adicionando planejamento — prontuario={}, procedimento={}",
                request.getProntuarioDentistaId(), request.getProcedimentoRealizado());

        ProntuarioDentista prontuario = prontuarioRepository.findById(request.getProntuarioDentistaId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "prontuario nao encontrado: " + request.getProntuarioDentistaId()));

        Profissional profissional = profissionalRepository.findById(profissionalId)
                .orElseThrow(() -> new IllegalArgumentException("Profissional nao encontrado: " + profissionalId));

        Paciente paciente = pacienteRepository.findById(request.getPacienteId())
                .orElseThrow(() -> new IllegalArgumentException("Paciente nao encontrado: " + request.getPacienteId()));

        Organizacao organizacao = new Organizacao();
        organizacao.setId(orgId);

        Consulta consulta = null;
        if (request.getConsultaId() != null) {
            consulta = consultaRepository.findById(request.getConsultaId()).orElse(null);
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
        if (!planejamentoRepository.existsById(id)) {
            throw new IllegalArgumentException("Planejamento nao encontrado: " + id);
        }
        planejamentoRepository.deleteById(id);
        log.info("Planejamento removido — id={}", id);
    }

    /**
     * Gera um token de assinatura para todos os itens do planejamento de um prontuario.
     *
     * @param prontuarioId ID do prontuario dentista
     * @return token gerado
     */
    @Transactional
    public String gerarLinkAssinatura(Long prontuarioId) {
        log.info("Gerando link de assinatura para prontuario={}", prontuarioId);

        List<PlanejamentoTerapeutico> planejamentos = planejamentoRepository.findByProntuarioId(prontuarioId);

        if (planejamentos.isEmpty()) {
            throw new IllegalStateException("Nenhum planejamento encontrado para este prontuario.");
        }

        // Gera um único token para todos os itens pendentes
        String token = UUID.randomUUID().toString();

        for (PlanejamentoTerapeutico p : planejamentos) {
            if (p.isPendente()) {
                p.setTokenAssinatura(token);
                planejamentoRepository.save(p);
            }
        }

        log.info("Token de assinatura gerado={} para {} itens", token, planejamentos.size());
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
        log.info("Assinando planejamento — token={}, ip={}", token, ipOrigem);

        int atualizados = planejamentoRepository.assinarPorToken(token, assinaturaBase64, ipOrigem);
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
