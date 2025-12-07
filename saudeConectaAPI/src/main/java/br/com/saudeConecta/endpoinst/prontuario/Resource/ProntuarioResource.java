package br.com.saudeConecta.endpoinst.prontuario.Resource;

import br.com.saudeConecta.endpoinst.consultaStatus.Entity.ConsultaStatus;
import br.com.saudeConecta.endpoinst.consultaStatus.Repository.ConsultaStatusRepository;
import br.com.saudeConecta.endpoinst.medico.Entity.Medico;
import br.com.saudeConecta.endpoinst.medico.Repository.MedicoRepository;
import br.com.saudeConecta.endpoinst.prontuario.DTO.DadosCadastraProntuario;
import br.com.saudeConecta.endpoinst.prontuario.DTO.DadosProntuarioView;
import br.com.saudeConecta.endpoinst.prontuario.DTO.HistoricoPaciente;
import br.com.saudeConecta.endpoinst.prontuario.Entity.Prontuario;
import br.com.saudeConecta.endpoinst.prontuario.Service.ProntuarioService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

@RequestMapping(value = "/prontuario")
@RestController
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class ProntuarioResource {


    private final ProntuarioService service;
    private final MedicoRepository medicoRepository;
    private final ConsultaStatusRepository consultaStatusRepository;

    @Autowired
    public ProntuarioResource(ProntuarioService service, MedicoRepository medicoRepository, ConsultaStatusRepository consultaStatusRepository) {
        this.service = service;
        this.medicoRepository = medicoRepository;
        this.consultaStatusRepository = consultaStatusRepository;
    }


    @PostMapping(value = "/Post")
    @Transactional
    public ResponseEntity<DadosProntuarioView> CadastraProntuario(@RequestBody @Valid @NotNull DadosCadastraProntuario dados,
                                                                  UriComponentsBuilder uriBuilder,
                                                                  BindingResult result) {

        if (result.hasErrors()) {
            return ResponseEntity.badRequest().build();
        }

        Long Medico = dados.prontCodigoMedico();
        Long Consulta = dados.prontCodigoConsulta();


        Optional<Medico> medicoOptional = medicoRepository.findById(Medico);
        Optional<ConsultaStatus> consultaOptional = consultaStatusRepository.findById(Consulta);

        if (medicoOptional.isEmpty() || consultaOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Prontuario prontuario = new Prontuario(dados, medicoOptional.get(), consultaOptional.get());



        service.CadastraProntuario(prontuario);


        URI uri = uriBuilder.path("/prontuario/{id}").buildAndExpand(prontuario.getProntCodigoProntuario()).toUri();
        return ResponseEntity.created(uri).body(new DadosProntuarioView(prontuario));
    }




    @GetMapping(value = "/BuscarPorProntuarioPassadoIdDeConsultaStatus/{id}")
    @Transactional
    public ResponseEntity<DadosProntuarioView> BuscarPorProntuarioPassadoIdDeConsultaStatus(@PathVariable("id") Long id) {
        Optional<Prontuario> prontuario = service.BuscaProntuario(id);
        if (prontuario.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new DadosProntuarioView(prontuario.get()));
    }




    @GetMapping(value = "/BuscandoHistoricoDoPaciente/{id}")
    @Transactional
    public ResponseEntity<HistoricoPaciente> BuscandoHistoricoDoPaciente(@PathVariable("id") Long id) {
        HistoricoPaciente historicoPaciente = service.BuscandoHistoricoDoPaciente(id);
        return ResponseEntity.ok(historicoPaciente);
    }


}
