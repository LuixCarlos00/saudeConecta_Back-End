package br.com.saudeConecta.email.EnviarService;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Serviço dedicado para envio de emails com credenciais de acesso.
 * Responsável por enviar login e senha para novos usuários cadastrados.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CredenciaisEmailService {

    private final EmailServices emailServices;

    /**
     * Envia email com credenciais de acesso para profissional.
     */
    public void enviarCredenciaisProfissional(String email, String nome, String login, String senha) {
        enviarCredenciais(email, nome, login, senha, "email-credenciais-template", "Profissional");
    }

    /**
     * Envia email com credenciais de acesso para o paciente.
     */
    public void enviarCredenciaisPaciente(String email, String nome, String login, String senha) {
        enviarCredenciais(email, nome, login, senha, "email-credenciais-template", "Paciente");
    }

    /**
     * Envia email com credenciais de acesso para usuário genérico.
     */
    public void enviarCredenciaisUsuario(String email, String nome, String login, String senha, String tipoUsuario) {
        enviarCredenciais(email, nome, login, senha, "email-credenciais-template", tipoUsuario);
    }

    /**
     * Método genérico para envio de credenciais.
     */
    private void enviarCredenciais(String email, String nome, String login, String senha, 
                                    String template, String tipoUsuario) {
        try {
            Map<String, Object> model = new HashMap<>();
            model.put("nome", nome);
            model.put("login", login);
            model.put("senha", senha);
            model.put("tipoUsuario", tipoUsuario);
            
            emailServices.enviarEmailComPaginaHTML(
                email, 
                "Bem-vindo ao Saúde Conecta - Suas credenciais de acesso", 
                template + ".html",
                model
            );
            log.info("Email com credenciais de {} enviado para: {}", tipoUsuario, email);
        } catch (MessagingException e) {
            log.error("Erro ao enviar email com credenciais de {} para: {} - {}", 
                tipoUsuario, email, e.getMessage());
        }
    }
}
