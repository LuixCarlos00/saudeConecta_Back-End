package br.com.saudeConecta.presentation.controller.consulta;

import br.com.saudeConecta.application.consulta.ConsultaEstatisticaService;
import br.com.saudeConecta.application.consulta.dto.EstatisticasDashboardAdminOrgResponse;
import br.com.saudeConecta.application.dashboard.SaldoFinanceiroService;
import br.com.saudeConecta.application.dashboard.dto.SaldoFinanceiroResponse;
import br.com.saudeConecta.config.tenant.TenantAccessDeniedException;
import br.com.saudeConecta.config.tenant.TenantContext;
import br.com.saudeConecta.config.tenant.TenantHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Estatísticas de consultas para os dashboards: contadores do gestor, do profissional e do
 * SUPER_ADMIN, contagem por status e durações de um médico, e o saldo financeiro.
 *
 * <p>Saiu do {@code ConsultaController} na fase B6. Usa o mesmo {@code @RequestMapping("/consultas")}
 * e as mesmas rotas, {@code @PreAuthorize} e validações de tenant: nenhuma URL mudou.
 */
@RestController
@RequestMapping("/consultas")
@RequiredArgsConstructor
@Slf4j
public class ConsultaEstatisticaController {

    private final ConsultaEstatisticaService consultaEstatisticaService;
    private final SaldoFinanceiroService saldoFinanceiroService;
    private final TenantHelper tenantHelper;

    // ===============================================================
    // BUSCAS DE ESTATISTICAS - Dashboard - Gestor
    // ===============================================================

    /**
     * Endpoint único que retorna todas as estatísticas do dashboard para AdminOrg.
     * Realiza uma única query ao banco agrupando por status e período (hoje vs semana).
     *
     * @param organizacaoId ID da organização
     * @return DTO com consultasHoje, consultasAguardando, consultasAtendidas,
     *         consultasSemana, canceladosSemana e confirmadosSemana
     */
    @GetMapping("/estatisticas/organizacao/{organizacaoId}/dashboard")
    public ResponseEntity<EstatisticasDashboardAdminOrgResponse> getEstatisticasDashboardAdminOrg(
            @PathVariable Long organizacaoId) {
        // Validado antes do service: o @Cacheable usa o orgId como chave e responderia do cache sem checar.
        tenantHelper.validateTenant(organizacaoId);

        EstatisticasDashboardAdminOrgResponse response =
            consultaEstatisticaService.getEstatisticasDashboardAdminOrg(organizacaoId);

        return ResponseEntity.ok(response);
    }

    // ===============================================================
    // BUSCAS DE ESTATISTICAS - Dashboard - PROFISSIONAL
    // ===============================================================

    /**
     * Endpoint único que retorna todas as estatísticas do dashboard para o Profissional.
     * Filtra por usuario.id via JOIN — o profissional vê apenas seus próprios dados.
     *
     * @param usuarioId ID do usuário logado
     * @return DTO com consultasHoje, consultasAguardando, consultasAtendidas,
     *         consultasSemana, canceladosSemana e confirmadosSemana
     */
    @GetMapping("/estatisticas/dashboard/profissional")
    public ResponseEntity<EstatisticasDashboardAdminOrgResponse> getEstatisticasDashboardProfissional(
            @RequestParam Long usuarioId) {
        // A query do dashboard filtra só por usuário (sem organização): restringe ao próprio usuário do token,
        // o que também torna a chave de cache (#usuarioId) a do usuário validado.
        if (!usuarioId.equals(tenantHelper.getCurrentUserId())) {
            throw new TenantAccessDeniedException("Acesso negado: dashboard de outro usuário");
        }
        return ResponseEntity.ok(consultaEstatisticaService.getEstatisticasDashboardProfissional(usuarioId));
    }

    // ===============================================================
    // BUSCAS DE ESTATISTICAS - Dashboard - SUPER_ADMIN (global)
    // ===============================================================

    /**
     * Endpoint único que retorna todas as estatísticas do dashboard para SuperAdmin.
     * Sem filtro de organização — cobre todas as consultas do sistema.
     *
     * @return DTO com consultasHoje, consultasAguardando, consultasAtendidas,
     *         consultasSemana, canceladosSemana e confirmadosSemana
     */
    @GetMapping("/estatisticas/dashboard/super-admin")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<EstatisticasDashboardAdminOrgResponse> getEstatisticasDashboardSuperAdmin() {
        return ResponseEntity.ok(consultaEstatisticaService.getEstatisticasDashboardSuperAdmin());
    }

    // ==========================================
    // ESTATÍSTICAS POR MÉDICO E INTERVALO
    // ==========================================

    /**
     * Busca estatísticas de consultas por médico e intervalo de datas
     * Retorna contagem de consultas agrupadas por status
     * 
     * Exemplo de resposta:
     * [
     *   [0, 5],  // 5 consultas AGENDADAS
     *   [1, 3],  // 3 consultas REALIZADAS
     *   [2, 1]   // 1 consulta CANCELADA
     * ]
     * 
     * @param medicoId ID do usuário do médico (será usado para buscar o profissional via JOIN)
     * @param dataInicial Data inicial do período (formato: yyyy-MM-dd)
     * @param dataFinal Data final do período (formato: yyyy-MM-dd)
     * @return Lista de arrays com [statusOrdinal, quantidade]
     */
    @GetMapping("/estatisticas/medico/{medicoId}")
    public ResponseEntity<List<Object[]>> buscarEstatisticasPorMedicoEIntervalo(
            @PathVariable Long medicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal) {
        
        
        Long organizacaoId = TenantContext.getCurrentTenant();
        
        if (organizacaoId == null) {
            log.warn("consulta.estatisticas negado motivo=SEM_ORGANIZACAO");
            return ResponseEntity.badRequest().build();
        }
        
        List<Object[]> estatisticas = consultaEstatisticaService.buscarEstatisticasPorMedicoEIntervalo(
            organizacaoId, 
            medicoId, 
            dataInicial, 
            dataFinal
        );
        
        return ResponseEntity.ok(estatisticas);
    }

    /**
     * Busca durações de consultas realizadas por médico e intervalo
     * Retorna lista bruta de durações para cálculo no frontend
     * 
     * Exemplo de resposta: [30, 45, 25, 60, 35, 40, 50]
     * 
     * @param medicoId ID do usuário do médico (será usado para buscar o profissional via JOIN)
     * @param dataInicial Data inicial do período (formato: yyyy-MM-dd)
     * @param dataFinal Data final do período (formato: yyyy-MM-dd)
     * @return Lista de durações em minutos
     */
    @GetMapping("/estatisticas/medico/{medicoId}/duracoes")
    public ResponseEntity<List<Integer>> buscarDuracoesConsultas(
            @PathVariable Long medicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal) {
        
        
        Long organizacaoId = TenantContext.getCurrentTenant();
        
        if (organizacaoId == null) {
            log.warn("consulta.estatisticas negado motivo=SEM_ORGANIZACAO");
            return ResponseEntity.badRequest().build();
        }
        
        List<Integer> duracoes = consultaEstatisticaService.buscarDuracoesConsultas(
            organizacaoId, 
            medicoId, 
            dataInicial, 
            dataFinal
        );
        
        return ResponseEntity.ok(duracoes);
    }

    // ==========================================
    // SALDO FINANCEIRO (DASHBOARD)
    // ==========================================

    /**
     * Retorna estatísticas financeiras: consultas realizadas + procedimentos terapêuticos.
     *
     * @param inicio    data de início (ISO: yyyy-MM-dd)
     * @param fim       data de fim (ISO: yyyy-MM-dd)
     * @param agruparPor "mes" (padrão) ou "semana"
     * @return SaldoFinanceiroResponse com totais e detalhamento
     */
    @GetMapping("/estatisticas/saldo-financeiro")
    public ResponseEntity<SaldoFinanceiroResponse> getSaldoFinanceiro(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(defaultValue = "mes") String agruparPor) {

        Long organizacaoId = TenantContext.getCurrentTenant();
        if (organizacaoId == null) {
            log.warn("consulta.saldoFinanceiro negado motivo=SEM_ORGANIZACAO");
            return ResponseEntity.badRequest().build();
        }

        SaldoFinanceiroResponse response = saldoFinanceiroService
                .calcularSaldo(organizacaoId, inicio, fim, agruparPor);
        return ResponseEntity.ok(response);
    }
}
