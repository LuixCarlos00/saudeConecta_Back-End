package br.com.saudeConecta.email.EnviarEmail;

import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.email.EnviarService.EmailServices;
import br.com.saudeConecta.infrastructure.persistence.repository.PacienteRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnviarEmail {

    private final EmailServices emailService;
    private final ProfissionalRepository profissionalRepository;
    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;

    public void enviarEmailVerificacaoDuasEtapas(String email, String codigoVerificacao) throws MessagingException {
        log.info("Enviando codigo de verificacao para: {}", email);
        Map<String, Object> model = new HashMap<>();
        model.put("message", codigoVerificacao);
        emailService.enviarEmailComPaginaHTML(email, "Verificacao de duas Etapas", "email-template.html", model);
        log.info("Codigo de verificacao enviado com sucesso");
    }

    public void enviarEmailDestinatarioUsuarioVerificacaoDuasEtapas(Usuario usuario, String codigoVerificacao) throws MessagingException {
        log.info("Enviando codigo de verificacao para usuario ID: {}", usuario.getId());
        Map<String, Object> model = new HashMap<>();
        model.put("message", codigoVerificacao);
        emailService.enviarEmailComPaginaHTML(usuario.getLogin(), "Verificacao de duas Etapas", "email-template.html", model);
        log.info("Codigo de verificacao enviado com sucesso para usuario");
    }

    public void enviarEmailDestinatarioPacienteVerificacaoDuasEtapas(Optional<Paciente> principal, String codigoVerificacao) throws MessagingException {
        if (principal.isEmpty()) {
            log.warn("Paciente não encontrado para envio de código de verificação");
            return;
        }
        Paciente paciente = principal.get();
        log.info("Enviando codigo de verificacao para paciente ID: {}", paciente.getPaciCodigo());
        Map<String, Object> model = new HashMap<>();
        model.put("message", codigoVerificacao);
        emailService.enviarEmailComPaginaHTML(paciente.getPaciEmail(), "Verificacao de duas Etapas", "email-template.html", model);
        log.info("Codigo de verificacao enviado com sucesso para paciente");
    }

    public void enviarEmailDestinatarioProfissionalVerificacaoDuasEtapas(Optional<Profissional> principal, String codigoVerificacao) throws MessagingException {
        if (principal.isEmpty()) {
            log.warn("Profissional não encontrado para envio de código de verificação");
            return;
        }
        Profissional profissional = principal.get();
        log.info("Enviando codigo de verificacao para profissional ID: {}", profissional.getId());
        Map<String, Object> model = new HashMap<>();
        model.put("message", codigoVerificacao);
        emailService.enviarEmailComPaginaHTML(profissional.getEmail(), "Verificacao de duas Etapas", "email-template.html", model);
        log.info("Codigo de verificacao enviado com sucesso para profissional");
    }

    public void enviarLoginDePaciente(Optional<Paciente> userPaciente, String login) throws MessagingException {
        if (userPaciente.isEmpty()) return;
        log.info("Enviando login para paciente: {}", userPaciente.get().getPaciEmail());
        Map<String, Object> model = new HashMap<>();
        model.put("message", login);
        emailService.enviarEmailComPaginaHTML(userPaciente.get().getPaciEmail(), "Login De Usuario", "emaiLogin-template.html", model);
    }

    public void enviarLoginDeProfissional(Optional<Profissional> userProfissional, String login) throws MessagingException {
        if (userProfissional.isEmpty()) return;
        log.info("Enviando login para profissional: {}", userProfissional.get().getEmail());
        Map<String, Object> model = new HashMap<>();
        model.put("message", login);
        emailService.enviarEmailComPaginaHTML(userProfissional.get().getEmail(), "Login De Usuario", "emaiLogin-template.html", model);
    }

    public void enviarLembreteDeAlertaParaPaciente(Optional<Paciente> userPaciente, String mensagem) throws MessagingException {
        if (userPaciente.isEmpty()) return;
        log.info("Enviando lembrete de alerta para paciente: {}", userPaciente.get().getPaciEmail());
        Map<String, Object> model = new HashMap<>();
        model.put("message", mensagem);
        emailService.enviarEmailComPaginaHTML(userPaciente.get().getPaciEmail(), "Lembrete", "Lembrete-template.html", model);
    }

    public void enviarLembreteDeAlertaParaProfissional(Optional<Profissional> userProfissional, String mensagem) throws MessagingException {
        if (userProfissional.isEmpty()) return;
        log.info("Enviando lembrete de alerta para profissional: {}", userProfissional.get().getEmail());
        Map<String, Object> model = new HashMap<>();
        model.put("message", mensagem);
        emailService.enviarEmailComPaginaHTML(userProfissional.get().getEmail(), "Lembrete", "Lembrete-template.html", model);
    }
}
