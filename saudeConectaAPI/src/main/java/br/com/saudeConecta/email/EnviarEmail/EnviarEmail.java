package br.com.saudeConecta.email.EnviarEmail;

import br.com.saudeConecta.domain.administrador.Administrador;
import br.com.saudeConecta.domain.medico.Medico;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.email.EnviarService.EmailServices;
import br.com.saudeConecta.infrastructure.persistence.repository.AdministradorRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.MedicoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.PacienteRepository;
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
        log.info("Enviando codigo de verificacao para administrador ID: {}", principal.get().getAdmCodigo());
        long id = principal.get().getAdmCodigo();
        Optional<Administrador> paciente = AdministradorRepository.findById(id);
        Map<String, Object> model = new HashMap<>();
        model.put("message", codigoVerificacao);
        emailService.enviarEmailComPaginaHTML(paciente.get().getAdmEmail(), "Verificacao de duas Etapas", "email-template.html", model);
        log.info("Codigo de verificacao enviado com sucesso para administrador");
    }

    public void enviarEmailDestinatarioPacienteVerificacaoDuasEtapas(@NotNull Optional<Paciente> principal, String codigoVerificacao) throws MessagingException {
        log.info("Enviando codigo de verificacao para paciente ID: {}", principal.get().getPaciCodigo());
        long id = principal.get().getPaciCodigo();
        Optional<Paciente> paciente = pacienteRepository.findById(id);
        Map<String, Object> model = new HashMap<>();
        model.put("message", codigoVerificacao);
        emailService.enviarEmailComPaginaHTML(paciente.get().getPaciEmail(), "Verificacao de duas Etapas", "email-template.html", model);
        log.info("Codigo de verificacao enviado com sucesso para paciente");
    }

    public void enviarEmailDestinatarioMedicoVerificacaoDuasEtapas(Optional<Medico> principal, String codigoVerificacao) throws MessagingException {
        log.info("Enviando codigo de verificacao para medico ID: {}", principal.get().getMedCodigo());
        long id = principal.get().getMedCodigo();
        Optional<Medico> medico = medicoRepository.findById(id);
        Map<String, Object> model = new HashMap<>();
        model.put("message", codigoVerificacao);
        emailService.enviarEmailComPaginaHTML(medico.get().getMedEmail(), "Verificacao de duas Etapas", "email-template.html", model);
        log.info("Codigo de verificacao enviado com sucesso para medico");
    }




    public void enviarLoginDePaciente(Optional<Paciente> Userpaciente, String login) throws MessagingException {
        log.info("Enviando login para paciente: {}", Userpaciente.get().getPaciEmail());
        Map<String, Object> model = new HashMap<>();
        model.put("message", login);
        emailService.enviarEmailComLoginPaciente(Userpaciente.get().getPaciEmail(), "Login De Usuario", "emaiLogin-template.html", model);
    }

    public void enviarLoginDeMedicoRecuperacaoDeLogin(Optional<Medico> Usermedico, String login) throws MessagingException {
        log.info("Enviando login para medico: {}", Usermedico.get().getMedEmail());
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
        log.info("Enviando lembrete de alerta para medico: {}", Usermedico.get().getMedEmail());
        Map<String, Object> model = new HashMap<>();
        model.put("message", messagem);
        emailService.enviarLembreteDeAlertaParaMedico(Usermedico.get().getMedEmail(), "Lembrete", "Lembrete-template.html", model);
    }
}
