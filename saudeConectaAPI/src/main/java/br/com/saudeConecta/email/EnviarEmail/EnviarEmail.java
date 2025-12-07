package br.com.saudeConecta.email.EnviarEmail;

import br.com.saudeConecta.email.EnviarService.EmailServices;
import br.com.saudeConecta.endpoinst.administrador.Entity.Administrador;
import br.com.saudeConecta.endpoinst.administrador.Repository.AdministradorRepository;
import br.com.saudeConecta.endpoinst.medico.Entity.Medico;
import br.com.saudeConecta.endpoinst.medico.Repository.MedicoRepository;
import br.com.saudeConecta.endpoinst.paciente.Entity.Paciente;
import br.com.saudeConecta.endpoinst.paciente.Repository.PacienteRepository;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class EnviarEmail {

    @Autowired
    private EmailServices emailService;

    @Autowired
    private MedicoRepository medicoRepository;

    @Autowired
    private AdministradorRepository AdministradorRepository;

    @Autowired
    private PacienteRepository pacienteRepository;

    public void enviarEmailDestinatarioAdministradorVerificacaoDuasEtapas(@NotNull Optional<Administrador> principal, String codigoVerificacao) throws MessagingException {
        log.info("Enviando código de verificação para administrador ID: {}", principal.get().getAdmCodigo());
        long id = principal.get().getAdmCodigo();
        Optional<Administrador> paciente = AdministradorRepository.findById(id);
        Map<String, Object> model = new HashMap<>();
        model.put("message", codigoVerificacao);
        emailService.enviarEmailComPaginaHTML(paciente.get().getAdmEmail(), "Verificação de duas Etapas", "email-template.html", model);
        log.info("Código de verificação enviado com sucesso para administrador");
    }

    public void enviarEmailDestinatarioPacienteVerificacaoDuasEtapas(@NotNull Optional<Paciente> principal, String codigoVerificacao) throws MessagingException {
        log.info("Enviando código de verificação para paciente ID: {}", principal.get().getPaciCodigo());
        long id = principal.get().getPaciCodigo();
        Optional<Paciente> paciente = pacienteRepository.findById(id);
        Map<String, Object> model = new HashMap<>();
        model.put("message", codigoVerificacao);
        emailService.enviarEmailComPaginaHTML(paciente.get().getPaciEmail(), "Verificação de duas Etapas", "email-template.html", model);
        log.info("Código de verificação enviado com sucesso para paciente");
    }

    public void enviarEmailDestinatarioMedicoVerificacaoDuasEtapas(Optional<Medico> principal, String codigoVerificacao) throws MessagingException {
        log.info("Enviando código de verificação para médico ID: {}", principal.get().getMedCodigo());
        long id = principal.get().getMedCodigo();
        Optional<Medico> medico = medicoRepository.findById(id);
        Map<String, Object> model = new HashMap<>();
        model.put("message", codigoVerificacao);
        emailService.enviarEmailComPaginaHTML(medico.get().getMedEmail(), "Verificação de duas Etapas", "email-template.html", model);
        log.info("Código de verificação enviado com sucesso para médico");
    }




    public void enviarLoginDePaciente(Optional<Paciente> Userpaciente, String login) throws MessagingException {
        log.info("Enviando login para paciente: {}", Userpaciente.get().getPaciEmail());
        Map<String, Object> model = new HashMap<>();
        model.put("message", login);
        emailService.enviarEmailComLoginPaciente(Userpaciente.get().getPaciEmail(), "Login De Usuario", "emaiLogin-template.html", model);
    }

    public void enviarLoginDeMedicoRecuperacaoDeLogin(Optional<Medico> Usermedico, String login) throws MessagingException {
        log.info("Enviando login para médico: {}", Usermedico.get().getMedEmail());
        Map<String, Object> model = new HashMap<>();
        model.put("message", login);
        emailService.enviarEmailComLoginMedico(Usermedico.get().getMedEmail(), "Login De Usuario", "emaiLogin-template.html", model);
    }

    public void enviarLembreteDeAlertaParaPaciente(Optional<Paciente> Userpaciente, String messagem) throws MessagingException {
        log.info("Enviando lembrete de alerta para paciente: {}", Userpaciente.get().getPaciEmail());
        Map<String, Object> model = new HashMap<>();
        model.put("message", messagem);
        emailService.enviarLembreteDeAlertaParaPaciente(Userpaciente.get().getPaciEmail(), "Lembrete", "Lembrete-template.html", model);
    }

    public void enviarLembreteDeAlertaParaMedico(Optional<Medico> Usermedico, String messagem) throws MessagingException {
        log.info("Enviando lembrete de alerta para médico: {}", Usermedico.get().getMedEmail());
        Map<String, Object> model = new HashMap<>();
        model.put("message", messagem);
        emailService.enviarLembreteDeAlertaParaMedico(Usermedico.get().getMedEmail(), "Lembrete", "Lembrete-template.html", model);
    }
}
