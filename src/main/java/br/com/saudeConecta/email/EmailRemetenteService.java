package br.com.saudeConecta.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Serviço responsável exclusivamente pelo envio de emails via SMTP.
 * Não conhece templates, retry ou mensageria — apenas envia o conteúdo recebido.
 */
@Service
@Slf4j
public class EmailRemetenteService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String remetenteEmail;

    public EmailRemetenteService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Envia um email com conteúdo HTML já renderizado.
     *
     * @param destinatario endereço de email do destinatário
     * @param assunto      assunto do email
     * @param corpoHtml    corpo do email em HTML (já renderizado)
     * @throws MessagingException se ocorrer erro no envio SMTP
     */
    public void enviarHtml(String destinatario, String assunto, String corpoHtml) throws MessagingException {
        log.info("Enviando email HTML para: {}", destinatario);
        MimeMessage mensagem = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mensagem, "UTF-8");
        helper.setFrom(remetenteEmail);
        helper.setTo(destinatario);
        helper.setSubject(assunto);
        helper.setText(corpoHtml, true);
        mailSender.send(mensagem);
        log.info("Email HTML enviado com sucesso para: {}", destinatario);
    }
}
