package br.com.saudeConecta.endpoinst.consultaStatus.Service;

import br.com.saudeConecta.endpoinst.consultaStatus.DTO.DadosConsultaStatusView;
import br.com.saudeConecta.endpoinst.consultaStatus.Entity.ConsultaStatus;
import br.com.saudeConecta.endpoinst.consultaStatus.Repository.ConsultaStatusRepository;
import br.com.saudeConecta.endpoinst.medico.Entity.Medico;
import br.com.saudeConecta.endpoinst.medico.Repository.MedicoRepository;
import br.com.saudeConecta.endpoinst.usuario.Entity.Usuario;
import br.com.saudeConecta.endpoinst.usuario.Repository.UsuarioRepository;
import br.com.saudeConecta.infra.exceptions.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.util.Collection;
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
        Medico medico = medicoRepository.findByUsuario_Id(idMedico).orElseThrow(() -> new ResourceNotFoundException("Médico não encontrado"));
        return repository.findByConSttMedico_MedCodigo(medico.getMedCodigo());
    }




    public List<ConsultaStatus> BuscandoTodasConsultas_CONCLUIDADAS_EmIntervaloDeDatas(String dataInicial, String dataFinal) {
        return repository.BuscandoTodasConsultas_CONCLUIDADAS_EmIntervaloDeDatas(dataInicial, dataFinal);
    }
}