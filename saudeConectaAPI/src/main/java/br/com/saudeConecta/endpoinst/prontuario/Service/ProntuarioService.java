package br.com.saudeConecta.endpoinst.prontuario.Service;

import br.com.saudeConecta.email.EnviarEmail.EnviarEmail;
import br.com.saudeConecta.endpoinst.codigoVerificacao.Repository.CodigoVerificacaoRepository;
import br.com.saudeConecta.endpoinst.consulta.Entity.Consulta;
import br.com.saudeConecta.endpoinst.consultaStatus.Entity.ConsultaStatus;
import br.com.saudeConecta.endpoinst.consultaStatus.Repository.ConsultaStatusRepository;
import br.com.saudeConecta.endpoinst.prontuario.DTO.DadosProntuarioView;
import br.com.saudeConecta.endpoinst.prontuario.DTO.HistoricoPaciente;
import br.com.saudeConecta.endpoinst.prontuario.Entity.Prontuario;
import br.com.saudeConecta.endpoinst.prontuario.Repository.ProntuarioRepository;
import br.com.saudeConecta.infra.exceptions.ResourceNotFoundException;
import br.com.saudeConecta.util.RecuperaSenha;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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