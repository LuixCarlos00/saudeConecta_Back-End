package br.com.saudeConecta.endpoinst.consultaStatus.Service;

import br.com.saudeConecta.domain.consultastatus.ConsultaStatus;
import br.com.saudeConecta.domain.medico.Medico;
import br.com.saudeConecta.infra.exceptions.ResourceNotFoundException;
import br.com.saudeConecta.infrastructure.persistence.repository.ConsultaStatusRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.MedicoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class ConsultaStatusService {

    @Autowired
    private ConsultaStatusRepository repository;

    @Autowired
    private MedicoRepository medicoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;



    
   





    public Optional<ConsultaStatus> BuscarRegistrosDeConsultaStatusPesquisandoPorTodosOsCampos(Long idMedico, String data, String horario, Long idPaciente, Long idAdm, String dataCriacao) {

        return repository.findByConSttMedico_MedCodigoAndConSttPaciente_PaciCodigoAndConSttHorarioAndConSttDataAndConSttDataCriacaoAndConSttAdm(idMedico, idPaciente, horario, data, dataCriacao, idAdm);
        }
    
    

    public List<ConsultaStatus> BuscarConsultaPorPaginas( ) {
        List<ConsultaStatus> consulta = repository.findAll();
        if (consulta.isEmpty()) {
            return Collections.emptyList();
        }
        return consulta;
    }



    public List<ConsultaStatus> BuscarDadosDeAgendaDeTodosOsMedicos() {
        return repository.findAll();
    }

  


    public List<ConsultaStatus> BuscarHistoricoDeAgendaDoMedico(Long idMedico) {
        Medico medico = medicoRepository.findByUsuario_Id(idMedico).orElseThrow(() -> new ResourceNotFoundException("Medico nao encontrado"));
        return repository.findByConSttMedico_MedCodigo(medico.getMedCodigo());
    }




    public List<ConsultaStatus> BuscandoTodasConsultas_CONCLUIDADAS_EmIntervaloDeDatas(String dataInicial, String dataFinal) {
        return repository.BuscandoTodasConsultas_CONCLUIDADAS_EmIntervaloDeDatas(dataInicial, dataFinal);
    }

    public List<ConsultaStatus> BuscandoTodasConsultas_Concluidas_EmIntervaloDeDatasComEspecialidade(String dataInicial, String dataFinal, String especialidade) {
        return  repository.BuscandoTodasConsultasEmIntervaloDeDatasComEspecialidade(dataInicial, dataFinal, especialidade);
    }


    public List<ConsultaStatus> BuscandoTodasConsultas_Concluidas_PorMedico(Long medicoID) {
        return repository.BuscandoTodasConsultas_Concluidas_PorMedico(medicoID);
    }

    public List<ConsultaStatus> BuscandoTodasConsultas_Concluidas_PorMedicoEmIntervaloDeDatas(Long medicoID, String dataInicio, String dataFim) {
        return  repository.BuscandoTodasConsultas_Concluidas_PorMedicoEmIntervaloDeDatas(dataInicio, dataFim, medicoID);
    }

    public List<ConsultaStatus> BuscandoTodasConsultas_Concluidas_PorEspecialidade(String especialidades) {
    return repository.BuscandoTodasConsultasPorEspecialidade(especialidades);
    }
}
