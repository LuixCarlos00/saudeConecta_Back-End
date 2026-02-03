package br.com.saudeConecta.endpoinst.consulta.Resource;

import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.endpoinst.consulta.DTO.DadosCadastraConsulta;
import br.com.saudeConecta.endpoinst.consulta.DTO.DadosConsultaView;
import br.com.saudeConecta.endpoinst.consulta.DTO.DadosSeendToNewMenssage;
import br.com.saudeConecta.endpoinst.consulta.Service.ConsultaService;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/consulta")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
@RequiredArgsConstructor
public class ConsultaResource {

    private final ConsultaService service;

    @GetMapping("/{id}")
    public ResponseEntity<DadosConsultaView> buscarPorId(@PathVariable Long id) {
        return service.buscarConsultaPorId(id)
                .map(consulta -> ResponseEntity.ok(new DadosConsultaView(consulta)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/verificar-disponibilidade/data={data}&horario={horario}&medico={medicoId}")
    public ResponseEntity<Boolean> verificarDisponibilidadeHorario(
            @PathVariable String data,
            @PathVariable String horario,
            @PathVariable Long medicoId) {
        Boolean disponivel = service.verificarDisponibilidadeHorario(data, horario, medicoId);
        return ResponseEntity.ok(disponivel);
    }

    @PostMapping
    public ResponseEntity<DadosConsultaView> criarConsulta(@RequestBody @Valid DadosCadastraConsulta dados) {
        DadosConsultaView consultaCriada = service.cadastrarConsulta(dados);
        return ResponseEntity.status(HttpStatus.CREATED).body(consultaCriada);
    }

    @GetMapping
    public ResponseEntity<List<DadosConsultaView>> listarTodasConsultas() {
        List<Consulta> consultas = service.listarTodasConsultas();
        List<DadosConsultaView> resultado = consultas.stream().map(DadosConsultaView::new).toList();
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/BuscandoTodasConsultasEmIntervaloDeDatas/dataInicial={dataInicial}&dataFinal={dataFinal}")
    public ResponseEntity<List<Consulta>> buscarConsultasPorIntervalo(
            @PathVariable String dataInicial,
            @PathVariable String dataFinal) {
        return ResponseEntity.ok(service.buscarConsultasPorIntervalo(dataInicial, dataFinal));
    }

    @GetMapping("/BuscandoTodasConsultasEmIntervaloDeDatasComEspecialidade/dataInicial={dataInicial}&dataFinal={dataFinal}&especialidades={especialidade}")
    public ResponseEntity<List<Consulta>> buscarConsultasPorIntervaloEEspecialidade(
            @PathVariable String dataInicial,
            @PathVariable String dataFinal,
            @PathVariable String especialidade) {
        return ResponseEntity.ok(service.buscarConsultasPorIntervaloEEspecialidade(dataInicial, dataFinal, especialidade));
    }

    @GetMapping("/BuscandoTodasConsultasPorMedico/{medicoId}")
    public ResponseEntity<List<Consulta>> buscarConsultasPorMedico(@PathVariable Long medicoId) {
        return ResponseEntity.ok(service.buscarConsultasPorMedico(medicoId));
    }

    @GetMapping("/BuscandoTodasConsultasPorMedicoEmIntervaloDeDatas/medico={medCodigo}&dataInicial={DataInicioFormatada}&dataFinal={DataFimFormatada}")
    public ResponseEntity<List<Consulta>> buscarConsultasPorMedicoEIntervalo(
            @PathVariable Long medCodigo,
            @PathVariable String DataInicioFormatada,
            @PathVariable String DataFimFormatada) {
        return ResponseEntity.ok(service.buscarConsultasPorMedicoEIntervalo(medCodigo, DataInicioFormatada, DataFimFormatada));
    }

    @GetMapping("/BuscandoTodasConsultasPorEspecialidade/especialidades={especialidades}")
    public ResponseEntity<List<Consulta>> buscarConsultasPorEspecialidade(@PathVariable String especialidades) {
        return ResponseEntity.ok(service.buscarConsultasPorEspecialidade(especialidades));
    }

    @GetMapping("/dia-atual")
    public ResponseEntity<List<DadosConsultaView>> buscarConsultasDoDiaAtual() {
        List<Consulta> consultas = service.buscarConsultasDoDiaAtual();
        List<DadosConsultaView> resultado = consultas.stream().map(DadosConsultaView::new).toList();
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/semana-atual")
    public ResponseEntity<List<DadosConsultaView>> buscarConsultasDaSemanaAtual() {
        List<Consulta> consultas = service.buscarConsultasDaSemanaAtual();
        List<DadosConsultaView> resultado = consultas.stream().map(DadosConsultaView::new).toList();
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/mes-atual")
    public ResponseEntity<List<DadosConsultaView>> buscarConsultasDoMesAtual() {
        List<Consulta> consultas = service.buscarConsultasDoMesAtual();
        List<DadosConsultaView> resultado = consultas.stream().map(DadosConsultaView::new).toList();
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/ano-atual")
    public ResponseEntity<List<DadosConsultaView>> buscarConsultasDoAnoAtual() {
        List<Consulta> consultas = service.buscarConsultasDoAnoAtual();
        List<DadosConsultaView> resultado = consultas.stream().map(DadosConsultaView::new).toList();
        return ResponseEntity.ok(resultado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirConsulta(@PathVariable Long id) {
        service.deletarPorId(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<DadosConsultaView> atualizarConsulta(
            @PathVariable Long id,
            @RequestBody @Valid DadosCadastraConsulta dados) {
        DadosConsultaView consultaAtualizada = service.atualizarConsulta(id, dados);
        return ResponseEntity.ok(consultaAtualizada);
    }

    @PutMapping("/{id}/concluir")
    public ResponseEntity<DadosConsultaView> concluirConsulta(@PathVariable Long id) {
        DadosConsultaView consulta = service.concluirConsulta(id);
        return ResponseEntity.ok(consulta);
    }

    @GetMapping("/horarios-ocupados/medico={medicoId}&data={data}")
    public ResponseEntity<List<String>> buscarHorariosOcupados(
            @PathVariable Long medicoId,
            @PathVariable String data) {
        List<String> horarios = service.buscarHorariosOcupados(medicoId, data);
        return ResponseEntity.ok(horarios);
    }

    @PostMapping("/enviar-mensagem")
    public ResponseEntity<Object> enviarMensagem(@RequestBody @Valid DadosSeendToNewMenssage dados) throws MessagingException {
        String emailDestino = !dados.medEmail().isEmpty() ? dados.medEmail() : dados.paciEmail();
        
        if (emailDestino.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        return service.enviarNotificacaoPorEmail(emailDestino, dados.mensagem())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.badRequest().build());
    }

    @GetMapping("/agenda-medico/{idUsuarioMedico}")
    public ResponseEntity<List<Consulta>> buscarAgendaMedico(@PathVariable Long idUsuarioMedico) {
        List<Consulta> agenda = service.buscarAgendaMedico(idUsuarioMedico);
        return ResponseEntity.ok(agenda);
    }

    @GetMapping("/estatisticas/medico={medicoId}&dataInicial={dataInicial}&dataFinal={dataFinal}")
    public ResponseEntity<List<Object[]>> contarConsultasPorStatusEMedico(
            @PathVariable Long medicoId,
            @PathVariable String dataInicial,
            @PathVariable String dataFinal) {
        List<Object[]> estatisticas = service.contarConsultasPorStatusEMedico(medicoId, dataInicial, dataFinal);
        return ResponseEntity.ok(estatisticas);
    }

    @GetMapping("/agenda-todos-medicos")
    public ResponseEntity<List<DadosConsultaView>> buscarAgendaTodosMedicos() {
        List<Consulta> consultas = service.listarTodasConsultas();
        List<DadosConsultaView> resultado = consultas.stream().map(DadosConsultaView::new).toList();
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/historico-medico/{idUsuarioMedico}")
    public ResponseEntity<List<DadosConsultaView>> buscarHistoricoAgendaMedico(@PathVariable Long idUsuarioMedico) {
        List<Consulta> consultas = service.buscarHistoricoAgendaMedico(idUsuarioMedico);
        List<DadosConsultaView> resultado = consultas.stream().map(DadosConsultaView::new).toList();
        return ResponseEntity.ok(resultado);
    }

    // ==========================================
    // CONSULTAS CONCLUÍDAS (FINALIZADAS)
    // ==========================================

    @GetMapping("/BuscandoTodasConsultasConcluidasEmIntervaloDeDatas/dataInicial={dataInicial}&dataFinal={dataFinal}")
    public ResponseEntity<List<Consulta>> buscarConsultasConcluidasPorIntervalo(
            @PathVariable String dataInicial,
            @PathVariable String dataFinal) {
        return ResponseEntity.ok(service.buscarConsultasConcluidasPorIntervalo(dataInicial, dataFinal));
    }

    @GetMapping("/BuscandoTodasConsultasConcluidasEmIntervaloDeDatasComEspecialidade/dataInicial={dataInicial}&dataFinal={dataFinal}&especialidades={especialidade}")
    public ResponseEntity<List<Consulta>> buscarConsultasConcluidasPorIntervaloEEspecialidade(
            @PathVariable String dataInicial,
            @PathVariable String dataFinal,
            @PathVariable String especialidade) {
        return ResponseEntity.ok(service.buscarConsultasConcluidasPorIntervaloEEspecialidade(dataInicial, dataFinal, especialidade));
    }

    @GetMapping("/BuscandoTodasConsultasConcluidasPorMedico/{medicoId}")
    public ResponseEntity<List<Consulta>> buscarConsultasConcluidasPorMedico(@PathVariable Long medicoId) {
        return ResponseEntity.ok(service.buscarConsultasConcluidasPorMedico(medicoId));
    }

    @GetMapping("/BuscandoTodasConsultasConcluidasPorMedicoEmIntervaloDeDatas/medico={medCodigo}&dataInicial={DataInicioFormatada}&dataFinal={DataFimFormatada}")
    public ResponseEntity<List<Consulta>> buscarConsultasConcluidasPorMedicoEIntervalo(
            @PathVariable Long medCodigo,
            @PathVariable String DataInicioFormatada,
            @PathVariable String DataFimFormatada) {
        return ResponseEntity.ok(service.buscarConsultasConcluidasPorMedicoEIntervalo(medCodigo, DataInicioFormatada, DataFimFormatada));
    }

    @GetMapping("/BuscandoTodasConsultasConcluidasPorEspecialidade/especialidades={especialidades}")
    public ResponseEntity<List<Consulta>> buscarConsultasConcluidasPorEspecialidade(@PathVariable String especialidades) {
        return ResponseEntity.ok(service.buscarConsultasConcluidasPorEspecialidade(especialidades));
    }

    @GetMapping("/BuscandoTodasConsultasConcluidasPorMedicoEEspecialidade/medico={medCodigo}&especialidades={especialidades}")
    public ResponseEntity<List<Consulta>> buscarConsultasConcluidasPorMedicoEEspecialidade(
            @PathVariable Long medCodigo,
            @PathVariable String especialidades) {
        return ResponseEntity.ok(service.buscarConsultasConcluidasPorMedicoEEspecialidade(medCodigo, especialidades));
    }

    @GetMapping("/BuscandoTodasConsultasConcluidasPorMedicoEspecialidadeEmIntervaloDeDatas/medico={medCodigo}&especialidades={especialidades}&dataInicial={dataInicial}&dataFinal={dataFinal}")
    public ResponseEntity<List<Consulta>> buscarConsultasConcluidasPorMedicoEspecialidadeEmIntervalo(
            @PathVariable Long medCodigo,
            @PathVariable String especialidades,
            @PathVariable String dataInicial,
            @PathVariable String dataFinal) {
        return ResponseEntity.ok(service.buscarConsultasConcluidasPorMedicoEspecialidadeEmIntervalo(medCodigo, especialidades, dataInicial, dataFinal));
    }

    // ==========================================
    // ESTATÍSTICAS DO DASHBOARD
    // ==========================================

    @GetMapping("/estatisticas/consultas-hoje")
    public ResponseEntity<Long> contarConsultasHoje() {
        return ResponseEntity.ok(service.contarConsultasHoje());
    }

    @GetMapping("/estatisticas/consultas-hoje/usuario={usuarioId}")
    public ResponseEntity<Long> contarConsultasHojePorUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(service.contarConsultasHojePorUsuario(usuarioId));
    }

    @GetMapping("/estatisticas/consultas-realizadas-hoje")
    public ResponseEntity<Long> contarConsultasRealizadasHoje() {
        return ResponseEntity.ok(service.contarConsultasRealizadasHoje());
    }

    @GetMapping("/estatisticas/consultas-realizadas-hoje/usuario={usuarioId}")
    public ResponseEntity<Long> contarConsultasRealizadasHojePorUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(service.contarConsultasRealizadasHojePorUsuario(usuarioId));
    }

    @GetMapping("/estatisticas/consultas-agendadas-hoje")
    public ResponseEntity<Long> contarConsultasAgendadasHoje() {
        return ResponseEntity.ok(service.contarConsultasAgendadasHoje());
    }

    @GetMapping("/estatisticas/consultas-agendadas-hoje/usuario={usuarioId}")
    public ResponseEntity<Long> contarConsultasAgendadasHojePorUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(service.contarConsultasAgendadasHojePorUsuario(usuarioId));
    }

    @GetMapping("/estatisticas/consultas-semana")
    public ResponseEntity<Long> contarConsultasDaSemanaAtual() {
        return ResponseEntity.ok(service.contarConsultasDaSemanaAtual());
    }

    @GetMapping("/estatisticas/consultas-semana/usuario={usuarioId}")
    public ResponseEntity<Long> contarConsultasDaSemanaAtualPorUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(service.contarConsultasDaSemanaAtualPorUsuario(usuarioId));
    }

    @GetMapping("/estatisticas/medicos-ativos")
    public ResponseEntity<Long> contarMedicosAtivos() {
        return ResponseEntity.ok(service.contarMedicosAtivos());
    }

}
