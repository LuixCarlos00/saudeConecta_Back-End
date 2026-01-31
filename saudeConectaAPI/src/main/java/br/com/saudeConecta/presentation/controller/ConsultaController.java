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
public class ConsultaController {

    private final ConsultaService consultaService;
    private final MedicoRepository medicoRepository;
    private final PacienteRepository pacienteRepository;
    private final AdministradorRepository administradorRepository;

    @GetMapping("/buscarId/{id}")
    @Transactional
    public ResponseEntity<ConsultaResponse> buscarPorId(@PathVariable Long id) {
        log.debug("Buscando consulta por ID: {}", id);
        return consultaService.buscarPorId(id)
                .map(consulta -> ResponseEntity.ok(new ConsultaResponse(consulta)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/listarTodos")
    @Transactional
    public ResponseEntity<List<ConsultaResponse>> buscarTodos() {
        log.debug("Buscando todas as consultas");
        List<ConsultaResponse> consultas = consultaService.buscarTodos().stream()
                .map(ConsultaResponse::new)
                .toList();
        return ResponseEntity.ok(consultas);
    }

    @GetMapping("/pagina")
    @Transactional
    public ResponseEntity<Page<ConsultaResponse>> buscarPorPaginas(
            @PageableDefault(size = 12, sort = {"conData"}) Pageable paginacao) {
        log.debug("Buscando consultas com paginação");
        Page<ConsultaResponse> consultas = consultaService.buscarTodos(paginacao)
                .map(ConsultaResponse::new);
        return ResponseEntity.ok(consultas);
    }

    @GetMapping("/buscarPorMedico/{medicoId}")
    @Transactional
    public ResponseEntity<List<ConsultaResponse>> buscarPorMedico(@PathVariable Long medicoId) {
        log.debug("Buscando consultas por médico: {}", medicoId);
        List<ConsultaResponse> consultas = consultaService.buscarPorMedico(medicoId).stream()
                .map(ConsultaResponse::new)
                .toList();
        return ResponseEntity.ok(consultas);
    }

    @GetMapping("/buscarPorPaciente/{pacienteId}")
    @Transactional
    public ResponseEntity<List<ConsultaResponse>> buscarPorPaciente(@PathVariable Long pacienteId) {
        log.debug("Buscando consultas por paciente: {}", pacienteId);
        List<ConsultaResponse> consultas = consultaService.buscarPorPaciente(pacienteId).stream()
                .map(ConsultaResponse::new)
                .toList();
        return ResponseEntity.ok(consultas);
    }

    @GetMapping("/buscarPorEspecialidade/{especialidade}")
    @Transactional
    public ResponseEntity<List<ConsultaResponse>> buscarPorEspecialidade(@PathVariable String especialidade) {
        log.debug("Buscando consultas por especialidade: {}", especialidade);
        List<ConsultaResponse> consultas = consultaService.buscarPorEspecialidade(especialidade).stream()
                .map(ConsultaResponse::new)
                .toList();
        return ResponseEntity.ok(consultas);
    }

    @GetMapping("/buscarEmIntervalo")
    @Transactional
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
    public ResponseEntity<ConsultaResponse> cadastrarConsulta(
            @RequestBody @Valid CadastrarConsultaRequest dados,
            UriComponentsBuilder uriBuilder) {
        
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
}
