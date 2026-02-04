package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.application.service.ConsultaService;
import br.com.saudeConecta.domain.administrador.Administrador;
import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.medico.Medico;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.infrastructure.persistence.repository.AdministradorRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.MedicoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.PacienteRepository;
import br.com.saudeConecta.presentation.dto.consulta.CadastrarConsultaRequest;
import br.com.saudeConecta.presentation.dto.consulta.ConsultaResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jdk.jfr.Description;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/consulta")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
@Description("Endpoints para gerenciamento de consultas")
public class ConsultaController {

    private final ConsultaService consultaService;
    private final MedicoRepository medicoRepository;
    private final PacienteRepository pacienteRepository;
    private final AdministradorRepository administradorRepository;

    @GetMapping("/buscarId/{id}")
    @Transactional
    @Description( "Busca consulta por ID. Utilizado em: AppointmentDetailComponent, AppointmentService")
    public ResponseEntity<ConsultaResponse> buscarPorId(@PathVariable Long id) {
        log.debug("Buscando consulta por ID: {}", id);
        return consultaService.buscarPorId(id)
                .map(consulta -> ResponseEntity.ok(new ConsultaResponse(consulta)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/listarTodos")
    @Transactional
    @Description( "Lista todas as consultas. Utilizado em: AppointmentListComponent, AppointmentService")
    public ResponseEntity<List<ConsultaResponse>> buscarTodos() {
        log.debug("Buscando todas as consultas");
        List<ConsultaResponse> consultas = consultaService.buscarTodos().stream()
                .map(ConsultaResponse::new)
                .toList();
        return ResponseEntity.ok(consultas);
    }

    @GetMapping("/pagina")
    @Transactional
    @Description( "Busca consultas com paginação. Utilizado em: AppointmentTableComponent, AppointmentService")
    public ResponseEntity<Page<ConsultaResponse>> buscarPorPaginas(
            @PageableDefault(size = 12, sort = {"conData"}) Pageable paginacao) {
        log.debug("Buscando consultas com paginação");
        Page<ConsultaResponse> consultas = consultaService.buscarTodos(paginacao)
                .map(ConsultaResponse::new);
        return ResponseEntity.ok(consultas);
    }

    @GetMapping("/buscarPorMedico/{medicoId}")
    @Transactional
    @Description( "Busca consultas por médico. Utilizado em: DoctorAppointmentsComponent, AppointmentService")
    public ResponseEntity<List<ConsultaResponse>> buscarPorMedico(@PathVariable Long medicoId) {
        log.debug("Buscando consultas por médico: {}", medicoId);
        List<ConsultaResponse> consultas = consultaService.buscarPorMedico(medicoId).stream()
                .map(ConsultaResponse::new)
                .toList();
        return ResponseEntity.ok(consultas);
    }

    @GetMapping("/buscarPorPaciente/{pacienteId}")
    @Transactional
    @Description( "Busca consultas por paciente. Utilizado em: PatientAppointmentsComponent, AppointmentService")
    public ResponseEntity<List<ConsultaResponse>> buscarPorPaciente(@PathVariable Long pacienteId) {
        log.debug("Buscando consultas por paciente: {}", pacienteId);
        List<ConsultaResponse> consultas = consultaService.buscarPorPaciente(pacienteId).stream()
                .map(ConsultaResponse::new)
                .toList();
        return ResponseEntity.ok(consultas);
    }

    @GetMapping("/buscarPorEspecialidade/{especialidade}")
    @Transactional
    @Description( "Busca consultas por especialidade. Utilizado em: SpecialtyFilterComponent, AppointmentService")
    public ResponseEntity<List<ConsultaResponse>> buscarPorEspecialidade(@PathVariable String especialidade) {
        log.debug("Buscando consultas por especialidade: {}", especialidade);
        List<ConsultaResponse> consultas = consultaService.buscarPorEspecialidade(especialidade).stream()
                .map(ConsultaResponse::new)
                .toList();
        return ResponseEntity.ok(consultas);
    }

    @GetMapping("/buscarEmIntervalo")
    @Transactional
    @Description( "Busca consultas em intervalo de datas. Utilizado em: DateRangeFilterComponent, AppointmentService")
    public ResponseEntity<List<ConsultaResponse>> buscarEmIntervalo(
            @RequestParam String dataInicial,
            @RequestParam String dataFinal) {
        log.debug("Buscando consultas em intervalo: {} a {}", dataInicial, dataFinal);
        List<ConsultaResponse> consultas = consultaService.buscarEmIntervaloDatas(dataInicial, dataFinal).stream()
                .map(ConsultaResponse::new)
                .toList();
        return ResponseEntity.ok(consultas);
    }

    @GetMapping("/verificarDisponibilidade")
    @Transactional
    @Description( "Verifica disponibilidade de hor\u00e1rio para consulta. Utilizado em: AppointmentSchedulerComponent, SchedulingService")
    public ResponseEntity<Boolean> verificarDisponibilidade(
            @RequestParam String horario,
            @RequestParam String data,
            @RequestParam Long medicoId) {
        log.debug("Verificando disponibilidade: {} {} {}", horario, data, medicoId);
        boolean existe = consultaService.existePorHorarioDataEMedico(horario, data, medicoId);
        return ResponseEntity.ok(!existe); // Retorna true se está disponível
    }

    // Estatísticas
    @GetMapping("/contarPorData/{data}")
    @Transactional
    public ResponseEntity<Long> contarPorData(@PathVariable String data) {
        log.debug("Contando consultas por data: {}", data);
        Long count = consultaService.contarPorData(data);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/contarRealizadasPorData/{data}")
    @Transactional
    public ResponseEntity<Long> contarRealizadasPorData(@PathVariable String data) {
        log.debug("Contando consultas realizadas por data: {}", data);
        Long count = consultaService.contarRealizadasPorData(data);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/contarAgendadasPorData/{data}")
    @Transactional
    public ResponseEntity<Long> contarAgendadasPorData(@PathVariable String data) {
        log.debug("Contando consultas agendadas por data: {}", data);
        Long count = consultaService.contarAgendadasPorData(data);
        return ResponseEntity.ok(count);
    }

    @PostMapping("/cadastrar")
    @Transactional
    @Description( "Cadastra nova consulta. Utilizado em: ScheduleAppointmentComponent, AppointmentService")
    public ResponseEntity<ConsultaResponse> cadastrarConsulta(
            @RequestBody @Valid CadastrarConsultaRequest dados,
            UriComponentsBuilder uriBuilder) {
        //todo arruar a regra de negonio para nao cadastra aqui
        log.debug("Cadastrando consulta: {}", dados.conData());
        
        // Verificar se já existe consulta no mesmo horário
        if (consultaService.existePorHorarioDataEMedico(dados.conHorario(), dados.conData(), dados.conMedico())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        
        var medicoOptional = medicoRepository.findById(dados.conMedico());
        if (medicoOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var pacienteOptional = pacienteRepository.findById(dados.conPaciente());
        if (pacienteOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var admOptional = administradorRepository.findById(dados.conAdm());
        if (admOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Medico medico = medicoOptional.get();
        Paciente paciente = pacienteOptional.get();
        Administrador adm = admOptional.get();
        
        Consulta consulta = new Consulta(medico, paciente, adm, dados);
        
        Consulta consultaSalva = consultaService.cadastrar(consulta);

        URI uri = uriBuilder.path("/consulta/buscarId/{id}")
                .buildAndExpand(consultaSalva.getConCodigoConsulta())
                .toUri();

        return ResponseEntity.created(uri).body(new ConsultaResponse(consultaSalva));
    }

    @DeleteMapping("/{id}")
    @Description( "Exclui consulta por ID. Utilizado em: AppointmentListComponent, AppointmentService")
    public ResponseEntity<Void> deleteConsultaById(@PathVariable Long id) {
        log.debug("Deletando consulta por ID: {}", id);
        try {
            consultaService.deletar(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Erro ao deletar consulta ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==========================================
    // ENDPOINTS DE ESTATÍSTICAS  
    // ==========================================

    @GetMapping("/estatisticas/consultas-agendadas-hoje")
    @Description( "Conta consultas agendadas para hoje. Utilizado em: DashboardComponent, StatisticsService")
    public ResponseEntity<Long> contarConsultasAgendadasHoje() {
        return ResponseEntity.ok(consultaService.contarConsultasAgendadasHoje());
    }

    @GetMapping("/estatisticas/consultas-hoje")
    @Description( "Conta todas as consultas de hoje. Utilizado em: DashboardComponent, StatisticsService")
    public ResponseEntity<Long> contarConsultasHoje() {
        return ResponseEntity.ok(consultaService.contarConsultasHoje());
    }

    @GetMapping("/estatisticas/consultas-realizadas-hoje")
    @Description( "Conta consultas realizadas hoje. Utilizado em: DashboardComponent, StatisticsService")
    public ResponseEntity<Long> contarConsultasRealizadasHoje() {
        return ResponseEntity.ok(consultaService.contarConsultasRealizadasHoje());
    }

    @GetMapping("/estatisticas/consultas-semana")
    @Description( "Conta consultas da semana atual. Utilizado em: WeeklyReportComponent, StatisticsService")
    public ResponseEntity<Long> contarConsultasDaSemanaAtual() {
        return ResponseEntity.ok(consultaService.contarConsultasDaSemanaAtual());
    }

    @GetMapping("/estatisticas/medicos-ativos")
    @Description( "Conta m\u00e9dicos ativos. Utilizado em: DashboardComponent, StatisticsService")
    public ResponseEntity<Long> contarMedicosAtivos() {
        return ResponseEntity.ok(consultaService.contarMedicosAtivos());
    }

    // URLs simplificadas substituem as anteriores:
    // Usar /buscarEmIntervalo para busca geral por intervalo
    // ENDPOINTS LONGOS REMOVIDOS POR DUPLICA\u00c7\u00c3O

    // ENDPOINT REMOVIDO - DUPLICA\u00c7\u00c3O DE /verificarDisponibilidade

    @GetMapping("/dia-atual")
    @Description( "Busca consultas do dia atual. Utilizado em: TodayAppointmentsComponent, AppointmentService")
    public ResponseEntity<List<ConsultaResponse>> buscarConsultasDoDiaAtual() {
        List<Consulta> consultas = consultaService.buscarConsultasDoDiaAtual();
        List<ConsultaResponse> resultado = consultas.stream().map(ConsultaResponse::new).toList();
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/semana-atual")
    @Description( "Busca consultas da semana atual. Utilizado em: WeeklyAppointmentsComponent, AppointmentService")
    public ResponseEntity<List<ConsultaResponse>> buscarConsultasDaSemanaAtual() {
        List<Consulta> consultas = consultaService.buscarConsultasDaSemanaAtual();
        List<ConsultaResponse> resultado = consultas.stream().map(ConsultaResponse::new).toList();
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/mes-atual")
    @Description( "Busca consultas do m\u00eas atual. Utilizado em: MonthlyAppointmentsComponent, AppointmentService")
    public ResponseEntity<List<ConsultaResponse>> buscarConsultasDoMesAtual() {
        List<Consulta> consultas = consultaService.buscarConsultasDoMesAtual();
        List<ConsultaResponse> resultado = consultas.stream().map(ConsultaResponse::new).toList();
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/ano-atual")
    @Description( "Busca consultas do ano atual. Utilizado em: YearlyAppointmentsComponent, AppointmentService")
    public ResponseEntity<List<ConsultaResponse>> buscarConsultasDoAnoAtual() {
        List<Consulta> consultas = consultaService.buscarConsultasDoAnoAtual();
        List<ConsultaResponse> resultado = consultas.stream().map(ConsultaResponse::new).toList();
        return ResponseEntity.ok(resultado);
    }

    @PutMapping("/{id}/concluir")
    @Description( "Marca consulta como conclu\u00edda. Utilizado em: AppointmentDetailComponent, AppointmentService")
    public ResponseEntity<ConsultaResponse> concluirConsulta(@PathVariable Long id) {
        Consulta consulta = consultaService.concluirConsulta(id);
        return ResponseEntity.ok(new ConsultaResponse(consulta));
    }

    @PutMapping("/{id}")
    @Description( "Atualiza dados da consulta. Utilizado em: EditAppointmentComponent, AppointmentService")
    public ResponseEntity<ConsultaResponse> atualizarConsulta(
            @PathVariable Long id,
            @RequestBody @Valid CadastrarConsultaRequest dados) {
        
        // Criar objeto Consulta com os dados atualizados
        Consulta consultaAtualizada = new Consulta();
        consultaAtualizada.setConData(dados.conData());
        consultaAtualizada.setConHorario(dados.conHorario());
        consultaAtualizada.setConStatus(dados.conStatus());
        
        Consulta consulta = consultaService.atualizarConsulta(id, consultaAtualizada);
        return ResponseEntity.ok(new ConsultaResponse(consulta));
    }

    @GetMapping("/horarios-ocupados")
    @Description( "Busca hor\u00e1rios ocupados de um m\u00e9dico em determinada data. Utilizado em: AppointmentSchedulerComponent, SchedulingService")
    public ResponseEntity<List<String>> buscarHorariosOcupados(
            @RequestParam Long medicoId,
            @RequestParam String data) {
        List<String> horarios = consultaService.buscarHorariosOcupados(medicoId, data);
        return ResponseEntity.ok(horarios);
    }

    @GetMapping("/agenda-medico/{idUsuarioMedico}")
    @Description( "Busca agenda de um m\u00e9dico espec\u00edfico. Utilizado em: DoctorAgendaComponent, AppointmentService")
    public ResponseEntity<List<ConsultaResponse>> buscarAgendaMedico(@PathVariable Long idUsuarioMedico) {
        List<Consulta> agenda = consultaService.buscarAgendaMedico(idUsuarioMedico);
        List<ConsultaResponse> resultado = agenda.stream().map(ConsultaResponse::new).toList();
        return ResponseEntity.ok(resultado);
    }

    // ENDPOINT REMOVIDO - DUPLICA\u00c7\u00c3O DE /listarTodos

    @GetMapping("/historico-medico/{idUsuarioMedico}")
    @Description( "Busca hist\u00f3rico de consultas de um m\u00e9dico. Utilizado em: DoctorHistoryComponent, AppointmentService")
    public ResponseEntity<List<ConsultaResponse>> buscarHistoricoAgendaMedico(@PathVariable Long idUsuarioMedico) {
        List<Consulta> consultas = consultaService.buscarHistoricoAgendaMedico(idUsuarioMedico);
        List<ConsultaResponse> resultado = consultas.stream().map(ConsultaResponse::new).toList();
        return ResponseEntity.ok(resultado);
    }

    // ENDPOINT REMOVIDO - URL excessivamente longa. Use /buscarEmIntervalo + /buscarPorEspecialidade

    // ENDPOINT REMOVIDO - URL excessivamente longa. Use /buscarPorMedico/{medicoId} combinado com filtros

    // ENDPOINTS REMOVIDOS - URLs excessivamente longas
    // Use endpoints simplificados existentes com parâmetros de query quando necessário

    @GetMapping("/estatisticas/medico/{medicoId}")
    @Description( "Estatísticas de consultas por médico e período. Utilizado em: DoctorStatisticsComponent, StatisticsService")
    public ResponseEntity<List<Object[]>> contarConsultasPorStatusEMedico(
            @PathVariable Long medicoId,
            @RequestParam String dataInicial,
            @RequestParam String dataFinal) {
        List<Object[]> estatisticas = consultaService.contarPorStatusEMedico(medicoId, dataInicial, dataFinal);
        return ResponseEntity.ok(estatisticas);
    }
}
