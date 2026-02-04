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
     * Envia email com credenciais de acesso para o médico.
     * 
     * @param email Email do destinatário
     * @param nome Nome do médico
     * @param login Login gerado (CPF)
     * @param senha Senha gerada
     */
    public void enviarCredenciaisMedico(String email, String nome, String login, String senha) {
        enviarCredenciais(email, nome, login, senha, "email-credenciais-medico", "Médico");
    }

    /**
     * Envia email com credenciais de acesso para o paciente.
     * 
     * @param email Email do destinatário
     * @param nome Nome do paciente
     * @param login Login gerado (CPF)
     * @param senha Senha gerada
     */
    public void enviarCredenciaisPaciente(String email, String nome, String login, String senha) {
        enviarCredenciais(email, nome, login, senha, "email-credenciais-paciente", "Paciente");
    }

    /**
     * Envia email com credenciais de acesso para a secretária.
     * 
     * @param email Email do destinatário
     * @param nome Nome da secretária
     * @param login Login gerado (CPF)
     * @param senha Senha gerada
     */
    public void enviarCredenciaisSecretaria(String email, String nome, String login, String senha) {
        enviarCredenciais(email, nome, login, senha, "email-credenciais-secretaria", "Secretária");
    }

    /**
     * Envia email com credenciais de acesso para o administrador.
     * 
     * @param email Email do destinatário
     * @param nome Nome do administrador
     * @param login Login gerado (CPF)
     * @param senha Senha gerada
     */
    public void enviarCredenciaisAdministrador(String email, String nome, String login, String senha) {
        enviarCredenciais(email, nome, login, senha, "email-credenciais-administrador", "Administrador");
    }

    /**
     * Método genérico para envio de credenciais.
     * 
     * @param email Email do destinatário
     * @param nome Nome do usuário
     * @param login Login gerado
     * @param senha Senha gerada
     * @param template Nome do template de email
     * @param tipoUsuario Tipo do usuário para log
     */
    private void enviarCredenciais(String email, String nome, String login, String senha, 
                                    String template, String tipoUsuario) {
        try {
            Map<String, Object> model = new HashMap<>();
            model.put("nome", nome);
            model.put("login", login);
            model.put("senha", senha);
            
            emailServices.enviarEmailComLoginMedico(
                email, 
                "Bem-vindo ao Saúde Conecta - Suas credenciais de acesso", 
                template,
                model
            );
            log.info("Email com credenciais de {} enviado para: {}", tipoUsuario, email);
        } catch (MessagingException e) {
            log.error("Erro ao enviar email com credenciais de {} para: {} - {}", 
                tipoUsuario, email, e.getMessage());
        }
    }
}
