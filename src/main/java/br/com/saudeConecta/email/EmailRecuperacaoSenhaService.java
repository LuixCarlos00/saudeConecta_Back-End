package br.com.saudeConecta.email;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Serviço dedicado para envio de emails de recuperação de senha.
 * Responsável por enviar novas credenciais para usuários que solicitaram recuperação.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailRecuperacaoSenhaService {

    private final MailService emailServices;

    /**
     * Envia email com nova senha para o usuário.
     * 
     * @param email Email do destinatário
     * @param nome Nome do usuário
     * @param login Login do usuário
     * @param novaSenha Nova senha gerada
     */
    public void enviarEmailRecuperacao(String email, String nome, String login, String novaSenha) {
        try {
            Map<String, Object> model = new HashMap<>();
            model.put("nome", nome);
            model.put("login", login);
            model.put("senha", novaSenha);

            emailServices.enviarEmailComPaginaHTML(
                email,
                "Saúde Conecta - Recuperação de Senha",
                "email-recuperacao-senha",
                model
            );
            log.info("Email de recuperação de senha enviado para: {}", email);
        } catch (MessagingException e) {
            log.error("Erro ao enviar email de recuperação para: {} - {}", email, e.getMessage());
        }
    }
}
