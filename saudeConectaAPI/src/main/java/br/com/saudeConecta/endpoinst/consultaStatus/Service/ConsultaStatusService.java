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
import org.springframework.stereotype.Service;

import java.sql.Date;
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



    public Optional<ConsultaStatus> buscarPacientePorId(Long id) {
        return repository.findById(id);
    }


    public Page<DadosConsultaStatusView> BuscarConsultaPorPaginas(Pageable paginacao) {
        return repository.findAll(paginacao).map(DadosConsultaStatusView::new);
    }


    @Transactional
    public void deletarPorId(Long id) throws ResourceNotFoundException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID inválido");
        }

        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Registro não encontrado");
        }

        try {
            repository.deleteById(id);
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException("Violação de Integridade");
        }
    }


    public List<ConsultaStatus> buscarTodasConsulta() {
        return repository.findAll();

    }


    public void CadastraRegistroConsulta(ConsultaStatus consulta) throws ResourceNotFoundException {
        repository.save(consulta);
    }


    public Boolean VericarSeExetemConsultasMarcadas(String data, String horario, Long medico) {

        Optional<Medico> Medico = medicoRepository.findById(medico);

        if (Medico.isEmpty()) {
            return false;
        }
        Date data2 = Date.valueOf(data);

        return repository.existsByConSttHorarioAndConSttDataAndConSttMedico_MedCodigo(horario, data, medico);

    }

    public Page<DadosConsultaStatusView> BuscarRegistrosDeConsulta(String busca) {

//        String stringBusca = busca.toString();
//        Date    dateBusca  = busca.toString();
//        Long  longBusca = busca.toString();
//
//    if (){
//        return repository.find
//    }


        return null;


    }

    public DadosConsultaStatusView EditarConsulta(ConsultaStatus consulta, Long id) {

      ConsultaStatus consulta1 = repository.getReferenceById(id);
      consulta1.update(consulta);
      repository.save(consulta1);
      return new DadosConsultaStatusView(consulta1);

    }

    public DadosConsultaStatusView ConcluirConsulta(Long id) {
        Optional<ConsultaStatus> optionalConsulta = repository.findById(id);

        if (optionalConsulta.isPresent()) {
            ConsultaStatus consulta = optionalConsulta.get();
            consulta.setConSttStatus((byte) 1); // Usando (byte) para garantir o tipo correto
            repository.save(consulta);
            return new DadosConsultaStatusView(consulta);
        } else {
            throw new EntityNotFoundException("Consulta não encontrada com o id: " + id);
        }
    }

    public Optional<ConsultaStatus> BuscarRegistrosDeConsultaStatusPesquisandoPorTodosOsCampos(Long idMedico, String data, String horario, Long idPaciente, Long idAdm, String dataCriacao) {

    return repository.findByConSttMedico_MedCodigoAndConSttPaciente_PaciCodigoAndConSttHorarioAndConSttDataAndConSttDataCriacaoAndConSttAdm(idMedico, idPaciente, horario, data, dataCriacao, idAdm);
    }

    public List<ConsultaStatus> BuscarHistoricoDeAgendaDoMedico(Long idMedico) {
        Medico medico = medicoRepository.findByUsuario_Id(idMedico).orElseThrow(() -> new ResourceNotFoundException("Médico não encontrado"));
        return repository.findByConSttMedico_MedCodigo(medico.getMedCodigo());
    }

}