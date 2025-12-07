package br.com.saudeConecta.endpoinst.prontuario.Service;

import br.com.saudeConecta.domain.consultastatus.ConsultaStatus;
import br.com.saudeConecta.domain.prontuario.Prontuario;
import br.com.saudeConecta.endpoinst.prontuario.DTO.HistoricoPaciente;
import br.com.saudeConecta.infrastructure.persistence.repository.ConsultaStatusRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProntuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ProntuarioService {

private final ProntuarioRepository prontuarioRepository;

private final ConsultaStatusRepository consultaStatusRepository;

    @Autowired
    public ProntuarioService(ProntuarioRepository prontuarioRepository, ConsultaStatusRepository consultaStatusRepository) {
        this.consultaStatusRepository = consultaStatusRepository;
        this.prontuarioRepository = prontuarioRepository;

    }



    public void CadastraProntuario(Prontuario prontuario) {
        prontuarioRepository.save(prontuario);
    }




    public Optional<Prontuario> BuscaProntuario(Long id) {

        return Optional.ofNullable(prontuarioRepository.findByProntCodigoConsulta_ConSttCodigoConsulata(id));
    }





    public HistoricoPaciente BuscandoHistoricoDoPaciente(Long id){

        ArrayList<Prontuario> TodosProntuarios = new ArrayList<>(prontuarioRepository.findAll());
        List<Long> CodigoConsulta = new ArrayList<>();

        for (int i = 0; i < TodosProntuarios.size(); i++) {
                CodigoConsulta.add(TodosProntuarios.get(i).getProntCodigoConsulta().getConSttCodigoConsulata());
        }



        List<ConsultaStatus> RegistrosPacientes = new ArrayList<>();

        List<ConsultaStatus> registros =  consultaStatusRepository.findByConSttPaciente_PaciCodigo(id);

        if (registros.size() > 0) {
            for (int i = 0; i < registros.size(); i++) {
                RegistrosPacientes.add(registros.get(i));
            }
        }



        System.out.println(RegistrosPacientes);

        List<Prontuario> prontuario2 = new ArrayList<>();

        for (int i = 0; i < RegistrosPacientes.size(); i++) {
            ConsultaStatus status = RegistrosPacientes.get(i) ;
            for (int j = 0; j < TodosProntuarios.size(); j++) {
                Prontuario prontuario = TodosProntuarios.get(j);

                Long codiggConsultaStatus = status.getConSttCodigoConsulata();
                Long codiggConsultaProntuario = prontuario.getProntCodigoConsulta().getConSttCodigoConsulata();

                if (codiggConsultaStatus.equals(codiggConsultaProntuario)) {
                    prontuario2.add(prontuario);
                }
            }
        }


        return new HistoricoPaciente(prontuario2, RegistrosPacientes);

    }




}