package br.com.saudeConecta.application.consulta;

import br.com.saudeConecta.domain.FusoHorario;
import br.com.saudeConecta.config.tenant.RequiresTenant;
import br.com.saudeConecta.config.tenant.TenantHelper;
import br.com.saudeConecta.domain.common.Periodo;
import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.consulta.StatusConsulta;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.infrastructure.persistence.repository.ConsultaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import br.com.saudeConecta.infrastructure.persistence.specification.ConsultaSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Leituras de consultas da organização do token: por id, listas do gerenciamento (dia, semana,
 * mês e ano), filtros dinâmicos, intervalos e agenda do médico por período.
 *
 * <p>Saiu do antigo {@code ConsultaService} na fase B6 sem mudança de comportamento. As
 * sobrecargas que recebem o {@code Usuario} continuam chamando a versão por profissional
 * <b>dentro da própria classe</b>, como antes: essa chamada não passa pelo proxy, então não usa
 * o {@code @Cacheable} da versão chamada (comportamento preservado de propósito).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConsultaBuscaService {

    private final ConsultaRepository consultaRepository;
    private final ProfissionalRepository profissionalRepository;
    private final TenantHelper tenantHelper;

    @RequiresTenant
    @Transactional(readOnly = true)
    public Optional<Consulta> buscarPorId(Long id) {
        Long orgId = tenantHelper.getCurrentTenantId();
        return consultaRepository.findByIdAndOrganizacao_Id(id, orgId);
    }

    /**
     * Busca a consulta da organização do token já com profissional, paciente, especialidade e
     * forma de pagamento carregados, para montar o {@code ConsultaResponse} fora da transação
     * (open-in-view desligado). Sem o fetch, o proxy do profissional estoura LazyInitializationException.
     *
     * @param id ID da consulta
     * @return a consulta, ou vazio se não existir ou for de outra clínica
     */
    @RequiresTenant
    @Transactional(readOnly = true)
    public Optional<Consulta> buscarPorIdComRelacoes(Long id) {
        Long orgId = tenantHelper.getCurrentTenantId();
        return consultaRepository.findByIdAndOrganizacao_IdWithRelations(id, orgId);
    }
    
    /**
     * Busca as consultas do dia informado (ou do dia atual, se `data` for nulo).
     *
     * @param data Data de referência (opcional). Quando nula, usa a data atual do servidor.
     * @return Lista de consultas do dia de referência
     */
    @RequiresTenant
    @Cacheable(value = "consultas-hoje-org", key = "@tenantHelper.getCurrentTenantId() + '-' + (#profissionalId != null ? #profissionalId : 'all') + '-' + (#data != null ? #data : java.time.LocalDate.now(FusoHorario.ZONA))")
    @Transactional(readOnly = true)
    public List<Consulta> buscarConsultasHoje(LocalDate data, Long profissionalId) {
        Long orgId = tenantHelper.getCurrentTenantId();
        LocalDate referencia = data != null ? data : LocalDate.now(FusoHorario.ZONA);
        Periodo dia = Periodo.dia(referencia);
        return consultaRepository.findByOrganizacaoIdAndPeriodo(orgId, profissionalId, dia.inicio(), dia.fim());
    }

    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Consulta> buscarConsultasHoje(LocalDate data, br.com.saudeConecta.domain.usuario.Usuario usuario) {
        Long profissionalId = extrairProfissionalId(usuario);
        return buscarConsultasHoje(data, profissionalId);
    }

    @Cacheable(value = "consultas-hoje-org", key = "@tenantHelper.getCurrentTenantId() + '-' + (#data != null ? #data : java.time.LocalDate.now(FusoHorario.ZONA))")
    @Transactional(readOnly = true)
    public List<Consulta> buscarConsultasHoje(LocalDate data) {
        return buscarConsultasHoje(data, (Long) null);
    }

    /**
     * Busca as consultas da semana que contém a data informada (ou a semana atual, se `data` for nulo).
     *
     * @param data Data de referência (opcional). Quando nula, usa a data atual do servidor.
     * @return Lista de consultas da semana de referência
     */
    @RequiresTenant
    @Cacheable(value = "consultas-semana-org", key = "@tenantHelper.getCurrentTenantId() + '-' + (#profissionalId != null ? #profissionalId : 'all') + '-' + (#data != null ? #data : java.time.LocalDate.now(FusoHorario.ZONA))")
    @Transactional(readOnly = true)
    public List<Consulta> buscarConsultasDaSemanaAtual(LocalDate data, Long profissionalId) {
        Long orgId = tenantHelper.getCurrentTenantId();
        LocalDate referencia = data != null ? data : LocalDate.now(FusoHorario.ZONA);
        Periodo semana = Periodo.semanaDe(referencia);
        return consultaRepository.findByOrganizacaoIdAndPeriodo(orgId, profissionalId, semana.inicio(), semana.fim());
    }

    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Consulta> buscarConsultasDaSemanaAtual(LocalDate data, br.com.saudeConecta.domain.usuario.Usuario usuario) {
        Long profissionalId = extrairProfissionalId(usuario);
        return buscarConsultasDaSemanaAtual(data, profissionalId);
    }

    @RequiresTenant
    @Cacheable(value = "consultas-semana-org", key = "@tenantHelper.getCurrentTenantId() + '-' + (#data != null ? #data : java.time.LocalDate.now(FusoHorario.ZONA))")
    @Transactional(readOnly = true)
    public List<Consulta> buscarConsultasDaSemanaAtual(LocalDate data) {
        return buscarConsultasDaSemanaAtual(data, (Long) null);
    }

    /**
     * Busca as consultas do mês que contém a data informada (ou o mês atual, se `data` for nulo).
     *
     * @param data Data de referência (opcional). Quando nula, usa a data atual do servidor.
     * @return Lista de consultas do mês de referência
     */
    @RequiresTenant
    @Cacheable(value = "consultas-mes-org", key = "@tenantHelper.getCurrentTenantId() + '-' + (#profissionalId != null ? #profissionalId : 'all') + '-' + (#data != null ? #data : java.time.LocalDate.now(FusoHorario.ZONA))")
    @Transactional(readOnly = true)
    public List<Consulta> buscarConsultasDoMesAtual(LocalDate data, Long profissionalId) {
        Long orgId = tenantHelper.getCurrentTenantId();
        LocalDate referencia = data != null ? data : LocalDate.now(FusoHorario.ZONA);
        Periodo mes = Periodo.mesDe(referencia);
        return consultaRepository.findByOrganizacaoIdAndPeriodo(orgId, profissionalId, mes.inicio(), mes.fim());
    }

    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Consulta> buscarConsultasDoMesAtual(LocalDate data, br.com.saudeConecta.domain.usuario.Usuario usuario) {
        Long profissionalId = extrairProfissionalId(usuario);
        return buscarConsultasDoMesAtual(data, profissionalId);
    }

    @RequiresTenant
    @Cacheable(value = "consultas-mes-org", key = "@tenantHelper.getCurrentTenantId() + '-' + (#data != null ? #data : java.time.LocalDate.now(FusoHorario.ZONA))")
    @Transactional(readOnly = true)
    public List<Consulta> buscarConsultasDoMesAtual(LocalDate data) {
        return buscarConsultasDoMesAtual(data, (Long) null);
    }
    
    @RequiresTenant
    @Cacheable(value = "consultas-ano-org", key = "@tenantHelper.getCurrentTenantId()")
    @Transactional(readOnly = true)
    public List<Consulta> buscarConsultasDoAnoAtual() {
        Long orgId = tenantHelper.getCurrentTenantId();
        Periodo ano = Periodo.anoDe(LocalDate.now(FusoHorario.ZONA));
        return consultaRepository.findByOrganizacaoIdAndPeriodo(orgId, ano.inicio(), ano.fim());
    }

    /**
     * Busca consultas com filtros dinâmicos opcionais
     * 
     * @param profissionalId ID do profissional/médico (opcional)
     * @param especialidade Nome da especialidade (opcional)
     * @param dataInicio Data/hora inicial (opcional)
     * @param dataFim Data/hora final (opcional)
     * @param statusList Lista de status (opcional)
     * @return Lista de consultas que atendem aos filtros
     */
    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Consulta> buscarComFiltrosDinamicos(
            Long profissionalId,
            String especialidade,
            LocalDateTime dataInicio,
            LocalDateTime dataFim,
            List<StatusConsulta> statusList) {
        
        Long orgId = tenantHelper.getCurrentTenantId();
        
        log.debug("consulta.buscar filtros orgId={} profissionalId={} especialidade={} dataInicio={} dataFim={} status={}",
                orgId, profissionalId, especialidade, dataInicio, dataFim, statusList);
        
        return consultaRepository.findAll(
            ConsultaSpecification.buscarComFiltros(
                orgId,
                profissionalId,
                especialidade,
                dataInicio,
                dataFim,
                statusList
            )
        );
    }

    @Cacheable(value = "consultas-intervalo-org", key = "@tenantHelper.getCurrentTenantIdOrNull() + '-' + #dataInicio + '-' + #dataFim")
    @Transactional(readOnly = true)
    public List<Consulta> buscarConsultasPorOrganizacaoEIntervalo(Long organizacaoId, LocalDate dataInicio, LocalDate dataFim) {
        tenantHelper.validateTenant(organizacaoId);
        Periodo periodo = Periodo.entre(dataInicio, dataFim);
        return consultaRepository.findByOrganizacaoIdAndDataHoraBetweenWithRelations(
            organizacaoId,
            periodo.inicio(),
            periodo.fimExclusivo()
        );
    }

    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Consulta> pesquisarClinicasEmIntervaloDeDatas(Long profissionalId, LocalDate dataInicio, LocalDate dataFim, String status) {
        Long orgId = tenantHelper.getCurrentTenantId();

        log.debug("consulta.pesquisar inicio profissionalId={} dataInicio={} dataFim={} status={}",
                profissionalId, dataInicio, dataFim, status);

        StatusConsulta statusEnum = converterStatus(status);

        Periodo periodo = Periodo.entre(dataInicio, dataFim);

        return consultaRepository.findByOrganizacaoIdAndUsuarioIdAndStatusOptionalAndDataHoraBetween(
            orgId,
            profissionalId,
            statusEnum,
            periodo.inicio(),
            periodo.fimExclusivo()
        );
    }

    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Consulta> buscarConsultasPorIntervalo(LocalDate dataInicio, LocalDate dataFim, String status) {
        Long orgId = tenantHelper.getCurrentTenantId();
        
        StatusConsulta statusEnum = converterStatus(status);

        Periodo periodo = Periodo.entre(dataInicio, dataFim);

        return consultaRepository.findByStatusOptionalAndDataHoraBetweenWithRelations(
            orgId,
            statusEnum,
            periodo.inicio(),
            periodo.fimExclusivo()
        );
    }

    /**
     * Converte o status recebido na requisicao para o enum correspondente.
     *
     * @param status nome do status; nulo, vazio ou "ALL" significa "sem filtro de status"
     * @return o enum correspondente ou null quando nao houver filtro
     * @throws IllegalArgumentException se o valor informado nao for um status valido
     */
    private StatusConsulta converterStatus(String status) {
        if (status == null || status.isBlank() || status.trim().equalsIgnoreCase("ALL")) {
            return null;
        }
        try {
            return StatusConsulta.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Status de consulta invalido: " + status);
        }
    }

    /**
     * Busca consultas de um médico por período (diário, semanal, mensal, anual)
     * Calcula automaticamente o intervalo de datas baseado no tipo de período
     * 
     * @param usuarioId ID do usuário do profissional
     * @param tipoPeriodo Tipo do período: "diario", "semanal", "mensal", "anual"
     * @return Lista de consultas do médico no período especificado
     */
    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Consulta> buscarConsultasPorMedicoEPeriodo(Long usuarioId, String tipoPeriodo) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.debug("consulta.buscar periodo usuarioId={} tipoPeriodo={}",
                usuarioId, tipoPeriodo);

        LocalDate hoje = LocalDate.now(FusoHorario.ZONA);
        Periodo periodo = switch (tipoPeriodo.toLowerCase()) {
            case "diario" -> Periodo.dia(hoje);
            case "semanal" -> Periodo.semanaDe(hoje);   // segunda a domingo
            case "mensal" -> Periodo.mesDe(hoje);
            case "anual" -> Periodo.anoDe(hoje);
            default -> {
                log.warn("consulta.periodo negado motivo=TIPO_INVALIDO tipo={} acao=USA_DIARIO", tipoPeriodo);
                yield Periodo.dia(hoje);
            }
        };
        LocalDateTime inicio = periodo.inicio();
        LocalDateTime fim = periodo.fim();

        log.debug("consulta.periodo calculado inicio={} fim={}", inicio, fim);
        return consultaRepository.findConsultasPorMedicoEPeriodo(orgId, usuarioId, inicio, fim);
    }

    /**
     * Extrai o ID do profissional a partir do usuário logado.
     * Se o usuário não for um profissional, retorna null.
     *
     * @param usuario Usuário logado
     * @return ID do profissional ou null se não for profissional
     */
    private Long extrairProfissionalId(br.com.saudeConecta.domain.usuario.Usuario usuario) {
        if (usuario == null || !usuario.isClinico()) {
            return null;
        }
        return profissionalRepository.findByUsuarioIdWithRelations(usuario.getId())
                .map(Profissional::getId)
                .orElse(null);
    }
}
