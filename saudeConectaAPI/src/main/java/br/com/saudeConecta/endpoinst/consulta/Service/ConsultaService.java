package br.com.saudeConecta.endpoinst.consulta.Service;

import br.com.saudeConecta.endpoinst.consulta.DTO.DadosConsultaView;
import br.com.saudeConecta.endpoinst.consulta.Entity.Consulta;
import br.com.saudeConecta.endpoinst.consulta.Repository.ConsultaRepository;
import br.com.saudeConecta.endpoinst.medico.Entity.Medico;
import br.com.saudeConecta.endpoinst.medico.Repository.MedicoRepository;
import br.com.saudeConecta.infra.exceptions.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

@Service
public class ConsultaService {

    @Autowired
    private ConsultaRepository repository;

    @Autowired
    private MedicoRepository medicoRepository;




    public Optional<Consulta> buscarPacientePorId(Long id) {
        return repository.findById(id);
    }



    public Page<DadosConsultaView> BuscarConsultaPorPaginas(Pageable paginacao) {
        return repository.findAll(paginacao).map(DadosConsultaView::new);
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


    public List<Consulta> buscarTodasConsulta() {
        return repository.findAll();

    }


    public void CadastraRegistroConsulta(Consulta consulta) throws ResourceNotFoundException {
         repository.save(consulta);
    }


    public Boolean VericarSeExetemConsultasMarcadas(String data, String horario,Long medico  ) {

     Optional<Medico> Medico =  medicoRepository.findById(medico);

     if (  Medico.isEmpty()) {
         return false;
     }
   Date data2 = Date.valueOf(data);

     return repository.existsByConHorarioAndConDataAndConMedico_MedCodigo(horario, data, medico);

    }

    public Page<DadosConsultaView> BuscarRegistrosDeConsulta(String busca) {

//        String stringBusca = busca.toString();
//        Date    dateBusca  = busca.toString();
//        Long  longBusca = busca.toString();
//
//    if (){
//        return repository.find
//    }


return  null;



    }
}