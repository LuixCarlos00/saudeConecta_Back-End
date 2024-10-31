package br.com.saudeConecta.endpoinst.consultaStatus.Resource;

import br.com.saudeConecta.endpoinst.consulta.Entity.Consulta;
import br.com.saudeConecta.endpoinst.consultaStatus.DTO.DadosCadastraConsultaStatus;
import br.com.saudeConecta.endpoinst.consultaStatus.DTO.DadosConsultaStatusView;
import br.com.saudeConecta.endpoinst.consultaStatus.Entity.ConsultaStatus;
import br.com.saudeConecta.endpoinst.consultaStatus.Service.ConsultaStatusService;
import br.com.saudeConecta.endpoinst.medico.Entity.Medico;
import br.com.saudeConecta.endpoinst.medico.Repository.MedicoRepository;
import br.com.saudeConecta.endpoinst.paciente.Entity.Paciente;
import br.com.saudeConecta.endpoinst.paciente.Repository.PacienteRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RequestMapping(value = "/consultaStatus")
@RestController
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class ConsultaStatusResource {

    @Autowired
    private ConsultaStatusService service;

    @Autowired
    private MedicoRepository medicoRepository;

    @Autowired
    private PacienteRepository pacienteRepository;


    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @GetMapping(value = "/Allcampos/medico={ConMedico}&data={ConData}&horario={ConHorario}&paciente={ConPaciente}&Administrador={ConAdm}&DataCriacao={ConDadaCriacao}")
    @Transactional
    public ResponseEntity<DadosConsultaStatusView> BuscarRegistrosDeConsultaStatusPesquisandoPorTodosOsCampos(@NotNull @Valid @PathVariable("ConMedico") Long IdMedico,
                                                                                                              @PathVariable("ConData") String Data,
                                                                                                              @PathVariable("ConHorario") String Horario,
                                                                                                              @PathVariable("ConPaciente") Long IdPaciente,
                                                                                                              @PathVariable("ConAdm") Long IdAdm,
                                                                                                              @PathVariable("ConDadaCriacao") String DataCriacao) {
        Optional<ConsultaStatus> consulta = service.BuscarRegistrosDeConsultaStatusPesquisandoPorTodosOsCampos(IdMedico, Data, Horario, IdPaciente, IdAdm, DataCriacao);

        return ResponseEntity.status(HttpStatus.OK).body(new DadosConsultaStatusView((consulta.get())));
    }


    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @GetMapping(value = "/Consultapagina")
    public ResponseEntity<List<DadosConsultaStatusView>> BuscarConsultaPorPaginas() {

        List<ConsultaStatus> consulta = service.BuscarConsultaPorPaginas();

        List<DadosConsultaStatusView> dadosConsultaStatusViews = consulta.stream().map(DadosConsultaStatusView::new).toList();

        return ResponseEntity.ok().body(dadosConsultaStatusViews);
    }


    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @GetMapping(value = "/BuscarHistoricoDeAgendaDoMedico/{id}")
    @Transactional
    public ResponseEntity<List<DadosConsultaStatusView>> BuscarHistoricoDeAgendaDoMedico(@NotNull @Valid @PathVariable("id") Long IdMedico) {
        List<ConsultaStatus> consulta = service.BuscarHistoricoDeAgendaDoMedico(IdMedico);
        List<DadosConsultaStatusView> dadosConsultaStatusViews = DadosConsultaStatusView.fromList(consulta);
        return ResponseEntity.status(HttpStatus.OK).body(dadosConsultaStatusViews);
    }


    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @GetMapping(value = "/BuscarDadosDeAgendaDeTodosOsMedicos")
    @Transactional
    public ResponseEntity<List<DadosConsultaStatusView>> BuscarDadosDeAgendaDeTodosOsMedicos() {
        List<ConsultaStatus> consulta = service.BuscarDadosDeAgendaDeTodosOsMedicos();
        List<DadosConsultaStatusView> dadosConsultaStatusViews = DadosConsultaStatusView.fromList(consulta);
        return ResponseEntity.status(HttpStatus.OK).body(dadosConsultaStatusViews);
    }










    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @GetMapping(value = "/BuscandoTodasConsultas_CONCLUIDADAS_EmIntervaloDeDatas/dataInicial={dataInicial}&dataFinal={dataFinal}")
    public ResponseEntity<List<DadosConsultaStatusView>> BuscandoTodasConsultas_CONCLUIDADAS_EmIntervaloDeDatas(@NotNull @PathVariable("dataInicial") String dataInicial,
                                                                   @NotNull @PathVariable("dataFinal") String dataFinal) {
       List <ConsultaStatus> consulta = service.BuscandoTodasConsultas_CONCLUIDADAS_EmIntervaloDeDatas(dataInicial, dataFinal);
       if (consulta.isEmpty()) {
           return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
       }
       List <DadosConsultaStatusView> result = DadosConsultaStatusView.fromList(consulta);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }






}
