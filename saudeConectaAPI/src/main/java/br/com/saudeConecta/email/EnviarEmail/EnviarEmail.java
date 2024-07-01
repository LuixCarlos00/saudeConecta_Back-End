package br.com.saudeConecta.email.EnviarEmail;

import br.com.saudeConecta.email.EnviarService.EmailServices;
import br.com.saudeConecta.endpoinst.administrador.Entity.Administrador;
import br.com.saudeConecta.endpoinst.administrador.Repository.AdministradorRepository;
import br.com.saudeConecta.endpoinst.medico.Entity.Medico;
import br.com.saudeConecta.endpoinst.medico.Repository.MedicoRepository;
import br.com.saudeConecta.endpoinst.paciente.Entity.Paciente;
import br.com.saudeConecta.endpoinst.paciente.Repository.PacienteRepository;
import jakarta.mail.MessagingException;
import jdk.jfr.Name;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
;

@Component
@Name("Essa classe e responsavel por pegar o email do usuário e fazer o envio ")
public class EnviarEmail {

    @Name("O Objet dessa classe e fazer o envio do email. O que muda e a Entidade a qual esta relacionada ")


    @Autowired
    private EmailServices emailService;

    @Autowired
    private MedicoRepository medicoRepository;

    @Autowired
    private AdministradorRepository  AdministradorRepository;

    @Autowired
    private PacienteRepository  pacienteRepository; ;



    public void enviarEmailDestinatarioAdministradorVerificacaoDuasEtapas(@NotNull Optional<Administrador> principal, String codigoVerificacao) throws MessagingException {
            long id = principal.get().getAdmCodigo();
            Optional<Administrador> paciente = AdministradorRepository.findById(id);
            Map<String, Object> model = new HashMap<>();
            model.put("message", codigoVerificacao);// "message" e uma variavel que dinamica que vai ser exibida dentro
        // da pagina html
            emailService.enviarEmailComPaginaHTML(paciente.get().getAdmEmail(), "Verificação de duas Etapas", "email-template.html", model );
    }

    public void enviarEmailDestinatarioPacienteVerificacaoDuasEtapas(@NotNull Optional<Paciente> principal, String codigoVerificacao) throws MessagingException {
        long id = principal.get().getPaciCodigo();
        Optional<Paciente> paciente = pacienteRepository.findById(id);
        Map<String, Object> model = new HashMap<>();
        model.put("message", codigoVerificacao);// "message" e uma variavel que dinamica que vai ser exibida dentro
        // da pagina html
        emailService.enviarEmailComPaginaHTML(paciente.get().getPaciEmail(), "Verificação de duas Etapas", "email-template.html", model );
    }


    public void enviarEmailDestinatarioMedicoVerificacaoDuasEtapas(Optional<Medico> principal, String codigoVerificacao) throws MessagingException {
            long id = principal.get().getMedCodigo();
            Optional<Medico> medico = medicoRepository.findById(id);
            Map<String, Object> model = new HashMap<>();
            model.put("message", codigoVerificacao);// "message" e uma variavel que dinamica que vai ser exibida dentro
        // da pagina html
            emailService.enviarEmailComPaginaHTML(medico.get().getMedEmail(), "Verificação de duas Etapas", "email-template.html", model);
    }







    public void enviarLoginDePaciente(Optional<Paciente> Userpaciente, String login) throws MessagingException {
        long id =  Userpaciente.get().getPaciCodigo();
        Map<String, Object> model = new HashMap<>();
        model.put("message", login);// "message" e uma variavel que dinamica que vai ser exibida dentro
        // da pagina html
        emailService.enviarEmailComLoginPaciente(Userpaciente.get().getPaciEmail(), "Login De Usuario ", "emaiLogin-template.html", model );
    }

    public void enviarLoginDeMedicoRecuperacaoDeLogin(Optional<Medico> Usermedico, String login) throws MessagingException {
        long id =  Usermedico.get().getMedCodigo();
        Map<String, Object> model = new HashMap<>();
        model.put("message", login);// "message" e uma variavel que dinamica que vai ser exibida dentro
        // da pagina html
        emailService.enviarEmailComLoginMedico(Usermedico.get().getMedEmail(), "Login De Usuario ", "emaiLogin-template.html", model );
    }





    public void enviarLembreteDeAlertaParaPaciente(Optional<Paciente> Userpaciente, String messagem) throws MessagingException {
        long id =  Userpaciente.get().getPaciCodigo();
        Map<String, Object> model = new HashMap<>();
        model.put("message", messagem);// "message" e uma variavel que dinamica que vai ser exibida dentro
        // da pagina html
        emailService.enviarLembreteDeAlertaParaPaciente(Userpaciente.get().getPaciEmail(), "  Lembrete ", "Lembrete-template.html", model );
    }

    public void enviarLembreteDeAlertaParaMedico(Optional<Medico> Usermedico, String messagem) throws MessagingException {
        long id =  Usermedico.get().getMedCodigo();
        Map<String, Object> model = new HashMap<>();
        model.put("message", messagem);// "message" e uma variavel que dinamica que vai ser exibida dentro
        // da pagina html
        emailService.enviarLembreteDeAlertaParaMedico(Usermedico.get().getMedEmail(), "  Lembrete ", "Lembrete-template.html", model );
    }



}
