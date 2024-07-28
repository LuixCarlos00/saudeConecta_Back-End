package br.com.saudeConecta.endpoinst.prontuario.Service;

import br.com.saudeConecta.email.EnviarEmail.EnviarEmail;
import br.com.saudeConecta.endpoinst.codigoVerificacao.Repository.CodigoVerificacaoRepository;
import br.com.saudeConecta.endpoinst.prontuario.DTO.DadosProntuarioView;
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

import java.util.List;
import java.util.Optional;

@Service
public class ProntuarioService {

private final ProntuarioRepository prontuarioRepository;


    @Autowired
    public ProntuarioService(ProntuarioRepository prontuarioRepository) {
        this.prontuarioRepository = prontuarioRepository;
    }



    public void CadastraProntuario(Prontuario prontuario) {
        prontuarioRepository.save(prontuario);
    }
}