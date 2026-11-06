package br.com.saudeConecta.application.consulta;

import br.com.saudeConecta.domain.FusoHorario;
import br.com.saudeConecta.application.consulta.dto.EstatisticasDashboardAdminOrgResponse;
import br.com.saudeConecta.config.tenant.TenantHelper;
import br.com.saudeConecta.domain.common.Periodo;
import br.com.saudeConecta.domain.consulta.StatusConsulta;
import br.com.saudeConecta.infrastructure.persistence.repository.ConsultaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Contagens e estatísticas de consultas: os três dashboards (gestor, profissional e SUPER_ADMIN),
 * a contagem por status de um médico num intervalo e as durações das consultas realizadas.
 *
 * <p>Saiu do antigo {@code ConsultaService} na fase B6 sem mudança de comportamento (mesmas
 * queries, {@code @Cacheable} e validação de tenant). O saldo financeiro continua no
 * {@code SaldoFinanceiroService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConsultaEstatisticaService {

    private final ConsultaRepository consultaRepository;
    private final TenantHelper tenantHelper;

    // ===============================================================
    // BUSCAS DE ESTATISTICAS - Dashboard - Gestor
    // ===============================================================

    /**
     * Retorna todas as estatísticas do dashboard para AdminOrg em uma única query.
     * Calcula a semana atual (segunda a domingo) e o dia atual, agrupando por status.
     *
     * @param organizacaoId ID da organização
     * @return DTO com consultasHoje, consultasAguardando, consultasAtendidas,
     *         consultasSemana, canceladosSemana e confirmadosSemana
     */
    @Cacheable(value = "dashboard-admin-org", key = "@tenantHelper.getCurrentTenantIdOrNull()")
    @Transactional(readOnly = true)
    public EstatisticasDashboardAdminOrgResponse getEstatisticasDashboardAdminOrg(Long organizacaoId) {
        tenantHelper.validateTenant(organizacaoId);
        LocalDate hoje = LocalDate.now(FusoHorario.ZONA);
        Periodo semana = Periodo.semanaDe(hoje);
        Periodo dia = Periodo.dia(hoje);
        LocalDate inicioSemana = semana.dataInicio();
        LocalDate fimSemana = semana.dataFim();

        LocalDateTime inicioSemanaLdt = semana.inicio();
        LocalDateTime fimSemanaLdt    = semana.fim();
        LocalDateTime inicioDiaLdt    = dia.inicio();
        LocalDateTime fimDiaLdt       = dia.fim();

        log.debug("consulta.dashboard buscar perfil=ADMIN_ORG semanaInicio={} semanaFim={} hoje={}",
                inicioSemana, fimSemana, hoje);

        List<Object[]> rows = consultaRepository.findEstatisticasDashboardByOrganizacao(
            organizacaoId, inicioSemanaLdt, fimSemanaLdt, inicioDiaLdt, fimDiaLdt
        );

        long consultasHoje = 0L;
        long consultasAguardando = 0L;
        long consultasAtendidas = 0L;
        long consultasSemana = 0L;
        long canceladosSemana = 0L;
        long confirmadosSemana = 0L;

        for (Object[] row : rows) {
            StatusConsulta status = (StatusConsulta) row[0];
            String periodo = (String) row[1];
            long quantidade = ((Number) row[2]).longValue();

            consultasSemana += quantidade;

            boolean isHoje = "HOJE".equals(periodo);

            if (isHoje) {
                consultasHoje += quantidade;
                if (status == StatusConsulta.AGENDADA || status == StatusConsulta.CONFIRMADA) {
                    consultasAguardando += quantidade;
                }
                if (status == StatusConsulta.REALIZADA) {
                    consultasAtendidas += quantidade;
                }
            }

            if (status == StatusConsulta.CANCELADA) {
                canceladosSemana += quantidade;
            }
            if (status == StatusConsulta.CONFIRMADA) {
                confirmadosSemana += quantidade;
            }
        }

        return EstatisticasDashboardAdminOrgResponse.builder()
            .consultasHoje(consultasHoje)
            .consultasAguardando(consultasAguardando)
            .consultasAtendidas(consultasAtendidas)
            .consultasSemana(consultasSemana)
            .canceladosSemana(canceladosSemana)
            .confirmadosSemana(confirmadosSemana)
            .build();
    }

    // ===============================================================
    // BUSCAS DE ESTATISTICAS - Dashboard - PROFISSIONAL
    // ===============================================================

    /**
     * Retorna todas as estatísticas do dashboard para o Profissional em uma única query.
     * Filtra por usuario.id via JOIN com profissional — o profissional vê apenas seus próprios dados.
     *
     * @param usuarioId ID do usuário logado
     * @return DTO com consultasHoje, consultasAguardando, consultasAtendidas,
     *         consultasSemana, canceladosSemana e confirmadosSemana
     */
    @Cacheable(value = "dashboard-profissional", key = "#usuarioId")
    @Transactional(readOnly = true)
    public EstatisticasDashboardAdminOrgResponse getEstatisticasDashboardProfissional(Long usuarioId) {
        LocalDate hoje = LocalDate.now(FusoHorario.ZONA);
        Periodo semana = Periodo.semanaDe(hoje);
        Periodo dia = Periodo.dia(hoje);
        LocalDate inicioSemana = semana.dataInicio();
        LocalDate fimSemana = semana.dataFim();

        LocalDateTime inicioSemanaLdt = semana.inicio();
        LocalDateTime fimSemanaLdt    = semana.fim();
        LocalDateTime inicioDiaLdt    = dia.inicio();
        LocalDateTime fimDiaLdt       = dia.fim();

        log.debug("consulta.dashboard buscar perfil=PROFISSIONAL usuarioId={} semanaInicio={} semanaFim={} hoje={}",
                usuarioId, inicioSemana, fimSemana, hoje);

        List<Object[]> rows = consultaRepository.findEstatisticasDashboardByProfissional(
            usuarioId, inicioSemanaLdt, fimSemanaLdt, inicioDiaLdt, fimDiaLdt
        );

        long consultasHoje       = 0L;
        long consultasAguardando = 0L;
        long consultasAtendidas  = 0L;
        long consultasSemana     = 0L;
        long canceladosSemana    = 0L;
        long confirmadosSemana   = 0L;

        for (Object[] row : rows) {
            StatusConsulta status = (StatusConsulta) row[0];
            String periodo        = (String) row[1];
            long quantidade       = ((Number) row[2]).longValue();

            consultasSemana += quantidade;

            boolean isHoje = "HOJE".equals(periodo);

            if (isHoje) {
                consultasHoje += quantidade;
                if (status == StatusConsulta.AGENDADA || status == StatusConsulta.CONFIRMADA) {
                    consultasAguardando += quantidade;
                }
                if (status == StatusConsulta.REALIZADA) {
                    consultasAtendidas += quantidade;
                }
            }

            if (status == StatusConsulta.CANCELADA)  { canceladosSemana  += quantidade; }
            if (status == StatusConsulta.CONFIRMADA) { confirmadosSemana += quantidade; }
        }

        return EstatisticasDashboardAdminOrgResponse.builder()
            .consultasHoje(consultasHoje)
            .consultasAguardando(consultasAguardando)
            .consultasAtendidas(consultasAtendidas)
            .consultasSemana(consultasSemana)
            .canceladosSemana(canceladosSemana)
            .confirmadosSemana(confirmadosSemana)
            .build();
    }

    // ===============================================================
    // BUSCAS DE ESTATISTICAS - Dashboard - SUPER_ADMIN (global)
    // ===============================================================

    /**
     * Retorna todas as estatísticas do dashboard para SuperAdmin em uma única query global.
     * Sem filtro de organização — abrange todas as consultas do sistema.
     *
     * @return DTO com consultasHoje, consultasAguardando, consultasAtendidas,
     *         consultasSemana, canceladosSemana e confirmadosSemana
     */
    @Cacheable(value = "dashboard-super-admin", key = "'global'")
    @Transactional(readOnly = true)
    public EstatisticasDashboardAdminOrgResponse getEstatisticasDashboardSuperAdmin() {
        LocalDate hoje = LocalDate.now(FusoHorario.ZONA);
        Periodo semana = Periodo.semanaDe(hoje);
        Periodo dia = Periodo.dia(hoje);
        LocalDate inicioSemana = semana.dataInicio();
        LocalDate fimSemana = semana.dataFim();

        LocalDateTime inicioSemanaLdt = semana.inicio();
        LocalDateTime fimSemanaLdt    = semana.fim();
        LocalDateTime inicioDiaLdt    = dia.inicio();
        LocalDateTime fimDiaLdt       = dia.fim();

        log.debug("consulta.dashboard buscar perfil=SUPER_ADMIN semanaInicio={} semanaFim={} hoje={}",
                inicioSemana, fimSemana, hoje);

        List<Object[]> rows = consultaRepository.findEstatisticasDashboardGlobal(
            inicioSemanaLdt, fimSemanaLdt, inicioDiaLdt, fimDiaLdt
        );

        long consultasHoje      = 0L;
        long consultasAguardando = 0L;
        long consultasAtendidas  = 0L;
        long consultasSemana    = 0L;
        long canceladosSemana   = 0L;
        long confirmadosSemana  = 0L;

        for (Object[] row : rows) {
            StatusConsulta status  = (StatusConsulta) row[0];
            String periodo         = (String) row[1];
            long quantidade        = ((Number) row[2]).longValue();

            consultasSemana += quantidade;

            boolean isHoje = "HOJE".equals(periodo);

            if (isHoje) {
                consultasHoje += quantidade;
                if (status == StatusConsulta.AGENDADA || status == StatusConsulta.CONFIRMADA) {
                    consultasAguardando += quantidade;
                }
                if (status == StatusConsulta.REALIZADA) {
                    consultasAtendidas += quantidade;
                }
            }

            if (status == StatusConsulta.CANCELADA)  { canceladosSemana  += quantidade; }
            if (status == StatusConsulta.CONFIRMADA) { confirmadosSemana += quantidade; }
        }

        return EstatisticasDashboardAdminOrgResponse.builder()
            .consultasHoje(consultasHoje)
            .consultasAguardando(consultasAguardando)
            .consultasAtendidas(consultasAtendidas)
            .consultasSemana(consultasSemana)
            .canceladosSemana(canceladosSemana)
            .confirmadosSemana(confirmadosSemana)
            .build();
    }

    /**
     * Busca estatísticas de consultas por médico e intervalo de datas
     * Retorna uma lista de arrays onde:
     * - Posição 0: Status da consulta (enum StatusConsulta)
     * - Posição 1: Quantidade de consultas com esse status
     * 
     * @param organizacaoId ID da organização
     * @param usuarioId ID do usuário (será usado para buscar o profissional via JOIN)
     * @param dataInicio Data de início do período
     * @param dataFim Data de fim do período
     * @return Lista de Object[] com [status, quantidade]
     */
    @Transactional(readOnly = true)
    public List<Object[]> buscarEstatisticasPorMedicoEIntervalo(
            Long organizacaoId, 
            Long usuarioId, 
            LocalDate dataInicio, 
            LocalDate dataFim) {
        
        log.debug("consulta.estatisticas buscar usuarioId={} inicio={} fim={}",
                usuarioId, dataInicio, dataFim);
        
        Periodo periodo = Periodo.entre(dataInicio, dataFim);
        
        return consultaRepository.findEstatisticasPorMedicoEIntervalo(
            organizacaoId,
            usuarioId,
            periodo.inicio(),
            periodo.fim()
        );
    }

    /**
     * Busca durações de consultas realizadas por médico e intervalo
     * Retorna lista bruta de durações para cálculo no frontend
     * 
     * @param organizacaoId ID da organização
     * @param usuarioId ID do usuário (será usado para buscar o profissional via JOIN)
     * @param dataInicio Data de início do período
     * @param dataFim Data de fim do período
     * @return Lista de durações em minutos
     */
    @Transactional(readOnly = true)
    public List<Integer> buscarDuracoesConsultas(
            Long organizacaoId, 
            Long usuarioId, 
            LocalDate dataInicio, 
            LocalDate dataFim) {
        
        log.debug("consulta.duracoes buscar usuarioId={} dataInicio={} dataFim={}",
                usuarioId, dataInicio, dataFim);

        Periodo periodo = Periodo.entre(dataInicio, dataFim);

        List<Integer> duracoes = consultaRepository.findDuracoesConsultasPorMedicoEIntervalo(
            organizacaoId,
            usuarioId,
            periodo.inicio(),
            periodo.fim()
        );
        
        log.debug("consulta.duracoes resultado quantidade={}", duracoes.size());
        
        return duracoes;
    }
}
