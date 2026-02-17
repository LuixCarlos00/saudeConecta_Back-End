package br.com.saudeConecta.email;

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

    private final MailService emailServices;

    /**
     * Envia email com credenciais de acesso para administrador.
     */
    public void enviarCredenciaisAdministrador(String email, String nome, String login, String senha) {
        log.info("Enviando credenciais para administrador: {}", email);
        sendPageWithPasswordByGeneric(email, nome, login, senha, "email-credenciais-administrador", "Administrador");
    }

    /**
     * Envia email com credenciais de acesso para secretária.
     */
    public void enviarCredenciaisSecretaria(String email, String nome, String login, String senha) {
        log.info("Enviando credenciais para secretária: {}", email);
        sendPageWithPasswordByGeneric(email, nome, login, senha, "email-credenciais-secretaria", "Secretária");
    }

    /**
     * Envia email com credenciais de acesso para médico/clínico.
     */
    public void enviarCredenciaisClinico(String email, String nome, String login, String senha, String nomeOrganizacao) {
        log.info("Enviando credenciais para médico: {}", email);
        sendPageWithPasswordByClinic(email, nome, login, senha, "email-credenciais-medico", "Médico", nomeOrganizacao);
    }




    /**
     * Método genérico para envio de credenciais.
     */
    private void sendPageWithPasswordByGeneric(String email, String nome, String login, String senha,
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




    /**
     * Método genérico para envio de credenciais com nome da organização.
     */
    private void sendPageWithPasswordByClinic(String email, String nome, String login, String senha,
                                                  String template, String tipoUsuario, String nomeOrganizacao) {
        try {
            Map<String, Object> model = new HashMap<>();
            model.put("nome", nome);
            model.put("login", login);
            model.put("senha", senha);
            model.put("tipoUsuario", tipoUsuario);
            model.put("nomeOrganizacao", nomeOrganizacao);
            
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
